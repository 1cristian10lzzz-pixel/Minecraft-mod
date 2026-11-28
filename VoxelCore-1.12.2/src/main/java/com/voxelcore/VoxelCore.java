package com.voxelcore;

import com.voxelcore.config.ModConfig;
import com.voxelcore.proxy.CommonProxy;
import com.voxelcore.registry.ModRegistry;
import com.voxelcore.world.ResoniteWorldGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraft.item.ItemStack;
import com.voxelcore.registry.ModContent;

@Mod(
        modid = VoxelCore.MODID,
        name = VoxelCore.NAME,
        version = VoxelCore.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:forge"
)
public final class VoxelCore {
    public static final String MODID = "voxelcore";
    public static final String NAME = "Minefade Mod";
    public static final String VERSION = "0.14.0";

    @SidedProxy(
            clientSide = "com.voxelcore.proxy.ClientProxy",
            serverSide = "com.voxelcore.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    public VoxelCore() {
        MinecraftForge.EVENT_BUS.register(new ModRegistry());
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.init(event.getSuggestedConfigurationFile());
        proxy.preInit();
        event.getModLog().info("Minefade Mod {} initialized for Minecraft 1.12.2", VERSION);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        GameRegistry.registerWorldGenerator(new ResoniteWorldGenerator(), 5);
        GameRegistry.addSmelting(ModContent.RESONITE_CRYSTAL, new ItemStack(ModContent.RESONITE_INGOT), 0.7F);
        proxy.init();
    }
}
