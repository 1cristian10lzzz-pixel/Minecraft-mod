package com.voxelcore.proxy;

import com.voxelcore.client.handler.ClientEventHandler;
import com.voxelcore.client.handler.ClientKeyHandler;
import com.voxelcore.client.handler.ClientModelRegistry;
import com.voxelcore.client.handler.GuiThemeHandler;
import com.voxelcore.client.handler.WaterEffectsHandler;
import com.voxelcore.client.handler.ConfigChangedHandler;
import com.voxelcore.client.handler.MinefadeDroppedItemRenderer;
import com.voxelcore.client.handler.NaturalContactHandler;
import com.voxelcore.client.handler.PlayerAnimationHandler;
import com.voxelcore.client.handler.SurfaceRealismHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        super.preInit();

        RenderingRegistry.registerEntityRenderingHandler(EntityItem.class, new IRenderFactory<EntityItem>() {
            @Override
            public Render<? super EntityItem> createRenderFor(RenderManager manager) {
                return new MinefadeDroppedItemRenderer(manager, Minecraft.getMinecraft().getRenderItem());
            }
        });

        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        MinecraftForge.EVENT_BUS.register(new PlayerAnimationHandler());
        FMLCommonHandler.instance().bus().register(new NaturalContactHandler());
        SurfaceRealismHandler surfaceRealism = new SurfaceRealismHandler();
        MinecraftForge.EVENT_BUS.register(surfaceRealism);
        FMLCommonHandler.instance().bus().register(surfaceRealism);
        MinecraftForge.EVENT_BUS.register(new ClientModelRegistry());
        MinecraftForge.EVENT_BUS.register(new GuiThemeHandler());

        WaterEffectsHandler waterEffects = new WaterEffectsHandler();
        MinecraftForge.EVENT_BUS.register(waterEffects);
        FMLCommonHandler.instance().bus().register(waterEffects);

        FMLCommonHandler.instance().bus().register(new ClientKeyHandler());
        FMLCommonHandler.instance().bus().register(new ConfigChangedHandler());
    }
}
