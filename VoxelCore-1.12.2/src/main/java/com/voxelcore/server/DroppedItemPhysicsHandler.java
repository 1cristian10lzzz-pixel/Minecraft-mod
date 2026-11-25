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
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

/** Server-authoritative dropped-item behaviour implemented with original Minefade code. */
public final class DroppedItemPhysicsHandler {
    private int tickCounter;

    @SubscribeEvent
    public void onAutomaticPickup(EntityItemPickupEvent event) {
        if (ModConfig.ENABLE_REALISTIC_ITEM_PHYSICS && ModConfig.MANUAL_ITEM_PICKUP) {
            event.setCanceled(true);
        }
    }

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
        if (++tickCounter % 3 != 0) return;
        List<Entity> entities = event.world.loadedEntityList;
        for (int i = 0, count = entities.size(); i < count; i++) {
            Entity entity = entities.get(i);
            if (!(entity instanceof EntityItem) || entity.isDead) continue;
            EntityItem item = (EntityItem) entity;
            if (item.getItem().isEmpty()) continue;
            Material fluid = getFluidAtItem(item);
            if (fluid == null) continue;
            double drag = fluid == Material.LAVA ? 0.76D : 0.88D;
            item.motionX *= drag;
            item.motionZ *= drag;
            double lift = fluid == Material.LAVA ? 0.055D : 0.032D;
            item.motionY = Math.max(-0.015D, Math.min(0.075D, item.motionY * 0.35D + lift));
            item.velocityChanged = true;
        }
    }

    private static Material getFluidAtItem(EntityItem item) {
        if (item.isInsideOfMaterial(Material.WATER)) return Material.WATER;
        if (item.isInsideOfMaterial(Material.LAVA)) return Material.LAVA;
        BlockPos position = new BlockPos(item.posX, item.posY, item.posZ);
        Material here = item.world.getBlockState(position).getMaterial();
        if (here == Material.WATER || here == Material.LAVA) return here;
        Material below = item.world.getBlockState(position.down()).getMaterial();
        return below == Material.WATER || below == Material.LAVA ? below : null;
    }
}
