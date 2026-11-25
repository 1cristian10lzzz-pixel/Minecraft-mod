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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Uses Minecraft's baked item models and lighting, with a separate ground
 * transform so resting drops lie horizontally instead of hovering/spinning.
 */
@SideOnly(Side.CLIENT)
public final class MinefadeDroppedItemRenderer extends RenderEntityItem {
    private final RenderItem itemRenderer;
    private final Random random = new Random();

    public MinefadeDroppedItemRenderer(RenderManager manager, RenderItem itemRenderer) {
        super(manager, itemRenderer);
        this.itemRenderer = itemRenderer;
        this.shadowSize = 0.12F;
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
        Item item = stack.getItem();
        random.setSeed((long) (Item.getIdFromItem(item) + stack.getMetadata()));

        GlStateManager.enableRescaleNormal();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.pushMatrix();

        IBakedModel model = itemRenderer.getItemModelWithOverrides(stack, entity.world, (EntityLivingBase) null);
        int count = getModelCount(stack);
        boolean is3d = model.isGui3d();
        float stableYaw = (entity.getEntityId() * 37.0F) % 360.0F;

        // Flat items sit close to the ground and receive a stable random-looking yaw.
        GlStateManager.translate((float) x, (float) y + 0.025F, (float) z);
        GlStateManager.rotate(stableYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);

        for (int i = 0; i < count; i++) {
            GlStateManager.pushMatrix();
            if (i > 0) {
                float spread = is3d ? 0.12F : 0.07F;
                GlStateManager.translate((random.nextFloat() * 2.0F - 1.0F) * spread,
                        (random.nextFloat() * 2.0F - 1.0F) * spread * 0.2F,
                        (random.nextFloat() * 2.0F - 1.0F) * spread);
            }
            model.getItemCameraTransforms().applyTransform(ItemCameraTransforms.TransformType.GROUND);
            itemRenderer.renderItem(stack, model);
            GlStateManager.popMatrix();
        }

        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        super.doRenderShadowAndFire(entity, x, y, z, entityYaw, partialTicks);
    }

    private static int getModelCount(ItemStack stack) {
        int count = stack.getCount();
        if (count > 48) return 5;
        if (count > 32) return 4;
        if (count > 16) return 3;
        if (count > 1) return 2;
        return 1;
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityItem entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
