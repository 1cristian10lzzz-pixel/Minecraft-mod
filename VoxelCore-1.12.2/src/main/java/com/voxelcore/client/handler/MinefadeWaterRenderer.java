package com.voxelcore.client.handler;

import com.voxelcore.config.ModConfig;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockFluidRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Replaces the visible top of Minecraft water while leaving the actual fluid
 * blocks, height metadata, collision, flow, buckets and world simulation alone.
 *
 * The vanilla renderer still draws the sides and all non-water fluids. The
 * custom top is a smoothly shaded, subdivided surface with biome/depth colour
 * and broad animated light glints. Vertex positions do not oscillate and no
 * expanding ring geometry is used.
 */
@SideOnly(Side.CLIENT)
public final class MinefadeWaterRenderer extends BlockFluidRenderer {
    private static final ResourceLocation WATER_STILL =
            new ResourceLocation("minecraft", "blocks/water_still");

    private static volatile TextureAtlasSprite waterStillSprite;

    public MinefadeWaterRenderer(BlockColors blockColors) {
        super(blockColors);
    }

    /** Called after atlas stitching and once when the renderer is installed. */
    public static void refreshAtlasSprites(TextureMap textureMap) {
        if (textureMap == null) return;
        waterStillSprite = textureMap.getAtlasSprite(WATER_STILL.toString());
    }

    @Override
    public boolean renderFluid(IBlockAccess blockAccess, IBlockState blockStateIn,
                               BlockPos blockPosIn, BufferBuilder bufferBuilderIn) {
        boolean vanillaRendered = super.renderFluid(blockAccess, blockStateIn, blockPosIn, bufferBuilderIn);

        // Restrict the custom height calculation to BlockLiquid implementations;
        // other mods sometimes use Material.WATER without the vanilla LEVEL property.
        if (!ModConfig.ENABLE_WATER_EFFECTS
                || !(blockStateIn.getBlock() instanceof BlockLiquid)
                || blockStateIn.getMaterial() != Material.WATER
                || !isTopExposed(blockAccess, blockPosIn)) {
            return vanillaRendered;
        }

        TextureAtlasSprite sprite = waterStillSprite;
        if (sprite == null) return vanillaRendered;

        renderCustomSurface(blockAccess, blockPosIn, bufferBuilderIn, sprite);
        return true;
    }

    private static boolean isTopExposed(IBlockAccess world, BlockPos pos) {
        BlockPos abovePos = pos.up();
        IBlockState above = world.getBlockState(abovePos);
        if (above.getMaterial() == Material.WATER) return false;
        return !above.isSideSolid(world, abovePos, EnumFacing.DOWN);
    }

    private static void renderCustomSurface(IBlockAccess world, BlockPos pos,
                                            BufferBuilder buffer, TextureAtlasSprite sprite) {
        int subdivisions = WaterEffectsHandler.isReducedQuality() ? 1 : 3;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        float h00 = getCornerHeight(world, pos.getX(), pos.getY(), pos.getZ(), cursor);
        float h10 = getCornerHeight(world, pos.getX() + 1, pos.getY(), pos.getZ(), cursor);
        float h11 = getCornerHeight(world, pos.getX() + 1, pos.getY(), pos.getZ() + 1, cursor);
        float h01 = getCornerHeight(world, pos.getX(), pos.getY(), pos.getZ() + 1, cursor);

        int depth = getWaterDepth(world, pos, cursor);
        int biomeColor = world.getBiome(pos).getWaterColorMultiplier();
        float biomeRed = ((biomeColor >> 16) & 255) / 255.0F;
        float biomeGreen = ((biomeColor >> 8) & 255) / 255.0F;
        float biomeBlue = (biomeColor & 255) / 255.0F;

        float deep = clamp((depth - 1) / 9.0F, 0.0F, 1.0F);
        float baseRed = lerp(lerp(biomeRed, 0.15F, 0.34F), 0.025F, deep * 0.88F);
        float baseGreen = lerp(lerp(biomeGreen, 0.61F, 0.34F), 0.095F, deep * 0.88F);
        float baseBlue = lerp(lerp(biomeBlue, 0.72F, 0.28F), 0.26F, deep * 0.76F);

        int packedLight = world.getCombinedLight(pos, 0);
        int lightU = packedLight & 65535;
        int lightV = (packedLight >> 16) & 65535;

        float causticStrength = ModConfig.ENABLE_WATER_RIPPLES
                ? clamp(ModConfig.WATER_WAVE_STRENGTH / 100.0F, 0.0F, 1.0F)
                : 0.0F;
        double time = System.currentTimeMillis() * 0.00022D;

        for (int z = 0; z < subdivisions; z++) {
            float z0 = z / (float) subdivisions;
            float z1 = (z + 1) / (float) subdivisions;

            for (int x = 0; x < subdivisions; x++) {
                float x0 = x / (float) subdivisions;
                float x1 = (x + 1) / (float) subdivisions;

                // Smoothly interpolate the four shared corner heights. The
                // surface is calm: only its light pattern changes over time.
                addTopVertex(pos, buffer, sprite, x0, z0, h00, h10, h11, h01,
                        baseRed, baseGreen, baseBlue, deep, causticStrength, time, lightU, lightV);
                addTopVertex(pos, buffer, sprite, x0, z1, h00, h10, h11, h01,
                        baseRed, baseGreen, baseBlue, deep, causticStrength, time, lightU, lightV);
                addTopVertex(pos, buffer, sprite, x1, z1, h00, h10, h11, h01,
                        baseRed, baseGreen, baseBlue, deep, causticStrength, time, lightU, lightV);
                addTopVertex(pos, buffer, sprite, x1, z0, h00, h10, h11, h01,
                        baseRed, baseGreen, baseBlue, deep, causticStrength, time, lightU, lightV);
            }
        }
    }

    /**
     * Each corner is shared mathematically by adjacent water blocks, avoiding
     * visible height seams where source and flowing water meet.
     */
    private static float getCornerHeight(IBlockAccess world, int cornerX, int y, int cornerZ,
                                         BlockPos.MutableBlockPos cursor) {
        float total = 0.0F;
        int samples = 0;

        for (int dz = -1; dz <= 0; dz++) {
            for (int dx = -1; dx <= 0; dx++) {
                cursor.setPos(cornerX + dx, y, cornerZ + dz);
                IBlockState sample = world.getBlockState(cursor);
                if (!(sample.getBlock() instanceof BlockLiquid)
                        || sample.getMaterial() != Material.WATER) continue;

                total += BlockLiquid.getBlockLiquidHeight(sample, world, cursor);
                samples++;
            }
        }

        return samples == 0 ? 0.89F : total / samples;
    }

    private static int getWaterDepth(IBlockAccess world, BlockPos pos,
                                     BlockPos.MutableBlockPos cursor) {
        int depth = 1;
        for (int y = 1; y <= 10; y++) {
            cursor.setPos(pos.getX(), pos.getY() - y, pos.getZ());
            IBlockState sample = world.getBlockState(cursor);
            if (!(sample.getBlock() instanceof BlockLiquid)
                    || sample.getMaterial() != Material.WATER) break;
            depth++;
        }
        return depth;
    }

    private static void addTopVertex(BlockPos pos, BufferBuilder buffer,
                                     TextureAtlasSprite sprite, float x, float z,
                                     float h00, float h10, float h11, float h01,
                                     float baseRed, float baseGreen, float baseBlue,
                                     float depth, float causticStrength, double time,
                                     int lightU, int lightV) {
        float front = lerp(h00, h10, x);
        float back = lerp(h01, h11, x);
        float height = lerp(front, back, z) + 0.0025F;

        double worldX = pos.getX() + x;
        double worldZ = pos.getZ() + z;

        // Two intersecting, low-frequency fields create irregular reflected
        // light. This is not a radial ripple and does not displace the surface.
        double fieldA = Math.sin(worldX * 0.43D + worldZ * 0.17D + time)
                * Math.cos(worldZ * 0.37D - worldX * 0.11D - time * 0.71D);
        double fieldB = Math.sin((worldX - worldZ) * 0.24D + time * 0.43D);
        float caustic = (float) (0.5D + 0.34D * fieldA + 0.16D * fieldB);
        caustic = clamp(caustic, 0.0F, 1.0F);

        float sheen = 0.94F + caustic * 0.11F * causticStrength;
        float glint = Math.max(0.0F, caustic - 0.84F) * 0.52F * causticStrength;

        float red = clamp(baseRed * sheen + glint * 0.48F, 0.0F, 1.0F);
        float green = clamp(baseGreen * sheen + glint * 0.82F, 0.0F, 1.0F);
        float blue = clamp(baseBlue * sheen + glint, 0.0F, 1.0F);

        int alpha = (int) (194.0F - depth * 1.5F);
        alpha = Math.max(166, Math.min(198, alpha));

        float u = sprite.getInterpolatedU(x * 16.0F);
        float v = sprite.getInterpolatedV(z * 16.0F);

        buffer.pos(worldX, pos.getY() + height, worldZ)
                .color((int) (red * 255.0F), (int) (green * 255.0F),
                        (int) (blue * 255.0F), alpha)
                .tex(u, v)
                .lightmap(lightU, lightV)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static float lerp(float start, float end, float amount) {
        return start + (end - start) * amount;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
