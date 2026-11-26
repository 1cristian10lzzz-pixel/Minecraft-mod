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
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Replaces the visible vanilla water top-face appearance with a procedural,
 * animated surface. It does not change water blocks, flow, levels, collisions,
 * lighting, chunk meshes or server simulation.
 *
 * The cached surface is rebuilt infrequently; the continuous wave, Fresnel-like
 * sky reflection, specular response and shore foam are calculated in one batched
 * draw call at render time. No PNGs, resource packs or external shaders are used.
 */
@SideOnly(Side.CLIENT)
public final class WaterSurfaceRenderer {
    private static final int NORMAL_GRID_STEPS = 4;
    private static final int LOW_GRID_STEPS = 2;
    private static final int MAX_CACHED_SURFACES = 1800;
    private static final List<SurfaceCell> SURFACES = new ArrayList<SurfaceCell>();

    private static World cachedWorld;
    private static int lastScanTick = -1000;
    private static int lastCentreX = Integer.MIN_VALUE;
    private static int lastCentreZ = Integer.MIN_VALUE;
    private static int lastCentreY = Integer.MIN_VALUE;

    private WaterSurfaceRenderer() { }

    public static void updateCache(Minecraft mc, int tickCounter, boolean reducedQuality) {
        if (mc == null || mc.world == null || mc.getRenderViewEntity() == null
                || !ModConfig.ENABLE_WATER_EFFECTS) {
            clear();
            return;
        }

        World world = mc.world;
        Entity view = mc.getRenderViewEntity();
        int centreX = MathHelper.floor(view.posX);
        int centreY = MathHelper.floor(view.posY);
        int centreZ = MathHelper.floor(view.posZ);
        int radius = Math.min(18, Math.max(8, ModConfig.WATER_EFFECT_RADIUS / 2));
        int interval = reducedQuality ? 20 : 12;

        boolean worldChanged = cachedWorld != world;
        boolean moved = Math.abs(centreX - lastCentreX) >= 3
                || Math.abs(centreZ - lastCentreZ) >= 3
                || Math.abs(centreY - lastCentreY) >= 8;
        if (!worldChanged && !moved && tickCounter - lastScanTick < interval) return;

        if (worldChanged) SURFACES.clear();
        cachedWorld = world;
        lastCentreX = centreX;
        lastCentreY = centreY;
        lastCentreZ = centreZ;
        lastScanTick = tickCounter;
        scanVisibleWaterColumns(world, centreX, centreY, centreZ, radius);
    }

    private static void scanVisibleWaterColumns(World world, int centreX, int centreY,
                                                int centreZ, int radius) {
        SURFACES.clear();
        int scanTop = Math.min(255, centreY + 24);
        int scanBottom = Math.max(0, centreY - 144);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = centreX - radius; x <= centreX + radius; x++) {
            for (int z = centreZ - radius; z <= centreZ + radius; z++) {
                if (!world.isBlockLoaded(new BlockPos(x, scanTop, z))) continue;

                for (int y = scanTop; y >= scanBottom; y--) {
                    mutable.setPos(x, y, z);
                    IBlockState state = world.getBlockState(mutable);
                    if (state.getMaterial() != net.minecraft.block.material.Material.WATER) continue;

                    mutable.setPos(x, y + 1, z);
                    if (world.getBlockState(mutable).getMaterial()
                            == net.minecraft.block.material.Material.WATER) continue;

                    int level = 0;
                    if (state.getBlock() instanceof BlockLiquid) {
                        level = state.getValue(BlockLiquid.LEVEL).intValue();
                    }
                    float fluidHeight = level >= 8 ? 1.0F
                            : 1.0F - ((Math.min(7, level) + 1) / 9.0F);
                    int depth = measureDepth(world, x, y, z);
                    BlockPos pos = new BlockPos(x, y, z);
                    int tint = world.getBiome(pos).getWaterColorMultiplier();

                    boolean north = !isWater(world, x, y, z - 1);
                    boolean south = !isWater(world, x, y, z + 1);
                    boolean west = !isWater(world, x - 1, y, z);
                    boolean east = !isWater(world, x + 1, y, z);

                    SURFACES.add(new SurfaceCell(x, z, y + fluidHeight, depth,
                            tint, north, south, west, east));
                    break;
                }

                if (SURFACES.size() >= MAX_CACHED_SURFACES) return;
            }
        }
    }

    private static int measureDepth(World world, int x, int y, int z) {
        int depth = 1;
        for (int offset = 1; offset <= 10; offset++) {
            BlockPos below = new BlockPos(x, y - offset, z);
            if (below.getY() < 0 || !world.isBlockLoaded(below)
                    || world.getBlockState(below).getMaterial()
                    != net.minecraft.block.material.Material.WATER) break;
            depth++;
        }
        return depth;
    }

    private static boolean isWater(World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return y >= 0 && y < 256 && world.isBlockLoaded(pos)
                && world.getBlockState(pos).getMaterial()
                == net.minecraft.block.material.Material.WATER;
    }

    public static void render(RenderWorldLastEvent event, boolean reducedQuality) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!ModConfig.ENABLE_WATER_EFFECTS || mc.world == null || mc.player == null
                || SURFACES.isEmpty() || cachedWorld != mc.world) return;

        Entity view = mc.getRenderViewEntity();
        if (view == null) return;

        double cameraX = mc.getRenderManager().viewerPosX;
        double cameraY = mc.getRenderManager().viewerPosY;
        double cameraZ = mc.getRenderManager().viewerPosZ;
        double renderRadius = Math.min(64.0D, Math.max(12.0D, ModConfig.WATER_EFFECT_RADIUS));
        double renderRadiusSq = renderRadius * renderRadius;

        float partialTicks = event.getPartialTicks();
        double worldTime = (mc.world.getTotalWorldTime() + partialTicks) * 0.035D;
        float waveStrength = ModConfig.ENABLE_WATER_RIPPLES
                ? ModConfig.WATER_WAVE_STRENGTH / 100.0F : 0.0F;
        int steps = reducedQuality ? LOW_GRID_STEPS : NORMAL_GRID_STEPS;
        float sunlight = mc.world.getSunBrightness(partialTicks);
        Vec3d sky = mc.world.getSkyColor(view, partialTicks);
        if (sky == null) sky = new Vec3d(0.36D, 0.57D, 0.78D);

        boolean wasTexture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean wasLighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean wasCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean previousDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(-cameraX, -cameraY, -cameraZ);
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableCull();
            GlStateManager.depthMask(false);

            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            for (SurfaceCell cell : SURFACES) {
                double centreDx = cell.x + 0.5D - cameraX;
                double centreDy = cell.surfaceY - cameraY;
                double centreDz = cell.z + 0.5D - cameraZ;
                if (centreDx * centreDx + centreDy * centreDy + centreDz * centreDz
                        > renderRadiusSq + 2.0D) continue;

                drawSurfaceCell(buffer, cell, steps, worldTime, waveStrength,
                        cameraX, cameraY, cameraZ, sky, sunlight);
                drawFoam(buffer, cell, worldTime, waveStrength);
            }
            tessellator.draw();
        } finally {
            GlStateManager.depthMask(previousDepthMask);
            if (wasCull) GlStateManager.enableCull(); else GlStateManager.disableCull();
            if (wasBlend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
            if (wasTexture) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
            if (wasLighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    private static void drawSurfaceCell(BufferBuilder buffer, SurfaceCell cell, int steps,
                                        double time, float waveStrength,
                                        double cameraX, double cameraY, double cameraZ,
                                        Vec3d sky, float sunlight) {
        for (int ix = 0; ix < steps; ix++) {
            double x0 = cell.x + ix / (double) steps;
            double x1 = cell.x + (ix + 1) / (double) steps;
            for (int iz = 0; iz < steps; iz++) {
                double z0 = cell.z + iz / (double) steps;
                double z1 = cell.z + (iz + 1) / (double) steps;
                putWaterVertex(buffer, cell, x0, z0, time, waveStrength,
                        cameraX, cameraY, cameraZ, sky, sunlight);
                putWaterVertex(buffer, cell, x0, z1, time, waveStrength,
                        cameraX, cameraY, cameraZ, sky, sunlight);
                putWaterVertex(buffer, cell, x1, z1, time, waveStrength,
                        cameraX, cameraY, cameraZ, sky, sunlight);
                putWaterVertex(buffer, cell, x1, z0, time, waveStrength,
                        cameraX, cameraY, cameraZ, sky, sunlight);
            }
        }
    }

    private static void putWaterVertex(BufferBuilder buffer, SurfaceCell cell,
                                       double x, double z, double time, float strength,
                                       double cameraX, double cameraY, double cameraZ,
                                       Vec3d sky, float sunlight) {
        double p1 = x * 0.115D + z * 0.061D + time * 0.78D;
        double p2 = z * 0.173D - x * 0.087D - time * 0.57D;
        double p3 = (x - z) * 0.205D + time * 1.08D;

        double s1 = Math.sin(p1);
        double s2 = Math.sin(p2);
        double s3 = Math.sin(p3);
        double wave = strength * (s1 * 0.018D + s2 * 0.010D + s3 * 0.0035D);
        double dHdx = strength * (Math.cos(p1) * 0.115D * 0.018D
                - Math.cos(p2) * 0.087D * 0.010D + Math.cos(p3) * 0.205D * 0.0035D);
        double dHdz = strength * (Math.cos(p1) * 0.061D * 0.018D
                + Math.cos(p2) * 0.173D * 0.010D - Math.cos(p3) * 0.205D * 0.0035D);

        double nx = -dHdx;
        double ny = 1.0D;
        double nz = -dHdz;
        double normalLength = Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= normalLength;
        ny /= normalLength;
        nz /= normalLength;

        double surfaceY = cell.surfaceY + wave + 0.004D;
        double vx = cameraX - x;
        double vy = cameraY - surfaceY;
        double vz = cameraZ - z;
        double viewLength = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (viewLength < 0.001D) viewLength = 0.001D;
        vx /= viewLength;
        vy /= viewLength;
        vz /= viewLength;

        double viewDot = Math.max(0.0D, Math.min(1.0D,
                Math.abs(nx * vx + ny * vy + nz * vz)));
        double fresnel = Math.pow(1.0D - viewDot, 4.0D);

        double lightX = Math.sin(time * 0.035D) * 0.42D;
        double lightY = 0.78D;
        double lightZ = Math.cos(time * 0.035D) * 0.42D;
        double hx = vx + lightX;
        double hy = vy + lightY;
        double hz = vz + lightZ;
        double halfLength = Math.sqrt(hx * hx + hy * hy + hz * hz);
        if (halfLength > 0.0001D) {
            hx /= halfLength;
            hy /= halfLength;
            hz /= halfLength;
        }
        double specular = Math.pow(Math.max(0.0D, nx * hx + ny * hy + nz * hz), 28.0D)
                * (0.10D + sunlight * 0.70D);

        float depth = MathHelper.clamp((cell.depth - 1) / 9.0F, 0.0F, 1.0F);
        float biomeR = ((cell.waterColor >> 16) & 255) / 255.0F;
        float biomeG = ((cell.waterColor >> 8) & 255) / 255.0F;
        float biomeB = (cell.waterColor & 255) / 255.0F;

        float ambient = 0.72F + sunlight * 0.22F;
        float red = biomeR * (0.84F - depth * 0.44F) * ambient + 0.045F;
        float green = biomeG * (0.86F - depth * 0.20F) * ambient + 0.065F;
        float blue = biomeB * (0.82F - depth * 0.08F) * ambient + 0.075F;

        float reflection = (float) (0.045D + fresnel * 0.46D);
        red = red * (1.0F - reflection) + (float) sky.x * reflection;
        green = green * (1.0F - reflection) + (float) sky.y * reflection;
        blue = blue * (1.0F - reflection) + (float) sky.z * reflection;

        float highlight = (float) Math.min(0.78D, specular * 0.74D);
        red += highlight * 0.82F;
        green += highlight * 0.91F;
        blue += highlight;

        int alpha = 232 + (int) (fresnel * 16.0D);
        buffer.pos(x, surfaceY, z).color(toByte(red), toByte(green), toByte(blue), alpha).endVertex();
    }

    private static void drawFoam(BufferBuilder buffer, SurfaceCell cell,
                                 double time, float waveStrength) {
        if (!cell.north && !cell.south && !cell.west && !cell.east) return;
        double x0 = cell.x + 0.02D;
        double x1 = cell.x + 0.98D;
        double z0 = cell.z + 0.02D;
        double z1 = cell.z + 0.98D;
        double band = 0.065D;

        if (cell.north) appendFoam(buffer, x0, x1, cell.z + 0.025D, cell.z + band,
                cell.surfaceY, time, waveStrength);
        if (cell.south) appendFoam(buffer, x0, x1, cell.z + 1.0D - band, cell.z + 0.975D,
                cell.surfaceY, time, waveStrength);
        if (cell.west) appendFoam(buffer, cell.x + 0.025D, cell.x + band, z0, z1,
                cell.surfaceY, time, waveStrength);
        if (cell.east) appendFoam(buffer, cell.x + 1.0D - band, cell.x + 0.975D, z0, z1,
                cell.surfaceY, time, waveStrength);
    }

    private static void appendFoam(BufferBuilder buffer, double x0, double x1,
                                   double z0, double z1, double y, double time,
                                   float strength) {
        double y00 = y + waveHeight(x0, z0, time, strength) + 0.008D;
        double y01 = y + waveHeight(x0, z1, time, strength) + 0.008D;
        double y11 = y + waveHeight(x1, z1, time, strength) + 0.008D;
        double y10 = y + waveHeight(x1, z0, time, strength) + 0.008D;
        buffer.pos(x0, y00, z0).color(188, 235, 243, 118).endVertex();
        buffer.pos(x0, y01, z1).color(210, 244, 248, 86).endVertex();
        buffer.pos(x1, y11, z1).color(210, 244, 248, 86).endVertex();
        buffer.pos(x1, y10, z0).color(188, 235, 243, 118).endVertex();
    }

    private static double waveHeight(double x, double z, double time, float strength) {
        if (strength <= 0.0F) return 0.0D;
        return strength * (Math.sin(x * 0.115D + z * 0.061D + time * 0.78D) * 0.018D
                + Math.sin(z * 0.173D - x * 0.087D - time * 0.57D) * 0.010D
                + Math.sin((x - z) * 0.205D + time * 1.08D) * 0.0035D);
    }

    private static int toByte(float value) {
        return MathHelper.clamp((int) (Math.max(0.0F, Math.min(1.0F, value)) * 255.0F), 0, 255);
    }

    public static void clear() {
        SURFACES.clear();
        cachedWorld = null;
        lastScanTick = -1000;
        lastCentreX = Integer.MIN_VALUE;
        lastCentreY = Integer.MIN_VALUE;
        lastCentreZ = Integer.MIN_VALUE;
    }

    private static final class SurfaceCell {
        private final int x;
        private final int z;
        private final double surfaceY;
        private final int depth;
        private final int waterColor;
        private final boolean north;
        private final boolean south;
        private final boolean west;
        private final boolean east;

        private SurfaceCell(int x, int z, double surfaceY, int depth, int waterColor,
                            boolean north, boolean south, boolean west, boolean east) {
            this.x = x;
            this.z = z;
            this.surfaceY = surfaceY;
            this.depth = depth;
            this.waterColor = waterColor;
            this.north = north;
            this.south = south;
            this.west = west;
            this.east = east;
        }
    }
}
