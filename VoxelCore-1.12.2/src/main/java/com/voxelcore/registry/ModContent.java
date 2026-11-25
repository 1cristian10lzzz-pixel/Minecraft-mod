package com.voxelcore.registry;

import com.voxelcore.VoxelCore;
import com.voxelcore.content.BlockResoniteLamp;
import com.voxelcore.content.BlockResoniteOre;
import com.voxelcore.content.ItemResoniteAxe;
import com.voxelcore.content.ItemResoniteArmor;
import com.voxelcore.content.ItemResoniteHoe;
import com.voxelcore.content.ItemResonitePickaxe;
import com.voxelcore.content.ItemResoniteShovel;
import com.voxelcore.content.ItemResoniteSword;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.EnumHelper;

public final class ModContent {
    public static final CreativeTabs TAB = new CreativeTabs(VoxelCore.MODID) {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(RESONITE_CRYSTAL);
        }
    };

    public static final Item.ToolMaterial RESONITE_TOOL_MATERIAL = EnumHelper.addToolMaterial(
            "RESONITE", 3, 1800, 8.5F, 3.0F, 18
    );

    public static final net.minecraft.item.ItemArmor.ArmorMaterial RESONITE_ARMOR_MATERIAL = EnumHelper.addArmorMaterial(
            "VOXELCORE_RESONITE", "voxelcore_resonite", 36,
            new int[] { 3, 6, 8, 3 }, 18, SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, 1.0F
    );

    public static final Block RESONITE_ORE = new BlockResoniteOre();
    public static final Block RESONITE_BLOCK = new Block(Material.IRON)
            .setRegistryName(VoxelCore.MODID, "resonite_block")
            .setUnlocalizedName(VoxelCore.MODID + ".resonite_block")
            .setHardness(5.0F)
            .setResistance(12.0F);
    public static final Block RESONITE_LAMP = new BlockResoniteLamp();

    public static final Item RESONITE_CRYSTAL = new Item()
            .setRegistryName(VoxelCore.MODID, "resonite_crystal")
            .setUnlocalizedName(VoxelCore.MODID + ".resonite_crystal")
            .setCreativeTab(TAB);
    public static final Item RESONITE_INGOT = new Item()
            .setRegistryName(VoxelCore.MODID, "resonite_ingot")
            .setUnlocalizedName(VoxelCore.MODID + ".resonite_ingot")
            .setCreativeTab(TAB);

    public static final Item RESONITE_PICKAXE = new ItemResonitePickaxe();
    public static final Item RESONITE_SWORD = new ItemResoniteSword();
    public static final Item RESONITE_SHOVEL = new ItemResoniteShovel();
    public static final Item RESONITE_AXE = new ItemResoniteAxe();
    public static final Item RESONITE_HOE = new ItemResoniteHoe();

    public static final Item RESONITE_HELMET = new ItemResoniteArmor(EntityEquipmentSlot.HEAD, "resonite_helmet");
    public static final Item RESONITE_CHESTPLATE = new ItemResoniteArmor(EntityEquipmentSlot.CHEST, "resonite_chestplate");
    public static final Item RESONITE_LEGGINGS = new ItemResoniteArmor(EntityEquipmentSlot.LEGS, "resonite_leggings");
    public static final Item RESONITE_BOOTS = new ItemResoniteArmor(EntityEquipmentSlot.FEET, "resonite_boots");

    static {
        RESONITE_BLOCK.setHarvestLevel("pickaxe", 2);
        RESONITE_BLOCK.setCreativeTab(TAB);
        RESONITE_TOOL_MATERIAL.setRepairItem(new ItemStack(RESONITE_INGOT));
    }

    private ModContent() { }

}
