package com.voxelcore.client.handler;

import com.voxelcore.config.ModConfig;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Lightweight additive player animation layer inspired by Mo' Bends.
 * It deliberately leaves vanilla limb locomotion, hitboxes and networking intact.
 * Every changed model angle is restored after rendering to avoid pose leakage.
 */
@SideOnly(Side.CLIENT)
public final class PlayerAnimationHandler {
    private final Map<ModelBiped, float[]> originalAngles = new IdentityHashMap<ModelBiped, float[]>();

    @SubscribeEvent
    public void onPlayerRenderPre(RenderPlayerEvent.Pre event) {
        if (!ModConfig.ENABLE_PLAYER_ANIMATIONS) return;
        EntityPlayer player = event.getEntityPlayer();
        RenderLivingBase<?> renderer = event.getRenderer();
        if (player == null || renderer == null || player.isDead) return;
        if (player.isRiding() || player.isPlayerSleeping()) return;

        ModelBiped model = (ModelBiped) renderer.getMainModel();
        if (model == null || originalAngles.containsKey(model)) return;

        originalAngles.put(model, capture(model));
        float walk = clamp(player.limbSwingAmount, 0.0F, 1.0F);
        float phase = player.limbSwing;
        float speed = (float) Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
        float bob = (float) Math.sin(phase * 0.5F) * walk;
        float sway = (float) Math.sin(phase) * walk;
        float turn = clamp(player.moveStrafing, -1.0F, 1.0F);
        float sprint = player.isSprinting() ? 1.0F : 0.0F;
        float crouch = player.isSneaking() ? 1.0F : 0.0F;
        float swing = player.swingProgress;

        // Small secondary motion layered over vanilla's own limb animation.
        model.bipedBody.rotateAngleX += 0.035F * bob + 0.075F * sprint + 0.025F * crouch;
        model.bipedBody.rotateAngleZ += -turn * (0.045F + 0.025F * walk);
        model.bipedHead.rotateAngleZ += turn * 0.025F;
        model.bipedRightArm.rotateAngleZ += -0.035F * sway + turn * 0.025F;
        model.bipedLeftArm.rotateAngleZ += 0.035F * sway + turn * 0.025F;
        model.bipedRightArm.rotateAngleX += -0.035F * bob;
        model.bipedLeftArm.rotateAngleX += 0.035F * bob;

        if (player.isInWater()) {
            float paddle = (float) Math.sin(phase * 0.65F) * 0.10F;
            model.bipedRightArm.rotateAngleX += paddle;
            model.bipedLeftArm.rotateAngleX -= paddle;
            model.bipedBody.rotateAngleX -= 0.035F;
        }
        if (player.isElytraFlying()) {
            model.bipedBody.rotateAngleX -= 0.10F;
            model.bipedRightArm.rotateAngleZ -= 0.08F;
            model.bipedLeftArm.rotateAngleZ += 0.08F;
        }
        if (swing > 0.0F) {
            float attack = (float) Math.sin(Math.sqrt(swing) * Math.PI);
            model.bipedBody.rotateAngleY += attack * 0.10F;
            model.bipedRightArm.rotateAngleX -= attack * 0.035F;
        }
        if (speed < 0.015F && !player.isInWater()) {
            float idle = (float) Math.sin(player.ticksExisted * 0.08F) * 0.008F;
            model.bipedBody.rotateAngleX += idle;
            model.bipedHead.rotateAngleX += idle * 0.35F;
        }
    }

    @SubscribeEvent
    public void onPlayerRenderPost(RenderPlayerEvent.Post event) {
        if (!ModConfig.ENABLE_PLAYER_ANIMATIONS) {
            restoreAll();
            return;
        }
        ModelBiped model = (ModelBiped) event.getRenderer().getMainModel();
        float[] angles = originalAngles.remove(model);
        if (angles != null) restore(model, angles);
    }

    private void restoreAll() {
        for (Map.Entry<ModelBiped, float[]> entry : originalAngles.entrySet()) {
            restore(entry.getKey(), entry.getValue());
        }
        originalAngles.clear();
    }

    private static float[] capture(ModelBiped m) {
        return new float[] {
                m.bipedHead.rotateAngleX, m.bipedHead.rotateAngleY, m.bipedHead.rotateAngleZ,
                m.bipedBody.rotateAngleX, m.bipedBody.rotateAngleY, m.bipedBody.rotateAngleZ,
                m.bipedRightArm.rotateAngleX, m.bipedRightArm.rotateAngleY, m.bipedRightArm.rotateAngleZ,
                m.bipedLeftArm.rotateAngleX, m.bipedLeftArm.rotateAngleY, m.bipedLeftArm.rotateAngleZ,
                m.bipedRightLeg.rotateAngleX, m.bipedRightLeg.rotateAngleY, m.bipedRightLeg.rotateAngleZ,
                m.bipedLeftLeg.rotateAngleX, m.bipedLeftLeg.rotateAngleY, m.bipedLeftLeg.rotateAngleZ
        };
    }

    private static void restore(ModelBiped m, float[] a) {
        m.bipedHead.rotateAngleX=a[0]; m.bipedHead.rotateAngleY=a[1]; m.bipedHead.rotateAngleZ=a[2];
        m.bipedBody.rotateAngleX=a[3]; m.bipedBody.rotateAngleY=a[4]; m.bipedBody.rotateAngleZ=a[5];
        m.bipedRightArm.rotateAngleX=a[6]; m.bipedRightArm.rotateAngleY=a[7]; m.bipedRightArm.rotateAngleZ=a[8];
        m.bipedLeftArm.rotateAngleX=a[9]; m.bipedLeftArm.rotateAngleY=a[10]; m.bipedLeftArm.rotateAngleZ=a[11];
        m.bipedRightLeg.rotateAngleX=a[12]; m.bipedRightLeg.rotateAngleY=a[13]; m.bipedRightLeg.rotateAngleZ=a[14];
        m.bipedLeftLeg.rotateAngleX=a[15]; m.bipedLeftLeg.rotateAngleY=a[16]; m.bipedLeftLeg.rotateAngleZ=a[17];
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
