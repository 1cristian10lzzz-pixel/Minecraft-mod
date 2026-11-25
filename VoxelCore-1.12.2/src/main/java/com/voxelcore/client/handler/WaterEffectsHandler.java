package com.voxelcore.client.handler;

import com.voxelcore.config.ModConfig;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.block.material.Material;
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
 * Original client-only water presentation layer for Forge 1.12.2.
 * It adds local wakes, rain rings, splash particles, waterfall mist and underwater tint.
 * It deliberately does not replace vanilla fluid blocks or modify server world state.
 */
@SideOnly(Side.CLIENT)
public class WaterEffectsHandler {
    private static final int MAX_RIPPLES = 28;
    private static final List<Ripple> RIPPLES = new ArrayList<Ripple>();
    private static final Random RANDOM = new Random();

    private double previousX;
    private double previousZ;
    private boolean previousInWater;
    private int tickCounter;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!ModConfig.ENABLE_WATER_EFFECTS || mc.world == null || mc.player == null) {
            if (mc.world == null || mc.player == null) clearAll();
            return;
        }

        tickCounter++;
        EntityPlayer player = mc.player;
        World world = mc.world;
        boolean ridingBoat = player.getRidingEntity() instanceof EntityBoat;
        boolean inWater = player.isInsideOfMaterial(Material.WATER) || ridingBoat;
        BlockPos surface = findNearbyWaterSurface(world, player.posX, player.posY, player.posZ, 3);

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
                if (ModConfig.ENABLE_WATER_SPLASHES && tickCounter % 4 == 0) {
                    double speed = Math.min(0.10D, Math.sqrt(movementSq) * 0.6D);
                    world.spawnParticle(EnumParticleTypes.WATER_WAKE, player.posX, surfaceY,
                            player.posZ, -dx * 2.0D, 0.0D, -dz * 2.0D);
                    if (speed > 0.025D) {
                        world.spawnParticle(EnumParticleTypes.WATER_SPLASH, player.posX, surfaceY,
                                player.posZ, dx * 0.7D, 0.025D, dz * 0.7D);
                    }
                }
            }
            previousInWater = true;
        } else {
            previousInWater = false;
        }

        if (surface != null && ModConfig.ENABLE_RAIN_RIPPLES && world.isRaining()
                && tickCounter % 5 == 0 && RANDOM.nextInt(5) == 0) {
            BlockPos rainWater = findNearbyWaterSurface(world,
                    surface.getX() + RANDOM.nextInt(7) - 3, surface.getY(), surface.getZ() + RANDOM.nextInt(7) - 3, 1);
            if (rainWater != null && world.canSeeSky(rainWater.up())) {
                addRipple(rainWater.getX() + 0.5D, rainWater.getY() + 1.015D,
                        rainWater.getZ() + 0.5D, 0.38F);
            }
        }

        if (ModConfig.ENABLE_WATERFALL_MIST && surface != null && tickCounter % 12 == 0) {
            spawnNearbyWaterfallMist(world, surface);
        }

        previousX = player.posX;
        previousZ = player.posZ;
        ageRipples();
    }

    private static BlockPos findNearbyWaterSurface(World world, double x, double y, double z, int radius) {
        BlockPos origin = new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    if (world.getBlockState(pos).getMaterial() != Material.WATER) continue;
                    if (world.getBlockState(pos.up()).getMaterial() == Material.WATER) continue;

                    double cx = pos.getX() + 0.5D - x;
                    double cz = pos.getZ() + 0.5D - z;
                    double distance = cx * cx + cz * cz + (pos.getY() + 1.0D - y) * (pos.getY() + 1.0D - y) * 0.5D;
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
        for (int i = 0; i < 7; i++) {
            double ox = (RANDOM.nextDouble() - 0.5D) * 0.65D;
            double oz = (RANDOM.nextDouble() - 0.5D) * 0.65D;
            world.spawnParticle(EnumParticleTypes.WATER_SPLASH, player.posX + ox, y,
                    player.posZ + oz, ox * 0.12D, 0.10D + RANDOM.nextDouble() * 0.08D, oz * 0.12D);
        }
        addRipple(player.posX, surface.getY() + 1.015D, player.posZ, 1.5F);
        addRipple(player.posX + 0.35D, surface.getY() + 1.016D, player.posZ - 0.2D, 0.75F);
    }

    private static void spawnNearbyWaterfallMist(World world, BlockPos centre) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -3; dy <= 1; dy++) {
                    BlockPos pos = centre.add(dx, dy, dz);
                    if (world.getBlockState(pos).getMaterial() != Material.WATER) continue;
                    if (world.getBlockState(pos.up()).getMaterial() != Material.WATER) continue;
                    if (!world.getBlockState(pos.down()).getMaterial().isSolid()) continue;

                    double x = pos.getX() + 0.5D;
                    double y = pos.getY() + 0.25D;
                    double z = pos.getZ() + 0.5D;
                    for (int i = 0; i < 2; i++) {
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
        if (RIPPLES.size() >= MAX_RIPPLES) RIPPLES.remove(0);
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
        float strength = ModConfig.WATER_WAVE_STRENGTH / 100.0F;

        GlStateManager.pushMatrix();
        GlStateManager.translate(-viewX, -viewY, -viewZ);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GL11.glLineWidth(1.35F);

        for (Ripple ripple : RIPPLES) {
            double dx = ripple.x - viewX;
            double dz = ripple.z - viewZ;
            if (dx * dx + dz * dz > maxDistance * maxDistance) continue;

            float progress = (ripple.age + event.getPartialTicks()) / (float) ripple.maxAge;
            float fade = Math.max(0.0F, 1.0F - progress);
            float alpha = fade * strength * 0.52F;
            if (alpha < 0.015F) continue;

            double base = (0.22D + progress * 2.4D) * ripple.scale;
            drawRing(ripple.x, ripple.y, ripple.z, base, alpha);
            drawRing(ripple.x, ripple.y + 0.001D, ripple.z, base * 0.58D,
                    alpha * 0.45F);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private static void drawRing(double x, double y, double z, double radius, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 40; i++) {
            double angle = Math.PI * 2.0D * i / 40.0D;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            int a = Math.max(0, Math.min(255, (int) (alpha * 255.0F)));
            buffer.pos(px, y, pz).color(125, 220, 242, a).endVertex();
        }
        tessellator.draw();
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
