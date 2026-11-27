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
 * and no sine-wave displacement. Appearance comes from biome/depth absorption,
 * sky reflection, view-angle Fresnel response, sunlight highlights and shoreline
 * foam. Nearby surface cells are cached and drawn in a single batched pass.
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
        if (centreY > 110) radius = Math.max(8, radius - 4);
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

                if (SURFACES.size() >= MAX_CACHED_SURFACES) {
                    smoothSharedCorners();
                    return;
                }
            }
        }
        smoothSharedCorners();
    }

    /**
     * Derive each vertex from all water columns sharing that corner. Adjacent
     * tiles therefore meet at the same height and form a softer continuous
     * surface, including where source and flowing water levels vary.
     */
    private static void smoothSharedCorners() {
        Map<Long, SurfaceCell> byPosition = new HashMap<Long, SurfaceCell>(SURFACES.size() * 2);
        for (SurfaceCell cell : SURFACES) {
            byPosition.put(positionKey(cell.x, cell.z), cell);
        }

        for (SurfaceCell cell : SURFACES) {
            cell.northWestHeight = sharedCornerHeight(byPosition, cell.x, cell.z, cell.surfaceY);
            cell.northEastHeight = sharedCornerHeight(byPosition, cell.x + 1, cell.z, cell.surfaceY);
            cell.southEastHeight = sharedCornerHeight(byPosition, cell.x + 1, cell.z + 1, cell.surfaceY);
            cell.southWestHeight = sharedCornerHeight(byPosition, cell.x, cell.z + 1, cell.surfaceY);
        }
    }

    private static double sharedCornerHeight(Map<Long, SurfaceCell> cells,
                                             int cornerX, int cornerZ, double fallback) {
        double sum = 0.0D;
        int count = 0;
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                SurfaceCell adjacent = cells.get(positionKey(cornerX + dx, cornerZ + dz));
                if (adjacent == null) continue;
                sum += adjacent.surfaceY;
                count++;
            }
        }
        return count == 0 ? fallback : sum / count;
    }

    private static long positionKey(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
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
        // Keep the existing option as a gloss/highlight control for backwards
        // compatibility. It no longer controls geometric waves or ring effects.
        float surfaceGloss = ModConfig.ENABLE_WATER_RIPPLES
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

                drawSurfaceCell(buffer, cell, steps, surfaceGloss,
                        cameraX, cameraY, cameraZ, sky, sunlight);
                drawFoam(buffer, cell, worldTime, surfaceGloss);
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
                                        float surfaceGloss,
                                        double cameraX, double cameraY, double cameraZ,
                                        Vec3d sky, float sunlight) {
        for (int ix = 0; ix < steps; ix++) {
            double x0 = ix / (double) steps;
            double x1 = (ix + 1) / (double) steps;
            for (int iz = 0; iz < steps; iz++) {
                double z0 = iz / (double) steps;
                double z1 = (iz + 1) / (double) steps;
                putWaterVertex(buffer, cell, x0, z0, surfaceGloss,
                        cameraX, cameraY, cameraZ, sky, sunlight);
                putWaterVertex(buffer, cell, x0, z1, surfaceGloss,
                        cameraX, cameraY, cameraZ, sky, sunlight);
                putWaterVertex(buffer, cell, x1, z1, surfaceGloss,
                        cameraX, cameraY, cameraZ, sky, sunlight);
                putWaterVertex(buffer, cell, x1, z0, surfaceGloss,
                        cameraX, cameraY, cameraZ, sky, sunlight);
            }
        }
    }

    private static void putWaterVertex(BufferBuilder buffer, SurfaceCell cell,
                                       double localX, double localZ, float surfaceGloss,
                                       double cameraX, double cameraY, double cameraZ,
                                       Vec3d sky, float sunlight) {
        // Bilinear interpolation of the shared corner heights creates one
        // continuous surface across neighboring water blocks without waves.
        double front = lerp(cell.northWestHeight, cell.northEastHeight, localX);
        double back = lerp(cell.southWestHeight, cell.southEastHeight, localX);
        double surfaceY = lerp(front, back, localZ) + 0.004D;
        double x = cell.x + localX;
        double z = cell.z + localZ;

        // Calm-water normal: all visual movement comes from changing reflected
        // light and the day/sky colour, never from moving the water geometry.
        double nx = 0.0D;
        double ny = 1.0D;
        double nz = 0.0D;

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

        // A moving sun direction changes the highlight, not the surface shape.
        double celestialAngle = Minecraft.getMinecraft().world.getCelestialAngle(0.0F)
                * Math.PI * 2.0D;
        double lightX = Math.cos(celestialAngle) * 0.42D;
        double lightY = 0.78D;
        double lightZ = Math.sin(celestialAngle) * 0.42D;
        double hx = vx + lightX;
        double hy = vy + lightY;
        double hz = vz + lightZ;
        double halfLength = Math.sqrt(hx * hx + hy * hy + hz * hz);
        if (halfLength > 0.0001D) {
            hx /= halfLength;
            hy /= halfLength;
            hz /= halfLength;
        }
        double specular = Math.pow(Math.max(0.0D, nx * hx + ny * hy + nz * hz), 34.0D)
                * (0.10D + sunlight * 0.80D);

        float depth = MathHelper.clamp((cell.depth - 1) / 9.0F, 0.0F, 1.0F);
        float biomeR = ((cell.waterColor >> 16) & 255) / 255.0F;
        float biomeG = ((cell.waterColor >> 8) & 255) / 255.0F;
        float biomeB = (cell.waterColor & 255) / 255.0F;

        // Red light is absorbed first, followed by green; deep water shifts
        // toward blue and loses brightness progressively.
        float ambient = 0.70F + sunlight * 0.24F;
        float red = biomeR * (0.82F - depth * 0.52F) * ambient + 0.025F;
        float green = biomeG * (0.84F - depth * 0.29F) * ambient + 0.055F;
        float blue = biomeB * (0.82F - depth * 0.13F) * ambient + 0.085F;

        float reflection = (float) (0.07D + fresnel * 0.56D);
        reflection *= 0.78F + surfaceGloss * 0.22F;
        red = red * (1.0F - reflection) + (float) sky.x * reflection;
        green = green * (1.0F - reflection) + (float) sky.y * reflection;
        blue = blue * (1.0F - reflection) + (float) sky.z * reflection;

        float highlight = (float) Math.min(0.72D, specular * (0.22D + surfaceGloss * 0.72D));
        red += highlight * 0.78F;
        green += highlight * 0.90F;
        blue += highlight;

        int alpha = 230 + (int) (fresnel * 18.0D);
        buffer.pos(x, surfaceY, z)
                .color(toByte(red), toByte(green), toByte(blue), alpha)
                .endVertex();
    }

    private static double lerp(double start, double end, double amount) {
        return start + (end - start) * amount;
    }

    private static void drawFoam(BufferBuilder buffer, SurfaceCell cell,
                                 double time, float surfaceGloss) {
        if (!cell.north && !cell.south && !cell.west && !cell.east) return;
        double x0 = cell.x + 0.02D;
        double x1 = cell.x + 0.98D;
        double z0 = cell.z + 0.02D;
        double z1 = cell.z + 0.98D;
        double band = 0.065D;

        // Foam stays attached to the shore. Its brightness softly breathes;
        // no vertex is vertically displaced and no radial ring is created.
        float pulse = 0.90F + 0.10F * (float) Math.sin(time * 0.42D
                + cell.x * 0.13D + cell.z * 0.17D);
        int brightAlpha = (int) (112.0F * pulse);
        int softAlpha = (int) (82.0F * pulse);

        if (cell.north) appendFoam(buffer, x0, x1, cell.z + 0.025D, cell.z + band,
                cell.surfaceY, brightAlpha, softAlpha);
        if (cell.south) appendFoam(buffer, x0, x1, cell.z + 1.0D - band, cell.z + 0.975D,
                cell.surfaceY, brightAlpha, softAlpha);
        if (cell.west) appendFoam(buffer, cell.x + 0.025D, cell.x + band, z0, z1,
                cell.surfaceY, brightAlpha, softAlpha);
        if (cell.east) appendFoam(buffer, cell.x + 1.0D - band, cell.x + 0.975D, z0, z1,
                cell.surfaceY, brightAlpha, softAlpha);
    }

    private static void appendFoam(BufferBuilder buffer, double x0, double x1,
                                   double z0, double z1, double y,
                                   int brightAlpha, int softAlpha) {
        double surfaceY = y + 0.009D;
        buffer.pos(x0, surfaceY, z0).color(188, 235, 243, brightAlpha).endVertex();
        buffer.pos(x0, surfaceY, z1).color(210, 244, 248, softAlpha).endVertex();
        buffer.pos(x1, surfaceY, z1).color(210, 244, 248, softAlpha).endVertex();
        buffer.pos(x1, surfaceY, z0).color(188, 235, 243, brightAlpha).endVertex();
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
            this.waterColor = waterColor;
            this.north = north;
            this.south = south;
            this.west = west;
            this.east = east;
        }
    }
}
