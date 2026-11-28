package com.voxelcore.server;

import com.voxelcore.config.ModConfig;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

/** Server-authoritative dropped-item behaviour using vanilla inventory collision logic. */
public final class DroppedItemPhysicsHandler {
    /**
     * Optional right-click fallback. Vanilla's normal walk-over pickup remains enabled;
     * this path only supplements it and still checks the real inventory capacity.
     */
    @SubscribeEvent
    public void onManualPickup(PlayerInteractEvent.EntityInteract event) {
        if (!ModConfig.ENABLE_REALISTIC_ITEM_PHYSICS || !ModConfig.MANUAL_ITEM_PICKUP
                || event.getWorld().isRemote || !(event.getTarget() instanceof EntityItem)) return;
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || event.getHand() != EnumHand.MAIN_HAND) return;

        EntityItem dropped = (EntityItem) event.getTarget();
        if (dropped.isDead || dropped.cannotPickup()) return;

        ItemStack transfer = dropped.getItem().copy();
        if (transfer.isEmpty()) return;
        int before = transfer.getCount();
        player.inventory.addItemStackToInventory(transfer);
        int collected = before - transfer.getCount();
        if (collected <= 0) return;

        if (transfer.isEmpty()) dropped.setDead();
        else dropped.setItem(transfer);
        player.inventory.markDirty();
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.20F,
                0.85F + player.world.rand.nextFloat() * 0.25F);
        player.onItemPickup(dropped, collected);
        event.setCanceled(true);
    }

    /**
     * Small server-side recovery check around each player. It delegates to the
     * vanilla EntityItem callback instead of duplicating stack transfer rules.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player == null) return;
        EntityPlayer player = event.player;
        if (player.world.isRemote || player.isSpectator()
                || !ModConfig.ENABLE_REALISTIC_ITEM_PHYSICS
                || player.ticksExisted % 2 != 0) return;

        List<EntityItem> nearby = player.world.getEntitiesWithinAABB(
                EntityItem.class, player.getEntityBoundingBox().grow(0.12D, 0.10D, 0.12D));
        for (EntityItem item : nearby) {
            if (item != null && !item.isDead && !item.getItem().isEmpty()) {
                item.onCollideWithPlayer(player);
            }
        }
    }

    @SubscribeEvent
    public void onSprintToss(ItemTossEvent event) {
        if (!ModConfig.ENABLE_REALISTIC_ITEM_PHYSICS || !ModConfig.ENABLE_SPRINT_TOSS) return;
        EntityPlayer player = event.getPlayer();
        EntityItem dropped = event.getEntityItem();
        if (player == null || dropped == null || !player.isSprinting()) return;

        Vec3d look = player.getLookVec();
        double impulse = ModConfig.ITEM_TOSS_STRENGTH / 100.0D;
        dropped.motionX = dropped.motionX * 0.35D + look.x * impulse + player.motionX * 0.35D;
        dropped.motionY = Math.max(0.12D, dropped.motionY + 0.12D + look.y * 0.08D);
        dropped.motionZ = dropped.motionZ * 0.35D + look.z * impulse + player.motionZ * 0.35D;
        dropped.setPickupDelay(ModConfig.MANUAL_ITEM_PICKUP ? 4 : 10);
        dropped.velocityChanged = true;
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote
                || !ModConfig.ENABLE_REALISTIC_ITEM_PHYSICS || !ModConfig.FLOAT_ITEMS_IN_FLUIDS) return;
        // Fluid buoyancy is cosmetic; do not scan every loaded entity every single tick.
        if (event.world.getTotalWorldTime() % 5L != 0L) return;

        List<Entity> entities = event.world.loadedEntityList;
        BlockPos.MutableBlockPos fluidPos = new BlockPos.MutableBlockPos();
        for (int i = 0, count = entities.size(); i < count; i++) {
            Entity entity = entities.get(i);
            if (!(entity instanceof EntityItem) || entity.isDead) continue;
            EntityItem item = (EntityItem) entity;
            if (item.getItem().isEmpty()) continue;

            Material fluid = getFluidAtItem(item, fluidPos);
            if (fluid == null) continue;
            double drag = fluid == Material.LAVA ? 0.80D : 0.91D;
            item.motionX *= drag;
            item.motionZ *= drag;

            fluidPos.setY(fluidPos.getY() + 1);
            boolean atSurface = !item.world.isBlockLoaded(fluidPos, false)
                    || item.world.getBlockState(fluidPos).getMaterial() != fluid;
            double lift = fluid == Material.LAVA ? 0.026D : 0.012D;
            if (atSurface) {
                // Dampen vertical movement near the air/fluid boundary to prevent bobbing jitter.
                item.motionY = Math.max(-0.025D, Math.min(0.025D,
                        item.motionY * 0.24D + lift * 0.20D));
            } else {
                item.motionY = Math.max(-0.035D, Math.min(0.045D,
                        item.motionY * 0.58D + lift));
            }
            item.velocityChanged = true;
        }
    }

    /**
     * Uses one reusable mutable position and never requests an unloaded block.
     * The block containing the item and the block immediately below cover the
     * item body crossing the liquid boundary without repeated volume scans.
     */
    private static Material getFluidAtItem(EntityItem item, BlockPos.MutableBlockPos position) {
        position.setPos(MathHelper.floor(item.posX), MathHelper.floor(item.posY),
                MathHelper.floor(item.posZ));
        if (!item.world.isBlockLoaded(position, false)) return null;

        Material here = item.world.getBlockState(position).getMaterial();
        if (here == Material.WATER || here == Material.LAVA) return here;

        position.setY(position.getY() - 1);
        if (position.getY() < 0 || !item.world.isBlockLoaded(position, false)) return null;
        Material below = item.world.getBlockState(position).getMaterial();
        return below == Material.WATER || below == Material.LAVA ? below : null;
    }
}