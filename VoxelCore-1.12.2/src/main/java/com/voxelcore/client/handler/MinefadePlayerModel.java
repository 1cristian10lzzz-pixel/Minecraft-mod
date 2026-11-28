package com.voxelcore.client.handler;

import com.voxelcore.config.ModConfig;
import net.minecraft.block.material.Material;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Conservative additive animation layer.
 *
 * Vanilla calculates the primary pose first. Minefade only adds small,
 * state-aware offsets; it never replaces the vanilla crouch, elytra, attack,
 * jump, skin or equipment pose. That avoids the old double-pose artefacts.
 */
@SideOnly(Side.CLIENT)
public final class MinefadePlayerModel extends ModelPlayer {
    public MinefadePlayerModel(float modelSize, boolean smallArms) {
        super(modelSize, smallArms);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, scaleFactor, entityIn);

        if (!ModConfig.ENABLE_PLAYER_ANIMATIONS || !(entityIn instanceof EntityPlayer)
                || ModConfig.PLAYER_ANIMATION_STRENGTH <= 0) return;

        EntityPlayer player = (EntityPlayer) entityIn;
        float intensity = clamp(ModConfig.PLAYER_ANIMATION_STRENGTH / 100.0F, 0.0F, 1.0F);
        float gait = clamp(limbSwingAmount, 0.0F, 1.0F);
        float phase = limbSwing;
        float stride = (float) Math.sin(phase) * gait;
        float oppositeStride = (float) Math.cos(phase) * gait;
        float strafe = clamp(player.moveStrafing, -1.0F, 1.0F);
        float forward = clamp(player.moveForward, -1.0F, 1.0F);
        boolean inWater = player.isInsideOfMaterial(Material.WATER);
        boolean onLadder = player.isOnLadder();
        boolean sprinting = player.isSprinting() && !inWater && !onLadder;

        // Small centre-of-mass response while walking and turning.
        bipedBody.rotateAngleY += -stride * 0.018F * intensity;
        bipedBody.rotateAngleZ -= strafe * (0.025F + gait * 0.012F) * intensity;
        bipedHead.rotateAngleZ += strafe * 0.010F * intensity;

        // Running adds only a little arm follow-through to the vanilla counter-swing.
        if (sprinting && player.onGround) {
            float pump = oppositeStride * 0.055F * intensity;
            bipedRightArm.rotateAngleX += pump;
            bipedLeftArm.rotateAngleX -= pump;
            bipedBody.rotateAngleX += Math.abs(forward) * 0.028F * intensity;
        }

        // Climbing: vanilla remains responsible for the base limb angles.
        // This adds a light alternating reach rather than forcing a new pose.
        if (onLadder && !inWater) {
            float climb = (float) Math.sin(ageInTicks * 0.16F) * 0.07F * intensity;
            bipedRightArm.rotateAngleX += climb;
            bipedLeftArm.rotateAngleX -= climb;
            bipedRightLeg.rotateAngleX -= climb * 0.32F;
            bipedLeftLeg.rotateAngleX += climb * 0.32F;
        }

        // Swimming strokes scale with actual movement; an idle underwater player
        // no longer paddles continuously while standing still.
        float swimActivity = clamp(Math.abs(forward) + Math.abs(strafe)
                + Math.min(0.45F, (float) Math.abs(player.motionY) * 1.8F), 0.0F, 1.0F);
        if (inWater && swimActivity > 0.06F) {
            float swimBlend = intensity * swimActivity;
            float swimPhase = ageInTicks * (0.065F + swimActivity * 0.04F) + phase * 0.16F;
            float rightStroke = (float) Math.sin(swimPhase) * 0.085F * swimBlend;
            float leftStroke = (float) Math.sin(swimPhase + Math.PI) * 0.085F * swimBlend;
            float rightKick = (float) Math.sin(swimPhase + Math.PI * 0.5D) * 0.070F * swimBlend;
            float leftKick = (float) Math.sin(swimPhase - Math.PI * 0.5D) * 0.070F * swimBlend;
            bipedBody.rotateAngleX -= 0.025F * swimBlend;
            bipedRightArm.rotateAngleX += rightStroke;
            bipedLeftArm.rotateAngleX += leftStroke;
            bipedRightLeg.rotateAngleX += rightKick;
            bipedLeftLeg.rotateAngleX += leftKick;
        }

        // The vanilla elytra pose and falling transitions are intentionally
        // left intact; no absolute arm angles are assigned here.
        float attack = this.swingProgress;
        if (attack > 0.0F && player.getPrimaryHand() == EnumHandSide.RIGHT) {
            bipedLeftArm.rotateAngleZ += attack * 0.018F * intensity;
        } else if (attack > 0.0F) {
            bipedRightArm.rotateAngleZ -= attack * 0.018F * intensity;
        }

        // Subtle breathing only while genuinely idle; never fights locomotion.
        double horizontalSpeedSq = player.motionX * player.motionX + player.motionZ * player.motionZ;
        if (horizontalSpeedSq < 0.0004D && !inWater && !player.isElytraFlying()
                && !player.isSneaking() && !onLadder) {
            float breath = (float) Math.sin(ageInTicks * 0.075F) * 0.0035F * intensity;
            bipedBody.rotateAngleX += breath;
            bipedHead.rotateAngleX += breath * 0.25F;
        }

        // Keep every wearable overlay aligned after all additive rotations.
        copyModelAngles(bipedHead, bipedHeadwear);
        copyModelAngles(bipedBody, bipedBodyWear);
        copyModelAngles(bipedRightArm, bipedRightArmwear);
        copyModelAngles(bipedLeftArm, bipedLeftArmwear);
        copyModelAngles(bipedRightLeg, bipedRightLegwear);
        copyModelAngles(bipedLeftLeg, bipedLeftLegwear);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
