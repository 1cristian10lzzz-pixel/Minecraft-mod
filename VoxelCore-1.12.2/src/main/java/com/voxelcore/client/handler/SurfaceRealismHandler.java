package com.voxelcore.client.handler;

import com.voxelcore.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Client-only surface feedback for Minefade.
 *
 * Mining impacts use their own option, independent of procedural surface
 * grains. Footsteps are distance-driven (not timer-driven), alternate feet,
 * and emit particles derived from the block currently under the player.
 */
@SideOnly(Side.CLIENT)
public final class SurfaceRealismHandler {
    private static final int MAX_MINING_IMPACTS = 256;
    private final Map<Long, Long> lastMiningImpact = new HashMap<Long, Long>();

    private double previousX;
    private double previousZ;
    private double distanceSinceStep;
    private boolean leftFoot = true;
    private World trackedWorld;
    private World impactWorld;

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() == null || !event.getWorld().isRemote) return;
        if (trackedWorld == event.getWorld()) {
            trackedWorld = null;
            previousX = previousZ = 0.0D;
            distanceSinceStep = 0.0D;
            leftFoot = true;
        }
        if (impactWorld == event.getWorld()) {
            impactWorld = null;
            lastMiningImpact.clear();
        }
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!ModConfig.ENABLE_MATERIAL_IMPACTS || event.getWorld() == null
                || !event.getWorld().isRemote
                || event.getEntityPlayer() != Minecraft.getMinecraft().player) return;

        World world = event.getWorld();
        if (impactWorld != world) {
            lastMiningImpact.clear();
            impactWorld = world;
        }

        BlockPos pos = event.getPos();
        if (pos == null || !world.isBlockLoaded(pos)) return;

        IBlockState state = world.getBlockState(pos);
        Material material = state.getMaterial();
        if (!isHardSurface(material)) return;

        long now = world.getTotalWorldTime();
        long key = pos.toLong();
        Long previous = lastMiningImpact.get(Long.valueOf(key));
        if (previous != null && now < previous.longValue()) {
            lastMiningImpact.clear();
            previous = null;
        }
        if (previous != null && now - previous.longValue() < 2L) return;
        lastMiningImpact.put(Long.valueOf(key), Long.valueOf(now));

        if (lastMiningImpact.size() > MAX_MINING_IMPACTS) {
            Iterator<Map.Entry<Long, Long>> iterator = lastMiningImpact.entrySet().iterator();
            while (iterator.hasNext()) {
                if (now - iterator.next().getValue().longValue() > 8L) iterator.remove();
            }
            if (lastMiningImpact.size() > MAX_MINING_IMPACTS) lastMiningImpact.clear();
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

        int particleSetting = Minecraft.getMinecraft().gameSettings.particleSetting;
        int count = material == Material.ROCK ? (particleSetting == 2 ? 2 : 6)
                : (particleSetting == 2 ? 1 : 4);
        int stateId = Block.getStateId(state);
        for (int i = 0; i < count; i++) {
            double spread = particleSetting == 2 ? 0.065D : 0.10D;
            double vx = (world.rand.nextDouble() - 0.5D) * spread;
            double vy = 0.018D + world.rand.nextDouble() * 0.075D;
            double vz = (world.rand.nextDouble() - 0.5D) * spread;
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
            distanceSinceStep = 0.0D;
            leftFoot = true;
        }

        double dx = player.posX - previousX;
        double dz = player.posZ - previousZ;
        double movementSq = dx * dx + dz * dz;
        previousX = player.posX;
        previousZ = player.posZ;

        // Local rain impacts complement vanilla weather particles on exposed
        // ground, while the water renderer handles impacts on open water.
        if (ModConfig.ENABLE_RAIN_RIPPLES && world.isRaining()
                && player.ticksExisted % 8 == 0
                && !player.isInsideOfMaterial(Material.WATER)
                && !player.isInsideOfMaterial(Material.LAVA)) {
            spawnRainImpact(world, player);
        }

        if (!ModConfig.ENABLE_MATERIAL_FOOTSTEPS || !player.onGround
                || player.isSneaking() || player.isInsideOfMaterial(Material.WATER)
                || player.isInsideOfMaterial(Material.LAVA) || movementSq < 0.0004D) {
            distanceSinceStep = 0.0D;
            return;
        }

        BlockPos underFoot = new BlockPos(player.posX,
                player.getEntityBoundingBox().minY - 0.06D, player.posZ);
        if (!world.isBlockLoaded(underFoot)) {
            distanceSinceStep = 0.0D;
            return;
        }

        IBlockState state = world.getBlockState(underFoot);
        if (!isFootstepSurface(state)) {
            distanceSinceStep = 0.0D;
            return;
        }

        double strideLength = player.isSprinting() ? 0.70D : 0.52D;
        distanceSinceStep = Math.min(strideLength * 1.25D,
                distanceSinceStep + Math.sqrt(movementSq));
        if (distanceSinceStep < strideLength) return;

        spawnFootstepParticles(world, player, underFoot, state, leftFoot);
        leftFoot = !leftFoot;
        distanceSinceStep = Math.max(0.0D, distanceSinceStep - strideLength);
    }

    private static void spawnRainImpact(World world,
                                        net.minecraft.entity.player.EntityPlayer player) {
        BlockPos underPlayer = new BlockPos(player.posX,
                player.getEntityBoundingBox().minY - 0.06D, player.posZ);
        if (!world.isBlockLoaded(underPlayer)) return;
        BlockPos impact = underPlayer.up();
        if (!world.isRainingAt(impact)) return;

        IBlockState ground = world.getBlockState(underPlayer);
        if (!ground.getMaterial().isSolid() || ground.getMaterial() == Material.WATER
                || ground.getMaterial() == Material.LAVA) return;

        int count = Minecraft.getMinecraft().gameSettings.particleSetting == 2 ? 1 : 2;
        for (int i = 0; i < count; i++) {
            double x = player.posX + (world.rand.nextDouble() - 0.5D) * 0.65D;
            double z = player.posZ + (world.rand.nextDouble() - 0.5D) * 0.65D;
            world.spawnParticle(EnumParticleTypes.WATER_SPLASH,
                    x, underPlayer.getY() + 1.012D, z,
                    (world.rand.nextDouble() - 0.5D) * 0.025D,
                    0.012D + world.rand.nextDouble() * 0.018D,
                    (world.rand.nextDouble() - 0.5D) * 0.025D);
        }
    }

    private static boolean isHardSurface(Material material) {
        return material == Material.ROCK || material == Material.IRON
                || material == Material.ANVIL || material == Material.GLASS
                || material == Material.ICE;
    }

    private static boolean isFootstepSurface(IBlockState state) {
        Material material = state.getMaterial();
        Block block = state.getBlock();
        return material == Material.ROCK || material == Material.GROUND
                || material == Material.GRASS || material == Material.SAND
                || material == Material.CLAY || material == Material.SNOW
                || block == Blocks.LEAVES || block == Blocks.LEAVES2
                || block == Blocks.VINE;
    }

    private static void spawnFootstepParticles(World world,
                                               net.minecraft.entity.player.EntityPlayer player,
                                               BlockPos pos, IBlockState state, boolean left) {
        Material material = state.getMaterial();
        int particleSetting = Minecraft.getMinecraft().gameSettings.particleSetting;
        double side = left ? -0.13D : 0.13D;
        double yaw = Math.toRadians(player.rotationYaw);
        double x = player.posX + Math.cos(yaw) * side;
        double z = player.posZ + Math.sin(yaw) * side;
        double y = pos.getY() + 1.012D;

        if (material == Material.SNOW) {
            int count = particleSetting == 2 ? 1 : 2;
            for (int i = 0; i < count; i++) {
                world.spawnParticle(EnumParticleTypes.SNOW_SHOVEL,
                        x, y, z,
                        -player.motionX * 0.12D + (world.rand.nextDouble() - 0.5D) * 0.025D,
                        0.012D + world.rand.nextDouble() * 0.020D,
                        -player.motionZ * 0.12D + (world.rand.nextDouble() - 0.5D) * 0.025D);
            }
            return;
        }

        int count;
        if (material == Material.SAND || material == Material.GROUND) {
            count = particleSetting == 2 ? 1 : 3;
        } else if (state.getBlock() == Blocks.LEAVES
                || state.getBlock() == Blocks.LEAVES2 || state.getBlock() == Blocks.VINE) {
            count = 1;
        } else {
            count = particleSetting == 2 ? 1 : 2;
        }

        int stateId = Block.getStateId(state);
        for (int i = 0; i < count; i++) {
            double jitterX = (world.rand.nextDouble() - 0.5D) * 0.035D;
            double jitterZ = (world.rand.nextDouble() - 0.5D) * 0.035D;
            world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                    x + jitterX, y, z + jitterZ,
                    -player.motionX * 0.18D + jitterX,
                    0.010D + world.rand.nextDouble() * 0.018D,
                    -player.motionZ * 0.18D + jitterZ,
                    stateId);
        }
    }
}
