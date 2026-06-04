package net.footblock.footblockultimate.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerAnimationTracker {
    public static class KickAnimation {
        public final long startTime;
        public final boolean isRightLeg;
        public final float power;

        public KickAnimation(long startTime, boolean isRightLeg, float power) {
            this.startTime = startTime;
            this.isRightLeg = isRightLeg;
            this.power = power;
        }
    }

    private static final Map<UUID, KickAnimation> ACTIVE_KICKS = new HashMap<>();

    public static void startKick(UUID playerUuid, boolean isRightLeg, float power) {
        ACTIVE_KICKS.put(playerUuid, new KickAnimation(System.currentTimeMillis(), isRightLeg, power));
    }

    public static KickAnimation getKick(UUID playerUuid) {
        KickAnimation anim = ACTIVE_KICKS.get(playerUuid);
        if (anim == null) return null;

        // Animation duration is set to 300ms (0.3s)
        if (System.currentTimeMillis() - anim.startTime > 300) {
            ACTIVE_KICKS.remove(playerUuid);
            return null;
        }
        return anim;
    }
}
