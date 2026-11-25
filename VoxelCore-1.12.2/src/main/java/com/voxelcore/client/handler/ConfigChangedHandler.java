package com.voxelcore.client.handler;

import com.voxelcore.VoxelCore;
import com.voxelcore.config.ModConfig;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ConfigChangedHandler {
    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (VoxelCore.MODID.equals(event.getModID())) {
            ModConfig.syncFromGui();
        }
    }
}
