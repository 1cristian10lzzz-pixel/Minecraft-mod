package com.voxelcore.client.handler;

import com.voxelcore.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Applies the vanilla minimal-particles option without permanently overwriting the user's setting. */
@SideOnly(Side.CLIENT)
public final class ParticleQualityController {
    private static int previousParticleSetting = -1;
    private static boolean applied;

    private ParticleQualityController() { }

    public static void update(Minecraft mc) {
        if (mc == null || mc.gameSettings == null) return;

        if (ModConfig.FORCE_MINIMAL_PARTICLES) {
            if (!applied) {
                previousParticleSetting = mc.gameSettings.particleSetting;
                applied = true;
            }
            if (mc.gameSettings.particleSetting != 2) mc.gameSettings.particleSetting = 2;
        } else {
            restore(mc);
        }
    }

    public static void restore(Minecraft mc) {
        if (mc != null && applied && previousParticleSetting >= 0) {
            mc.gameSettings.particleSetting = previousParticleSetting;
        }
        previousParticleSetting = -1;
        applied = false;
    }
}
