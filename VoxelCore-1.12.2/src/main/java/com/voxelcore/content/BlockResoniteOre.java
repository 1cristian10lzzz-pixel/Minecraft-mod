package com.voxelcore.content;

import com.voxelcore.VoxelCore;
import com.voxelcore.registry.ModContent;
import net.minecraft.block.BlockOre;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;

import java.util.Random;

public class BlockResoniteOre extends BlockOre {
    public BlockResoniteOre() {
        super();
        setRegistryName(VoxelCore.MODID, "resonite_ore");
        setUnlocalizedName(VoxelCore.MODID + ".resonite_ore");
        setHardness(3.2F);
        setResistance(5.0F);
        setHarvestLevel("pickaxe", 2);
        setCreativeTab(ModContent.TAB);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return ModContent.RESONITE_CRYSTAL;
    }
}
