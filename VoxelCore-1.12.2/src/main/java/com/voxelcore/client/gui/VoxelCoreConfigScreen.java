package com.voxelcore.client.gui;

import com.voxelcore.VoxelCore;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;

import java.util.ArrayList;
import java.util.List;

public class VoxelCoreConfigScreen extends GuiConfig {
    public VoxelCoreConfigScreen(GuiScreen parent) {
        super(parent, getElements(), VoxelCore.MODID, false, false, "VoxelCore Settings");
    }

    private static List<net.minecraftforge.fml.client.config.IConfigElement> getElements() {
        List<net.minecraftforge.fml.client.config.IConfigElement> elements = new ArrayList<>();
        if (com.voxelcore.config.ModConfig.getConfiguration() != null) {
            for (String category : com.voxelcore.config.ModConfig.getConfiguration().getCategoryNames()) {
                elements.add(new ConfigElement(com.voxelcore.config.ModConfig.getConfiguration().getCategory(category)));
            }
        }
        return elements;
    }
}
