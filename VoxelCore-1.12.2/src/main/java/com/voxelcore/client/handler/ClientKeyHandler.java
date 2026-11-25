package com.voxelcore.client.handler;

import com.voxelcore.client.gui.MinefadeSettingsScreen;
import com.voxelcore.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class ClientKeyHandler {
    private static final KeyBinding OPEN_SETTINGS = new KeyBinding(
            "key.voxelcore.open_settings", Keyboard.KEY_O, "key.categories.voxelcore");
    private static final KeyBinding TOGGLE_OVERLAY = new KeyBinding(
            "key.voxelcore.toggle_overlay", Keyboard.KEY_F8, "key.categories.voxelcore");
    private static final KeyBinding TOGGLE_ENTITY_CULLING = new KeyBinding(
            "key.voxelcore.toggle_entity_culling", Keyboard.KEY_F7, "key.categories.voxelcore");
    private static final KeyBinding TOGGLE_PARTICLE_LIMIT = new KeyBinding(
            "key.voxelcore.toggle_particle_limit", Keyboard.KEY_F6, "key.categories.voxelcore");

    public ClientKeyHandler() {
        ClientRegistry.registerKeyBinding(OPEN_SETTINGS);
        ClientRegistry.registerKeyBinding(TOGGLE_OVERLAY);
        ClientRegistry.registerKeyBinding(TOGGLE_ENTITY_CULLING);
        ClientRegistry.registerKeyBinding(TOGGLE_PARTICLE_LIMIT);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        ParticleQualityController.update(mc);

        while (OPEN_SETTINGS.isPressed()) {
            if (mc.currentScreen == null) mc.displayGuiScreen(new MinefadeSettingsScreen(null));
        }
        while (TOGGLE_OVERLAY.isPressed()) {
            ModConfig.setOverlayEnabled(!ModConfig.SHOW_OVERLAY);
        }
        while (TOGGLE_ENTITY_CULLING.isPressed()) {
            ModConfig.setEntityCullingEnabled(!ModConfig.ENABLE_ENTITY_DISTANCE_CULLING);
        }
        while (TOGGLE_PARTICLE_LIMIT.isPressed()) {
            ModConfig.setMinimalParticlesEnabled(!ModConfig.FORCE_MINIMAL_PARTICLES);
        }
    }
}
