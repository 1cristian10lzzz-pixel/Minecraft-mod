package com.voxelcore.content;

import com.voxelcore.VoxelCore;
import com.voxelcore.registry.ModContent;
import net.minecraft.item.ItemHoe;

public class ItemResoniteHoe extends ItemHoe {
    public ItemResoniteHoe() {
        super(ModContent.RESONITE_TOOL_MATERIAL);
        setRegistryName(VoxelCore.MODID, "resonite_hoe");
        setTranslationKey(VoxelCore.MODID + ".resonite_hoe");
        setCreativeTab(ModContent.TAB);
    }
}
