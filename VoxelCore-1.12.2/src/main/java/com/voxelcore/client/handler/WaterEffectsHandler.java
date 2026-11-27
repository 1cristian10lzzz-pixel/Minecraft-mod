package com.voxelcore.client.handler;

import com.voxelcore.config.ModConfig;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Client-only water presentation for Forge 1.12.2.
 *
 * The actual Minecraft water blocks and simulation stay authoritative. This
 * handler installs MinefadeWaterRenderer into Minecraft's existing fluid
 * renderer, manages lightweight splashes and waterfall mist, and keeps the
 * underwater colour biome-aware. No ring meshes or radial wave overlays are
 * generated.
 */
@SideOnly(Side.CLIENT)
public class WaterEffectsHandler {
    private static final Logger LOGGER = LogManager.getLogger("Minefade");
    private static final java.util.Random RANDOM = new java.util.Random();

    private static volatile boolean reducedQuality;

    private double previousX;
    private double previousZ;
    private boolean previousInWater;
    private int tickCounter;
    private int lastSurfaceScanTick = -100;
    private int qualitySampleTicks;
    private int slowSamples;
    private int recoverySamples;
    private BlockPos cachedSurface;
    private World trackedWorld;
    private BlockRendererDispatcher installedDispatcher;
    private boolean rendererInstallWarningLogged;

    public static boolean isReducedQuality() {
        return reducedQuality;
    }

    @SubscribeEvent
    public void onTextureStitchPost(TextureStitchEvent.Post event) {
        MinefadeWaterRenderer.refreshAtlasSprites(event.getMap());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (!ModConfig.ENABLE_WATER_EFFECTS) {
            resetTracking();
            return;
        }

        installCustomWaterRenderer(mc);
        if (mc.world == null || mc.player == null) {
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

        int scanInterval = reducedQuality ? 8 : 4;
        boolean movedToAnotherArea = cachedSurface == null
                || Math.abs(player.posX - cachedSurface.getX()) > 5.0D
                || Math.abs(player.posZ - cachedSurface.getZ()) > 5.0D
                || Math.abs(player.posY - cachedSurface.getY()) > 5.0D;
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

            // Small contact splashes give movement feedback without drawing
            // expanding circles or disturbing the water geometry.
            int splashInterval = reducedQuality ? 8 : 4;
            if (movementSq > 0.0012D && ModConfig.ENABLE_WATER_SPLASHES
                    && tickCounter % splashInterval == 0) {
                double surfaceY = surface.getY() + 0.91D;
                int particles = ridingBoat ? 2 : 1;
                for (int i = 0; i < particles; i++) {
                    double offsetX = (RANDOM.nextDouble() - 0.5D) * 0.45D;
                    double offsetZ = (RANDOM.nextDouble() - 0.5D) * 0.45D;
                    world.spawnParticle(EnumParticleTypes.WATER_SPLASH,
                            player.posX + offsetX, surfaceY, player.posZ + offsetZ,
                            -dx * 0.35D, 0.025D, -dz * 0.35D);
                }
            }
            previousInWater = true;
        } else {
            previousInWater = false;
        }

        // Rain produces occasional impact droplets instead of circular rings.
        int rainInterval = reducedQuality ? 18 : 10;
        if (surface != null && ModConfig.ENABLE_RAIN_RIPPLES && world.isRaining()
                && tickCounter % rainInterval == 0 && RANDOM.nextInt(5) == 0) {
            BlockPos rainWater = findNearbyWaterSurface(world,
                    surface.getX() + RANDOM.nextInt(5) - 2, surface.getY(),
                    surface.getZ() + RANDOM.nextInt(5) - 2, 1);
            if (rainWater != null && world.canSeeSky(rainWater.up())) {
                world.spawnParticle(EnumParticleTypes.WATER_SPLASH,
                        rainWater.getX() + 0.5D, rainWater.getY() + 0.94D,
                        rainWater.getZ() + 0.5D, 0.0D, 0.018D, 0.0D);
            }
        }

        int mistInterval = reducedQuality ? 40 : 24;
        if (ModConfig.ENABLE_WATERFALL_MIST && surface != null && tickCounter % mistInterval == 0) {
            spawnNearbyWaterfallMist(world, surface);
        }

        previousX = player.posX;
        previousZ = player.posZ;
    }

    private void installCustomWaterRenderer(Minecraft mc) {
        BlockRendererDispatcher dispatcher = mc.getBlockRendererDispatcher();
        if (dispatcher == null || dispatcher == installedDispatcher) return;

        try {
            MinefadeWaterRenderer replacement = new MinefadeWaterRenderer(mc.getBlockColors());
            // 1.12.2 SRG field name for BlockRendererDispatcher.fluidRenderer.
            // This keeps Minecraft's normal chunk build and translucent pass.
            ObfuscationReflectionHelper.setPrivateValue(
                    BlockRendererDispatcher.class, dispatcher, replacement, "field_175025_e");
            installedDispatcher = dispatcher;
            MinefadeWaterRenderer.refreshAtlasSprites(mc.getTextureMapBlocks());
            LOGGER.info("Minefade realistic water surface renderer installed.");
        } catch (RuntimeException exception) {
            if (!rendererInstallWarningLogged) {
                rendererInstallWarningLogged = true;
                LOGGER.warn("Minefade could not install its optional water renderer; vanilla water rendering will be retained.",
                        exception);
            }
        }
    }

    /**
     * Hysteresis keeps visual quality from changing every few frames when FPS
     * fluctuates around a threshold. Only optional surface detail is reduced.
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

        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    if (!world.isBlockLoaded(pos) || !world.isBlockLoaded(pos.up())) continue;
                    if (world.getBlockState(pos).getMaterial() != Material.WATER) continue;
                    if (world.getBlockState(pos.up()).getMaterial() == Material.WATER) continue;

                    double cx = pos.getX() + 0.5D - x;
                    double cz = pos.getZ() + 0.5D - z;
                    double cy = pos.getY() + 1.0D - y;
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
        double y = surface.getY() + 0.94D;
        int particleCount = Minecraft.getMinecraft().gameSettings.particleSetting == 2 ? 3 : 6;
        for (int i = 0; i < particleCount; i++) {
            double ox = (RANDOM.nextDouble() - 0.5D) * 0.65D;
            double oz = (RANDOM.nextDouble() - 0.5D) * 0.65D;
            world.spawnParticle(EnumParticleTypes.WATER_SPLASH, player.posX + ox, y,
                    player.posZ + oz, ox * 0.12D, 0.10D + RANDOM.nextDouble() * 0.08D, oz * 0.12D);
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
                        world.spawnParticle(EnumParticleTypes.CLOUD, x + (RANDOM.nextDouble() - 0.5D) * 0.7D,
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

    /** Kept for existing world-unload cleanup calls. There are no ring buffers to clear. */
    public static void clearAll() {
        reducedQuality = false;
    }

    @SubscribeEvent
    public void onFogColors(EntityViewRenderEvent.FogColors event) {
        if (!ModConfig.ENABLE_WATER_EFFECTS || !ModConfig.ENABLE_UNDERWATER_TINT) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null || !mc.player.isInsideOfMaterial(Material.WATER)) return;

        BlockPos pos = new BlockPos(mc.player.posX, mc.player.posY, mc.player.posZ);
        int waterColor = mc.world.getBiome(pos).getWaterColorMultiplier();
        float waterRed = ((waterColor >> 16) & 255) / 255.0F;
        float waterGreen = ((waterColor >> 8) & 255) / 255.0F;
        float waterBlue = (waterColor & 255) / 255.0F;

        // The further the player is below the local surface, the cooler and
        // darker the fog becomes. The scan is bounded to keep it inexpensive.
        int submergedBlocks = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 1; y <= 8; y++) {
            cursor.setPos(pos.getX(), pos.getY() + y, pos.getZ());
            if (mc.world.getBlockState(cursor).getMaterial() != Material.WATER) break;
            submergedBlocks++;
        }
        float depth = MathHelper.clamp(submergedBlocks / 8.0F, 0.0F, 1.0F);
        float deepRed = waterRed * (1.0F - depth) + 0.012F * depth;
        float deepGreen = waterGreen * (1.0F - depth) + 0.075F * depth;
        float deepBlue = waterBlue * (1.0F - depth) + 0.19F * depth;

        event.setRed(event.getRed() * 0.22F + deepRed * 0.78F);
        event.setGreen(event.getGreen() * 0.22F + deepGreen * 0.78F);
        event.setBlue(event.getBlue() * 0.22F + deepBlue * 0.78F);
    }
}
