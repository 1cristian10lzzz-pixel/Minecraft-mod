package com.voxelcore.world;

import com.voxelcore.config.ModConfig;
import com.voxelcore.registry.ModContent;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

/** Generates the sample ore only during normal server-side chunk generation. */
public class ResoniteWorldGenerator implements IWorldGenerator {
    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (world.isRemote || world.provider.getDimension() != 0 || !ModConfig.GENERATE_RESONITE_ORE) return;

        int attempts = clamp(ModConfig.ORE_VEINS_PER_CHUNK, 0, 16);
        int veinSize = clamp(ModConfig.ORE_VEIN_SIZE, 1, 12);
        int minY = clamp(ModConfig.ORE_MIN_Y, 1, 254);
        int maxY = clamp(ModConfig.ORE_MAX_Y, minY, 255);
        if (attempts == 0) return;

        WorldGenMinable generator = new WorldGenMinable(ModContent.RESONITE_ORE.getDefaultState(), veinSize,
                state -> state.getBlock() == Blocks.STONE);
        int heightRange = maxY - minY + 1;

        for (int i = 0; i < attempts; i++) {
            int x = (chunkX << 4) + random.nextInt(16);
            int y = minY + random.nextInt(heightRange);
            int z = (chunkZ << 4) + random.nextInt(16);
            generator.generate(world, random, new BlockPos(x, y, z));
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
