package com.voxelcore.client.handler;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Lightweight contact feedback for vanilla vegetation and slime blocks.
 *
 * This is deliberately particle-only: it does not replace vanilla blocks,
 * alter collision/harvest rules, or run a costly per-block animation system.
 */
@SideOnly(Side.CLIENT)
public final class NaturalContactHandler {
    private double previousX;
    private double previousZ;
    private int tick;
    private int lastSlimeLandingTick = -100;
    private World trackedWorld;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player == null
                || event.player != Minecraft.getMinecraft().player) return;

        net.minecraft.entity.player.EntityPlayer player = event.player;
        World world = player.world;
        if (!world.isRemote || player.isDead) return;

        if (trackedWorld != world) {
            trackedWorld = world;
            previousX = player.posX;
            previousZ = player.posZ;
            tick = 0;
            lastSlimeLandingTick = -100;
        }
        tick++;

        double dx = player.posX - previousX;
        double dz = player.posZ - previousZ;
        double movementSq = dx * dx + dz * dz;
        BlockPos feet = new BlockPos(player.posX,
                player.getEntityBoundingBox().minY - 0.04D, player.posZ);
        BlockPos ground = feet.down();

        if (player.onGround && movementSq > 0.0009D && tick % 4 == 0) {
            spawnVegetationContact(world, player, ground);
        }

        // Vanilla slime blocks retain their normal bounce. Minefade adds only
        // a small contact burst after a meaningful fall; no extra force is added.
        IBlockState groundState = world.getBlockState(ground);
        if (player.onGround && groundState.getBlock() == Blocks.SLIME_BLOCK
                && player.fallDistance > 1.5F && tick - lastSlimeLandingTick > 8) {
            lastSlimeLandingTick = tick;
            int particleState = net.minecraft.block.Block.getStateId(groundState);
            for (int i = 0; i < 5; i++) {
                double ox = (world.rand.nextDouble() - 0.5D) * 0.7D;
                double oz = (world.rand.nextDouble() - 0.5D) * 0.7D;
                world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                        player.posX + ox, ground.getY() + 1.02D,
                        player.posZ + oz, ox * 0.12D, 0.04D,
                        oz * 0.12D, particleState);
            }
        }

        previousX = player.posX;
        previousZ = player.posZ;
    }

    private static void spawnVegetationContact(World world,
                                               net.minecraft.entity.player.EntityPlayer player,
                                               BlockPos ground) {
        // Search a tiny footprint around the feet, not the whole chunk.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = ground.add(dx, 0, dz);
                if (!world.isBlockLoaded(pos)) continue;
                IBlockState state = world.getBlockState(pos);
                if (!isVegetation(state.getBlock())) continue;

                int particleState = net.minecraft.block.Block.getStateId(state);
                double x = pos.getX() + 0.5D + (world.rand.nextDouble() - 0.5D) * 0.35D;
                double z = pos.getZ() + 0.5D + (world.rand.nextDouble() - 0.5D) * 0.35D;
                world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, x,
                        pos.getY() + 0.55D, z,
                        -player.motionX * 0.25D, 0.035D,
                        -player.motionZ * 0.25D, particleState);
                return; // one tiny contact puff per step
            }
        }
    }

    private static boolean isVegetation(net.minecraft.block.Block block) {
        return block == Blocks.TALLGRASS
                || block == Blocks.DOUBLE_PLANT
                || block == Blocks.LEAVES
                || block == Blocks.LEAVES2
                || block == Blocks.VINE
                || block == Blocks.DEADBUSH;
    }
}
