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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders a calm, custom water surface over Minecraft's existing water blocks.
 * It does not change block states, flow, levels, collisions or server simulation.
 *
 * The surface geometry is deliberately stationary: there are no expanding rings
 * and only tiny multi-directional surface relief, not large ocean-like sine waves. Appearance comes from biome/depth absorption,
 * sky reflection, view-angle Fresnel response, sunlight highlights and shoreline
 * foam. Nearby surface cells are cached and drawn in a single batched pass.
 */
@SideOnly(Side.CLIENT)
public final class WaterSurfaceRenderer {
    private static final int MAX_CACHED_SURFACES = 760;
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
        int radius = Math.min(14, Math.max(6, ModConfig.WATER_EFFECT_RADIUS / 3));
        if (centreY > 110) radius = Math.max(6, radius - 3);
        int interval = reducedQuality ? 36 : 20;

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
        int scanTop = Math.min(255, centreY + 20);
        int scanBottom = Math.max(0, centreY - 28);
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

                    // Ask the 1.12.2 vanilla fluid block for its rendered height.
                    // This includes its falling/source-state and above-fluid rules.
                    mutable.setPos(x, y, z);
                    float topY = BlockLiquid.getLiquidHeight(state, world, mutable);
                    int depth = measureDepth(world, x, y, z);
                    int tint = world.getBiome(mutable).getWaterColorMultiplier();

                    boolean north = isExposedWaterEdge(world, x, y, z - 1);
                    boolean south = isExposedWaterEdge(world, x, y, z + 1);
                    boolean west = isExposedWaterEdge(world, x - 1, y, z);
                    boolean east = isExposedWaterEdge(world, x + 1, y, z);

                    SURFACES.add(new SurfaceCell(x, z, topY, depth,
                            tint, north, south, west, east));
                    break;
                }

                if (SURFACES.size() >= MAX_CACHED_SURFACES) {
                    smoothSharedCorners();
                    return;
                }
            }
        }
        smoothSharedCorners();
    }

    private static int measureDepth(World world, int x, int y, int z) {
        int depth = 1;
        BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos(x, y, z);
        for (int offset = 1; offset <= 12; offset++) {
            below.setPos(x, y - offset, z);
            if (below.getY() < 0 || !world.isBlockLoaded(below)
                    || world.getBlockState(below).getMaterial()
                    != net.minecraft.block.material.Material.WATER) break;
            depth++;
        }
        return depth;
    }

    /**
     * Blend the four heights around each water-cell corner. Only nearby water
     * columns at a similar level contribute, preventing the surface from
     * bridging banks, waterfalls or separate pools.
     */
    private static void smoothSharedCorners() {
        Map<Long, SurfaceCell> cells = new HashMap<Long, SurfaceCell>(SURFACES.size() * 2);
        for (SurfaceCell cell : SURFACES) {
            cells.put(positionKey(cell.x, cell.z), cell);
        }
        for (SurfaceCell cell : SURFACES) {
            cell.northWestHeight = sharedCornerHeight(cells, cell.x, cell.z, cell.surfaceY);
            cell.northEastHeight = sharedCornerHeight(cells, cell.x + 1, cell.z, cell.surfaceY);
            cell.southEastHeight = sharedCornerHeight(cells, cell.x + 1, cell.z + 1, cell.surfaceY);
            cell.southWestHeight = sharedCornerHeight(cells, cell.x, cell.z + 1, cell.surfaceY);
        }
    }

    private static double sharedCornerHeight(Map<Long, SurfaceCell> cells,
                                             int cornerX, int cornerZ, double fallback) {
        double sum = 0.0D;
        int count = 0;
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                SurfaceCell adjacent = cells.get(positionKey(cornerX + dx, cornerZ + dz));
                if (adjacent == null || Math.abs(adjacent.surfaceY - fallback) > 0.22D) continue;
                sum += adjacent.surfaceY;
                count++;
            }
        }
        return count == 0 ? fallback : sum / count;
    }

    private static long positionKey(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

    private static boolean isExposedWaterEdge(World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return y >= 0 && y < 256 && world.isBlockLoaded(pos)
                && world.getBlockState(pos).getMaterial()
                != net.minecraft.block.material.Material.WATER;
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
        double renderRadius = Math.min(64.0D, Math.max(10.0D, ModConfig.WATER_EFFECT_RADIUS));
        double renderRadiusSq = renderRadius * renderRadius;

        float partialTicks = event.getPartialTicks();
        double worldTime = (mc.world.getTotalWorldTime() + partialTicks) * 0.055D;
        double celestialAngle = mc.world.getCelestialAngle(partialTicks) * Math.PI * 2.0D;
        float gloss = ModConfig.ENABLE_WATER_RIPPLES
                ? ModConfig.WATER_WAVE_STRENGTH / 100.0F : 0.0F;
        float sunlight = mc.world.getSunBrightness(partialTicks);
        Vec3d sky = mc.world.getSkyColor(view, partialTicks);
        if (sky == null) sky = new Vec3d(0.34D, 0.52D, 0.70D);
        // The sun direction is identical for every vertex in this frame.
        // Compute and normalize it once instead of doing trig per water vertex.
        double sunX = -Math.cos(celestialAngle) * 0.45D;
        double sunY = 0.78D;
        double sunZ = -Math.sin(celestialAngle) * 0.45D;
        double sunLength = Math.sqrt(sunX * sunX + sunY * sunY + sunZ * sunZ);
        sunX /= sunLength;
        sunY /= sunLength;
        sunZ /= sunLength;

        WorldRenderStateGuard renderState = WorldRenderStateGuard.capture();

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
                double dx = cell.x + 0.5D - cameraX;
                double dy = cell.surfaceY - cameraY;
                double dz = cell.z + 0.5D - cameraZ;
                if (dx * dx + dy * dy + dz * dz > renderRadiusSq) continue;

                // A low-alpha tint sits just above vanilla's liquid. It must never
                // replace the vanilla surface with an opaque, untextured white sheet.
                int gridSegments = ModConfig.WATER_RENDER_QUALITY >= 2
                        && !reducedQuality ? 2 : 1;
                drawTintedSurface(buffer, cell, gloss, worldTime,
                        cameraX, cameraY, cameraZ, sky, sunlight,
                        sunX, sunY, sunZ, gridSegments);
                if (ModConfig.ENABLE_WATER_HIGHLIGHTS && ModConfig.WATER_RENDER_QUALITY > 0
                        && !reducedQuality) {
                    drawMicroHighlight(buffer, cell, worldTime, gloss, sky, sunlight);
                }
                if (ModConfig.ENABLE_WATER_FOAM) {
                    drawFoam(buffer, cell, worldTime, gloss);
                }
            }
            tessellator.draw();
        } finally {
            renderState.restore();
            GlStateManager.popMatrix();
        }
    }

    private static void drawTintedSurface(BufferBuilder buffer, SurfaceCell cell,
                                          float gloss, double time,
                                          double cameraX, double cameraY, double cameraZ,
                                          Vec3d sky, float sunlight,
                                          double sunX, double sunY, double sunZ, int segments) {
        for (int gz = 0; gz < segments; gz++) {
            double v0 = gz / (double) segments;
            double v1 = (gz + 1) / (double) segments;
            for (int gx = 0; gx < segments; gx++) {
                double u0 = gx / (double) segments;
                double u1 = (gx + 1) / (double) segments;
                emitWaterVertex(buffer, cell, u0, v0, gloss, time,
                        cameraX, cameraY, cameraZ, sky, sunlight, sunX, sunY, sunZ);
                emitWaterVertex(buffer, cell, u0, v1, gloss, time,
                        cameraX, cameraY, cameraZ, sky, sunlight, sunX, sunY, sunZ);
                emitWaterVertex(buffer, cell, u1, v1, gloss, time,
                        cameraX, cameraY, cameraZ, sky, sunlight, sunX, sunY, sunZ);
                emitWaterVertex(buffer, cell, u1, v0, gloss, time,
                        cameraX, cameraY, cameraZ, sky, sunlight, sunX, sunY, sunZ);
            }
        }
    }

    /**
     * A tiny multi-directional surface relief gives the water a changing normal
     * without turning it into large, obvious sine waves. The same world-space
     * function is used at every neighbouring cell edge, so vertices stay joined.
     */
    private static double waveHeight(double x, double z, double time, float gloss) {
        if (gloss <= 0.001F) return 0.0D;
        double a = Math.sin(x * 0.83D + z * 0.51D + time * 1.13D);
        double b = Math.sin(x * 0.34D - z * 0.71D - time * 0.76D);
        double c = Math.sin(x * 1.31D + z * 1.17D - time * 0.43D);
        return (a * 0.0048D + b * 0.0031D + c * 0.0015D) * gloss;
    }

    private static void emitWaterVertex(BufferBuilder buffer, SurfaceCell cell,
                                        double u, double v, float gloss, double time,
                                        double cameraX, double cameraY, double cameraZ,
                                        Vec3d sky, float sunlight,
                                        double sunX, double sunY, double sunZ) {
        double x = cell.x + u;
        double z = cell.z + v;
        double north = lerp(cell.northWestHeight, cell.northEastHeight, u);
        double south = lerp(cell.southWestHeight, cell.southEastHeight, u);
        double baseY = lerp(north, south, v);
        double t1 = x * 0.83D + z * 0.51D + time * 1.13D;
        double t2 = x * 0.34D - z * 0.71D - time * 0.76D;
        double t3 = x * 1.31D + z * 1.17D - time * 0.43D;
        double sin1 = Math.sin(t1), cos1 = Math.cos(t1);
        double sin2 = Math.sin(t2), cos2 = Math.cos(t2);
        double sin3 = Math.sin(t3), cos3 = Math.cos(t3);
        double wave = gloss <= 0.001F ? 0.0D
                : (sin1 * 0.0048D + sin2 * 0.0031D + sin3 * 0.0015D) * gloss;
        double y = baseY + 0.004D + wave;

        double baseSlopeX = lerp(cell.northEastHeight - cell.northWestHeight,
                cell.southEastHeight - cell.southWestHeight, v);
        double baseSlopeZ = south - north;
        double waveSlopeX = gloss <= 0.001F ? 0.0D
                : (cos1 * 0.83D * 0.0048D + cos2 * 0.34D * 0.0031D
                + cos3 * 1.31D * 0.0015D) * gloss;
        double waveSlopeZ = gloss <= 0.001F ? 0.0D
                : (cos1 * 0.51D * 0.0048D - cos2 * 0.71D * 0.0031D
                + cos3 * 1.17D * 0.0015D) * gloss;

        double nx = -(baseSlopeX + waveSlopeX);
        double ny = 1.0D;
        double nz = -(baseSlopeZ + waveSlopeZ);
        double normalLength = Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= normalLength;
        ny /= normalLength;
        nz /= normalLength;

        double vx = cameraX - x;
        double vy = cameraY - y;
        double vz = cameraZ - z;
        double viewLength = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (viewLength > 0.001D) {
            vx /= viewLength;
            vy /= viewLength;
            vz /= viewLength;
        } else {
            vx = 0.0D; vy = 1.0D; vz = 0.0D;
        }

        double halfX = sunX + vx;
        double halfY = sunY + vy;
        double halfZ = sunZ + vz;
        double halfLength = Math.sqrt(halfX * halfX + halfY * halfY + halfZ * halfZ);
        if (halfLength > 0.001D) {
            halfX /= halfLength; halfY /= halfLength; halfZ /= halfLength;
        }

        double ndotv = Math.max(0.0D, nx * vx + ny * vy + nz * vz);
        double fresnelBase = 1.0D - ndotv;
        double fresnelSq = fresnelBase * fresnelBase;
        double fresnel = fresnelSq * fresnelSq * fresnelBase;
        double diffuse = Math.max(0.0D, nx * sunX + ny * sunY + nz * sunZ);
        double specularBase = Math.max(0.0D, nx * halfX + ny * halfY + nz * halfZ);
        double specularPow4 = specularBase * specularBase;
        specularPow4 *= specularPow4;
        double specularPow8 = specularPow4 * specularPow4;
        double specular = specularPow8 * specularPow8 * specularPow8 * specularPow4
                * sunlight * (0.04D + gloss * 0.28D);

        float depth = MathHelper.clamp((cell.depth - 1) / 11.0F, 0.0F, 1.0F);
        float biomeRed = ((cell.waterColor >> 16) & 255) / 255.0F;
        float biomeGreen = ((cell.waterColor >> 8) & 255) / 255.0F;
        float biomeBlue = (cell.waterColor & 255) / 255.0F;
        float absorption = 0.76F - depth * 0.34F;
        float red = biomeRed * absorption * (0.62F + (float) diffuse * 0.12F);
        float green = biomeGreen * (0.70F - depth * 0.13F) * (0.66F + (float) diffuse * 0.12F);
        float blue = biomeBlue * (0.78F - depth * 0.07F) * (0.72F + (float) diffuse * 0.13F);

        double reflection = 0.025D + fresnel * (0.08D + gloss * 0.08D);
        red = (float) (red * (1.0D - reflection) + sky.x * reflection);
        green = (float) (green * (1.0D - reflection) + sky.y * reflection);
        blue = (float) (blue * (1.0D - reflection) + sky.z * reflection);
        red += (float) specular;
        green += (float) (specular * 0.98D);
        blue += (float) (specular * 0.92D);

        int alpha = 34 + (int) (gloss * 12.0F);
        buffer.pos(x, y, z).color(toByte(red), toByte(green), toByte(blue), alpha).endVertex();
    }

    private static void drawMicroHighlight(BufferBuilder buffer, SurfaceCell cell,
                                           double time, float gloss, Vec3d sky, float sunlight) {
        double phase = cell.highlightPhase;
        double glide = (time * cell.highlightSpeed + cell.highlightOffset) % 1.0D;
        double u = 0.16D + glide * 0.62D;
        double v = cell.highlightV;
        double length = cell.highlightLength;
        double width = cell.highlightWidth;
        double dx = Math.cos(phase) * length;
        double dz = Math.sin(phase) * length;
        double px = -Math.sin(phase) * width;
        double pz = Math.cos(phase) * width;
        double x = cell.x + u;
        double z = cell.z + v;
        double y = cell.surfaceY + waveHeight(x, z, time, gloss) + 0.008D;
        int alpha = 12 + (int) (sunlight * 18.0F);
        int red = toByte((float) Math.min(1.0D, sky.x * 0.72D + 0.20D));
        int green = toByte((float) Math.min(1.0D, sky.y * 0.78D + 0.20D));
        int blue = toByte((float) Math.min(1.0D, sky.z * 0.82D + 0.18D));
        emitQuad(buffer,
                x - dx - px, y, z - dz - pz,
                x - dx + px, y, z - dz + pz,
                x + dx + px, y, z + dz + pz,
                x + dx - px, y, z + dz - pz,
                red, green, blue, alpha);
    }

    private static void emitQuad(BufferBuilder buffer,
                                 double x0, double y0, double z0,
                                 double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 double x3, double y3, double z3,
                                 int red, int green, int blue, int alpha) {
        buffer.pos(x0, y0, z0).color(red, green, blue, alpha).endVertex();
        buffer.pos(x1, y1, z1).color(red, green, blue, alpha).endVertex();
        buffer.pos(x2, y2, z2).color(red, green, blue, alpha).endVertex();
        buffer.pos(x3, y3, z3).color(red, green, blue, alpha).endVertex();
    }

    private static double lerp(double start, double end, double amount) {
        return start + (end - start) * amount;
    }

    private static void drawFoam(BufferBuilder buffer, SurfaceCell cell,
                                 double time, float gloss) {
        if (!cell.north && !cell.south && !cell.west && !cell.east) return;
        double x0 = cell.x + 0.03D;
        double x1 = cell.x + 0.97D;
        double z0 = cell.z + 0.03D;
        double z1 = cell.z + 0.97D;
        double band = 0.035D + gloss * 0.020D;
        float pulse = 0.72F + 0.28F * (float) Math.sin(time * 0.32D
                + cell.x * 0.13D + cell.z * 0.17D);
        int brightAlpha = (int) (26.0F * pulse);
        int softAlpha = (int) (16.0F * pulse);
        double y = cell.surfaceY + 0.008D;

        if (cell.north) emitQuad(buffer, x0, y, cell.z + 0.012D,
                x0, y, cell.z + band, x1, y, cell.z + band, x1, y, cell.z + 0.012D,
                180, 220, 225, brightAlpha);
        if (cell.south) emitQuad(buffer, x0, y, cell.z + 1.0D - band,
                x0, y, cell.z + 0.988D, x1, y, cell.z + 0.988D, x1, y, cell.z + 1.0D - band,
                190, 228, 232, softAlpha);
        if (cell.west) emitQuad(buffer, cell.x + 0.012D, y, z0,
                cell.x + band, y, z0, cell.x + band, y, z1, cell.x + 0.012D, y, z1,
                180, 220, 225, brightAlpha);
        if (cell.east) emitQuad(buffer, cell.x + 1.0D - band, y, z0,
                cell.x + 0.988D, y, z0, cell.x + 0.988D, y, z1, cell.x + 1.0D - band, y, z1,
                190, 228, 232, softAlpha);
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
        private double northWestHeight;
        private double northEastHeight;
        private double southEastHeight;
        private double southWestHeight;
        private final int depth;
        private final double highlightPhase;
        private final double highlightSpeed;
        private final double highlightOffset;
        private final double highlightV;
        private final double highlightLength;
        private final double highlightWidth;
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
            this.northWestHeight = surfaceY;
            this.northEastHeight = surfaceY;
            this.southEastHeight = surfaceY;
            this.southWestHeight = surfaceY;
            this.depth = depth;
            int seed = mix(x * 73428767 ^ z * 438289 ^ (int) (surfaceY * 912931.0D));
            this.highlightPhase = unit(seed) * Math.PI * 2.0D;
            this.highlightSpeed = 0.055D + unit(mix(seed + 13)) * 0.025D;
            this.highlightOffset = unit(mix(seed + 27));
            this.highlightV = 0.20D + unit(mix(seed + 41)) * 0.60D;
            this.highlightLength = 0.14D + unit(mix(seed + 59)) * 0.20D;
            this.highlightWidth = 0.010D + unit(mix(seed + 71)) * 0.012D;
            this.waterColor = waterColor;
            this.north = north;
            this.south = south;
            this.west = west;
            this.east = east;
        }
    }
}
