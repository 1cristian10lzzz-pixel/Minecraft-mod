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
 * Small secondary animation layer. Vanilla first calculates the base pose;
 * Minefade then adds motion and synchronizes the skin overlay pieces.
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
        float sway = (float) Math.sin(phase) * walk;
        float bob = (float) Math.sin(phase * 0.5F) * walk;
        float strafe = clamp(player.moveStrafing, -1.0F, 1.0F);
        float sprint = player.isSprinting() ? 1.0F : 0.0F;
        float crouch = player.isSneaking() ? 1.0F : 0.0F;

        // Subtle body weight shift and arm counter-swing over vanilla locomotion.
        bipedBody.rotateAngleX += 0.035F * bob + 0.045F * sprint + 0.018F * crouch;
        bipedBody.rotateAngleZ -= strafe * (0.035F + 0.018F * walk);
        bipedHead.rotateAngleZ += strafe * 0.018F;
        bipedRightArm.rotateAngleZ += -0.028F * sway + strafe * 0.018F;
        bipedLeftArm.rotateAngleZ += 0.028F * sway + strafe * 0.018F;
        bipedRightArm.rotateAngleX -= 0.025F * bob;
        bipedLeftArm.rotateAngleX += 0.025F * bob;
        bipedRightLeg.rotateAngleZ -= strafe * 0.010F;
        bipedLeftLeg.rotateAngleZ -= strafe * 0.010F;

        if (player.isInsideOfMaterial(Material.WATER)) {
            float swimPhase = phase * 0.62F;
            float paddle = (float) Math.sin(swimPhase) * 0.085F;
            float kick = (float) Math.sin(swimPhase + Math.PI * 0.5D) * 0.11F;
            bipedRightArm.rotateAngleX += paddle;
            bipedLeftArm.rotateAngleX -= paddle;
            bipedRightLeg.rotateAngleX += kick;
            bipedLeftLeg.rotateAngleX -= kick;
            bipedBody.rotateAngleX -= 0.035F;
        }

        if (player.isElytraFlying()) {
            bipedBody.rotateAngleX -= 0.075F;
            bipedRightArm.rotateAngleZ -= 0.055F;
            bipedLeftArm.rotateAngleZ += 0.055F;
        }

        float attack = this.swingProgress;
        if (attack > 0.0F) {
            float impact = (float) Math.sin(Math.sqrt(attack) * Math.PI);
            bipedBody.rotateAngleY += impact * 0.075F;
            if (player.getPrimaryHand() == EnumHandSide.RIGHT) {
                bipedRightArm.rotateAngleX -= impact * 0.025F;
            } else {
                bipedLeftArm.rotateAngleX -= impact * 0.025F;
            }
        }

        double horizontalSpeedSq = player.motionX * player.motionX + player.motionZ * player.motionZ;
        if (horizontalSpeedSq < 0.0004D && !player.isInsideOfMaterial(Material.WATER)) {
            float breath = (float) Math.sin(ageInTicks * 0.08F) * 0.006F;
            bipedBody.rotateAngleX += breath;
            bipedHead.rotateAngleX += breath * 0.30F;
        }

        // ModelPlayer copies these angles during super.setRotationAngles(), before our additions.
        // Copy again so sleeves, trousers and torso overlay follow the animated base parts.
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