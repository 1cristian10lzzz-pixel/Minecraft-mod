package com.voxelcore.client.handler;

import com.voxelcore.config.ModConfig;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
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
    private boolean wasOnGround;
    private double previousMotionY;
    private float maxFallDistance;
    private World trackedWorld;

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() == null || !event.getWorld().isRemote) return;
        if (trackedWorld == event.getWorld()) {
            trackedWorld = null;
            previousX = previousZ = 0.0D;
            tick = 0;
            lastSlimeLandingTick = -100;
            wasOnGround = false;
            previousMotionY = 0.0D;
            maxFallDistance = 0.0F;
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player == null
                || event.player != Minecraft.getMinecraft().player) return;

        boolean vegetationEnabled = com.voxelcore.config.ModConfig.ENABLE_VEGETATION_CONTACT;
        boolean slimeEnabled = com.voxelcore.config.ModConfig.ENABLE_SLIME_CONTACT;
        net.minecraft.entity.player.EntityPlayer player = event.player;
        World world = player.world;
        if (!world.isRemote || player.isDead) return;

        if (trackedWorld != world) {
            trackedWorld = world;
            previousX = player.posX;
            previousZ = player.posZ;
            tick = 0;
            lastSlimeLandingTick = -100;
            wasOnGround = player.onGround;
            previousMotionY = player.motionY;
            maxFallDistance = 0.0F;
        }
        tick++;

        double dx = player.posX - previousX;
        double dz = player.posZ - previousZ;
        double movementSq = dx * dx + dz * dz;
        BlockPos feet = new BlockPos(player.posX,
                player.getEntityBoundingBox().minY - 0.04D, player.posZ);
        BlockPos ground = feet;

        if (vegetationEnabled && player.onGround
                && movementSq > 0.0009D && tick % 4 == 0) {
            spawnVegetationContact(world, player, ground);
        }

        boolean landedThisTick = player.onGround && !wasOnGround
                && previousMotionY < -0.08D;
        if (!player.onGround) {
            maxFallDistance = Math.max(maxFallDistance, player.fallDistance);
        }

        // Read the supporting block only on a real landing. This keeps the
        // per-tick contact handler from doing an unnecessary block lookup.
        if (landedThisTick && world.isBlockLoaded(ground)) {
            IBlockState groundState = world.getBlockState(ground);
            float impactDistance = Math.max(maxFallDistance, player.fallDistance);

            // A light, material-matched landing response; vanilla fall damage,
            // knockback, collisions and slime bounce are left untouched.
            if (ModConfig.ENABLE_LANDING_IMPACTS && impactDistance >= 1.35F
                    && groundState.getBlock() != Blocks.SLIME_BLOCK) {
                spawnLandingImpact(world, player, ground, groundState, impactDistance);
            }

            // Vanilla slime blocks retain their normal bounce. Minefade adds
            // only a small contact burst after the falling-to-ground transition.
            if (slimeEnabled && groundState.getBlock() == Blocks.SLIME_BLOCK
                    && tick - lastSlimeLandingTick > 8) {
                lastSlimeLandingTick = tick;
                int particleState = net.minecraft.block.Block.getStateId(groundState);
                int particleCount = Minecraft.getMinecraft().gameSettings.particleSetting == 2 ? 2 : 5;
                for (int i = 0; i < particleCount; i++) {
                    double ox = (world.rand.nextDouble() - 0.5D) * 0.7D;
                    double oz = (world.rand.nextDouble() - 0.5D) * 0.7D;
                    world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                            player.posX + ox, ground.getY() + 1.02D,
                            player.posZ + oz, ox * 0.12D, 0.04D,
                            oz * 0.12D, particleState);
                }
            }
        }
        if (player.onGround) maxFallDistance = 0.0F;

        previousX = player.posX;
        previousZ = player.posZ;
        wasOnGround = player.onGround;
        previousMotionY = player.motionY;
    }

    private static void spawnLandingImpact(World world, EntityPlayer player,
                                            BlockPos ground, IBlockState state,
                                            float fallDistance) {
        Material material = state.getMaterial();
        if (material != Material.SNOW && !material.isSolid()) return;

        int particleSetting = Minecraft.getMinecraft().gameSettings.particleSetting;
        int count = 2 + Math.min(5, Math.max(0, (int) ((fallDistance - 1.0F) * 1.35F)));
        if (particleSetting == 2) count = 1;
        else if (particleSetting == 1) count = Math.max(1, (count + 1) / 2);

        EnumParticleTypes particle = material == Material.SNOW
                ? EnumParticleTypes.SNOW_SHOVEL : EnumParticleTypes.BLOCK_CRACK;
        int stateId = net.minecraft.block.Block.getStateId(state);
        double y = ground.getY() + 1.015D;
        double speed = 0.018D + Math.min(0.045D, fallDistance * 0.004D);

        for (int i = 0; i < count; i++) {
            double angle = world.rand.nextDouble() * Math.PI * 2.0D;
            double radius = 0.10D + world.rand.nextDouble() * 0.24D;
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            double x = player.posX + Math.cos(angle) * radius;
            double z = player.posZ + Math.sin(angle) * radius;
            double vy = 0.012D + world.rand.nextDouble() * 0.024D;

            if (particle == EnumParticleTypes.SNOW_SHOVEL) {
                world.spawnParticle(particle, x, y, z, vx, vy, vz);
            } else {
                world.spawnParticle(particle, x, y, z, vx, vy, vz, stateId);
            }
        }
    }

    private static void spawnVegetationContact(World world,
                                               net.minecraft.entity.player.EntityPlayer player,
                                               BlockPos ground) {
        // Plants occupy the block above the supporting ground block. Check the
        // player's lower two body blocks instead of looking inside the soil.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 1; dy <= 2; dy++) {
                    BlockPos pos = ground.add(dx, dy, dz);
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
