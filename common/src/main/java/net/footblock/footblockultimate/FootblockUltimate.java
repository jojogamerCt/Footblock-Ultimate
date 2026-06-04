package net.footblock.footblockultimate;

import dev.architectury.networking.NetworkManager;
import net.footblock.footblockultimate.entity.FootballEntity;
import net.footblock.footblockultimate.registry.ModEntities;
import net.footblock.footblockultimate.registry.ModItems;
import net.footblock.footblockultimate.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class FootblockUltimate {
    public static final String MOD_ID = "footblockultimate";
    public static final ResourceLocation KICK_PACKET_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "kick_packet");
    public static final ResourceLocation PASS_PACKET_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "pass_packet");
    public static final ResourceLocation KICK_ANIM_S2C_PACKET_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "kick_anim_s2c");

    public static void init() {
        ModItems.init();
        ModEntities.init();
        ModSounds.init();

        // Register server-side receiver for client kicks
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, KICK_PACKET_ID, (buf, context) -> {
            float power = buf.readFloat();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    AABB searchArea = player.getBoundingBox().inflate(3.0);
                    List<FootballEntity> balls = player.level().getEntitiesOfClass(
                            FootballEntity.class,
                            searchArea,
                            ball -> player.getUUID().equals(ball.getAttachedPlayerUUID())
                    );

                    if (!balls.isEmpty()) {
                        for (FootballEntity ball : balls) {
                            ball.kickFromPlayerCharged(player, power);
                        }
                    }
                }
            });
        });

        // Register server-side receiver for client passes
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PASS_PACKET_ID, (buf, context) -> {
            float power = buf.readFloat();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    AABB searchArea = player.getBoundingBox().inflate(3.0);
                    List<FootballEntity> balls = player.level().getEntitiesOfClass(
                            FootballEntity.class,
                            searchArea,
                            ball -> player.getUUID().equals(ball.getAttachedPlayerUUID())
                    );

                    if (!balls.isEmpty()) {
                        for (FootballEntity ball : balls) {
                            ball.passFromPlayerCharged(player, power);
                        }
                    }
                }
            });
        });
    }
}
