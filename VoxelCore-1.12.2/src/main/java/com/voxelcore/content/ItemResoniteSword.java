package com.voxelcore.content;

import com.voxelcore.VoxelCore;
import com.voxelcore.registry.ModContent;
import net.minecraft.item.ItemSword;

public class ItemResoniteSword extends ItemSword {
    public ItemResoniteSword() {
        super(ModContent.RESONITE_TOOL_MATERIAL);
        setRegistryName(VoxelCore.MODID, "resonite_sword");
        setTranslationKey(VoxelCore.MODID + ".resonite_sword");
        setCreativeTab(ModContent.TAB);
    }
}
