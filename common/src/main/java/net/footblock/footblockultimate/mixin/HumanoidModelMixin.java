package net.footblock.footblockultimate.mixin;

import net.footblock.footblockultimate.client.PlayerAnimationTracker;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<T extends LivingEntity> {
    @Shadow @Final public ModelPart rightLeg;
    @Shadow @Final public ModelPart leftLeg;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player) {
            PlayerAnimationTracker.KickAnimation anim = PlayerAnimationTracker.getKick(player.getUUID());
            if (anim != null) {
                long elapsed = System.currentTimeMillis() - anim.startTime;
                float t = elapsed / 300.0f;
                if (t >= 0.0f && t <= 1.0f) {
                    float progress = (float) Math.sin(t * Math.PI);
                    float powerFactor = 0.3f + 0.7f * anim.power; // ensures even weak kicks are clearly visible

                    if (anim.isRightLeg) {
                        // Swing right leg forward
                        float kickAngle = -1.2f * progress * powerFactor;
                        rightLeg.xRot = rightLeg.xRot * (1.0f - progress) + kickAngle;

                        // Balance left arm forward
                        float armAngle = -1.0f * progress * powerFactor;
                        leftArm.xRot = leftArm.xRot * (1.0f - progress) + armAngle;

                        // Draw right arm backward
                        float sameArmAngle = 0.6f * progress * powerFactor;
                        rightArm.xRot = rightArm.xRot * (1.0f - progress) + sameArmAngle;

                        // Support leg (left leg) stays planted
                        leftLeg.xRot = leftLeg.xRot * (1.0f - progress);
                    } else {
                        // Swing left leg forward
                        float kickAngle = -1.2f * progress * powerFactor;
                        leftLeg.xRot = leftLeg.xRot * (1.0f - progress) + kickAngle;

                        // Balance right arm forward
                        float armAngle = -1.0f * progress * powerFactor;
                        rightArm.xRot = rightArm.xRot * (1.0f - progress) + armAngle;

                        // Draw left arm backward
                        float sameArmAngle = 0.6f * progress * powerFactor;
                        leftArm.xRot = leftArm.xRot * (1.0f - progress) + sameArmAngle;

                        // Support leg (right leg) stays planted
                        rightLeg.xRot = rightLeg.xRot * (1.0f - progress);
                    }
                }
            }
        }
    }
}
