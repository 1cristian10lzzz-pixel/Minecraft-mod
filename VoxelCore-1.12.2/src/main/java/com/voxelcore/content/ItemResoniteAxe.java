package com.voxelcore.content;

import com.voxelcore.VoxelCore;
import com.voxelcore.registry.ModContent;
import net.minecraft.item.ItemAxe;

public class ItemResoniteAxe extends ItemAxe {
    public ItemResoniteAxe() {
        super(ModContent.RESONITE_TOOL_MATERIAL, 8.0F, -3.0F);
        setRegistryName(VoxelCore.MODID, "resonite_axe");
        setUnlocalizedName(VoxelCore.MODID + ".resonite_axe");
        setCreativeTab(ModContent.TAB);
    }
}
