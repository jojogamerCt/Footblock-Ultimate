package net.footblock.footblockultimate.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.footblock.footblockultimate.FootblockUltimate;
import net.footblock.footblockultimate.client.model.FootballEntityModel;
import net.footblock.footblockultimate.client.renderer.FootballRenderer;
import net.footblock.footblockultimate.entity.FootballEntity;
import net.footblock.footblockultimate.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import io.netty.buffer.Unpooled;

import java.util.List;

public final class FootblockUltimateClient {
    public static float shootCharge = 0.0f;

    public static void init() {
        EntityRendererRegistry.register(ModEntities.FOOTBALL, FootballRenderer::new);
        EntityModelLayerRegistry.register(FootballEntityModel.LAYER_LOCATION, FootballEntityModel::createBodyLayer);

        // Register client tick event to track charge input
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            if (minecraft.player == null) {
                shootCharge = 0.0f;
                return;
            }

            FootballEntity ball = getDribbledBall(minecraft.player);
            if (ball != null) {
                if (minecraft.options.keyAttack.isDown()) {
                    // Charges 5% per tick -> 20 ticks (1s) to reach 100%
                    shootCharge = Math.min(1.0f, shootCharge + 0.05f);
                } else {
                    if (shootCharge > 0.0f) {
                        sendShootPacket(shootCharge);
                        shootCharge = 0.0f;
                    }
                }
            } else {
                shootCharge = 0.0f;
            }
        });

        // Register HUD rendering event to display power bar
        ClientGuiEvent.RENDER_HUD.register((graphics, deltaTracker) -> {
            if (shootCharge > 0.0f) {
                renderPowerBar(graphics);
            }
        });
    }

    private static FootballEntity getDribbledBall(Player player) {
        if (player == null || player.level() == null) {
            return null;
        }
        AABB searchArea = player.getBoundingBox().inflate(3.0);
        List<FootballEntity> balls = player.level().getEntitiesOfClass(
                FootballEntity.class,
                searchArea,
                ball -> player.getUUID().equals(ball.getAttachedPlayerUUID())
        );
        return balls.isEmpty() ? null : balls.get(0);
    }

    private static void sendShootPacket(float power) {
        FootballEntity ball = getDribbledBall(Minecraft.getInstance().player);
        if (ball != null) {
            // Client side prediction: detach and kick locally immediately
            ball.setAttachedPlayer(null);

            Player player = Minecraft.getInstance().player;
            if (player != null && Minecraft.getInstance().level != null) {
                Vec3 lookVec = player.getLookAngle();
                double minForce = 0.15;
                double maxHorizontalForce = player.isSprinting() ? 1.5 : 0.9;
                double maxVerticalLift = player.isSprinting() ? 0.5 : 0.25;

                double horizontalForce = minForce + (maxHorizontalForce - minForce) * power;
                double verticalLift = minForce * 0.5 + (maxVerticalLift - minForce * 0.5) * power;

                if (player.isCrouching()) {
                    horizontalForce = 0.3 * power;
                    verticalLift = 0.02 * power;
                }

                double vx = lookVec.x * horizontalForce;
                double vy = Math.max(0.1, lookVec.y * horizontalForce + verticalLift);
                double vz = lookVec.z * horizontalForce;

                ball.setDeltaMovement(vx, vy, vz);

                // Send packet to server using RegistryFriendlyByteBuf
                RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                        Unpooled.buffer(),
                        Minecraft.getInstance().level.registryAccess()
                );
                buf.writeFloat(power);
                NetworkManager.sendToServer(FootblockUltimate.KICK_PACKET_ID, buf);
            }
        }
    }

    private static void renderPowerBar(GuiGraphics graphics) {
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;

        // Position power bar 15 pixels below the crosshair
        int x1 = centerX - 20;
        int x2 = centerX + 20;
        int y1 = centerY + 15;
        int y2 = centerY + 19;

        // 1. Draw black border (1px outline)
        graphics.fill(x1 - 1, y1 - 1, x2 + 1, y1, 0xFF000000); // top border
        graphics.fill(x1 - 1, y2, x2 + 1, y2 + 1, 0xFF000000); // bottom border
        graphics.fill(x1 - 1, y1, x1, y2, 0xFF000000); // left border
        graphics.fill(x2, y1, x2 + 1, y2, 0xFF000000); // right border

        // 2. Draw dark translucent background
        graphics.fill(x1, y1, x2, y2, 0x80000000);

        // 3. Draw filled power progress (vibrant golden yellow color)
        int fillWidth = (int) (40.0f * shootCharge);
        if (fillWidth > 0) {
            graphics.fill(x1, y1, x1 + fillWidth, y2, 0xFFFFCC00);
        }
    }
}
