package com.voxelcore.content;

import com.voxelcore.VoxelCore;
import com.voxelcore.registry.ModContent;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class ItemResoniteArmor extends ItemArmor {
    public ItemResoniteArmor(EntityEquipmentSlot slot, String name) {
        super(ModContent.RESONITE_ARMOR_MATERIAL, 0, slot);
        setRegistryName(VoxelCore.MODID, name);
        setTranslationKey(VoxelCore.MODID + "." + name);
        setCreativeTab(ModContent.TAB);
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return (repair != null && !repair.isEmpty() && repair.getItem() == ModContent.RESONITE_INGOT)
                || super.getIsRepairable(toRepair, repair);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
        int layer = slot == EntityEquipmentSlot.LEGS ? 2 : 1;
        return new ResourceLocation(VoxelCore.MODID,
                "textures/models/armor/resonite_layer_" + layer + ".png").toString();
    }
}
