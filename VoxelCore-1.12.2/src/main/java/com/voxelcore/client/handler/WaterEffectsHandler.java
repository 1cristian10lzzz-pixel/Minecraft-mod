package com.voxelcore.client.handler;

import com.voxelcore.config.ModConfig;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
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
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Client-only water presentation for Forge 1.12.2.
 *
 * The renderer batches every ripple into one GL draw call per frame. Surface
 * lookups and waterfall scans are throttled, while the cached surface follows
 * the player as they move. Vanilla fluid blocks and server world state remain
 * untouched.
 */
@SideOnly(Side.CLIENT)
public class WaterEffectsHandler {
    private static final int MAX_RIPPLES = 24;
    private static final int NORMAL_RIPPLE_LIMIT = 20;
    private static final int LOW_RIPPLE_LIMIT = 9;
    private static final int NORMAL_RING_SEGMENTS = 32;
    private static final int LOW_RING_SEGMENTS = 20;
    private static final List<Ripple> RIPPLES = new ArrayList<Ripple>();
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

        int scanInterval = reducedQuality ? 6 : 3;
        boolean movedToAnotherArea = cachedSurface == null
                || Math.abs(player.posX - cachedSurface.getX()) > 5.0D
                || Math.abs(player.posZ - cachedSurface.getZ()) > 5.0D
                || Math.abs(player.posY - cachedSurface.getY()) > 5.0D;
        if (movedToAnotherArea || tickCounter - lastSurfaceScanTick >= scanInterval) {
            cachedSurface = findNearbyWaterSurface(world, player.posX, player.posY, player.posZ, 3);
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

            if (movementSq > 0.0008D && ModConfig.ENABLE_WATER_RIPPLES) {
                double surfaceY = surface.getY() + 1.015D;
                addRipple(player.posX, surfaceY, player.posZ, ridingBoat ? 1.25F : 0.85F);
                int wakeInterval = reducedQuality ? 8 : 4;
                if (ModConfig.ENABLE_WATER_SPLASHES && tickCounter % wakeInterval == 0) {
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

        int rainInterval = reducedQuality ? 18 : 10;
        if (surface != null && ModConfig.ENABLE_RAIN_RIPPLES && world.isRaining()
                && tickCounter % rainInterval == 0 && RANDOM.nextInt(5) == 0) {
            BlockPos rainWater = findNearbyWaterSurface(world,
                    surface.getX() + RANDOM.nextInt(5) - 2, surface.getY(),
                    surface.getZ() + RANDOM.nextInt(5) - 2, 1);
            if (rainWater != null && world.canSeeSky(rainWater.up())) {
                addRipple(rainWater.getX() + 0.5D, rainWater.getY() + 1.015D,
                        rainWater.getZ() + 0.5D, 0.38F);
            }
        }

        int mistInterval = reducedQuality ? 40 : 24;
        if (ModConfig.ENABLE_WATERFALL_MIST && surface != null && tickCounter % mistInterval == 0) {
            spawnNearbyWaterfallMist(world, surface);
        }

        previousX = player.posX;
        previousZ = player.posZ;
        ageRipples();
    }

    /**
     * Hysteresis keeps visual quality from changing every few frames when FPS
     * fluctuates around a threshold. It only reduces optional Minefade effects.
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
        double y = surface.getY() + 0.96D;
        int particleCount = Minecraft.getMinecraft().gameSettings.particleSetting == 2 ? 3 : 6;
        for (int i = 0; i < particleCount; i++) {
            double ox = (RANDOM.nextDouble() - 0.5D) * 0.65D;
            double oz = (RANDOM.nextDouble() - 0.5D) * 0.65D;
            world.spawnParticle(EnumParticleTypes.WATER_SPLASH, player.posX + ox, y,
                    player.posZ + oz, ox * 0.12D, 0.10D + RANDOM.nextDouble() * 0.08D, oz * 0.12D);
        }
        addRipple(player.posX, surface.getY() + 1.015D, player.posZ, 1.5F);
        if (Minecraft.getMinecraft().gameSettings.particleSetting != 2) {
            addRipple(player.posX + 0.35D, surface.getY() + 1.016D, player.posZ - 0.2D, 0.75F);
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

    private static void addRipple(double x, double y, double z, float scale) {
        if (ModConfig.WATER_WAVE_STRENGTH <= 0) return;
        int limit = ModConfig.ENABLE_ADAPTIVE_WATER_QUALITY
                && Minecraft.getDebugFPS() > 0 && Minecraft.getDebugFPS() < 28
                ? LOW_RIPPLE_LIMIT : NORMAL_RIPPLE_LIMIT;
        if (RIPPLES.size() >= Math.min(MAX_RIPPLES, limit)) RIPPLES.remove(0);
        RIPPLES.add(new Ripple(x, y, z, scale));
    }

    private static void ageRipples() {
        Iterator<Ripple> iterator = RIPPLES.iterator();
        while (iterator.hasNext()) {
            Ripple ripple = iterator.next();
            ripple.age++;
            if (ripple.age >= ripple.maxAge) iterator.remove();
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
        RIPPLES.clear();
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!ModConfig.ENABLE_WATER_EFFECTS || !ModConfig.ENABLE_WATER_RIPPLES
                || ModConfig.WATER_WAVE_STRENGTH <= 0 || mc.world == null || mc.player == null
                || RIPPLES.isEmpty()) return;

        double viewX = mc.getRenderManager().viewerPosX;
        double viewY = mc.getRenderManager().viewerPosY;
        double viewZ = mc.getRenderManager().viewerPosZ;
        int maxDistance = ModConfig.WATER_EFFECT_RADIUS;
        double maxDistanceSq = (double) maxDistance * (double) maxDistance;
        float strength = ModConfig.WATER_WAVE_STRENGTH / 100.0F;
        int segments = reducedQuality ? LOW_RING_SEGMENTS : NORMAL_RING_SEGMENTS;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(-viewX, -viewY, -viewZ);
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableCull();
            GlStateManager.depthMask(false);
            GL11.glLineWidth(1.25F);

            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            for (Ripple ripple : RIPPLES) {
                double dx = ripple.x - viewX;
                double dy = ripple.y - viewY;
                double dz = ripple.z - viewZ;
                if (dx * dx + dy * dy + dz * dz > maxDistanceSq) continue;

                float progress = (ripple.age + event.getPartialTicks()) / (float) ripple.maxAge;
                float fade = Math.max(0.0F, 1.0F - progress);
                float alpha = fade * strength * 0.52F;
                if (alpha < 0.015F) continue;

                double radius = (0.22D + progress * 2.4D) * ripple.scale;
                int a = MathHelper.clamp((int) (alpha * 255.0F), 0, 255);
                appendRing(buffer, ripple.x, ripple.y, ripple.z, radius, a, segments);
                appendRing(buffer, ripple.x, ripple.y + 0.001D, ripple.z, radius * 0.58D,
                        Math.max(0, a / 2), segments);
            }
            tessellator.draw();
        } finally {
            GlStateManager.depthMask(true);
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            GlStateManager.enableTexture2D();
            GlStateManager.enableLighting();
            GlStateManager.popMatrix();
            GL11.glLineWidth(1.0F);
        }
    }

    /** Adds one complete ring as independent line segments to the shared frame buffer. */
    private static void appendRing(BufferBuilder buffer, double x, double y, double z,
                                   double radius, int alpha, int segments) {
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0D * i / segments;
            double a1 = Math.PI * 2.0D * (i + 1) / segments;
            buffer.pos(x + Math.cos(a0) * radius, y, z + Math.sin(a0) * radius)
                    .color(125, 220, 242, alpha).endVertex();
            buffer.pos(x + Math.cos(a1) * radius, y, z + Math.sin(a1) * radius)
                    .color(125, 220, 242, alpha).endVertex();
        }
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

        event.setRed(event.getRed() * 0.28F + waterRed * 0.72F);
        event.setGreen(event.getGreen() * 0.28F + waterGreen * 0.72F);
        event.setBlue(event.getBlue() * 0.28F + waterBlue * 0.72F);
    }

    private static final class Ripple {
        private final double x;
        private final double y;
        private final double z;
        private final float scale;
        private final int maxAge = 34;
        private int age;

        private Ripple(double x, double y, double z, float scale) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.scale = scale;
        }
    }
}
