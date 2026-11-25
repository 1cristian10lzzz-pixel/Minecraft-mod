package com.voxelcore.registry;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ModRegistry {
    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(ModContent.RESONITE_ORE);
        event.getRegistry().register(ModContent.RESONITE_BLOCK);
        event.getRegistry().register(ModContent.RESONITE_LAMP);
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new ItemBlock(ModContent.RESONITE_ORE)
                .setRegistryName(ModContent.RESONITE_ORE.getRegistryName()));
        event.getRegistry().register(new ItemBlock(ModContent.RESONITE_BLOCK)
                .setRegistryName(ModContent.RESONITE_BLOCK.getRegistryName()));
        event.getRegistry().register(new ItemBlock(ModContent.RESONITE_LAMP)
                .setRegistryName(ModContent.RESONITE_LAMP.getRegistryName()));

        event.getRegistry().register(ModContent.RESONITE_CRYSTAL);
        event.getRegistry().register(ModContent.RESONITE_INGOT);
        event.getRegistry().register(ModContent.RESONITE_PICKAXE);
        event.getRegistry().register(ModContent.RESONITE_SWORD);
        event.getRegistry().register(ModContent.RESONITE_SHOVEL);
        event.getRegistry().register(ModContent.RESONITE_AXE);
        event.getRegistry().register(ModContent.RESONITE_HOE);
        event.getRegistry().register(ModContent.RESONITE_HELMET);
        event.getRegistry().register(ModContent.RESONITE_CHESTPLATE);
        event.getRegistry().register(ModContent.RESONITE_LEGGINGS);
        event.getRegistry().register(ModContent.RESONITE_BOOTS);
    }
}
