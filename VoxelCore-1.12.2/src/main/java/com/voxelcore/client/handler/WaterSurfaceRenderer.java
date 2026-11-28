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
    private static final int MAX_CACHED_SURFACES = 900;
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
        int scanBottom = Math.max(0, centreY - 88);
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
                    // Match BlockLiquid's 1.12.2 surface height for both source and falling states.
                    int heightLevel = level >= 8 ? 0 : Math.min(7, level);
                    float fluidHeight = 1.0F - ((heightLevel + 1) / 9.0F);
                    int depth = measureDepth(world, x, y, z);
                    BlockPos pos = new BlockPos(x, y, z);
                    int tint = world.getBiome(pos).getWaterColorMultiplier();

                    boolean north = !isWater(world, x, y, z - 1);
                    boolean south = !isWater(world, x, y, z + 1);
                    boolean west = !isWater(world, x - 1, y, z);
                    boolean east = !isWater(world, x + 1, y, z);

                    double top = y + fluidHeight;
                    SURFACES.add(new SurfaceCell(x, z, top, depth,
                            tint, north, south, west, east));
                    break;
                }

                if (SURFACES.size() >= MAX_CACHED_SURFACES) {
                    return;
                }
            }
        }

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

        boolean wasTexture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean wasLighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean wasCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

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
                drawTintedSurface(buffer, cell, gloss, celestialAngle,
                        cameraX, cameraY, cameraZ, sky, sunlight);
                if (ModConfig.ENABLE_WATER_HIGHLIGHTS && ModConfig.WATER_RENDER_QUALITY > 0
                        && (!reducedQuality || ModConfig.WATER_RENDER_QUALITY >= 2)) {
                    drawMicroHighlight(buffer, cell, worldTime, sky, sunlight);
                }
                if (ModConfig.ENABLE_WATER_FOAM) {
                    drawFoam(buffer, cell, worldTime, gloss);
                }
            }
            tessellator.draw();
        } finally {
            GlStateManager.depthMask(depthMask);
            if (wasCull) GlStateManager.enableCull(); else GlStateManager.disableCull();
            if (wasBlend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
            if (wasTexture) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
            if (wasLighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    private static void drawTintedSurface(BufferBuilder buffer, SurfaceCell cell,
                                          float gloss, double celestialAngle,
                                          double cameraX, double cameraY, double cameraZ,
                                          Vec3d sky, float sunlight) {
        // Use the correct fluid height per cell. No averaging across shores or
        // across different flowing-water levels, so the mesh cannot bridge banks.
        double x0 = cell.x + 0.006D;
        double x1 = cell.x + 0.994D;
        double z0 = cell.z + 0.006D;
        double z1 = cell.z + 0.994D;
        double y = cell.surfaceY + 0.003D;

        float depth = MathHelper.clamp((cell.depth - 1) / 8.0F, 0.0F, 1.0F);
        float br = ((cell.waterColor >> 16) & 255) / 255.0F;
        float bg = ((cell.waterColor >> 8) & 255) / 255.0F;
        float bb = (cell.waterColor & 255) / 255.0F;
        float ambient = 0.46F + sunlight * 0.16F;

        float red = br * (0.68F - depth * 0.34F) * ambient + 0.012F;
        float green = bg * (0.72F - depth * 0.20F) * ambient + 0.025F;
        float blue = bb * (0.76F - depth * 0.10F) * ambient + 0.040F;

        double fresnel = calculateFresnel(x0 + 0.5D, y, z0 + 0.5D,
                cameraX, cameraY, cameraZ);
        float reflection = (float) (0.018D + fresnel * 0.105D);
        reflection *= 0.70F + gloss * 0.30F;
        red = red * (1.0F - reflection) + (float) sky.x * reflection;
        green = green * (1.0F - reflection) + (float) sky.y * reflection;
        blue = blue * (1.0F - reflection) + (float) sky.z * reflection;

        double sunPulse = 0.5D + 0.5D * Math.sin(celestialAngle + cell.x * 0.011D + cell.z * 0.013D);
        float sunTint = (float) (sunPulse * sunlight * (0.025D + gloss * 0.045D));
        red += sunTint * 0.70F;
        green += sunTint * 0.88F;
        blue += sunTint;

        // Deliberately modest alpha: vanilla water texture, transparency and
        // its animated flow remain visible beneath Minefade's surface treatment.
        int alpha = 42 + (int) (gloss * 10.0F);
        emitQuad(buffer, x0, y, z0, x0, y, z1, x1, y, z1, x1, y, z0,
                toByte(red), toByte(green), toByte(blue), alpha);
    }

    private static double calculateFresnel(double x, double y, double z,
                                           double cameraX, double cameraY, double cameraZ) {
        double vx = cameraX - x;
        double vy = cameraY - y;
        double vz = cameraZ - z;
        double length = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (length < 0.001D) return 0.0D;
        double cosine = Math.max(0.0D, Math.min(1.0D, Math.abs(vy / length)));
        return Math.pow(1.0D - cosine, 3.0D);
    }

    private static void drawMicroHighlight(BufferBuilder buffer, SurfaceCell cell,
                                           double time, Vec3d sky, float sunlight) {
        int seed = mix(cell.x * 73428767 ^ cell.z * 438289);
        double phase = unit(seed) * Math.PI * 2.0D;
        double glide = (time * 0.07D + unit(mix(seed + 13))) % 1.0D;
        double u = 0.16D + glide * 0.62D;
        double v = 0.20D + unit(mix(seed + 27)) * 0.60D;
        double length = 0.14D + unit(mix(seed + 41)) * 0.20D;
        double width = 0.010D + unit(mix(seed + 59)) * 0.012D;
        double dx = Math.cos(phase) * length;
        double dz = Math.sin(phase) * length;
        double px = -Math.sin(phase) * width;
        double pz = Math.cos(phase) * width;
        double x = cell.x + u;
        double z = cell.z + v;
        double y = cell.surfaceY + 0.006D;
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
