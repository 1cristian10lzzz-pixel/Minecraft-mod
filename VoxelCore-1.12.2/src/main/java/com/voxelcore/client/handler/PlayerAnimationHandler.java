package com.voxelcore.client.handler;

import com.voxelcore.config.ModConfig;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Installs Minefade's additive player model before vanilla calculates its pose.
 * This keeps vanilla movement, skin geometry, armour layers and hitboxes intact.
 */
@SideOnly(Side.CLIENT)
public final class PlayerAnimationHandler {
    private static final Logger LOGGER = LogManager.getLogger("Minefade");
    private static boolean injectionWarningLogged;

    @SubscribeEvent
    public void onPlayerRenderPre(RenderPlayerEvent.Pre event) {
        if (!ModConfig.ENABLE_PLAYER_ANIMATIONS) return;

        EntityPlayer player = event.getEntityPlayer();
        RenderPlayer renderer = event.getRenderer();
        if (player == null || renderer == null || player.isDead) return;

        ModelBase current = renderer.getMainModel();
        // Do not replace a renderer supplied by another mod.
        if (current == null || current.getClass() != ModelPlayer.class) return;

        boolean slim = player instanceof AbstractClientPlayer
                && "slim".equalsIgnoreCase(((AbstractClientPlayer) player).getSkinType());
        ModelPlayer minefadeModel = new MinefadePlayerModel(0.0F, slim);

        try {
            // field_77045_g is RenderLivingBase's stable SRG field name in 1.12.2.
            // Replacing it in Pre is important: vanilla calculates model angles afterwards.
            ObfuscationReflectionHelper.setPrivateValue(
                    RenderLivingBase.class, renderer, minefadeModel, "field_77045_g");
        } catch (RuntimeException exception) {
            if (!injectionWarningLogged) {
                injectionWarningLogged = true;
                LOGGER.warn("Minefade could not install its optional player animation model; vanilla rendering will be kept.", exception);
            }
        }
    }
}