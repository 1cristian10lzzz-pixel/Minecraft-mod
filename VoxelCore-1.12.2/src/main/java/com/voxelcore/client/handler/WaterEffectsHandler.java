package com.voxelcore.client.handler;

import com.voxelcore.config.ModConfig;
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

        int scanInterval = reducedQuality ? 10 : 5;
        boolean movedToAnotherArea = cachedSurface == null
                || Math.abs(player.posX - cachedSurface.getX()) > 5.0D
                || Math.abs(player.posZ - cachedSurface.getZ()) > 5.0D
                || Math.abs(player.posY - cachedSurface.getY()) > 7.0D;
        if (movedToAnotherArea || tickCounter - lastSurfaceScanTick >= scanInterval) {
            cachedSurface = findNearbyWaterSurface(world, player.posX, player.posY, player.posZ, 2);
            lastSurfaceScanTick = tickCounter;
        }

        boolean ridingBoat = player.getRidingEntity() instanceof EntityBoat;
        boolean inWater = player.isInsideOfMaterial(Material.WATER) || ridingBoat;
        BlockPos surface = cachedSurface;

        if (inWater && surface != null) {
            double dx = player.posX - previousX;
            double dz = player.posZ - previousZ;
            double movementSq = dx * dx + dz * dz;

            if (!previousInWater && ModConfig.ENABLE_WATER_SPLASHES) {
                spawnEntrySplash(world, player, surface);
            }

            if (movementSq > 0.0008D) {
                int wakeInterval = reducedQuality ? 8 : 4;
                if (ModConfig.ENABLE_WATER_SPLASHES && tickCounter % wakeInterval == 0) {
                    double surfaceY = surface.getY() + 0.90D;
                    world.spawnParticle(EnumParticleTypes.WATER_WAKE, player.posX, surfaceY,
                            player.posZ, -dx * 2.0D, 0.0D, -dz * 2.0D);
                    if (movementSq > 0.0018D) {
                        world.spawnParticle(EnumParticleTypes.WATER_SPLASH, player.posX, surfaceY,
                                player.posZ, dx * 0.7D, 0.025D, dz * 0.7D);
                    }
                }
            }
            previousInWater = true;
        } else {
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
                        rainWater.getX() + 0.5D, rainWater.getY() + 0.90D,
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

    private static BlockPos findNearbyWaterSurface(World world, double x, double y, double z, int radius) {
        BlockPos origin = new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int dy = -8; dy <= 8; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    if (!world.isBlockLoaded(pos) || !world.isBlockLoaded(pos.up())) continue;
                    if (world.getBlockState(pos).getMaterial() != Material.WATER) continue;
                    if (world.getBlockState(pos.up()).getMaterial() == Material.WATER) continue;

                    double cx = pos.getX() + 0.5D - x;
                    double cz = pos.getZ() + 0.5D - z;
                    double cy = pos.getY() + 0.9D - y;
                    double distance = cx * cx + cz * cz + cy * cy * 0.5D;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    private static void spawnEntrySplash(World world, EntityPlayer player, BlockPos surface) {
        double y = surface.getY() + 0.86D;
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
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -2; dy <= 0; dy++) {
                    BlockPos pos = centre.add(dx, dy, dz);
                    if (!world.isBlockLoaded(pos) || !world.isBlockLoaded(pos.up())) continue;
                    if (world.getBlockState(pos).getMaterial() != Material.WATER) continue;
                    if (world.getBlockState(pos.up()).getMaterial() != Material.WATER) continue;
                    if (!world.getBlockState(pos.down()).getMaterial().isSolid()) continue;

                    double x = pos.getX() + 0.5D;
                    double y = pos.getY() + 0.25D;
                    double z = pos.getZ() + 0.5D;
                    int particles = reducedQuality ? 1 : 2;
                    for (int i = 0; i < particles; i++) {
                        world.spawnParticle(EnumParticleTypes.CLOUD,
                                x + (RANDOM.nextDouble() - 0.5D) * 0.7D,
                                y, z + (RANDOM.nextDouble() - 0.5D) * 0.7D,
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

    private static float getUnderwaterDepth(EntityPlayer player) {
        if (player == null) return 4.0F;
        BlockPos surface = new BlockPos(player.posX, player.posY, player.posZ);
        BlockPos found = null;
        for (int dy = 0; dy <= 32; dy++) {
            BlockPos check = surface.up(dy);
            if (!player.world.isBlockLoaded(check)) break;
            if (player.world.getBlockState(check).getMaterial() != Material.WATER) {
                if (dy > 0 && player.world.getBlockState(check.down()).getMaterial() == Material.WATER) {
                    found = check.down();
                }
                break;
            }
        }
        return found == null ? 4.0F : Math.max(0.0F, (float) (found.getY() + 0.9D - player.posY));
    }
}
