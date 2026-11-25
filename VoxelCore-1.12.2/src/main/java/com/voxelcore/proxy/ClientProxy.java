package com.voxelcore.proxy;

import com.voxelcore.client.handler.ClientEventHandler;
import com.voxelcore.client.handler.ClientKeyHandler;
import com.voxelcore.client.handler.ClientModelRegistry;
import com.voxelcore.client.handler.GuiThemeHandler;
import com.voxelcore.client.handler.ConfigChangedHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        MinecraftForge.EVENT_BUS.register(new ClientModelRegistry());
        MinecraftForge.EVENT_BUS.register(new GuiThemeHandler());
        FMLCommonHandler.instance().bus().register(new ClientKeyHandler());
        FMLCommonHandler.instance().bus().register(new ConfigChangedHandler());
    }
}
