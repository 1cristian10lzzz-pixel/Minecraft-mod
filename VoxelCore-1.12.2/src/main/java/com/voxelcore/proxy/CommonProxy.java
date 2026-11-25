package com.voxelcore.proxy;

import com.voxelcore.server.DroppedItemPhysicsHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class CommonProxy {
    public void preInit() {
        DroppedItemPhysicsHandler handler = new DroppedItemPhysicsHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
    }

    public void init() {
        // Common/server initialization point.
    }
}
