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
import java.util.UUID;

public final class FootblockUltimateClient {
    public static float shootCharge = 0.0f;
    public static float passCharge = 0.0f;

    public static void init() {
        EntityRendererRegistry.register(ModEntities.FOOTBALL, FootballRenderer::new);
        EntityModelLayerRegistry.register(FootballEntityModel.LAYER_LOCATION, FootballEntityModel::createBodyLayer);

        // Register client tick event to track charge input
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            if (minecraft.player == null) {
                shootCharge = 0.0f;
                passCharge = 0.0f;
                return;
            }

            FootballEntity ball = getDribbledBall(minecraft.player);
            if (ball != null) {
                if (minecraft.options.keyAttack.isDown()) {
                    if (passCharge == 0.0f) {
                        // Charges 5% per tick -> 20 ticks (1s) to reach 100%
                        shootCharge = Math.min(1.0f, shootCharge + 0.05f);
                    }
                } else {
                    if (shootCharge > 0.0f) {
                        sendShootPacket(shootCharge);
                        shootCharge = 0.0f;
                    }
                }

                if (minecraft.options.keyUse.isDown()) {
                    if (shootCharge == 0.0f) {
                        passCharge = Math.min(1.0f, passCharge + 0.05f);
                        // Consume the use key clicks to prevent block placement / item use while charging
                        while (minecraft.options.keyUse.consumeClick()) {}
                    }
                } else {
                    if (passCharge > 0.0f) {
                        sendPassPacket(passCharge);
                        passCharge = 0.0f;
                    }
                }
            } else {
                shootCharge = 0.0f;
                passCharge = 0.0f;
            }
        });

        // Register HUD rendering event to display power bar
        ClientGuiEvent.RENDER_HUD.register((graphics, deltaTracker) -> {
            if (shootCharge > 0.0f || passCharge > 0.0f) {
                renderPowerBar(graphics);
            }
        });

        // Register S2C receiver for other players' kick animations
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, FootblockUltimate.KICK_ANIM_S2C_PACKET_ID, (buf, context) -> {
            UUID playerUuid = buf.readUUID();
            boolean isRightLeg = buf.readBoolean();
            float power = buf.readFloat();
            context.queue(() -> {
                PlayerAnimationTracker.startKick(playerUuid, isRightLeg, power);
            });
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, FootblockUltimate.SCORE_MANAGER_STATE_PACKET_ID, (buf, context) -> {
            var pos = buf.readBlockPos();
            int redScore = buf.readInt();
            int blueScore = buf.readInt();
            boolean active = buf.readBoolean();
            boolean openScreen = buf.readBoolean();
            context.queue(() -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (openScreen) {
                    minecraft.setScreen(new ScoreManagerScreen(pos, redScore, blueScore, active));
                } else if (minecraft.screen instanceof ScoreManagerScreen screen && screen.isFor(pos)) {
                    screen.updateState(redScore, blueScore, active);
                }
            });
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
                // Determine leg locally for client prediction
                boolean isRightLeg;
                float bodyYawRad = player.yBodyRot * ((float) Math.PI / 180.0f);
                double bodyX = -Math.sin(bodyYawRad);
                double bodyZ = Math.cos(bodyYawRad);
                Vec3 look = player.getLookAngle();
                double cross = bodyX * look.z - bodyZ * look.x;

                if (cross < -0.35) {
                    isRightLeg = false; // Looking to the right -> use Left Leg
                } else if (cross > 0.35) {
                    isRightLeg = true;  // Looking to the left -> use Right Leg
                } else {
                    float swing = player.walkAnimation.position();
                    isRightLeg = Math.sin(swing) <= 0.0;
                }

                PlayerAnimationTracker.startKick(player.getUUID(), isRightLeg, power);

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
                ball.applyCurveFromPlayer(player, power, 1.0f, false);

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

    private static void sendPassPacket(float power) {
        FootballEntity ball = getDribbledBall(Minecraft.getInstance().player);
        if (ball != null) {
            // Client side prediction: detach and pass locally immediately
            ball.setAttachedPlayer(null);

            Player player = Minecraft.getInstance().player;
            if (player != null && Minecraft.getInstance().level != null) {
                // Determine leg locally for client prediction
                boolean isRightLeg;
                float bodyYawRad = player.yBodyRot * ((float) Math.PI / 180.0f);
                double bodyX = -Math.sin(bodyYawRad);
                double bodyZ = Math.cos(bodyYawRad);
                Vec3 look = player.getLookAngle();
                double cross = bodyX * look.z - bodyZ * look.x;

                if (cross < -0.35) {
                    isRightLeg = false; // Looking to the right -> use Left Leg
                } else if (cross > 0.35) {
                    isRightLeg = true;  // Looking to the left -> use Right Leg
                } else {
                    float swing = player.walkAnimation.position();
                    isRightLeg = Math.sin(swing) <= 0.0;
                }

                PlayerAnimationTracker.startKick(player.getUUID(), isRightLeg, power);

                // Find target player on client too for prediction!
                Player target = null;
                double bestScore = -1.0;
                Vec3 kickerPos = player.position();
                Vec3 lookVec = player.getLookAngle().normalize();

                for (Player t : Minecraft.getInstance().level.players()) {
                    if (t == player || t.isSpectator() || !t.isAlive()) {
                        continue;
                    }
                    if (player.getTeam() != null && t.getTeam() != player.getTeam()) {
                        continue;
                    }
                    Vec3 toTarget = t.position().subtract(kickerPos);
                    double distance = toTarget.length();
                    if (distance > 30.0 || distance < 1.0) {
                        continue;
                    }
                    Vec3 toTargetDir = toTarget.normalize();
                    double dot = lookVec.dot(toTargetDir);
                    if (dot > 0.707) {
                        double score = dot - (distance * 0.01);
                        if (score > bestScore) {
                            bestScore = score;
                            target = t;
                        }
                    }
                }

                double vx, vy, vz;
                if (target != null) {
                    Vec3 toTarget = target.position().subtract(ball.position());
                    double dist = toTarget.length();
                    Vec3 flatDir = new Vec3(toTarget.x, 0, toTarget.z).normalize();

                    double horizontalForce = (dist * 0.04 + 0.25) * (0.3f + 0.7f * power);
                    double verticalLift = Math.min(0.2, dist * 0.01) * (0.2f + 0.8f * power);

                    vx = flatDir.x * horizontalForce;
                    vy = Math.max(0.05, verticalLift);
                    vz = flatDir.z * horizontalForce;
                } else {
                    double horizontalForce = (player.isSprinting() ? 1.0 : 0.6) * power;
                    double verticalLift = 0.05 * power;

                    vx = lookVec.x * horizontalForce;
                    vy = Math.max(0.02, lookVec.y * horizontalForce + verticalLift);
                    vz = lookVec.z * horizontalForce;
                }

                ball.setDeltaMovement(vx, vy, vz);
                ball.applyCurveFromPlayer(player, power, 0.35f, true);

                // Send packet to server using RegistryFriendlyByteBuf
                RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                        Unpooled.buffer(),
                        Minecraft.getInstance().level.registryAccess()
                );
                buf.writeFloat(power);
                NetworkManager.sendToServer(FootblockUltimate.PASS_PACKET_ID, buf);
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

        // 3. Draw filled power progress (vibrant golden yellow for shoot, vibrant athletic blue for pass)
        float charge = shootCharge > 0.0f ? shootCharge : passCharge;
        int color = shootCharge > 0.0f ? 0xFFFFCC00 : 0xFF00CCFF;

        int fillWidth = (int) (40.0f * charge);
        if (fillWidth > 0) {
            graphics.fill(x1, y1, x1 + fillWidth, y2, color);
        }
    }
}
