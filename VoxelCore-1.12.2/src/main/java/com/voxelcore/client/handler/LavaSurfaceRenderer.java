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
    private static final int MAX_SURFACES = 520;
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

    private static void rebuild(World world, int cx, int cy, int cz) {
        SURFACES.clear();
        int radius = MathHelper.clamp(ModConfig.LAVA_EFFECT_RADIUS / 3, 4, 10);
        if (reducedQuality) radius = Math.max(4, radius - 2);
        int top = Math.min(255, cy + 8);
        int bottom = Math.max(0, cy - 28);
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

                    int level = state.getBlock() instanceof BlockLiquid
                            ? state.getValue(BlockLiquid.LEVEL).intValue() : 0;
                    int adjusted = level >= 8 ? 0 : Math.min(7, level);
                    double height = y + 1.0D - ((adjusted + 1) / 9.0D);
                    boolean north = isExposedLavaEdge(world, x, y, z - 1);
                    boolean south = isExposedLavaEdge(world, x, y, z + 1);
                    boolean west = isExposedLavaEdge(world, x - 1, y, z);
                    boolean east = isExposedLavaEdge(world, x + 1, y, z);
                    SURFACES.add(new LavaCell(x, z, height, north, south, west, east,
                            mix(x * 73428767 ^ z * 438289 ^ y * 912931)));
                    break;
                }
                if (SURFACES.size() >= MAX_SURFACES) return;
            }
        }
    }

    private static boolean isExposedLavaEdge(World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return y >= 0 && y < 256 && world.isBlockLoaded(pos)
                && world.getBlockState(pos).getMaterial()
                != net.minecraft.block.material.Material.LAVA;
    }

    private static boolean isLava(World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return y >= 0 && y < 256 && world.isBlockLoaded(pos)
                && world.getBlockState(pos).getMaterial()
                == net.minecraft.block.material.Material.LAVA;
    }

    private void spawnNearbyHeat(Minecraft mc, double px, double py, double pz) {
        double radius = ModConfig.LAVA_EFFECT_RADIUS;
        double radiusSq = radius * radius;
        int spawned = 0;
        for (int i = 0; i < SURFACES.size() && spawned < 2; i++) {
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

        boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

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
                drawMoltenVeins(buffer, cell, time,
                        reducedQuality ? Math.max(0, quality - 1) : quality);
            }
            tessellator.draw();
        } finally {
            GlStateManager.depthMask(depthMask);
            if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
            if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
            if (texture) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
            if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    private static void drawMoltenVeins(BufferBuilder b, LavaCell cell, double time, int quality) {
        int veins = quality <= 0 ? 1 : (quality == 1 ? 2 : 3);
        for (int i = 0; i < veins; i++) {
            int seed = mix(cell.seed + i * 19349663);
            double phase = unit(seed) * Math.PI * 2.0D;
            double travel = (time * (0.055D + unit(mix(seed + 3)) * 0.025D)
                    + unit(mix(seed + 7))) % 1.0D;
            double u = 0.12D + travel * 0.72D;
            double v = 0.14D + unit(mix(seed + 11)) * 0.72D;
            double length = 0.18D + unit(mix(seed + 17)) * 0.34D;
            double width = 0.012D + unit(mix(seed + 23)) * 0.017D;
            double dx = Math.cos(phase) * length;
            double dz = Math.sin(phase) * length;
            double px = -Math.sin(phase) * width;
            double pz = Math.cos(phase) * width;
            double x = cell.x + u;
            double z = cell.z + v;
            double y = cell.surfaceY + 0.006D + i * 0.0004D;
            double pulse = 0.5D + 0.5D * Math.sin(time * 1.4D + phase + cell.seed * 0.00001D);
            int alpha = (int) ((quality == 0 ? 24.0D : 34.0D) + pulse * 24.0D);
            int red = i == veins - 1 ? 255 : 220;
            int green = i == veins - 1 ? (int) (112.0D + pulse * 48.0D) : (int) (62.0D + pulse * 46.0D);
            int blue = i == veins - 1 ? 36 : 12;
            b.pos(x - dx - px, y, z - dz - pz).color(red, green, blue, alpha).endVertex();
            b.pos(x - dx + px, y, z - dz + pz).color(red, green, blue, alpha).endVertex();
            b.pos(x + dx + px, y, z + dz + pz).color(red, green, blue, alpha).endVertex();
            b.pos(x + dx - px, y, z + dz - pz).color(red, green, blue, alpha).endVertex();
        }
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
        final boolean north, south, west, east;

        LavaCell(int x, int z, double surfaceY,
                 boolean north, boolean south, boolean west, boolean east, int seed) {
            this.x = x;
            this.z = z;
            this.surfaceY = surfaceY;
            this.north = north;
            this.south = south;
            this.west = west;
            this.east = east;
            this.seed = seed;
        }
    }
}
