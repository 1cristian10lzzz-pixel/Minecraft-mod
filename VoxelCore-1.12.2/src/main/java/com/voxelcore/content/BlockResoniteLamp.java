package com.voxelcore.content;

import com.voxelcore.VoxelCore;
import com.voxelcore.registry.ModContent;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;

public class BlockResoniteLamp extends Block {
    public BlockResoniteLamp() {
        super(Material.GLASS);
        setRegistryName(VoxelCore.MODID, "resonite_lamp");
        setUnlocalizedName(VoxelCore.MODID + ".resonite_lamp");
        setHardness(0.5F);
        setResistance(2.0F);
        setLightLevel(0.95F);
        setLightOpacity(1);
        setSoundType(SoundType.GLASS);
        setCreativeTab(ModContent.TAB);
    }
}
