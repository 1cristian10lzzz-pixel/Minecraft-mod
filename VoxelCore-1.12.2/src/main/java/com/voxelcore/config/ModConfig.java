package com.voxelcore.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;

/** Central persistent configuration for Minefade Mod. Defaults favour stability and compatibility. */
public final class ModConfig {
    private static final String CAT_PERFORMANCE = "performance";
    private static final String CAT_DIAGNOSTICS = "diagnostics";
    private static final String CAT_WORLDGEN = "world_generation";
    private static final String CAT_INTERFACE = "interface";
    private static final String CAT_WATER = "water";
    private static final String CAT_ITEMS = "dropped_items";
    private static final String CAT_ANIMATION = "animations";

    private static Configuration configuration;

    public static boolean ENABLE_UI_THEME = true;
    public static boolean SHOW_OVERLAY = true;
    public static boolean SHOW_CHUNK_COUNT = true;
    public static boolean SHOW_MEMORY = true;
    public static boolean SHOW_ENTITY_COUNT = true;
    public static boolean SHOW_TILE_ENTITY_COUNT = false;

    public static boolean ENABLE_ENTITY_DISTANCE_CULLING = false;
    public static int ENTITY_DISTANCE_PERCENT = 85;
    public static boolean FORCE_MINIMAL_PARTICLES = false;
    public static boolean ENABLE_ITEM_DISTANCE_CULLING = false;
    public static int ITEM_RENDER_RADIUS = 64;

    public static boolean ENABLE_WATER_EFFECTS = true;
    public static boolean ENABLE_WATER_RIPPLES = true;
    public static boolean ENABLE_WATER_SPLASHES = true;
    public static boolean ENABLE_RAIN_RIPPLES = true;
    public static boolean ENABLE_WATERFALL_MIST = true;
    public static boolean ENABLE_UNDERWATER_TINT = true;
    public static boolean ENABLE_ADAPTIVE_WATER_QUALITY = true;
    public static int WATER_WAVE_STRENGTH = 70;
    public static int WATER_EFFECT_RADIUS = 32;

    public static boolean ENABLE_REALISTIC_ITEM_PHYSICS = true;
    public static boolean FLAT_GROUND_ITEMS = true;
    public static boolean MANUAL_ITEM_PICKUP = true;
    public static boolean ENABLE_SPRINT_TOSS = true;
    public static boolean FLOAT_ITEMS_IN_FLUIDS = true;
    public static int ITEM_TOSS_STRENGTH = 85;
    public static boolean ENABLE_PLAYER_ANIMATIONS = true;

    public static boolean GENERATE_RESONITE_ORE = true;
    public static int ORE_VEINS_PER_CHUNK = 4;
    public static int ORE_VEIN_SIZE = 5;
    public static int ORE_MIN_Y = 5;
    public static int ORE_MAX_Y = 48;

    private ModConfig() { }

    public static void init(File file) {
        configuration = new Configuration(file);
        load();
    }

    public static void load() {
        if (configuration == null) return;
        configuration.load();

        ENABLE_UI_THEME = configuration.get(CAT_INTERFACE, "enableUiTheme", true,
                "Apply the Minefade visual theme to vanilla and compatible mod GUI screens.").getBoolean(true);
        SHOW_OVERLAY = configuration.get(CAT_DIAGNOSTICS, "showOverlay", true,
                "Show the Minefade performance monitor in the game HUD.").getBoolean(true);
        SHOW_CHUNK_COUNT = configuration.get(CAT_DIAGNOSTICS, "showChunkCount", true,
                "Show the number of chunks currently loaded by the client.").getBoolean(true);
        SHOW_MEMORY = configuration.get(CAT_DIAGNOSTICS, "showMemory", true,
                "Show current and maximum Java heap usage.").getBoolean(true);
        SHOW_ENTITY_COUNT = configuration.get(CAT_DIAGNOSTICS, "showEntityCount", true,
                "Show the number of loaded entities in the current client world.").getBoolean(true);
        SHOW_TILE_ENTITY_COUNT = configuration.get(CAT_DIAGNOSTICS, "showTileEntityCount", false,
                "Show the number of loaded tile entities. This count is diagnostic only.").getBoolean(false);

        ENABLE_ENTITY_DISTANCE_CULLING = configuration.get(CAT_PERFORMANCE, "enableEntityDistanceCulling", false,
                "Optionally skip rendering distant non-player living entities. Disabled by default.").getBoolean(false);
        ENTITY_DISTANCE_PERCENT = configuration.get(CAT_PERFORMANCE, "entityDistancePercent", 85,
                "Entity render cutoff as a percentage of the configured view distance.", 50, 100).getInt(85);
        FORCE_MINIMAL_PARTICLES = configuration.get(CAT_PERFORMANCE, "forceMinimalParticles", false,
                "Use Minecraft's minimal-particles setting while enabled.").getBoolean(false);
        ENABLE_ITEM_DISTANCE_CULLING = configuration.get(CAT_PERFORMANCE, "enableItemDistanceCulling", false,
                "Optionally skip rendering dropped items beyond the configured radius. Disabled by default.").getBoolean(false);
        ITEM_RENDER_RADIUS = configuration.get(CAT_PERFORMANCE, "itemRenderRadius", 64,
                "Maximum Minefade dropped-item render distance.", 16, 128).getInt(64);

        ENABLE_WATER_EFFECTS = configuration.get(CAT_WATER, "enableWaterEffects", true,
                "Enable Minefade's lightweight client-side water surface effects.").getBoolean(true);
        ENABLE_WATER_RIPPLES = configuration.get(CAT_WATER, "enableWaterRipples", true,
                "Render expanding ripples when swimmers and boats move through water.").getBoolean(true);
        ENABLE_WATER_SPLASHES = configuration.get(CAT_WATER, "enableWaterSplashes", true,
                "Add splash and wake particles when entering or moving through water.").getBoolean(true);
        ENABLE_RAIN_RIPPLES = configuration.get(CAT_WATER, "enableRainRipples", true,
                "Add small rain rings to nearby exposed water surfaces.").getBoolean(true);
        ENABLE_WATERFALL_MIST = configuration.get(CAT_WATER, "enableWaterfallMist", true,
                "Add a small mist plume near nearby falling-water impacts.").getBoolean(true);
        ENABLE_UNDERWATER_TINT = configuration.get(CAT_WATER, "enableUnderwaterTint", true,
                "Use a biome-aware underwater fog tint.").getBoolean(true);
        ENABLE_ADAPTIVE_WATER_QUALITY = configuration.get(CAT_WATER, "adaptiveQuality", true,
                "Automatically reduce optional water effect frequency and detail when FPS stays low.").getBoolean(true);
        WATER_WAVE_STRENGTH = configuration.get(CAT_WATER, "waveStrength", 70,
                "Strength of Minefade's client-side ripple rings.", 0, 100).getInt(70);
        WATER_EFFECT_RADIUS = configuration.get(CAT_WATER, "effectRadius", 32,
                "Maximum radius for nearby water effects.", 8, 64).getInt(32);

        ENABLE_REALISTIC_ITEM_PHYSICS = configuration.get(CAT_ITEMS, "enablePhysics", true,
                "Enable Minefade's original dropped-item presentation and light physics.").getBoolean(true);
        FLAT_GROUND_ITEMS = configuration.get(CAT_ITEMS, "flatGroundItems", true,
                "Lay dropped item models flat when they rest on the ground.").getBoolean(true);
        MANUAL_ITEM_PICKUP = configuration.get(CAT_ITEMS, "manualPickup", true,
                "Require right-click to collect dropped items instead of automatic walking pickup.").getBoolean(true);
        ENABLE_SPRINT_TOSS = configuration.get(CAT_ITEMS, "sprintToss", true,
                "Add a forward toss impulse when dropping items while sprinting.").getBoolean(true);
        FLOAT_ITEMS_IN_FLUIDS = configuration.get(CAT_ITEMS, "floatInFluids", true,
                "Add gentle buoyancy and drag to dropped items in water and lava.").getBoolean(true);
        ITEM_TOSS_STRENGTH = configuration.get(CAT_ITEMS, "tossStrength", 85,
                "Sprint toss impulse strength in percent.", 30, 140).getInt(85);

        GENERATE_RESONITE_ORE = configuration.get(CAT_WORLDGEN, "generateResoniteOre", true,
                "Generate Resonite ore in newly generated Overworld chunks.").getBoolean(true);
        ORE_VEINS_PER_CHUNK = configuration.get(CAT_WORLDGEN, "oreVeinsPerChunk", 4,
                "Maximum attempted ore veins per newly generated Overworld chunk.", 0, 16).getInt(4);
        ORE_VEIN_SIZE = configuration.get(CAT_WORLDGEN, "oreVeinSize", 5,
                "Blocks per Resonite ore vein.", 1, 12).getInt(5);
        ORE_MIN_Y = configuration.get(CAT_WORLDGEN, "oreMinY", 5,
                "Minimum generation height for Resonite ore.", 1, 254).getInt(5);
        ORE_MAX_Y = configuration.get(CAT_WORLDGEN, "oreMaxY", 48,
                "Maximum generation height for Resonite ore.", 1, 255).getInt(48);

        if (ORE_MAX_Y < ORE_MIN_Y) {
            int swap = ORE_MAX_Y;
            ORE_MAX_Y = ORE_MIN_Y;
            ORE_MIN_Y = swap;
        }
        clampValues();
        if (configuration.hasChanged()) configuration.save();
    }

    public static void syncFromGui() {
        if (configuration == null) return;
        ENABLE_UI_THEME = configuration.get(CAT_INTERFACE, "enableUiTheme", true).getBoolean(true);
        SHOW_OVERLAY = configuration.get(CAT_DIAGNOSTICS, "showOverlay", true).getBoolean(true);
        SHOW_CHUNK_COUNT = configuration.get(CAT_DIAGNOSTICS, "showChunkCount", true).getBoolean(true);
        SHOW_MEMORY = configuration.get(CAT_DIAGNOSTICS, "showMemory", true).getBoolean(true);
        SHOW_ENTITY_COUNT = configuration.get(CAT_DIAGNOSTICS, "showEntityCount", true).getBoolean(true);
        SHOW_TILE_ENTITY_COUNT = configuration.get(CAT_DIAGNOSTICS, "showTileEntityCount", false).getBoolean(false);
        ENABLE_ENTITY_DISTANCE_CULLING = configuration.get(CAT_PERFORMANCE, "enableEntityDistanceCulling", false).getBoolean(false);
        ENTITY_DISTANCE_PERCENT = configuration.get(CAT_PERFORMANCE, "entityDistancePercent", 85).getInt(85);
        FORCE_MINIMAL_PARTICLES = configuration.get(CAT_PERFORMANCE, "forceMinimalParticles", false).getBoolean(false);
        ENABLE_ITEM_DISTANCE_CULLING = configuration.get(CAT_PERFORMANCE, "enableItemDistanceCulling", false).getBoolean(false);
        ITEM_RENDER_RADIUS = configuration.get(CAT_PERFORMANCE, "itemRenderRadius", 64).getInt(64);

        ENABLE_WATER_EFFECTS = configuration.get(CAT_WATER, "enableWaterEffects", true).getBoolean(true);
        ENABLE_WATER_RIPPLES = configuration.get(CAT_WATER, "enableWaterRipples", true).getBoolean(true);
        ENABLE_WATER_SPLASHES = configuration.get(CAT_WATER, "enableWaterSplashes", true).getBoolean(true);
        ENABLE_RAIN_RIPPLES = configuration.get(CAT_WATER, "enableRainRipples", true).getBoolean(true);
        ENABLE_WATERFALL_MIST = configuration.get(CAT_WATER, "enableWaterfallMist", true).getBoolean(true);
        ENABLE_UNDERWATER_TINT = configuration.get(CAT_WATER, "enableUnderwaterTint", true).getBoolean(true);
        ENABLE_ADAPTIVE_WATER_QUALITY = configuration.get(CAT_WATER, "adaptiveQuality", true).getBoolean(true);
        WATER_WAVE_STRENGTH = configuration.get(CAT_WATER, "waveStrength", 70).getInt(70);
        WATER_EFFECT_RADIUS = configuration.get(CAT_WATER, "effectRadius", 32).getInt(32);

        ENABLE_REALISTIC_ITEM_PHYSICS = configuration.get(CAT_ITEMS, "enablePhysics", true).getBoolean(true);
        FLAT_GROUND_ITEMS = configuration.get(CAT_ITEMS, "flatGroundItems", true).getBoolean(true);
        MANUAL_ITEM_PICKUP = configuration.get(CAT_ITEMS, "manualPickup", true).getBoolean(true);
        ENABLE_SPRINT_TOSS = configuration.get(CAT_ITEMS, "sprintToss", true).getBoolean(true);
        FLOAT_ITEMS_IN_FLUIDS = configuration.get(CAT_ITEMS, "floatInFluids", true).getBoolean(true);
        ITEM_TOSS_STRENGTH = configuration.get(CAT_ITEMS, "tossStrength", 85).getInt(85);
        ENABLE_PLAYER_ANIMATIONS = configuration.get(CAT_ANIMATION, "enablePlayerAnimations", true).getBoolean(true);

        GENERATE_RESONITE_ORE = configuration.get(CAT_WORLDGEN, "generateResoniteOre", true).getBoolean(true);
        ORE_VEINS_PER_CHUNK = configuration.get(CAT_WORLDGEN, "oreVeinsPerChunk", 4).getInt(4);
        ORE_VEIN_SIZE = configuration.get(CAT_WORLDGEN, "oreVeinSize", 5).getInt(5);
        ORE_MIN_Y = configuration.get(CAT_WORLDGEN, "oreMinY", 5).getInt(5);
        ORE_MAX_Y = configuration.get(CAT_WORLDGEN, "oreMaxY", 48).getInt(48);
        clampValues();
        save();
    }

    public static void setOverlayEnabled(boolean enabled) {
        SHOW_OVERLAY = enabled;
        setBoolean(CAT_DIAGNOSTICS, "showOverlay", enabled);
    }

    public static void setEntityCullingEnabled(boolean enabled) {
        ENABLE_ENTITY_DISTANCE_CULLING = enabled;
        setBoolean(CAT_PERFORMANCE, "enableEntityDistanceCulling", enabled);
    }

    public static void setMinimalParticlesEnabled(boolean enabled) {
        FORCE_MINIMAL_PARTICLES = enabled;
        setBoolean(CAT_PERFORMANCE, "forceMinimalParticles", enabled);
    }

    private static void setBoolean(String category, String key, boolean value) {
        if (configuration == null) return;
        Property property = configuration.get(category, key, value);
        property.set(value);
        configuration.save();
    }

    private static void clampValues() {
        ENTITY_DISTANCE_PERCENT = Math.max(50, Math.min(100, ENTITY_DISTANCE_PERCENT));
        WATER_WAVE_STRENGTH = Math.max(0, Math.min(100, WATER_WAVE_STRENGTH));
        WATER_EFFECT_RADIUS = Math.max(8, Math.min(64, WATER_EFFECT_RADIUS));
        ITEM_TOSS_STRENGTH = Math.max(30, Math.min(140, ITEM_TOSS_STRENGTH));
        ITEM_RENDER_RADIUS = Math.max(16, Math.min(128, ITEM_RENDER_RADIUS));
        ORE_VEINS_PER_CHUNK = Math.max(0, Math.min(16, ORE_VEINS_PER_CHUNK));
        ORE_VEIN_SIZE = Math.max(1, Math.min(12, ORE_VEIN_SIZE));
        ORE_MIN_Y = Math.max(1, Math.min(254, ORE_MIN_Y));
        ORE_MAX_Y = Math.max(ORE_MIN_Y, Math.min(255, ORE_MAX_Y));
    }

    public static void save() {
        if (configuration == null) return;
        clampValues();
        configuration.get(CAT_INTERFACE, "enableUiTheme", true).set(ENABLE_UI_THEME);
        configuration.get(CAT_DIAGNOSTICS, "showOverlay", true).set(SHOW_OVERLAY);
        configuration.get(CAT_DIAGNOSTICS, "showChunkCount", true).set(SHOW_CHUNK_COUNT);
        configuration.get(CAT_DIAGNOSTICS, "showMemory", true).set(SHOW_MEMORY);
        configuration.get(CAT_DIAGNOSTICS, "showEntityCount", true).set(SHOW_ENTITY_COUNT);
        configuration.get(CAT_DIAGNOSTICS, "showTileEntityCount", false).set(SHOW_TILE_ENTITY_COUNT);
        configuration.get(CAT_PERFORMANCE, "enableEntityDistanceCulling", false).set(ENABLE_ENTITY_DISTANCE_CULLING);
        configuration.get(CAT_PERFORMANCE, "entityDistancePercent", 85).set(ENTITY_DISTANCE_PERCENT);
        configuration.get(CAT_PERFORMANCE, "forceMinimalParticles", false).set(FORCE_MINIMAL_PARTICLES);
        configuration.get(CAT_PERFORMANCE, "enableItemDistanceCulling", false).set(ENABLE_ITEM_DISTANCE_CULLING);
        configuration.get(CAT_PERFORMANCE, "itemRenderRadius", 64).set(ITEM_RENDER_RADIUS);
        configuration.get(CAT_WATER, "enableWaterEffects", true).set(ENABLE_WATER_EFFECTS);
        configuration.get(CAT_WATER, "enableWaterRipples", true).set(ENABLE_WATER_RIPPLES);
        configuration.get(CAT_WATER, "enableWaterSplashes", true).set(ENABLE_WATER_SPLASHES);
        configuration.get(CAT_WATER, "enableRainRipples", true).set(ENABLE_RAIN_RIPPLES);
        configuration.get(CAT_WATER, "enableWaterfallMist", true).set(ENABLE_WATERFALL_MIST);
        configuration.get(CAT_WATER, "enableUnderwaterTint", true).set(ENABLE_UNDERWATER_TINT);
        configuration.get(CAT_WATER, "adaptiveQuality", true).set(ENABLE_ADAPTIVE_WATER_QUALITY);
        configuration.get(CAT_WATER, "waveStrength", 70).set(WATER_WAVE_STRENGTH);
        configuration.get(CAT_WATER, "effectRadius", 32).set(WATER_EFFECT_RADIUS);
        configuration.get(CAT_ITEMS, "enablePhysics", true).set(ENABLE_REALISTIC_ITEM_PHYSICS);
        configuration.get(CAT_ITEMS, "flatGroundItems", true).set(FLAT_GROUND_ITEMS);
        configuration.get(CAT_ITEMS, "manualPickup", true).set(MANUAL_ITEM_PICKUP);
        configuration.get(CAT_ITEMS, "sprintToss", true).set(ENABLE_SPRINT_TOSS);
        configuration.get(CAT_ITEMS, "floatInFluids", true).set(FLOAT_ITEMS_IN_FLUIDS);
        configuration.get(CAT_ITEMS, "tossStrength", 85).set(ITEM_TOSS_STRENGTH);
        configuration.get(CAT_ANIMATION, "enablePlayerAnimations", true).set(ENABLE_PLAYER_ANIMATIONS);
        configuration.get(CAT_WORLDGEN, "generateResoniteOre", true).set(GENERATE_RESONITE_ORE);
        configuration.get(CAT_WORLDGEN, "oreVeinsPerChunk", 4).set(ORE_VEINS_PER_CHUNK);
        configuration.get(CAT_WORLDGEN, "oreVeinSize", 5).set(ORE_VEIN_SIZE);
        configuration.get(CAT_WORLDGEN, "oreMinY", 5).set(ORE_MIN_Y);
        configuration.get(CAT_WORLDGEN, "oreMaxY", 48).set(ORE_MAX_Y);
        if (configuration.hasChanged()) configuration.save();
    }

    public static Configuration getConfiguration() {
        return configuration;
    }
}
