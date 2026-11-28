package com.voxelcore.client.handler;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/**
 * Captures only the fixed-function state changed by Minefade's batched world
 * overlays. Avoid querying unrelated alpha/light/current-colour state on every
 * render pass; this reduces driver synchronisation while still restoring all
 * state that water, lava and material-detail passes actually modify.
 */
public final class WorldRenderStateGuard {
    private final boolean texture;
    private final boolean lighting;
    private final boolean blend;
    private final boolean cull;
    private final boolean depthMask;
    private final int blendSrcRgb;
    private final int blendDstRgb;
    private final int blendSrcAlpha;
    private final int blendDstAlpha;

    private WorldRenderStateGuard() {
        texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        blend = GL11.glIsEnabled(GL11.GL_BLEND);
        cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    }

    public static WorldRenderStateGuard capture() {
        return new WorldRenderStateGuard();
    }

    public void restore() {
        GlStateManager.depthMask(depthMask);
        GlStateManager.tryBlendFuncSeparate(
                blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
        if (texture) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
        if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
        if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
        if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
    }
}
