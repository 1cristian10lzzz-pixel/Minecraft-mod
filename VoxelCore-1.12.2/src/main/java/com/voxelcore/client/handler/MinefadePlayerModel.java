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
 * Additive procedural animation layer. Vanilla still owns the base pose,
 * skin geometry, equipment, hitboxes and interaction state.
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

        if (!ModConfig.ENABLE_PLAYER_ANIMATIONS || !(entityIn instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entityIn;

        float walk = clamp(limbSwingAmount, 0.0F, 1.0F);
        float phase = limbSwing;
        float stride = (float) Math.sin(phase) * walk;
        float oppositeStride = (float) Math.sin(phase + Math.PI) * walk;
        float bodyBob = (float) Math.sin(phase * 2.0F) * walk;
        float strafe = clamp(player.moveStrafing, -1.0F, 1.0F);
        float forward = clamp(player.moveForward, -1.0F, 1.0F);
        float sprint = player.isSprinting() ? 1.0F : 0.0F;
        float crouch = player.isSneaking() ? 1.0F : 0.0F;
        boolean inWater = player.isInsideOfMaterial(Material.WATER);

        // Weight transfer and a slight pelvis/torso counter-rotation.
        bipedBody.rotateAngleX += 0.025F * bodyBob + 0.055F * sprint + 0.025F * crouch;
        bipedBody.rotateAngleY += -stride * 0.035F + strafe * 0.045F;
        bipedBody.rotateAngleZ -= strafe * (0.045F + 0.018F * walk);
        bipedHead.rotateAngleZ += strafe * 0.022F;
        bipedHead.rotateAngleY += -strafe * 0.018F;

        // Arms follow the body's momentum rather than moving as rigid mirrors.
        bipedRightArm.rotateAngleZ += -0.025F * stride + strafe * 0.025F;
        bipedLeftArm.rotateAngleZ += 0.025F * stride + strafe * 0.025F;
        bipedRightArm.rotateAngleX -= 0.018F * bodyBob;
        bipedLeftArm.rotateAngleX += 0.018F * bodyBob;
        bipedRightLeg.rotateAngleZ -= strafe * 0.014F;
        bipedLeftLeg.rotateAngleZ -= strafe * 0.014F;

        if (sprint > 0.0F && !inWater) {
            float pump = (float) Math.sin(phase * 1.7F) * walk;
            bipedRightArm.rotateAngleX -= 0.10F + pump * 0.045F;
            bipedLeftArm.rotateAngleX -= 0.10F - pump * 0.045F;
            bipedBody.rotateAngleX += 0.025F * Math.abs(forward);
        }

        if (crouch > 0.0F) {
            bipedBody.rotateAngleX += 0.035F;
            bipedRightArm.rotateAngleX -= 0.035F;
            bipedLeftArm.rotateAngleX -= 0.035F;
        }

        if (player.isOnLadder() && !inWater) {
            float climb = (float) Math.sin(ageInTicks * 0.22F) * 0.16F;
            bipedRightArm.rotateAngleX = -1.15F + climb;
            bipedLeftArm.rotateAngleX = -1.15F - climb;
            bipedRightLeg.rotateAngleX = 0.45F - climb * 0.55F;
            bipedLeftLeg.rotateAngleX = 0.45F + climb * 0.55F;
            bipedBody.rotateAngleX -= 0.06F;
        }

        if (inWater) {
            float swimPhase = phase * 0.62F + ageInTicks * 0.035F;
            float paddle = (float) Math.sin(swimPhase) * 0.12F;
            float kick = (float) Math.sin(swimPhase + Math.PI * 0.5D) * 0.14F;
            bipedRightArm.rotateAngleX += paddle;
            bipedLeftArm.rotateAngleX -= paddle;
            bipedRightArm.rotateAngleZ -= 0.035F;
            bipedLeftArm.rotateAngleZ += 0.035F;
            bipedRightLeg.rotateAngleX += kick;
            bipedLeftLeg.rotateAngleX -= kick;
            bipedBody.rotateAngleX -= 0.055F;
        }

        if (player.isElytraFlying()) {
            bipedBody.rotateAngleX -= 0.10F;
            bipedRightArm.rotateAngleZ -= 0.075F;
            bipedLeftArm.rotateAngleZ += 0.075F;
            bipedRightArm.rotateAngleX = -0.95F;
            bipedLeftArm.rotateAngleX = -0.95F;
            bipedRightLeg.rotateAngleX += 0.10F;
            bipedLeftLeg.rotateAngleX += 0.10F;
        } else if (!player.onGround && !inWater && player.motionY < -0.08D) {
            // A restrained falling pose; vanilla still controls jump and flight state.
            float fall = clamp((float) (-player.motionY * 0.12D), 0.0F, 0.22F);
            bipedBody.rotateAngleX -= fall * 0.35F;
            bipedRightArm.rotateAngleZ -= fall * 0.35F;
            bipedLeftArm.rotateAngleZ += fall * 0.35F;
            bipedRightLeg.rotateAngleX += fall * 0.45F;
            bipedLeftLeg.rotateAngleX += fall * 0.45F;
        }

        float attack = this.swingProgress;
        if (attack > 0.0F) {
            float impact = (float) Math.sin(Math.sqrt(attack) * Math.PI);
            bipedBody.rotateAngleY += impact * 0.085F;
            if (player.getPrimaryHand() == EnumHandSide.RIGHT) {
                bipedRightArm.rotateAngleX -= impact * 0.035F;
                bipedLeftArm.rotateAngleX += impact * 0.012F;
            } else {
                bipedLeftArm.rotateAngleX -= impact * 0.035F;
                bipedRightArm.rotateAngleX += impact * 0.012F;
            }
        }

        // Breathing and tiny posture shifts keep a completely still player alive.
        double horizontalSpeedSq = player.motionX * player.motionX + player.motionZ * player.motionZ;
        if (horizontalSpeedSq < 0.0004D && !inWater && !player.isElytraFlying()) {
            float breath = (float) Math.sin(ageInTicks * 0.075F) * 0.008F;
            float weightShift = (float) Math.sin(ageInTicks * 0.031F) * 0.006F;
            bipedBody.rotateAngleX += breath;
            bipedBody.rotateAngleZ += weightShift;
            bipedHead.rotateAngleX += breath * 0.28F;
            bipedRightArm.rotateAngleZ += weightShift * 0.5F;
            bipedLeftArm.rotateAngleZ += weightShift * 0.5F;
        }

        // Keep the skin overlays aligned with the animated base parts.
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
