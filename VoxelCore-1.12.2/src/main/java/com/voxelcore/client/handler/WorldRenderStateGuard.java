package com.voxelcore.client.handler;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/**
 * Captures the fixed-function states touched by Minefade's additive world
 * render passes. This prevents water/lava/material overlays leaking blend
 * factors or capability toggles into later vanilla/mod rendering.
 */
public final class WorldRenderStateGuard {
    private final boolean texture;
    private final boolean lighting;
    private final boolean blend;
    private final boolean cull;
    private final boolean depthTest;
    private final boolean alphaTest;
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
        depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        alphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
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
        setEnabled(GL11.GL_TEXTURE_2D, texture);
        setEnabled(GL11.GL_LIGHTING, lighting);
        setEnabled(GL11.GL_BLEND, blend);
        setEnabled(GL11.GL_CULL_FACE, cull);
        setEnabled(GL11.GL_DEPTH_TEST, depthTest);
        setEnabled(GL11.GL_ALPHA_TEST, alphaTest);
    }

    private static void setEnabled(int capability, boolean enabled) {
        switch (capability) {
            case GL11.GL_TEXTURE_2D:
                if (enabled) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
                break;
            case GL11.GL_LIGHTING:
                if (enabled) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
                break;
            case GL11.GL_BLEND:
                if (enabled) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
                break;
            case GL11.GL_CULL_FACE:
                if (enabled) GlStateManager.enableCull(); else GlStateManager.disableCull();
                break;
            case GL11.GL_DEPTH_TEST:
                if (enabled) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
                break;
            case GL11.GL_ALPHA_TEST:
                if (enabled) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
                break;
            default:
                break;
        }
    }
}
