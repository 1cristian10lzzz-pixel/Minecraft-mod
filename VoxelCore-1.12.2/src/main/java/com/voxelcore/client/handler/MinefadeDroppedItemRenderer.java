package com.voxelcore.client.handler;

import com.voxelcore.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Ground items use Minecraft's baked models and GROUND transforms. Each dropped
 * entity is drawn once: stack count no longer creates an oversized pile of copies.
 */
@SideOnly(Side.CLIENT)
public final class MinefadeDroppedItemRenderer extends RenderEntityItem {
    private final RenderItem itemRenderer;

    public MinefadeDroppedItemRenderer(RenderManager manager, RenderItem itemRenderer) {
        super(manager, itemRenderer);
        this.itemRenderer = itemRenderer;
        this.shadowSize = 0.10F;
        this.shadowOpaque = 0.55F;
    }

    @Override
    public boolean shouldRender(EntityItem entity, ICamera camera, double camX, double camY, double camZ) {
        if (ModConfig.ENABLE_ITEM_DISTANCE_CULLING) {
            Entity view = Minecraft.getMinecraft().getRenderViewEntity();
            if (view != null) {
                double radius = ModConfig.ITEM_RENDER_RADIUS;
                double dx = entity.posX - view.posX;
                double dy = entity.posY - view.posY;
                double dz = entity.posZ - view.posZ;
                if (dx * dx + dy * dy + dz * dz > radius * radius) return false;
            }
        }
        return super.shouldRender(entity, camera, camX, camY, camZ);
    }

    @Override
    public void doRender(EntityItem entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (!ModConfig.ENABLE_REALISTIC_ITEM_PHYSICS || !ModConfig.FLAT_GROUND_ITEMS || !entity.onGround) {
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
            return;
        }

        ItemStack stack = entity.getItem();
        if (stack.isEmpty()) return;

        // Bind the vanilla block atlas explicitly before drawing baked quads.
        bindEntityTexture(entity);
        WorldRenderStateGuard renderState = WorldRenderStateGuard.capture();
        GlStateManager.enableRescaleNormal();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.pushMatrix();
        try {
            IBakedModel model = itemRenderer.getItemModelWithOverrides(stack, entity.world, (EntityLivingBase) null);
            boolean is3d = model.isGui3d();
            float stableYaw = (entity.getEntityId() * 37.0F) % 360.0F;
            float groundScale = is3d ? 0.64F : 0.54F;

            GlStateManager.translate((float) x, (float) y + 0.012F, (float) z);
            GlStateManager.rotate(stableYaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.scale(groundScale, groundScale, groundScale);
            model.getItemCameraTransforms().applyTransform(ItemCameraTransforms.TransformType.GROUND);
            itemRenderer.renderItem(stack, model);
        } finally {
            GlStateManager.popMatrix();
            RenderHelper.disableStandardItemLighting();
            renderState.restore();
            // Keep the block atlas selected for Minecraft's remaining world renders.
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        }
        super.doRenderShadowAndFire(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityItem entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}