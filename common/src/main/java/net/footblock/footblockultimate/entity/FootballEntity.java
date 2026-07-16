package net.footblock.footblockultimate.entity;

import net.footblock.footblockultimate.FootblockUltimate;
import net.footblock.footblockultimate.block.GoalLineBlock;
import net.footblock.footblockultimate.registry.ModItems;
import net.footblock.footblockultimate.registry.ModSounds;
import net.footblock.footblockultimate.match.WorldCupMatchManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public class FootballEntity extends Entity {
    private static final int GOAL_SENSOR_HEIGHT = 4;
    private static final double GOAL_SCAN_STEPS_PER_BLOCK = 4.0;
    private static final EntityDataAccessor<Optional<UUID>> ATTACHED_PLAYER_UUID =
            SynchedEntityData.defineId(FootballEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> BALL_VARIANT =
            SynchedEntityData.defineId(FootballEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CURVE_SPIN =
            SynchedEntityData.defineId(FootballEntity.class, EntityDataSerializers.FLOAT);

    private int lastAttachTick = 0;
    private int lastKickTick = 0;
    private UUID lastTouchPlayerUUID;
    private boolean goalHandled;
    private float rollX = 0.0f;
    private float rollY = 0.0f;
    private float rollZ = 0.0f;

    public FootballEntity(EntityType<? extends Entity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ATTACHED_PLAYER_UUID, Optional.empty());
        builder.define(BALL_VARIANT, FootballVariant.CLASSIC.getId());
        builder.define(CURVE_SPIN, 0.0f);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.hasUUID("AttachedPlayer")) {
            this.setAttachedPlayerUUID(compound.getUUID("AttachedPlayer"));
        }
        this.setVariant(FootballVariant.byId(compound.getInt("BallVariant")));
        this.setCurveSpin(compound.getFloat("CurveSpin"));
        if (compound.hasUUID("LastTouchPlayer")) {
            this.lastTouchPlayerUUID = compound.getUUID("LastTouchPlayer");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        UUID attached = this.getAttachedPlayerUUID();
        if (attached != null) {
            compound.putUUID("AttachedPlayer", attached);
        }
        compound.putInt("BallVariant", this.getVariant().getId());
        compound.putFloat("CurveSpin", this.getCurveSpin());
        if (this.lastTouchPlayerUUID != null) {
            compound.putUUID("LastTouchPlayer", this.lastTouchPlayerUUID);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
    }

    public UUID getAttachedPlayerUUID() {
        return this.entityData.get(ATTACHED_PLAYER_UUID).orElse(null);
    }

    public void setAttachedPlayerUUID(UUID uuid) {
        this.entityData.set(ATTACHED_PLAYER_UUID, Optional.ofNullable(uuid));
        if (uuid != null) {
            this.lastAttachTick = this.tickCount;
        }
    }

    public void setAttachedPlayer(Player player) {
        this.setAttachedPlayerUUID(player != null ? player.getUUID() : null);
        if (player != null) {
            this.lastTouchPlayerUUID = player.getUUID();
        }
    }

    public FootballVariant getVariant() {
        return FootballVariant.byId(this.entityData.get(BALL_VARIANT));
    }

    public void setVariant(FootballVariant variant) {
        this.entityData.set(BALL_VARIANT, variant.getId());
    }

    public float getCurveSpin() {
        return this.entityData.get(CURVE_SPIN);
    }

    public void setCurveSpin(float curveSpin) {
        this.entityData.set(CURVE_SPIN, Mth.clamp(curveSpin, -1.0f, 1.0f));
    }

    public UUID getLastTouchPlayerUUID() {
        return this.lastTouchPlayerUUID;
    }

    public boolean isGoalEligible() {
        boolean inPlay = this.getAttachedPlayerUUID() != null || this.lastTouchPlayerUUID != null;
        return !this.goalHandled && inPlay;
    }

    public void markGoalHandled() {
        this.goalHandled = true;
        this.setAttachedPlayer(null);
        this.setDeltaMovement(Vec3.ZERO);
    }

    public ItemStack getPickupStack() {
        return new ItemStack(this.getVariant() == FootballVariant.WORLD_CUP_2026
                ? ModItems.WORLD_CUP_2026_BALL.get()
                : ModItems.FOOTBALL.get());
    }

    public void stopForWhistle() {
        this.setAttachedPlayer(null);
        this.setDeltaMovement(Vec3.ZERO);
        this.setCurveSpin(0.0f);
        this.lastKickTick = this.tickCount;
        this.hasImpulse = true;
    }

    public float getRollX() {
        return this.rollX;
    }

    public float getRollY() {
        return this.rollY;
    }

    public float getRollZ() {
        return this.rollZ;
    }

    @Override
    public void tick() {
        Vec3 tickStartPosition = this.position();
        super.tick();

        UUID attachedUuid = this.getAttachedPlayerUUID();
        if (attachedUuid != null) {
            // Dribbling state
            Player player = this.level().getPlayerByUUID(attachedUuid);
            if (player == null || !player.isAlive() || player.isSpectator()) {
                if (!this.level().isClientSide()) {
                    this.setAttachedPlayer(null);
                }
            } else {
                // Position ball in front of player's feet
                float yaw = player.getYRot();
                float rad = yaw * ((float) Math.PI / 180.0f);
                double distance = 0.55;
                double targetX = player.getX() - Math.sin(rad) * distance;
                double targetY = player.getY();
                double targetZ = player.getZ() + Math.cos(rad) * distance;

                this.setPos(targetX, targetY, targetZ);
                this.tryCheckInsideBlocks();
                if (this.isRemoved()) {
                    return;
                }
                if (!this.level().isClientSide()) {
                    this.setDeltaMovement(Vec3.ZERO);
                    this.setCurveSpin(0.0f);
                }

                // Visual roll based on player movement
                double dx = this.getX() - this.xo;
                double dz = this.getZ() - this.zo;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0.002) {
                    this.rollX += (float) (dz * 4.0);
                    this.rollZ -= (float) (dx * 4.0);
                }

                // Drop ball if player crouches
                if (!this.level().isClientSide() && player.isCrouching()) {
                    this.setAttachedPlayer(null);
                }
            }
        } else {
            // Physics / Rolling state
            if (!this.isNoGravity()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, -0.04, 0));
            }

            Vec3 movement = this.getDeltaMovement();
            double prevX = movement.x;
            double prevY = movement.y;
            double prevZ = movement.z;

            // Perform movement
            this.move(MoverType.SELF, movement);
            if (this.isRemoved()) {
                return;
            }

            double actualX = this.getX() - this.xo;
            double actualY = this.getY() - this.yo;
            double actualZ = this.getZ() - this.zo;

            double bounce = 0.6;
            double newX = prevX;
            double newY = prevY;
            double newZ = prevZ;

            if (this.horizontalCollision) {
                if (Math.abs(actualX) < Math.abs(prevX) * 0.5) {
                    newX = -prevX * bounce;
                }
                if (Math.abs(actualZ) < Math.abs(prevZ) * 0.5) {
                    newZ = -prevZ * bounce;
                }
            }
            if (this.verticalCollision) {
                if (prevY < 0) {
                    if (Math.abs(prevY) > 0.1) {
                        newY = -prevY * bounce;
                    } else {
                        newY = 0;
                    }
                } else if (prevY > 0) {
                    newY = -prevY * bounce;
                }
            }

            // Apply friction
            double drag = 0.98;
            if (this.onGround()) {
                drag = 0.90; // Higher friction on grass/ground
                newY = 0;
            }

            newX *= drag;
            newZ *= drag;

            if (Math.abs(newX) < 0.001) newX = 0;
            if (Math.abs(newZ) < 0.001) newZ = 0;
            if (Math.abs(newY) < 0.001) newY = 0;

            this.setDeltaMovement(newX, newY, newZ);
            this.applyCurvePhysics();

            // Visual roll based on physical movement
            double dist = Math.sqrt(actualX * actualX + actualZ * actualZ);
            if (dist > 0.002) {
                this.rollX += (float) (actualZ * 4.0);
                this.rollZ -= (float) (actualX * 4.0);
            }
            this.rollY += this.getCurveSpin() * 0.08f;
        }

        if (!this.isRemoved()) {
            this.checkGoalLineCrossing(tickStartPosition);
        }
    }

    private void checkGoalLineCrossing(Vec3 previousPosition) {
        if (!(this.level() instanceof ServerLevel serverLevel) || !this.isGoalEligible()) {
            return;
        }

        Vec3 currentPosition = this.position();
        double deltaX = currentPosition.x - previousPosition.x;
        double deltaZ = currentPosition.z - previousPosition.z;
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (horizontalDistance <= 1.0E-4) {
            return;
        }

        int steps = Math.max(1, Mth.ceil(horizontalDistance * GOAL_SCAN_STEPS_PER_BLOCK));
        for (int step = 0; step <= steps; step++) {
            Vec3 sample = previousPosition.lerp(currentPosition, step / (double) steps);
            int sampleX = Mth.floor(sample.x);
            int sampleY = Mth.floor(sample.y);
            int sampleZ = Mth.floor(sample.z);

            for (int depth = 0; depth <= GOAL_SENSOR_HEIGHT; depth++) {
                BlockPos sensorPos = new BlockPos(sampleX, sampleY - depth, sampleZ);
                BlockState sensorState = serverLevel.getBlockState(sensorPos);
                if (sensorState.getBlock() instanceof GoalLineBlock goalLine) {
                    BlockPos frameBase = sensorPos.above();
                    BlockState frameBaseState = serverLevel.getBlockState(frameBase);
                    if (!frameBaseState.getCollisionShape(serverLevel, frameBase).isEmpty()) {
                        break;
                    }
                    if (goalLine.isScoringCrossing(sensorState, sensorPos, previousPosition, currentPosition)) {
                        WorldCupMatchManager.registerGoal(
                                serverLevel,
                                sensorPos,
                                this,
                                goalLine.getScoringSide()
                        );
                        return;
                    }
                }

                // Stadium frame blocks mask the sensor column. A crossbar cuts off
                // shots above it, while posts over the edge detectors cut out those
                // cells and leave a rectangular scoring aperture between them.
                if (!sensorState.getCollisionShape(serverLevel, sensorPos).isEmpty()) {
                    break;
                }
            }
        }
    }

    @Override
    public void playerTouch(Player player) {
        if (!this.level().isClientSide() && !player.isSpectator()) {
            // Prevent immediate re-attachment after kicking the ball
            if (this.tickCount - this.lastKickTick < 15) {
                return;
            }

            UUID attachedUuid = this.getAttachedPlayerUUID();
            if (attachedUuid == null) {
                // Check if player already has a ball attached
                AABB checkArea = player.getBoundingBox().inflate(5.0);
                List<FootballEntity> attachedBalls = player.level().getEntitiesOfClass(
                        FootballEntity.class,
                        checkArea,
                        b -> player.getUUID().equals(b.getAttachedPlayerUUID())
                );
                if (attachedBalls.isEmpty()) {
                    this.setAttachedPlayer(player);
                }
            } else if (!attachedUuid.equals(player.getUUID())) {
                if (this.tickCount - this.lastAttachTick > 10) {
                    // Check if player already has a ball attached
                    AABB checkArea = player.getBoundingBox().inflate(5.0);
                    List<FootballEntity> attachedBalls = player.level().getEntitiesOfClass(
                            FootballEntity.class,
                            checkArea,
                            b -> player.getUUID().equals(b.getAttachedPlayerUUID())
                    );
                    if (attachedBalls.isEmpty()) {
                        this.setAttachedPlayer(player);
                    }
                }
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide()) {
            ItemStack ballStack = this.getPickupStack();
            if (!player.getInventory().add(ballStack)) {
                player.drop(ballStack, false);
            }
            this.discard();
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    public void kickFromPlayer(Player player) {
        this.kickFromPlayerCharged(player, 1.0f);
    }

    public void kickFromPlayerCharged(Player player, float power) {
        if (this.level().isClientSide()) {
            return;
        }
        this.setAttachedPlayer(null);
        this.lastKickTick = this.tickCount;
        this.lastTouchPlayerUUID = player.getUUID();

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

        this.setDeltaMovement(vx, vy, vz);
        this.applyCurveFromPlayer(player, power, 1.0f);
        this.hasImpulse = true;

        float pitch = 1.0f + (1.0f - power) * 0.5f;
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.FOOTBALL_KICK.get(), SoundSource.PLAYERS, 1.0f, pitch);

        boolean isRightLeg = determineKickLeg(player);
        broadcastKickAnimation(player, power, isRightLeg);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isRemoved()) {
            return false;
        }

        if (source.getEntity() instanceof Player player) {
            if (!this.level().isClientSide()) {
                this.kickFromPlayer(player);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    public void passFromPlayerCharged(Player player, float power) {
        if (this.level().isClientSide()) {
            return;
        }
        this.setAttachedPlayer(null);
        this.lastKickTick = this.tickCount;
        this.lastTouchPlayerUUID = player.getUUID();

        Player target = findPassTarget(player);
        double vx, vy, vz;

        if (target != null) {
            Vec3 toTarget = target.position().subtract(this.position());
            double dist = toTarget.length();
            Vec3 flatDir = new Vec3(toTarget.x, 0, toTarget.z).normalize();

            double horizontalForce = (dist * 0.04 + 0.25) * (0.3f + 0.7f * power);
            double verticalLift = Math.min(0.2, dist * 0.01) * (0.2f + 0.8f * power);

            vx = flatDir.x * horizontalForce;
            vy = Math.max(0.05, verticalLift);
            vz = flatDir.z * horizontalForce;
        } else {
            Vec3 lookVec = player.getLookAngle();
            double horizontalForce = (player.isSprinting() ? 1.0 : 0.6) * power;
            double verticalLift = 0.05 * power;

            vx = lookVec.x * horizontalForce;
            vy = Math.max(0.02, lookVec.y * horizontalForce + verticalLift);
            vz = lookVec.z * horizontalForce;
        }

        this.setDeltaMovement(vx, vy, vz);
        this.applyCurveFromPlayer(player, power, 0.35f);
        this.hasImpulse = true;

        float pitch = 1.2f + (1.0f - power) * 0.4f;
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.FOOTBALL_KICK.get(), SoundSource.PLAYERS, 0.8f, pitch);

        boolean isRightLeg = determineKickLeg(player);
        broadcastKickAnimation(player, power, isRightLeg);
    }

    private Player findPassTarget(Player kicker) {
        Player bestTarget = null;
        double bestScore = -1.0;
        Vec3 kickerPos = kicker.position();
        Vec3 lookVec = kicker.getLookAngle().normalize();

        for (Player target : kicker.level().players()) {
            if (target == kicker || target.isSpectator() || !target.isAlive()) {
                continue;
            }
            if (kicker.getTeam() != null && target.getTeam() != kicker.getTeam()) {
                continue;
            }

            Vec3 toTarget = target.position().subtract(kickerPos);
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
                    bestTarget = target;
                }
            }
        }
        return bestTarget;
    }

    private boolean determineKickLeg(Player player) {
        float bodyYawRad = player.yBodyRot * ((float) Math.PI / 180.0f);
        double bodyX = -Math.sin(bodyYawRad);
        double bodyZ = Math.cos(bodyYawRad);
        Vec3 look = player.getLookAngle();
        double cross = bodyX * look.z - bodyZ * look.x;

        if (cross < -0.35) {
            return false; // Looking to the right -> use Left Leg
        } else if (cross > 0.35) {
            return true;  // Looking to the left -> use Right Leg
        } else {
            // Walking cycle swing
            float swing = player.walkAnimation.position();
            return Math.sin(swing) <= 0.0;
        }
    }

    public void applyCurveFromPlayer(Player player, float power, float multiplier) {
        if (this.getVariant() != FootballVariant.WORLD_CUP_2026 || player.isCrouching()) {
            this.setCurveSpin(0.0f);
            return;
        }

        float bodyYawRad = player.yBodyRot * ((float) Math.PI / 180.0f);
        double bodyX = -Math.sin(bodyYawRad);
        double bodyZ = Math.cos(bodyYawRad);
        Vec3 look = player.getLookAngle();
        double cross = bodyX * look.z - bodyZ * look.x;
        float curve = Mth.clamp((float) cross * power * multiplier, -0.75f, 0.75f);
        this.setCurveSpin(curve);

    }

    private void applyCurvePhysics() {
        float curve = this.getCurveSpin();
        if (Math.abs(curve) < 0.002f) {
            return;
        }

        Vec3 velocity = this.getDeltaMovement();
        if (!this.onGround() && velocity.horizontalDistanceSqr() > 0.01) {
            double angle = -curve * 0.018;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double curvedX = velocity.x * cos - velocity.z * sin;
            double curvedZ = velocity.x * sin + velocity.z * cos;
            this.setDeltaMovement(curvedX, velocity.y, curvedZ);
            if (!this.level().isClientSide()) {
                this.setCurveSpin(curve * 0.985f);
            }
        } else if (!this.level().isClientSide()) {
            this.setCurveSpin(curve * 0.82f);
        }
    }

    private void broadcastKickAnimation(Player player, float power, boolean isRightLeg) {
        if (player.level().isClientSide()) {
            return;
        }

        if (player.level().getServer() != null) {
            for (net.minecraft.server.level.ServerPlayer serverPlayer : player.level().getServer().getPlayerList().getPlayers()) {
                if (serverPlayer.level() == player.level()) {
                    net.minecraft.network.RegistryFriendlyByteBuf buf = new net.minecraft.network.RegistryFriendlyByteBuf(
                            io.netty.buffer.Unpooled.buffer(),
                            player.level().registryAccess()
                    );
                    buf.writeUUID(player.getUUID());
                    buf.writeBoolean(isRightLeg);
                    buf.writeFloat(power);
                    dev.architectury.networking.NetworkManager.sendToPlayer(serverPlayer, FootblockUltimate.KICK_ANIM_S2C_PACKET_ID, buf);
                }
            }
        }
    }
}
