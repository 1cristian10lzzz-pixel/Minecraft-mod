package com.voxelcore.client.handler;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Small, client-only material feedback that makes existing vanilla surfaces
 * feel less static without replacing their models or textures.
 */
@SideOnly(Side.CLIENT)
public final class SurfaceRealismHandler {
    private final Map<Long, Long> lastMiningImpact = new HashMap<Long, Long>();
    private int stepTicks;
    private double previousX;
    private double previousZ;
    private World trackedWorld;

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getWorld() == null || !event.getWorld().isRemote
                || event.getEntityPlayer() != Minecraft.getMinecraft().player) return;

        World world = event.getWorld();
        BlockPos pos = event.getPos();
        if (pos == null || !world.isBlockLoaded(pos)) return;

        IBlockState state = world.getBlockState(pos);
        Material material = state.getMaterial();
        if (!isHardSurface(material)) return;

        long now = world.getTotalWorldTime();
        long key = pos.toLong();
        Long previous = lastMiningImpact.get(key);
        if (previous != null && now - previous.longValue() < 2L) return;
        lastMiningImpact.put(key, Long.valueOf(now));

        if (lastMiningImpact.size() > 256) {
            Iterator<Map.Entry<Long, Long>> it = lastMiningImpact.entrySet().iterator();
            while (it.hasNext()) {
                if (now - it.next().getValue().longValue() > 8L) it.remove();
            }
            if (lastMiningImpact.size() > 256) lastMiningImpact.clear();
        }

        EnumFacing face = event.getFace();
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        if (face != null) {
            x += face.getXOffset() * 0.49D;
            y += face.getYOffset() * 0.49D;
            z += face.getZOffset() * 0.49D;
        }

        int stateId = net.minecraft.block.Block.getStateId(state);
        int count = material == Material.ROCK ? 7 : 5;
        for (int i = 0; i < count; i++) {
            double vx = (world.rand.nextDouble() - 0.5D) * 0.10D;
            double vy = 0.025D + world.rand.nextDouble() * 0.075D;
            double vz = (world.rand.nextDouble() - 0.5D) * 0.10D;
            if (face != null) {
                vx += face.getXOffset() * 0.045D;
                vy += face.getYOffset() * 0.045D;
                vz += face.getZOffset() * 0.045D;
            }
            world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                    x, y, z, vx, vy, vz, stateId);
        }
    }

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
            stepTicks = 0;
        }
        stepTicks++;

        double dx = player.posX - previousX;
        double dz = player.posZ - previousZ;
        double movementSq = dx * dx + dz * dz;
        if (player.onGround && !player.isSneaking() && movementSq > 0.0025D
                && stepTicks >= 6) {
            BlockPos underFoot = new BlockPos(player.posX,
                    player.getEntityBoundingBox().minY - 0.05D, player.posZ);
            if (world.isBlockLoaded(underFoot)) {
                IBlockState state = world.getBlockState(underFoot);
                Material material = state.getMaterial();
                if (isDustySurface(material)) {
                    spawnFootstepDust(world, player, underFoot, state);
                    stepTicks = 0;
                }
            }
        }

        previousX = player.posX;
        previousZ = player.posZ;
    }

    private static boolean isHardSurface(Material material) {
        return material == Material.ROCK || material == Material.IRON
                || material == Material.ANVIL || material == Material.GLASS
                || material == Material.ICE;
    }

    private static boolean isDustySurface(Material material) {
        return material == Material.ROCK || material == Material.GROUND
                || material == Material.GRASS || material == Material.SAND
                || material == Material.CLAY || material == Material.SNOW
                || material == Material.IRON;
    }

    private static void spawnFootstepDust(World world,
                                          net.minecraft.entity.player.EntityPlayer player,
                                          BlockPos pos, IBlockState state) {
        int stateId = net.minecraft.block.Block.getStateId(state);
        double side = player.ticksExisted % 2 == 0 ? -0.13D : 0.13D;
        double x = player.posX + side;
        double z = player.posZ;
        int count = state.getMaterial() == Material.SAND
                || state.getMaterial() == Material.GROUND ? 2 : 1;
        for (int i = 0; i < count; i++) {
            world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                    x, pos.getY() + 1.015D, z,
                    -player.motionX * 0.18D + (world.rand.nextDouble() - 0.5D) * 0.025D,
                    0.012D + world.rand.nextDouble() * 0.018D,
                    -player.motionZ * 0.18D + (world.rand.nextDouble() - 0.5D) * 0.025D,
                    stateId);
        }
    }
}
