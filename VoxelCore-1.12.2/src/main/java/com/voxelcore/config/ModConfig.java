package com.voxelcore.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;

/** Central, persistent configuration for Minefade Mod. Keep defaults conservative for mod compatibility. */
public final class ModConfig {
    private static final String CAT_PERFORMANCE = "performance";
    private static final String CAT_DIAGNOSTICS = "diagnostics";
    private static final String CAT_WORLDGEN = "world_generation";
    private static final String CAT_INTERFACE = "interface";

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
                "Optionally skip rendering distant non-player living entities. This can hide distant mobs and is disabled by default.").getBoolean(false);
        ENTITY_DISTANCE_PERCENT = configuration.get(CAT_PERFORMANCE, "entityDistancePercent", 85,
                "Entity render cutoff as a percentage of the configured view distance.", 50, 100).getInt(85);
        FORCE_MINIMAL_PARTICLES = configuration.get(CAT_PERFORMANCE, "forceMinimalParticles", false,
                "Use Minecraft's minimal-particles setting while enabled. The previous setting is restored when disabled.").getBoolean(false);

        GENERATE_RESONITE_ORE = configuration.get(CAT_WORLDGEN, "generateResoniteOre", true,
                "Generate the example Resonite ore in newly generated Overworld chunks.").getBoolean(true);
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

        if (configuration.hasChanged()) configuration.save();
    }

    /** Called after Forge's GuiConfig screen has written values into the Configuration object. */
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
        GENERATE_RESONITE_ORE = configuration.get(CAT_WORLDGEN, "generateResoniteOre", true).getBoolean(true);
        ORE_VEINS_PER_CHUNK = configuration.get(CAT_WORLDGEN, "oreVeinsPerChunk", 4).getInt(4);
        ORE_VEIN_SIZE = configuration.get(CAT_WORLDGEN, "oreVeinSize", 5).getInt(5);
        ORE_MIN_Y = configuration.get(CAT_WORLDGEN, "oreMinY", 5).getInt(5);
        ORE_MAX_Y = configuration.get(CAT_WORLDGEN, "oreMaxY", 48).getInt(48);
        if (configuration.hasChanged()) configuration.save();
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


    /** Persist every live setting modified by the in-game Minefade control panel. */
    public static void save() {
        if (configuration == null) return;
        configuration.get(CAT_INTERFACE, "enableUiTheme", true).set(ENABLE_UI_THEME);
        configuration.get(CAT_DIAGNOSTICS, "showOverlay", true).set(SHOW_OVERLAY);
        configuration.get(CAT_DIAGNOSTICS, "showChunkCount", true).set(SHOW_CHUNK_COUNT);
        configuration.get(CAT_DIAGNOSTICS, "showMemory", true).set(SHOW_MEMORY);
        configuration.get(CAT_DIAGNOSTICS, "showEntityCount", true).set(SHOW_ENTITY_COUNT);
        configuration.get(CAT_DIAGNOSTICS, "showTileEntityCount", false).set(SHOW_TILE_ENTITY_COUNT);
        configuration.get(CAT_PERFORMANCE, "enableEntityDistanceCulling", false).set(ENABLE_ENTITY_DISTANCE_CULLING);
        configuration.get(CAT_PERFORMANCE, "entityDistancePercent", 85).set(Math.max(50, Math.min(100, ENTITY_DISTANCE_PERCENT)));
        configuration.get(CAT_PERFORMANCE, "forceMinimalParticles", false).set(FORCE_MINIMAL_PARTICLES);
        configuration.get(CAT_WORLDGEN, "generateResoniteOre", true).set(GENERATE_RESONITE_ORE);
        configuration.get(CAT_WORLDGEN, "oreVeinsPerChunk", 4).set(Math.max(0, Math.min(16, ORE_VEINS_PER_CHUNK)));
        configuration.get(CAT_WORLDGEN, "oreVeinSize", 5).set(Math.max(1, Math.min(12, ORE_VEIN_SIZE)));
        configuration.get(CAT_WORLDGEN, "oreMinY", 5).set(Math.max(1, Math.min(254, ORE_MIN_Y)));
        configuration.get(CAT_WORLDGEN, "oreMaxY", 48).set(Math.max(1, Math.min(255, ORE_MAX_Y)));
        if (configuration.hasChanged()) configuration.save();
    }

    public static Configuration getConfiguration() {
        return configuration;
    }
}
