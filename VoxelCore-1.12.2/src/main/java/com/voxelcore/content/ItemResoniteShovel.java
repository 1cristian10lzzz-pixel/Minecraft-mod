package com.voxelcore.content;

import com.voxelcore.VoxelCore;
import com.voxelcore.registry.ModContent;
import net.minecraft.item.ItemSpade;

public class ItemResoniteShovel extends ItemSpade {
    public ItemResoniteShovel() {
        super(ModContent.RESONITE_TOOL_MATERIAL);
        setRegistryName(VoxelCore.MODID, "resonite_shovel");
        setTranslationKey(VoxelCore.MODID + ".resonite_shovel");
        setCreativeTab(ModContent.TAB);
    }
}
