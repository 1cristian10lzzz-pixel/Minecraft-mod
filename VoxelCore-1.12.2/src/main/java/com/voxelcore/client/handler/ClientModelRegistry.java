package com.voxelcore.client.handler;

import com.voxelcore.VoxelCore;
import com.voxelcore.registry.ModContent;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientModelRegistry {
    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        register(Item.getItemFromBlock(ModContent.RESONITE_ORE), "resonite_ore");
        register(Item.getItemFromBlock(ModContent.RESONITE_BLOCK), "resonite_block");
        register(Item.getItemFromBlock(ModContent.RESONITE_LAMP), "resonite_lamp");
        register(ModContent.RESONITE_CRYSTAL, "resonite_crystal");
        register(ModContent.RESONITE_INGOT, "resonite_ingot");
        register(ModContent.RESONITE_PICKAXE, "resonite_pickaxe");
        register(ModContent.RESONITE_SWORD, "resonite_sword");
        register(ModContent.RESONITE_SHOVEL, "resonite_shovel");
        register(ModContent.RESONITE_AXE, "resonite_axe");
        register(ModContent.RESONITE_HOE, "resonite_hoe");
        register(ModContent.RESONITE_HELMET, "resonite_helmet");
        register(ModContent.RESONITE_CHESTPLATE, "resonite_chestplate");
        register(ModContent.RESONITE_LEGGINGS, "resonite_leggings");
        register(ModContent.RESONITE_BOOTS, "resonite_boots");
    }

    private static void register(Item item, String name) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(VoxelCore.MODID + ":" + name, "inventory"));
    }
}
