package com.voxelcore.client.handler;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;

import java.nio.FloatBuffer;

/**
 * Captures the fixed-function states touched by Minefade's additive world
 * render passes. This prevents water/lava/material overlays leaking blend
 * factors or capability toggles into later vanilla/mod rendering.
 */
public final class WorldRenderStateGuard {
    private static final FloatBuffer CURRENT_COLOR = BufferUtils.createFloatBuffer(4);
    private final boolean texture;
    private final boolean lighting;
    private final boolean blend;
    private final boolean cull;
    private final boolean depthTest;
    private final boolean alphaTest;
    private final boolean light0;
    private final boolean light1;
    private final boolean colorMaterial;
    private final boolean rescaleNormal;
    private final boolean depthMask;
    private final int blendSrcRgb;
    private final int blendDstRgb;
    private final int blendSrcAlpha;
    private final int blendDstAlpha;
    private final int alphaFunction;
    private final float alphaReference;
    private final int colorMaterialFace;
    private final int colorMaterialParameter;
    private final float colorRed;
    private final float colorGreen;
    private final float colorBlue;
    private final float colorAlpha;

    private WorldRenderStateGuard() {
        texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        blend = GL11.glIsEnabled(GL11.GL_BLEND);
        cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        alphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        light0 = GL11.glIsEnabled(GL11.GL_LIGHT0);
        light1 = GL11.glIsEnabled(GL11.GL_LIGHT1);
        colorMaterial = GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);
        rescaleNormal = GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL);
        depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        alphaFunction = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        alphaReference = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        colorMaterialFace = GL11.glGetInteger(GL11.GL_COLOR_MATERIAL_FACE);
        colorMaterialParameter = GL11.glGetInteger(GL11.GL_COLOR_MATERIAL_PARAMETER);
        CURRENT_COLOR.clear();
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, CURRENT_COLOR);
        CURRENT_COLOR.rewind();
        colorRed = CURRENT_COLOR.get();
        colorGreen = CURRENT_COLOR.get();
        colorBlue = CURRENT_COLOR.get();
        colorAlpha = CURRENT_COLOR.get();
    }

    public static WorldRenderStateGuard capture() {
        return new WorldRenderStateGuard();
    }

    public void restore() {
        GlStateManager.depthMask(depthMask);
        GlStateManager.tryBlendFuncSeparate(
                blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
        GlStateManager.alphaFunc(alphaFunction, alphaReference);
        GlStateManager.colorMaterial(colorMaterialFace, colorMaterialParameter);
        GlStateManager.color(colorRed, colorGreen, colorBlue, colorAlpha);
        setEnabled(GL11.GL_TEXTURE_2D, texture);
        setEnabled(GL11.GL_LIGHTING, lighting);
        setEnabled(GL11.GL_BLEND, blend);
        setEnabled(GL11.GL_CULL_FACE, cull);
        setEnabled(GL11.GL_DEPTH_TEST, depthTest);
        setEnabled(GL11.GL_ALPHA_TEST, alphaTest);
        setEnabled(GL11.GL_LIGHT0, light0);
        setEnabled(GL11.GL_LIGHT1, light1);
        setEnabled(GL11.GL_COLOR_MATERIAL, colorMaterial);
        setEnabled(GL12.GL_RESCALE_NORMAL, rescaleNormal);
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
            case GL11.GL_LIGHT0:
                if (enabled) GlStateManager.enableLight(0); else GlStateManager.disableLight(0);
                break;
            case GL11.GL_LIGHT1:
                if (enabled) GlStateManager.enableLight(1); else GlStateManager.disableLight(1);
                break;
            case GL11.GL_COLOR_MATERIAL:
                if (enabled) GlStateManager.enableColorMaterial(); else GlStateManager.disableColorMaterial();
                break;
            case GL12.GL_RESCALE_NORMAL:
                if (enabled) GlStateManager.enableRescaleNormal(); else GlStateManager.disableRescaleNormal();
                break;
            default:
                break;
        }
    }
}
