package com.voxelcore.client.handler;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds restrained, deterministic mineral-grain highlights to existing stone
 * surfaces. It uses procedural vertex colour only: no texture replacement,
 * block registration, metadata changes, or world mutation.
 */
@SideOnly(Side.CLIENT)
public final class BlockSurfaceDetailRenderer {
    private static final int HORIZONTAL_RADIUS = 7;
    private static final int VERTICAL_RADIUS = 5;
    private static final int MAX_DETAILS = 600;
    private static final List<DetailBlock> DETAILS = new ArrayList<DetailBlock>();
    private static final float[][] LIGHT_COLOURS = {
            {0.72F, 0.72F, 0.72F}, {0.72F, 0.66F, 0.53F},
            {0.62F, 0.31F, 0.25F}, {0.88F, 0.84F, 0.61F},
            {0.28F, 0.28F, 0.29F}, {0.72F, 0.56F, 0.42F},
            {0.94F, 0.76F, 0.30F}, {0.90F, 0.24F, 0.19F},
            {0.34F, 0.48F, 0.86F}, {0.34F, 0.88F, 0.91F},
            {0.35F, 0.82F, 0.39F}, {0.92F, 0.88F, 0.77F},
            {0.78F, 0.48F, 0.38F}, {0.82F, 0.82F, 0.78F},
            {0.59F, 0.62F, 0.61F}, {0.88F, 0.77F, 0.57F},
            {0.87F, 0.55F, 0.34F}, {0.42F, 0.40F, 0.53F},
            {0.70F, 0.57F, 0.39F}, {0.72F, 0.39F, 0.29F},
            {0.35F, 0.70F, 0.68F}
    };
    private static final float[][] DARK_COLOURS = {
            {0.34F, 0.35F, 0.36F}, {0.38F, 0.34F, 0.28F},
            {0.28F, 0.12F, 0.10F}, {0.49F, 0.45F, 0.31F},
            {0.08F, 0.08F, 0.09F}, {0.36F, 0.25F, 0.18F},
            {0.49F, 0.35F, 0.10F}, {0.42F, 0.07F, 0.06F},
            {0.10F, 0.17F, 0.39F}, {0.08F, 0.38F, 0.43F},
            {0.09F, 0.34F, 0.13F}, {0.47F, 0.43F, 0.36F},
            {0.38F, 0.19F, 0.16F}, {0.42F, 0.42F, 0.40F},
            {0.28F, 0.30F, 0.30F}, {0.48F, 0.38F, 0.24F},
            {0.47F, 0.22F, 0.12F}, {0.12F, 0.11F, 0.18F},
            {0.35F, 0.25F, 0.16F}, {0.34F, 0.15F, 0.11F},
            {0.12F, 0.33F, 0.33F}
    };

    private World cachedWorld;
    private int lastScanTick = Integer.MIN_VALUE;
    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;
    private int lastZ = Integer.MIN_VALUE;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.getRenderViewEntity() == null) {
            DETAILS.clear();
            cachedWorld = null;
            return;
        }

        World world = mc.world;
        BlockPos cameraPos = new BlockPos(mc.getRenderViewEntity().posX,
                mc.getRenderViewEntity().posY, mc.getRenderViewEntity().posZ);
        int tick = (int) world.getTotalWorldTime();
        if (cachedWorld != world || tick - lastScanTick >= 8
                || Math.abs(cameraPos.getX() - lastX) >= 2
                || Math.abs(cameraPos.getY() - lastY) >= 2
                || Math.abs(cameraPos.getZ() - lastZ) >= 2) {
            rebuild(world, cameraPos);
            cachedWorld = world;
            lastScanTick = tick;
            lastX = cameraPos.getX();
            lastY = cameraPos.getY();
            lastZ = cameraPos.getZ();
        }
        if (DETAILS.isEmpty()) return;

        double camX = mc.getRenderManager().viewerPosX;
        double camY = mc.getRenderManager().viewerPosY;
        double camZ = mc.getRenderManager().viewerPosZ;
        boolean wasTexture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean wasLighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean wasCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
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
            for (DetailBlock detail : DETAILS) {
                drawBlockGrains(buffer, detail);
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

    private static void rebuild(World world, BlockPos centre) {
        DETAILS.clear();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = centre.getX() - HORIZONTAL_RADIUS; x <= centre.getX() + HORIZONTAL_RADIUS; x++) {
            for (int z = centre.getZ() - HORIZONTAL_RADIUS; z <= centre.getZ() + HORIZONTAL_RADIUS; z++) {
                for (int y = Math.max(1, centre.getY() - VERTICAL_RADIUS);
                     y <= Math.min(254, centre.getY() + VERTICAL_RADIUS); y++) {
                    pos.setPos(x, y, z);
                    if (!world.isBlockLoaded(pos)) continue;
                    IBlockState state = world.getBlockState(pos);
                    int palette = paletteFor(state);
                    if (palette < 0) continue;

                    int faces = 0;
                    if (isExposed(world, pos, EnumFace.UP)) faces |= 1;
                    if (isExposed(world, pos, EnumFace.DOWN)) faces |= 2;
                    if (isExposed(world, pos, EnumFace.NORTH)) faces |= 4;
                    if (isExposed(world, pos, EnumFace.SOUTH)) faces |= 8;
                    if (isExposed(world, pos, EnumFace.WEST)) faces |= 16;
                    if (isExposed(world, pos, EnumFace.EAST)) faces |= 32;
                    if (faces != 0) {
                        DETAILS.add(new DetailBlock(x, y, z, faces, palette,
                                mix(x * 73428767 ^ y * 912931 ^ z * 438289)));
                        if (DETAILS.size() >= MAX_DETAILS) return;
                    }
                }
            }
        }
    }

    private static boolean isExposed(World world, BlockPos pos, EnumFace face) {
        BlockPos adjacent;
        switch (face) {
            case UP: adjacent = pos.up(); break;
            case DOWN: adjacent = pos.down(); break;
            case NORTH: adjacent = pos.north(); break;
            case SOUTH: adjacent = pos.south(); break;
            case WEST: adjacent = pos.west(); break;
            default: adjacent = pos.east(); break;
        }
        return world.isBlockLoaded(adjacent) && !world.getBlockState(adjacent).isOpaqueCube();
    }

    /** Returns a procedural material palette, or -1 for blocks left untouched. */
    private static int paletteFor(IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.STONE) {
            // BlockStone metadata distinguishes the granite, diorite and andesite families.
            int variant = block.getMetaFromState(state);
            if (variant == 1 || variant == 2) return 12;
            if (variant == 3 || variant == 4) return 13;
            if (variant == 5 || variant == 6) return 14;
            return 0;
        }
        if (block == Blocks.COBBLESTONE || block == Blocks.MOSSY_COBBLESTONE) return 0;
        if (block == Blocks.GRAVEL) return 1;
        if (block == Blocks.NETHERRACK) return 2;
        if (block == Blocks.END_STONE) return 3;
        if (block == Blocks.COAL_ORE) return 4;
        if (block == Blocks.IRON_ORE) return 5;
        if (block == Blocks.GOLD_ORE) return 6;
        if (block == Blocks.REDSTONE_ORE || block == Blocks.LIT_REDSTONE_ORE) return 7;
        if (block == Blocks.LAPIS_ORE) return 8;
        if (block == Blocks.DIAMOND_ORE) return 9;
        if (block == Blocks.EMERALD_ORE) return 10;
        if (block == Blocks.QUARTZ_ORE) return 11;
        if (block == Blocks.SANDSTONE) return 12;
        if (block == Blocks.RED_SANDSTONE) return 13;
        if (block == Blocks.OBSIDIAN) return 14;
        if (block == Blocks.PRISMARINE) return 15;
        if (block == Blocks.STONEBRICK) return 16;
        if (block == Blocks.HARDENED_CLAY) return 17;
        if (block == Blocks.BRICK_BLOCK) return 18;
        if (block == Blocks.QUARTZ_BLOCK) return 19;
        return -1;
    }

    private static void drawBlockGrains(BufferBuilder buffer, DetailBlock detail) {
        // Two tiny mineral flecks per exposed face; deterministic positions avoid shimmer.
        for (int face = 0; face < 6; face++) {
            if ((detail.faces & (1 << face)) == 0) continue;
            for (int grain = 0; grain < 1; grain++) {
                int seed = mix(detail.seed + face * 19349663 + grain * 83492791);
                double u = 0.16D + unit(seed) * 0.68D;
                double v = 0.16D + unit(mix(seed + 31)) * 0.68D;
                double radius = 0.018D + unit(mix(seed + 71)) * 0.025D;
                boolean bright = (seed & 1) == 0;
                float[] colour = colour(detail.palette, bright);
                drawDiamond(buffer, detail.x, detail.y, detail.z, face, u, v,
                        radius, colour[0], colour[1], colour[2], 0.24F);
            }
        }
    }

    private static void drawDiamond(BufferBuilder b, int x, int y, int z, int face,
                                    double u, double v, double r,
                                    float red, float green, float blue, float alpha) {
        double x0 = x, y0 = y, z0 = z;
        switch (face) {
            case 0: // top
                vertex(b, x0 + u, y0 + 1.0015D, z0 + v - r, red, green, blue, alpha);
                vertex(b, x0 + u + r, y0 + 1.0015D, z0 + v, red, green, blue, alpha);
                vertex(b, x0 + u, y0 + 1.0015D, z0 + v + r, red, green, blue, alpha);
                vertex(b, x0 + u - r, y0 + 1.0015D, z0 + v, red, green, blue, alpha);
                break;
            case 1: // bottom
                vertex(b, x0 + u, y0 - 0.0015D, z0 + v - r, red, green, blue, alpha);
                vertex(b, x0 + u + r, y0 - 0.0015D, z0 + v, red, green, blue, alpha);
                vertex(b, x0 + u, y0 - 0.0015D, z0 + v + r, red, green, blue, alpha);
                vertex(b, x0 + u - r, y0 - 0.0015D, z0 + v, red, green, blue, alpha);
                break;
            case 2: // north
            case 3: // south
                double zz = z0 + (face == 2 ? -0.0015D : 1.0015D);
                vertex(b, x0 + u, y0 + v - r, zz, red, green, blue, alpha);
                vertex(b, x0 + u + r, y0 + v, zz, red, green, blue, alpha);
                vertex(b, x0 + u, y0 + v + r, zz, red, green, blue, alpha);
                vertex(b, x0 + u - r, y0 + v, zz, red, green, blue, alpha);
                break;
            default: // west/east
                double xx = x0 + (face == 4 ? -0.0015D : 1.0015D);
                vertex(b, xx, y0 + v - r, z0 + u, red, green, blue, alpha);
                vertex(b, xx, y0 + v, z0 + u + r, red, green, blue, alpha);
                vertex(b, xx, y0 + v + r, z0 + u, red, green, blue, alpha);
                vertex(b, xx, y0 + v, z0 + u - r, red, green, blue, alpha);
                break;
        }
    }

    private static void vertex(BufferBuilder b, double x, double y, double z,
                               float r, float g, float blue, float a) {
        b.pos(x, y, z).color(r, g, blue, a).endVertex();
    }

    private static float[] colour(int palette, boolean bright) {
        int safePalette = Math.max(0, Math.min(LIGHT_COLOURS.length - 1, palette));
        return bright ? LIGHT_COLOURS[safePalette] : DARK_COLOURS[safePalette];
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

    private enum EnumFace { UP, DOWN, NORTH, SOUTH, WEST, EAST }

    private static final class DetailBlock {
        final int x, y, z, faces, palette, seed;
        DetailBlock(int x, int y, int z, int faces, int palette, int seed) {
            this.x = x; this.y = y; this.z = z;
            this.faces = faces; this.palette = palette; this.seed = seed;
        }
    }
}
