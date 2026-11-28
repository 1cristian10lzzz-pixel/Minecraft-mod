package com.voxelcore.client.handler;

import com.voxelcore.config.ModConfig;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Ambient water behaviour for Forge 1.12.2. The old expanding ring renderer is
 * removed: WaterSurfaceRenderer now renders a calm custom surface with biome
 * depth colour, sky reflections, sunlight highlights and shoreline foam.
 * Splashes and bubbles are local contact particles, not water-surface waves.
 * Vanilla water blocks, flow, collision and server state remain untouched.
 */
@SideOnly(Side.CLIENT)
public class WaterEffectsHandler {
    private static final Random RANDOM = new Random();

    private double previousX;
    private double previousZ;
    private boolean previousInWater;
    private int tickCounter;
    private int lastSurfaceScanTick = -100;
    private int qualitySampleTicks;
    private int slowSamples;
    private int recoverySamples;
    private boolean reducedQuality;
    private BlockPos cachedSurface;
    private World trackedWorld;
    private int lastLookupX = Integer.MIN_VALUE;
    private int lastLookupY = Integer.MIN_VALUE;
    private int lastLookupZ = Integer.MIN_VALUE;
    private final BlockPos.MutableBlockPos depthProbe = new BlockPos.MutableBlockPos();
    private World depthWorld;
    private int depthX = Integer.MIN_VALUE;
    private int depthY = Integer.MIN_VALUE;
    private int depthZ = Integer.MIN_VALUE;
    private long depthSampleTick = Long.MIN_VALUE;
    private float cachedUnderwaterDepth = 4.0F;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (!ModConfig.ENABLE_WATER_EFFECTS || mc.world == null || mc.player == null) {
            resetTracking();
            return;
        }

        EntityPlayer player = mc.player;
        World world = mc.world;
        if (trackedWorld != world) {
            clearAll();
            lastLookupX = lastLookupY = lastLookupZ = Integer.MIN_VALUE;
            trackedWorld = world;
            cachedSurface = null;
            lastSurfaceScanTick = -100;
            previousInWater = false;
            previousX = player.posX;
            previousZ = player.posZ;
        }

        tickCounter++;
        updateAdaptiveQuality();
        WaterSurfaceRenderer.updateCache(mc, tickCounter, reducedQuality);

        int scanInterval = reducedQuality ? 24 : 12;
        int lookupX = MathHelper.floor(player.posX);
        int lookupY = MathHelper.floor(player.posY);
        int lookupZ = MathHelper.floor(player.posZ);
        boolean movedToAnotherArea = lastLookupX == Integer.MIN_VALUE
                || Math.abs(lookupX - lastLookupX) >= 3
                || Math.abs(lookupY - lastLookupY) >= 6
                || Math.abs(lookupZ - lastLookupZ) >= 3;
        // Cache null results too. Without this guard, dry land triggered a full
        // nearby-water scan every tick because cachedSurface stayed null.
        if (movedToAnotherArea || tickCounter - lastSurfaceScanTick >= scanInterval) {
            cachedSurface = findNearbyWaterSurface(world, player.posX, player.posY, player.posZ, 1);
            lastSurfaceScanTick = tickCounter;
            lastLookupX = lookupX;
            lastLookupY = lookupY;
            lastLookupZ = lookupZ;
        }

        boolean ridingBoat = player.getRidingEntity() instanceof EntityBoat;
        boolean inWater = player.isInsideOfMaterial(Material.WATER) || ridingBoat;
        BlockPos surface = cachedSurface;

        if (inWater) {
            // Keep the transition state even when the local surface scan has
            // no result. This avoids a false entry splash several ticks after
            // the player was already submerged.
            if (surface != null) {
                double dx = player.posX - previousX;
                double dz = player.posZ - previousZ;
                double movementSq = dx * dx + dz * dz;

                if (!previousInWater && ModConfig.ENABLE_WATER_SPLASHES) {
                    spawnEntrySplash(world, player, surface);
                }

                if (movementSq > 0.0008D) {
                    int wakeInterval = reducedQuality ? 8 : 4;
                    if (ModConfig.ENABLE_WATER_SPLASHES && tickCounter % wakeInterval == 0) {
                        double surfaceY = getWaterSurfaceY(world, surface) + 0.012D;
                        world.spawnParticle(EnumParticleTypes.WATER_WAKE, player.posX, surfaceY,
                                player.posZ, -dx * 2.0D, 0.0D, -dz * 2.0D);
                        if (movementSq > 0.0018D) {
                            world.spawnParticle(EnumParticleTypes.WATER_SPLASH, player.posX, surfaceY,
                                    player.posZ, dx * 0.7D, 0.025D, dz * 0.7D);
                        }
                    }
                }
            }
            previousInWater = true;
        } else {
            if (previousInWater && surface != null && ModConfig.ENABLE_WATER_SPLASHES) {
                spawnExitSplash(world, player, surface);
            }
            previousInWater = false;
        }

        // Small rising bubbles improve the underwater volume without screen overlays.
        if (player.isInsideOfMaterial(Material.WATER) && tickCounter % (reducedQuality ? 9 : 6) == 0) {
            int count = mc.gameSettings.particleSetting == 2 ? 1 : (reducedQuality ? 1 : 2);
            for (int i = 0; i < count; i++) {
                double x = player.posX + (RANDOM.nextDouble() - 0.5D) * 0.75D;
                double y = player.posY + RANDOM.nextDouble() * Math.max(0.3D, player.height * 0.8D);
                double z = player.posZ + (RANDOM.nextDouble() - 0.5D) * 0.75D;
                world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, x, y, z,
                        player.motionX * 0.15D, 0.025D + RANDOM.nextDouble() * 0.025D,
                        player.motionZ * 0.15D);
            }
        }

        int rainInterval = reducedQuality ? 20 : 12;
        if (surface != null && ModConfig.ENABLE_RAIN_RIPPLES && world.isRaining()
                && tickCounter % rainInterval == 0 && RANDOM.nextInt(4) == 0) {
            BlockPos rainWater = findNearbyWaterSurface(world,
                    surface.getX() + RANDOM.nextInt(5) - 2, surface.getY(),
                    surface.getZ() + RANDOM.nextInt(5) - 2, 1);
            if (rainWater != null && world.canSeeSky(rainWater.up())) {
                world.spawnParticle(EnumParticleTypes.WATER_SPLASH,
                        rainWater.getX() + 0.5D, getWaterSurfaceY(world, rainWater) + 0.012D,
                        rainWater.getZ() + 0.5D,
                        (RANDOM.nextDouble() - 0.5D) * 0.035D, 0.015D,
                        (RANDOM.nextDouble() - 0.5D) * 0.035D);
            }
        }

        int mistInterval = reducedQuality ? 40 : 24;
        if (ModConfig.ENABLE_WATERFALL_MIST && surface != null && tickCounter % mistInterval == 0) {
            spawnNearbyWaterfallMist(world, surface);
        }

        previousX = player.posX;
        previousZ = player.posZ;
    }

    /**
     * Hysteresis keeps optional effects from changing quality every few frames.
     * The water surface renderer uses a coarser mesh when this flag is active.
     */
    private void updateAdaptiveQuality() {
        if (!ModConfig.ENABLE_ADAPTIVE_WATER_QUALITY) {
            reducedQuality = false;
            slowSamples = 0;
            recoverySamples = 0;
            qualitySampleTicks = 0;
            return;
        }

        if (++qualitySampleTicks < 40) return;
        qualitySampleTicks = 0;
        int fps = Minecraft.getDebugFPS();
        if (fps <= 0) return;

        if (fps < 28) {
            slowSamples++;
            recoverySamples = 0;
            if (slowSamples >= 2) reducedQuality = true;
        } else if (fps > 48) {
            recoverySamples++;
            slowSamples = 0;
            if (recoverySamples >= 3) reducedQuality = false;
        } else {
            slowSamples = 0;
            recoverySamples = 0;
        }
    }

    private static BlockPos findNearbyWaterSurface(World world, double x, double y,
                                                    double z, int radius) {
        int originX = MathHelper.floor(x);
        int originY = MathHelper.floor(y);
        int originZ = MathHelper.floor(z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int bestX = 0;
        int bestY = 0;
        int bestZ = 0;
        double bestDistance = Double.MAX_VALUE;
        boolean found = false;

        for (int dy = -8; dy <= 8; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int blockX = originX + dx;
                    int blockY = originY + dy;
                    int blockZ = originZ + dz;
                    if (blockY < 0 || blockY >= 255) continue;
                    pos.setPos(blockX, blockY, blockZ);
                    if (!world.isBlockLoaded(pos)) continue;
                    IBlockState waterState = world.getBlockState(pos);
                    if (waterState.getMaterial() != Material.WATER) continue;

                    pos.setY(blockY + 1);
                    if (!world.isBlockLoaded(pos)
                            || world.getBlockState(pos).getMaterial() == Material.WATER) continue;

                    pos.setPos(blockX, blockY, blockZ);
                    double surfaceY = BlockLiquid.getLiquidHeight(waterState, world, pos);
                    double cx = blockX + 0.5D - x;
                    double cz = blockZ + 0.5D - z;
                    double cy = surfaceY - y;
                    double distance = cx * cx + cz * cz + cy * cy * 0.5D;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestX = blockX;
                        bestY = blockY;
                        bestZ = blockZ;
                        found = true;
                    }
                }
            }
        }
        return found ? new BlockPos(bestX, bestY, bestZ) : null;
    }

    /**
     * Uses the level-aware vanilla fluid height, not a fixed 0.9-block guess.
     * Flowing water (levels 1-7), source blocks and falling water therefore
     * place wakes and splashes on the actual visible surface.
     */
    private static double getWaterSurfaceY(World world, BlockPos surface) {
        if (surface == null || !world.isBlockLoaded(surface)) return 0.0D;
        IBlockState state = world.getBlockState(surface);
        if (state.getMaterial() != Material.WATER) return surface.getY() + 0.9D;
        return BlockLiquid.getLiquidHeight(state, world, surface);
    }

    private static void spawnExitSplash(World world, EntityPlayer player, BlockPos surface) {
        if (surface == null || !world.isBlockLoaded(surface)) return;
        double surfaceY = getWaterSurfaceY(world, surface);
        double dx = player.posX - (surface.getX() + 0.5D);
        double dz = player.posZ - (surface.getZ() + 0.5D);
        if (dx * dx + dz * dz > 2.25D
                || Math.abs(player.posY - surfaceY) > 1.6D) return;

        int count = Minecraft.getMinecraft().gameSettings.particleSetting == 2 ? 1 : 3;
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
            double speed = 0.018D + RANDOM.nextDouble() * 0.035D;
            world.spawnParticle(EnumParticleTypes.WATER_SPLASH,
                    surface.getX() + 0.5D + (RANDOM.nextDouble() - 0.5D) * 0.32D,
                    surfaceY + 0.018D,
                    surface.getZ() + 0.5D + (RANDOM.nextDouble() - 0.5D) * 0.32D,
                    Math.cos(angle) * speed, 0.025D + RANDOM.nextDouble() * 0.025D,
                    Math.sin(angle) * speed);
        }
    }

    private static void spawnEntrySplash(World world, EntityPlayer player, BlockPos surface) {
        double y = getWaterSurfaceY(world, surface) + 0.018D;
        int particleCount = Minecraft.getMinecraft().gameSettings.particleSetting == 2 ? 3 : 7;
        for (int i = 0; i < particleCount; i++) {
            double ox = (RANDOM.nextDouble() - 0.5D) * 0.65D;
            double oz = (RANDOM.nextDouble() - 0.5D) * 0.65D;
            world.spawnParticle(EnumParticleTypes.WATER_SPLASH, player.posX + ox, y,
                    player.posZ + oz, ox * 0.12D, 0.10D + RANDOM.nextDouble() * 0.08D,
                    oz * 0.12D);
        }
    }

    private void spawnNearbyWaterfallMist(World world, BlockPos centre) {
        int radius = reducedQuality ? 1 : 2;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -2; dy <= 0; dy++) {
                    int x = centre.getX() + dx;
                    int y = centre.getY() + dy;
                    int z = centre.getZ() + dz;
                    if (y < 1 || y >= 255) continue;
                    pos.setPos(x, y, z);
                    if (!world.isBlockLoaded(pos)
                            || world.getBlockState(pos).getMaterial() != Material.WATER) continue;

                    pos.setY(y + 1);
                    if (!world.isBlockLoaded(pos)
                            || world.getBlockState(pos).getMaterial() != Material.WATER) continue;

                    pos.setY(y - 1);
                    if (!world.isBlockLoaded(pos)
                            || !world.getBlockState(pos).getMaterial().isSolid()) continue;

                    double px = x + 0.5D;
                    double py = y + 0.25D;
                    double pz = z + 0.5D;
                    int particles = reducedQuality ? 1 : 2;
                    for (int i = 0; i < particles; i++) {
                        world.spawnParticle(EnumParticleTypes.CLOUD,
                                px + (RANDOM.nextDouble() - 0.5D) * 0.7D,
                                py, pz + (RANDOM.nextDouble() - 0.5D) * 0.7D,
                                (RANDOM.nextDouble() - 0.5D) * 0.025D, 0.015D,
                                (RANDOM.nextDouble() - 0.5D) * 0.025D);
                    }
                    return;
                }
            }
        }
    }

    private void resetTracking() {
        clearAll();
        trackedWorld = null;
        cachedSurface = null;
        lastSurfaceScanTick = -100;
        lastLookupX = lastLookupY = lastLookupZ = Integer.MIN_VALUE;
        depthWorld = null;
        depthX = depthY = depthZ = Integer.MIN_VALUE;
        depthSampleTick = Long.MIN_VALUE;
        cachedUnderwaterDepth = 4.0F;
        previousInWater = false;
        previousX = 0.0D;
        previousZ = 0.0D;
        slowSamples = 0;
        recoverySamples = 0;
        qualitySampleTicks = 0;
        reducedQuality = false;
    }

    public static void clearAll() {
        WaterSurfaceRenderer.clear();
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        WaterSurfaceRenderer.render(event, reducedQuality);
    }

    @SubscribeEvent
    public void onFogColors(EntityViewRenderEvent.FogColors event) {
        if (ModConfig.ENABLE_LAVA_EFFECTS && event.getEntity() != null
                && event.getEntity().isInsideOfMaterial(Material.LAVA)) {
            Minecraft mc = Minecraft.getMinecraft();
            float pulse = mc.world == null ? 0.0F
                    : (float) (0.5D + 0.5D * Math.sin(mc.world.getTotalWorldTime() * 0.035D));
            float lavaRed = 0.52F + pulse * 0.10F;
            float lavaGreen = 0.105F + pulse * 0.045F;
            float lavaBlue = 0.025F + pulse * 0.012F;
            // Add a warm reflected-light tint while preserving most of vanilla's
            // lava fog contribution so visibility and the vanilla danger cue remain.
            event.setRed(event.getRed() * 0.58F + lavaRed * 0.42F);
            event.setGreen(event.getGreen() * 0.72F + lavaGreen * 0.28F);
            event.setBlue(event.getBlue() * 0.84F + lavaBlue * 0.16F);
            return;
        }

        if (!ModConfig.ENABLE_WATER_EFFECTS || !ModConfig.ENABLE_UNDERWATER_TINT) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null
                || !mc.player.isInsideOfMaterial(Material.WATER)) return;

        BlockPos pos = new BlockPos(mc.player.posX, mc.player.posY, mc.player.posZ);
        int waterColor = mc.world.getBiome(pos).getWaterColorMultiplier();
        float waterRed = ((waterColor >> 16) & 255) / 255.0F;
        float waterGreen = ((waterColor >> 8) & 255) / 255.0F;
        float waterBlue = (waterColor & 255) / 255.0F;
        float depth = getUnderwaterDepth(mc.player);

        float absorbedRed = waterRed * (0.64F - Math.min(0.28F, depth * 0.012F));
        float absorbedGreen = waterGreen * (0.78F - Math.min(0.14F, depth * 0.006F));
        float absorbedBlue = waterBlue * (0.88F - Math.min(0.08F, depth * 0.003F));

        event.setRed(event.getRed() * 0.16F + absorbedRed * 0.84F);
        event.setGreen(event.getGreen() * 0.16F + absorbedGreen * 0.84F);
        event.setBlue(event.getBlue() * 0.16F + absorbedBlue * 0.84F);
    }

    @SubscribeEvent
    public void onFogDensity(EntityViewRenderEvent.FogDensity event) {
        if (!ModConfig.ENABLE_WATER_EFFECTS || !ModConfig.ENABLE_UNDERWATER_TINT
                || event.getEntity() == null
                || !event.getEntity().isInsideOfMaterial(Material.WATER)) return;

        float depth = getUnderwaterDepth(Minecraft.getMinecraft().player);
        float density = MathHelper.clamp(0.014F + Math.min(24.0F, depth) * 0.00072F,
                0.014F, 0.032F);
        event.setDensity(density);
        // Forge requires this event to be cancelled for the custom density to apply.
        event.setCanceled(true);
    }

    /**
     * Fog events can run multiple times per rendered frame. Cache the water
     * column lookup for four world ticks and reuse one mutable position so
     * fog colour and density do not allocate/scan up to 33 BlockPos objects
     * every frame while the player is underwater.
     */
    private float getUnderwaterDepth(EntityPlayer player) {
        if (player == null || player.world == null) return 4.0F;
        World world = player.world;
        int x = MathHelper.floor(player.posX);
        int y = MathHelper.floor(player.posY);
        int z = MathHelper.floor(player.posZ);
        long now = world.getTotalWorldTime();

        if (depthWorld == world && depthX == x && depthY == y && depthZ == z
                && now >= depthSampleTick && now - depthSampleTick < 4L) {
            return cachedUnderwaterDepth;
        }

        depthWorld = world;
        depthX = x;
        depthY = y;
        depthZ = z;
        depthSampleTick = now;
        cachedUnderwaterDepth = 4.0F;

        for (int dy = 0; dy <= 32; dy++) {
            depthProbe.setPos(x, y + dy, z);
            if (!world.isBlockLoaded(depthProbe)) break;
            if (world.getBlockState(depthProbe).getMaterial() != Material.WATER) {
                if (dy > 0) {
                    depthProbe.setPos(x, y + dy - 1, z);
                    if (world.isBlockLoaded(depthProbe)
                            && world.getBlockState(depthProbe).getMaterial() == Material.WATER) {
                        cachedUnderwaterDepth = Math.max(0.0F,
                                (float) (depthProbe.getY() + 0.9D - player.posY));
                    }
                }
                break;
            }
        }
        return cachedUnderwaterDepth;
    }
}
