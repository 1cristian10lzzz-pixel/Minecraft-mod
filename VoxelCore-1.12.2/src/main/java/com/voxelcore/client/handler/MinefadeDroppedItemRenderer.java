package com.voxelcore.client.handler;

import com.voxelcore.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Retains vanilla item models while laying resting drops flat and optionally culling distant drops. */
@SideOnly(Side.CLIENT)
public final class MinefadeDroppedItemRenderer extends RenderEntityItem {
    public MinefadeDroppedItemRenderer(RenderManager manager, RenderItem itemRenderer) {
        super(manager, itemRenderer);
        this.shadowSize = 0.12F;
    }

    @Override
    public boolean shouldBob() {
        return !ModConfig.ENABLE_REALISTIC_ITEM_PHYSICS || !ModConfig.FLAT_GROUND_ITEMS;
    }

    @Override
    public boolean shouldRender(EntityItem entity, ICamera camera, double camX, double camY, double camZ) {
        if (ModConfig.ENABLE_ITEM_DISTANCE_CULLING) {
            Entity view = Minecraft.getMinecraft().getRenderViewEntity();
            if (view != null) {
                double radius = ModConfig.ITEM_RENDER_RADIUS;
                if (entity.getDistanceSqToEntity(view) > radius * radius) return false;
            }
        }
        return super.shouldRender(entity, camera, camX, camY, camZ);
    }

    @Override
    public void doRender(EntityItem entity, double x, double y, double z, float yaw, float partialTicks) {
        if (!ModConfig.ENABLE_REALISTIC_ITEM_PHYSICS || !ModConfig.FLAT_GROUND_ITEMS || !entity.onGround) {
            super.doRender(entity, x, y, z, yaw, partialTicks);
            return;
        }
        float oldHover = entity.hoverStart;
        entity.hoverStart = -((float) entity.getAge() + partialTicks) / 20.0F;
        try {
            super.doRender(entity, x, y, z, yaw, partialTicks);
        } finally {
            entity.hoverStart = oldHover;
        }
    }
}
