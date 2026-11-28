package com.voxelcore.client.handler;

import com.voxelcore.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client-side contact feedback for nearby living entities.
 *
 * It adds material-aware footsteps, small landing puffs and water entry/exit
 * splashes without changing AI, movement, collisions, damage or server state.
 * Tracking is weak-keyed so removed entities do not stay referenced by Minefade.
 */
@SideOnly(Side.CLIENT)
public final class LivingEntityContactHandler {
    private static final double MAX_VIEW_DISTANCE_SQ = 144.0D;
    private final Map<EntityLivingBase, ContactState> states =
            new WeakHashMap<EntityLivingBase, ContactState>();
    private World trackedWorld;

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity == null || entity.world == null || !entity.world.isRemote
                || entity.isDead) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world != entity.world || mc.player == null || entity == mc.player) return;

        if (trackedWorld != entity.world) {
            states.clear();
            trackedWorld = entity.world;
        }

        Entity observer = mc.getRenderViewEntity();
        if (observer == null) observer = mc.player;
        double dx = entity.posX - observer.posX;
        double dy = entity.posY - observer.posY;
        double dz = entity.posZ - observer.posZ;
        if (dx * dx + dy * dy + dz * dz > MAX_VIEW_DISTANCE_SQ) {
            states.remove(entity);
            return;
        }

        // A passenger has no independent ground contact. Its mount provides
        // the visible contact response instead.
        if (entity.isRiding()) {
            states.remove(entity);
            return;
        }

        ContactState state = states.get(entity);
        if (state == null) {
            state = new ContactState(entity);
            states.put(entity, state);
        }
        updateContact(entity, state);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() != null && event.getWorld().isRemote
                && event.getWorld() == trackedWorld) {
            states.clear();
            trackedWorld = null;
        }
    }

    private static void updateContact(EntityLivingBase entity, ContactState state) {
        World world = entity.world;
        double dx = entity.posX - state.previousX;
        double dz = entity.posZ - state.previousZ;
        double movementSq = dx * dx + dz * dz;
        state.previousX = entity.posX;
        state.previousZ = entity.posZ;
        if (movementSq > 4.0D) state.stepDistance = 0.0D;

        boolean inWater = entity.isInsideOfMaterial(Material.WATER);
        boolean inLava = entity.isInsideOfMaterial(Material.LAVA);

        // Refresh the nearest visible liquid surface infrequently while
        // swimming. BlockLiquid supplies the real height for flowing water.
        if (inWater && (!state.wasInWater || !state.hasWaterSurface
                || entity.ticksExisted % 8 == 0)) {
            SurfaceInfo surface = findWaterSurface(world, entity);
            if (surface != null) {
                state.waterSurfaceX = surface.x;
                state.waterSurfaceY = surface.y;
                state.waterSurfaceZ = surface.z;
                state.hasWaterSurface = true;
            }
        }

        if (inWater && !state.wasInWater && ModConfig.ENABLE_WATER_SPLASHES
                && state.hasWaterSurface) {
            spawnWaterSplash(world, entity, state, true);
        } else if (!inWater && state.wasInWater && !inLava
                && ModConfig.ENABLE_WATER_SPLASHES && state.hasWaterSurface) {
            spawnWaterSplash(world, entity, state, false);
        }
        state.wasInWater = inWater;

        boolean landed = entity.onGround && !state.wasOnGround
                && state.previousMotionY < -0.08D;
        if (!entity.onGround) {
            state.maxFallDistance = Math.max(state.maxFallDistance, entity.fallDistance);
        }

        if (landed && ModConfig.ENABLE_LANDING_IMPACTS) {
            float fallDistance = Math.max(state.maxFallDistance, entity.fallDistance);
            if (fallDistance >= 1.35F) {
                BlockPos ground = new BlockPos(entity.posX,
                        entity.getEntityBoundingBox().minY - 0.04D, entity.posZ);
                if (world.isBlockLoaded(ground)) {
                    IBlockState groundState = world.getBlockState(ground);
                    if (groundState.getBlock() != Blocks.SLIME_BLOCK) {
                        spawnLandingImpact(world, entity, ground, groundState, fallDistance);
                    }
                }
            }
        }
        if (entity.onGround) state.maxFallDistance = 0.0F;

        if (!ModConfig.ENABLE_MATERIAL_FOOTSTEPS || !entity.onGround
                || entity.isSneaking() || inWater || inLava || movementSq < 0.0004D) {
            state.stepDistance = 0.0D;
        } else {
            double stride = Math.max(0.32D, Math.min(0.82D, entity.width * 0.72D));
            state.stepDistance = Math.min(stride * 1.25D,
                    state.stepDistance + Math.sqrt(movementSq));
            if (state.stepDistance >= stride) {
                spawnFootstep(world, entity);
                state.stepDistance = Math.max(0.0D, state.stepDistance - stride);
            }
        }

        state.wasOnGround = entity.onGround;
        state.previousMotionY = entity.motionY;
    }

    private static SurfaceInfo findWaterSurface(World world, EntityLivingBase entity) {
        int originX = MathHelper.floor(entity.posX);
        int originY = MathHelper.floor(entity.getEntityBoundingBox().minY);
        int originZ = MathHelper.floor(entity.posZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double bestScore = Double.MAX_VALUE;
        SurfaceInfo best = null;

        for (int dy = -1; dy <= 5; dy++) {
            int y = originY + dy;
            if (y < 0 || y >= 255) continue;
            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    int x = originX + ox;
                    int z = originZ + oz;
                    pos.setPos(x, y, z);
                    if (!world.isBlockLoaded(pos)) continue;
                    IBlockState liquid = world.getBlockState(pos);
                    if (liquid.getMaterial() != Material.WATER) continue;

                    pos.setY(y + 1);
                    if (!world.isBlockLoaded(pos)
                            || world.getBlockState(pos).getMaterial() == Material.WATER) continue;

                    pos.setPos(x, y, z);
                    double surfaceY = BlockLiquid.getLiquidHeight(liquid, world, pos);
                    double vertical = surfaceY - entity.posY;
                    if (Math.abs(vertical) > 3.5D) continue;
                    double horizontalX = x + 0.5D - entity.posX;
                    double horizontalZ = z + 0.5D - entity.posZ;
                    double score = horizontalX * horizontalX + horizontalZ * horizontalZ
                            + vertical * vertical * 0.45D;
                    if (score < bestScore) {
                        bestScore = score;
                        best = new SurfaceInfo(x + 0.5D, surfaceY, z + 0.5D);
                    }
                }
            }
        }
        return best;
    }

    private static void spawnWaterSplash(World world, EntityLivingBase entity,
                                         ContactState state, boolean entering) {
        double dx = entity.posX - state.waterSurfaceX;
        double dz = entity.posZ - state.waterSurfaceZ;
        if (dx * dx + dz * dz > 2.75D
                || Math.abs(entity.posY - state.waterSurfaceY) > 2.25D) return;

        int particleSetting = Minecraft.getMinecraft().gameSettings.particleSetting;
        int count = entering ? (particleSetting == 2 ? 1 : 4)
                : (particleSetting == 2 ? 1 : 2);
        for (int i = 0; i < count; i++) {
            double spreadX = (world.rand.nextDouble() - 0.5D) * 0.36D;
            double spreadZ = (world.rand.nextDouble() - 0.5D) * 0.36D;
            double horizontalScale = entering ? 0.42D : 0.22D;
            double verticalSpeed = entering ? 0.055D : 0.032D;
            world.spawnParticle(EnumParticleTypes.WATER_SPLASH,
                    entity.posX + spreadX, state.waterSurfaceY + 0.014D,
                    entity.posZ + spreadZ,
                    entity.motionX * horizontalScale + spreadX * 0.10D,
                    verticalSpeed + world.rand.nextDouble() * 0.025D,
                    entity.motionZ * horizontalScale + spreadZ * 0.10D);
        }
    }

    private static void spawnLandingImpact(World world, EntityLivingBase entity,
                                           BlockPos ground, IBlockState state,
                                           float fallDistance) {
        Material material = state.getMaterial();
        if (material != Material.SNOW && !material.isSolid()) return;

        int particleSetting = Minecraft.getMinecraft().gameSettings.particleSetting;
        int count = 1 + Math.min(2, Math.max(0, (int) ((fallDistance - 1.0F) * 0.55F)));
        if (particleSetting == 2) count = 1;
        else if (particleSetting == 1) count = Math.min(2, count);

        boolean snow = material == Material.SNOW;
        int stateId = Block.getStateId(state);
        for (int i = 0; i < count; i++) {
            double angle = world.rand.nextDouble() * Math.PI * 2.0D;
            double radius = 0.08D + world.rand.nextDouble() * 0.20D;
            double vx = Math.cos(angle) * 0.024D;
            double vz = Math.sin(angle) * 0.024D;
            double x = entity.posX + Math.cos(angle) * radius;
            double z = entity.posZ + Math.sin(angle) * radius;
            double y = ground.getY() + 1.012D;
            double vy = 0.014D + world.rand.nextDouble() * 0.022D;
            if (snow) {
                world.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, x, y, z, vx, vy, vz);
            } else {
                world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, x, y, z, vx, vy, vz, stateId);
            }
        }
    }

    private static void spawnFootstep(World world, EntityLivingBase entity) {
        BlockPos ground = new BlockPos(entity.posX,
                entity.getEntityBoundingBox().minY - 0.05D, entity.posZ);
        if (!world.isBlockLoaded(ground)) return;
        IBlockState state = world.getBlockState(ground);
        Material material = state.getMaterial();
        boolean snow = material == Material.SNOW;
        boolean leaves = state.getBlock() == Blocks.LEAVES
                || state.getBlock() == Blocks.LEAVES2
                || state.getBlock() == Blocks.VINE;
        boolean softGround = material == Material.GROUND || material == Material.GRASS
                || material == Material.SAND || material == Material.CLAY
                || state.getBlock() == Blocks.GRAVEL;
        if (!snow && !leaves && !softGround) return;

        int setting = Minecraft.getMinecraft().gameSettings.particleSetting;
        int count = setting == 2 ? 1 : 2;
        int stateId = Block.getStateId(state);
        for (int i = 0; i < count; i++) {
            double spread = Math.max(0.18D, entity.width * 0.42D);
            double x = entity.posX + (world.rand.nextDouble() - 0.5D) * spread;
            double z = entity.posZ + (world.rand.nextDouble() - 0.5D) * spread;
            double y = ground.getY() + 1.008D;
            double vx = -entity.motionX * 0.10D + (world.rand.nextDouble() - 0.5D) * 0.018D;
            double vz = -entity.motionZ * 0.10D + (world.rand.nextDouble() - 0.5D) * 0.018D;
            double vy = 0.008D + world.rand.nextDouble() * 0.014D;
            if (snow) {
                world.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, x, y, z, vx, vy, vz);
            } else {
                world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, x, y, z, vx, vy, vz, stateId);
            }
        }
    }

    private static final class ContactState {
        double previousX;
        double previousZ;
        double previousMotionY;
        double stepDistance;
        float maxFallDistance;
        boolean wasOnGround;
        boolean wasInWater;
        boolean hasWaterSurface;
        double waterSurfaceX;
        double waterSurfaceY;
        double waterSurfaceZ;

        ContactState(EntityLivingBase entity) {
            previousX = entity.posX;
            previousZ = entity.posZ;
            previousMotionY = entity.motionY;
            wasOnGround = entity.onGround;
            wasInWater = entity.isInsideOfMaterial(Material.WATER);
        }
    }

    private static final class SurfaceInfo {
        final double x;
        final double y;
        final double z;

        SurfaceInfo(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
