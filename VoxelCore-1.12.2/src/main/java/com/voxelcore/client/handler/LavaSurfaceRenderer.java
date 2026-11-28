package com.voxelcore.client.handler;

import com.voxelcore.config.ModConfig;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Client-only molten-surface accents for vanilla lava.
 *
 * Vanilla still supplies the animated lava texture, lighting, flow and
 * collision. Minefade draws only thin, translucent molten veins on exposed
 * lava tops and a few local heat particles. It never replaces the fluid mesh.
 */
@SideOnly(Side.CLIENT)
public final class LavaSurfaceRenderer {
    private static final int MAX_SURFACES = 460;
    private static final List<LavaCell> SURFACES = new ArrayList<LavaCell>();
    private static final Random RANDOM = new Random();

    private World cachedWorld;
    private int tick;
    private int lastScanTick = -1000;
    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;
    private int lastZ = Integer.MIN_VALUE;
    private int lastEmberTick = -100;
    private int qualitySampleTicks;
    private int slowSamples;
    private int recoverySamples;
    private boolean reducedQuality;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!ModConfig.ENABLE_LAVA_EFFECTS || mc.world == null
                || mc.getRenderViewEntity() == null) {
            clear();
            return;
        }

        tick++;
        updateAdaptiveQuality();
        Entity view = mc.getRenderViewEntity();
        int cx = MathHelper.floor(view.posX);
        int cy = MathHelper.floor(view.posY);
        int cz = MathHelper.floor(view.posZ);
        boolean moved = Math.abs(cx - lastX) >= 3 || Math.abs(cy - lastY) >= 6
                || Math.abs(cz - lastZ) >= 3;
        if (cachedWorld != mc.world || moved || tick - lastScanTick >= 24) {
            rebuild(mc.world, cx, cy, cz);
            cachedWorld = mc.world;
            lastX = cx;
            lastY = cy;
            lastZ = cz;
            lastScanTick = tick;
        }

        if (ModConfig.ENABLE_LAVA_EMBERS && tick - lastEmberTick >= (reducedQuality ? 26 : 14)
                && mc.gameSettings.particleSetting < 2 && !SURFACES.isEmpty()) {
            spawnNearbyHeat(mc, view.posX, view.posY, view.posZ);
            lastEmberTick = tick;
        }
    }

    private void updateAdaptiveQuality() {
        if (!ModConfig.ENABLE_ADAPTIVE_WATER_QUALITY) {
            reducedQuality = false;
            qualitySampleTicks = 0;
            slowSamples = 0;
            recoverySamples = 0;
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

    private void rebuild(World world, int cx, int cy, int cz) {
        SURFACES.clear();
        int radius = MathHelper.clamp(ModConfig.LAVA_EFFECT_RADIUS / 3, 4, 10);
        if (reducedQuality) radius = Math.max(4, radius - 2);
        int top = Math.min(255, cy + 12);
        int bottom = Math.max(0, cy - 24);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                BlockPos column = new BlockPos(x, top, z);
                if (!world.isBlockLoaded(column)) continue;
                for (int y = top; y >= bottom; y--) {
                    pos.setPos(x, y, z);
                    IBlockState state = world.getBlockState(pos);
                    if (state.getMaterial() != net.minecraft.block.material.Material.LAVA) continue;
                    pos.setPos(x, y + 1, z);
                    if (world.getBlockState(pos).getMaterial()
                            == net.minecraft.block.material.Material.LAVA) continue;

                    // Use the same liquid-height routine as the vanilla 1.12.2 renderer.
                    double height = BlockLiquid.getLiquidHeight(state, world, new BlockPos(x, y, z));
                    SURFACES.add(new LavaCell(x, z, height,
                            mix(x * 73428767 ^ z * 438289 ^ y * 912931)));
                    break;
                }
                if (SURFACES.size() >= MAX_SURFACES) return;
            }
        }
    }

    private void spawnNearbyHeat(Minecraft mc, double px, double py, double pz) {
        double radius = ModConfig.LAVA_EFFECT_RADIUS;
        double radiusSq = radius * radius;
        int spawned = 0;
        for (int i = 0; i < 12 && spawned < 2 && !SURFACES.isEmpty(); i++) {
            LavaCell cell = SURFACES.get(RANDOM.nextInt(SURFACES.size()));
            double x = cell.x + 0.5D;
            double z = cell.z + 0.5D;
            double dx = x - px;
            double dy = cell.surfaceY - py;
            double dz = z - pz;
            if (dx * dx + dy * dy + dz * dz > radiusSq) continue;
            double ox = (RANDOM.nextDouble() - 0.5D) * 0.42D;
            double oz = (RANDOM.nextDouble() - 0.5D) * 0.42D;
            mc.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    x + ox, cell.surfaceY + 0.10D, z + oz,
                    ox * 0.025D, 0.025D + RANDOM.nextDouble() * 0.025D, oz * 0.025D);
            if (RANDOM.nextInt(3) == 0) {
                mc.world.spawnParticle(EnumParticleTypes.FLAME,
                        x + ox, cell.surfaceY + 0.04D, z + oz,
                        ox * 0.02D, 0.018D, oz * 0.02D);
            }
            spawned++;
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!ModConfig.ENABLE_LAVA_EFFECTS || mc.world == null || SURFACES.isEmpty()
                || cachedWorld != mc.world || mc.getRenderViewEntity() == null) return;

        double camX = mc.getRenderManager().viewerPosX;
        double camY = mc.getRenderManager().viewerPosY;
        double camZ = mc.getRenderManager().viewerPosZ;
        double radius = ModConfig.LAVA_EFFECT_RADIUS;
        double radiusSq = radius * radius;
        double time = (mc.world.getTotalWorldTime() + event.getPartialTicks()) * 0.045D;

        WorldRenderStateGuard renderState = WorldRenderStateGuard.capture();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(-camX, -camY, -camZ);
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableCull();
            GlStateManager.depthMask(false);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

            int quality = ModConfig.LAVA_RENDER_QUALITY;
            for (LavaCell cell : SURFACES) {
                double dx = cell.x + 0.5D - camX;
                double dy = cell.surfaceY - camY;
                double dz = cell.z + 0.5D - camZ;
                if (dx * dx + dy * dy + dz * dz > radiusSq) continue;
                drawMoltenCracks(buffer, cell, time,
                        reducedQuality ? Math.max(0, quality - 1) : quality);
            }
            tessellator.draw();
        } finally {
            renderState.restore();
            GlStateManager.popMatrix();
        }
    }

    /**
     * Draws a stationary branching crack network. Only its glow pulses; the
     * channels do not slide over the block like artificial painted stripes.
     */
    private static void drawMoltenCracks(BufferBuilder buffer, LavaCell cell,
                                         double time, int quality) {
        int cracks = quality <= 0 ? 1 : (quality == 1 ? 2 : 3);
        for (int i = 0; i < cracks; i++) {
            CrackPath path = cell.cracks[i];
            double pulse = 0.5D + 0.5D * Math.sin(time * path.pulseSpeed + path.pulsePhase);
            int rimAlpha = (int) ((quality == 0 ? 22.0D : 31.0D) + pulse * 18.0D);
            int coreAlpha = (int) ((quality == 0 ? 31.0D : 44.0D) + pulse * 38.0D);
            double y = cell.surfaceY + 0.006D + i * 0.0005D;

            drawCrackSegment(buffer, path.x0, path.z0, path.x1, path.z1, y,
                    path.rimWidth, 148, 35, 10, rimAlpha);
            drawCrackSegment(buffer, path.x1, path.z1, path.x2, path.z2, y,
                    path.rimWidth * 0.82D, 148, 35, 10, rimAlpha);
            drawCrackSegment(buffer, path.x0, path.z0, path.x1, path.z1, y + 0.0005D,
                    path.coreWidth, 255, (int) (96.0D + pulse * 80.0D), 22, coreAlpha);
            drawCrackSegment(buffer, path.x1, path.z1, path.x2, path.z2, y + 0.0005D,
                    path.coreWidth * 0.72D, 255, (int) (118.0D + pulse * 70.0D), 32, coreAlpha);

            double spot = path.junctionRadius + pulse * 0.004D;
            drawHotDiamond(buffer, path.x1, y + 0.001D, path.z1,
                    spot, 255, (int) (138.0D + pulse * 76.0D), 34, coreAlpha);
        }
    }

    private static void drawCrackSegment(BufferBuilder buffer,
                                         double x0, double z0, double x1, double z1,
                                         double y, double halfWidth,
                                         int red, int green, int blue, int alpha) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.0001D) return;
        double px = -dz / length * halfWidth;
        double pz = dx / length * halfWidth;
        buffer.pos(x0 - px, y, z0 - pz).color(red, green, blue, alpha).endVertex();
        buffer.pos(x0 + px, y, z0 + pz).color(red, green, blue, alpha).endVertex();
        buffer.pos(x1 + px, y, z1 + pz).color(red, green, blue, alpha).endVertex();
        buffer.pos(x1 - px, y, z1 - pz).color(red, green, blue, alpha).endVertex();
    }

    private static void drawHotDiamond(BufferBuilder buffer, double x, double y, double z,
                                       double radius, int red, int green, int blue, int alpha) {
        buffer.pos(x, y, z - radius).color(red, green, blue, alpha).endVertex();
        buffer.pos(x + radius, y, z).color(red, green, blue, alpha).endVertex();
        buffer.pos(x, y, z + radius).color(red, green, blue, alpha).endVertex();
        buffer.pos(x - radius, y, z).color(red, green, blue, alpha).endVertex();
    }

    private static int mix(int value) {
        value ^= value >>> 16;
        value *= 0x7feb352d;
        value ^= value >>> 15;
        value *= 0x846ca68b;
        return value ^ (value >>> 16);
    }

    private static double unit(int value) {
        return (value & 0x00ffffff) / 16777215.0D;
    }

    public void clear() {
        SURFACES.clear();
        cachedWorld = null;
        lastScanTick = -1000;
        lastEmberTick = -100;
        qualitySampleTicks = 0;
        slowSamples = 0;
        recoverySamples = 0;
        reducedQuality = false;
        lastX = lastY = lastZ = Integer.MIN_VALUE;
    }

    private static final class LavaCell {
        final int x, z, seed;
        final double surfaceY;
        final CrackPath[] cracks = new CrackPath[3];

        LavaCell(int x, int z, double surfaceY, int seed) {
            this.x = x;
            this.z = z;
            this.surfaceY = surfaceY;
            this.seed = seed;
            for (int i = 0; i < cracks.length; i++) {
                cracks[i] = new CrackPath(x, z, mix(seed + i * 19349663));
            }
        }
    }

    /** Immutable geometry precomputed once per cached lava column. */
    private static final class CrackPath {
        final double x0, z0, x1, z1, x2, z2;
        final double rimWidth, coreWidth, junctionRadius, pulseSpeed, pulsePhase;

        CrackPath(int blockX, int blockZ, int seed) {
            double xStart = blockX + 0.16D + unit(seed + 3) * 0.68D;
            double zStart = blockZ + 0.16D + unit(seed + 7) * 0.68D;
            double angle = unit(seed + 11) * Math.PI * 2.0D;
            double length = 0.18D + unit(seed + 17) * 0.28D;
            double bend = (unit(seed + 23) - 0.5D) * 0.95D;
            x0 = xStart;
            z0 = zStart;
            x1 = clampCell(xStart + Math.cos(angle) * length * 0.55D, blockX);
            z1 = clampCell(zStart + Math.sin(angle) * length * 0.55D, blockZ);
            x2 = clampCell(x1 + Math.cos(angle + bend) * length * 0.45D, blockX);
            z2 = clampCell(z1 + Math.sin(angle + bend) * length * 0.45D, blockZ);
            rimWidth = 0.028D + unit(seed + 29) * 0.006D;
            coreWidth = 0.007D + unit(seed + 31) * 0.003D;
            junctionRadius = 0.012D + unit(seed + 37) * 0.004D;
            pulseSpeed = 0.82D + unit(seed + 41) * 0.44D;
            pulsePhase = unit(seed + 43) * Math.PI * 2.0D;
        }
    }

}
