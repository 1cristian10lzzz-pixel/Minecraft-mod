package com.voxelcore.content;

import com.voxelcore.VoxelCore;
import com.voxelcore.registry.ModContent;
import net.minecraft.item.ItemPickaxe;

public class ItemResonitePickaxe extends ItemPickaxe {
    public ItemResonitePickaxe() {
        super(ModContent.RESONITE_TOOL_MATERIAL);
        setRegistryName(VoxelCore.MODID, "resonite_pickaxe");
        setTranslationKey(VoxelCore.MODID + ".resonite_pickaxe");
        setCreativeTab(ModContent.TAB);
    }
}
