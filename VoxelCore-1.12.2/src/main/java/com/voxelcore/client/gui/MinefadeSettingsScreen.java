package com.voxelcore.client.gui;

import com.voxelcore.config.ModConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * OptiFine-inspired video settings layout:
 * category grid, compact two-column controls, click-to-cycle values and a
 * separate page for each tuning group. It uses vanilla GuiButton styling and
 * does not require new GUI images or a new resource pack.
 */
public final class MinefadeSettingsScreen extends GuiScreen {
    private static final int PAGE_QUALITY = 0;
    private static final int PAGE_DETAILS = 1;
    private static final int PAGE_PERFORMANCE = 2;
    private static final int PAGE_ANIMATIONS = 3;
    private static final int PAGE_FLUIDS = 4;
    private static final int PAGE_ITEMS = 5;
    private static final int PAGE_WORLD = 6;
    private static final int PAGE_OTHER = 7;

    private static final int ID_NAV_BASE = 10;
    private static final int ID_CLOSE = 99;
    private static final int ID_OPTION_BASE = 100;

    private static final String[] PAGE_NAMES = {
            "Calidad...", "Detalles...", "Rendimiento...", "Animaciones...",
            "Agua/Lava...", "Objetos...", "Mundo...", "Otros..."
    };

    private final GuiScreen parent;
    private int page = PAGE_QUALITY;
    private int panelLeft;
    private int panelWidth;
    private int firstOptionY;
    private int rowGap;

    public MinefadeSettingsScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        panelWidth = Math.min(720, this.width - 20);
        panelLeft = (this.width - panelWidth) / 2;

        int gap = 5;
        int navWidth = (panelWidth - 24 - gap * 3) / 4;
        for (int i = 0; i < PAGE_NAMES.length; i++) {
            int column = i % 4;
            int row = i / 4;
            int x = panelLeft + 12 + column * (navWidth + gap);
            int y = 36 + row * 23;
            this.buttonList.add(new GuiButton(ID_NAV_BASE + i, x, y, navWidth, 20, PAGE_NAMES[i]));
        }

        firstOptionY = 94;
        int footerY = this.height - 27;
        rowGap = Math.max(22, Math.min(28, (footerY - firstOptionY - 22) / 6));
        buildPageOptions();
        this.buttonList.add(new GuiButton(ID_CLOSE, this.width / 2 - 100,
                this.height - 25, 200, 20, "Volver"));
    }

    private void addToggle(int index, int column, int row, String label, boolean enabled) {
        addOption(index, column, row, label + ": " + (enabled ? "ON" : "OFF"));
    }

    private void addValue(int index, int column, int row, String label, String value) {
        addOption(index, column, row, label + ": " + value);
    }

    private void addOption(int index, int column, int row, String label) {
        int gap = 8;
        int width = (panelWidth - 28 - gap) / 2;
        int x = panelLeft + 12 + column * (width + gap);
        int y = firstOptionY + row * rowGap;
        this.buttonList.add(new GuiButton(ID_OPTION_BASE + index, x, y, width, 20, label));
    }

    private void buildPageOptions() {
        if (page == PAGE_QUALITY) {
            addToggle(0, 0, 0, "Gráficos detallados", mc.gameSettings.fancyGraphics);
            addValue(1, 1, 0, "Iluminación suave", mc.gameSettings.ambientOcclusion == 0 ? "OFF"
                    : (mc.gameSettings.ambientOcclusion == 1 ? "MÍNIMA" : "MÁXIMA"));
            addToggle(2, 0, 1, "Detalles procedurales de bloques", ModConfig.ENABLE_BLOCK_SURFACE_DETAILS);
            addValue(3, 1, 1, "Densidad mineral", densityName());
            addValue(4, 0, 2, "Calidad de agua", qualityName(ModConfig.WATER_RENDER_QUALITY));
            addToggle(5, 1, 2, "Reflejos sutiles", ModConfig.ENABLE_WATER_HIGHLIGHTS);
            addToggle(6, 0, 3, "Espuma de orilla", ModConfig.ENABLE_WATER_FOAM);
            addToggle(7, 1, 3, "Color submarino", ModConfig.ENABLE_UNDERWATER_TINT);
        } else if (page == PAGE_DETAILS) {
            addToggle(0, 0, 0, "Contacto con vegetación", ModConfig.ENABLE_VEGETATION_CONTACT);
            addToggle(1, 1, 0, "Impactos de slime", ModConfig.ENABLE_SLIME_CONTACT);
            addToggle(2, 0, 1, "Huellas por material", ModConfig.ENABLE_MATERIAL_FOOTSTEPS);
            addToggle(3, 1, 1, "Salpicaduras al entrar/nadar", ModConfig.ENABLE_WATER_SPLASHES);
            addToggle(4, 0, 2, "Impactos de lluvia", ModConfig.ENABLE_RAIN_RIPPLES);
            addToggle(5, 1, 2, "Neblina de cascadas", ModConfig.ENABLE_WATERFALL_MIST);
            addToggle(6, 0, 3, "Chispas de lava", ModConfig.ENABLE_LAVA_EMBERS);
        } else if (page == PAGE_PERFORMANCE) {
            addValue(0, 0, 0, "Distancia de render Minefade", ModConfig.WATER_EFFECT_RADIUS + " bloques");
            addValue(1, 1, 0, "Distancia de criaturas", ModConfig.ENTITY_DISTANCE_PERCENT + "%");
            addValue(2, 0, 1, "Radio de objetos", ModConfig.ITEM_RENDER_RADIUS + " bloques");
            addValue(3, 1, 1, "FPS máximo", mc.gameSettings.limitFramerate == 260
                    ? "ILIMITADO" : String.valueOf(mc.gameSettings.limitFramerate));
            addValue(4, 0, 2, "Distancia de render", mc.gameSettings.renderDistanceChunks + " chunks");
            addValue(5, 1, 2, "Partículas", particleName());
            addToggle(6, 0, 3, "Calidad adaptativa de agua", ModConfig.ENABLE_ADAPTIVE_WATER_QUALITY);
            addToggle(7, 1, 3, "Partículas mínimas", ModConfig.FORCE_MINIMAL_PARTICLES);
            addToggle(8, 0, 4, "Recorte de criaturas lejanas", ModConfig.ENABLE_ENTITY_DISTANCE_CULLING);
            addToggle(9, 1, 4, "Recorte de objetos lejanos", ModConfig.ENABLE_ITEM_DISTANCE_CULLING);
            addToggle(10, 0, 5, "Sincronización vertical", mc.gameSettings.enableVsync);
        } else if (page == PAGE_ANIMATIONS) {
            addToggle(0, 0, 0, "Animaciones del jugador", ModConfig.ENABLE_PLAYER_ANIMATIONS);
            addValue(1, 1, 0, "Intensidad del movimiento", ModConfig.PLAYER_ANIMATION_STRENGTH + "%");
            addToggle(2, 0, 1, "Microbrillos animados del agua", ModConfig.ENABLE_WATER_HIGHLIGHTS);
            addValue(3, 1, 1, "Calidad de superficie de lava", qualityName(ModConfig.LAVA_RENDER_QUALITY));
            addToggle(4, 0, 2, "Partículas de lava", ModConfig.ENABLE_LAVA_EMBERS);
            addToggle(5, 1, 2, "Salpicaduras y estelas", ModConfig.ENABLE_WATER_SPLASHES);
        } else if (page == PAGE_FLUIDS) {
            addToggle(0, 0, 0, "Efectos de agua", ModConfig.ENABLE_WATER_EFFECTS);
            addToggle(1, 1, 0, "Detalles de superficie", ModConfig.ENABLE_WATER_HIGHLIGHTS);
            addToggle(2, 0, 1, "Espuma de orilla", ModConfig.ENABLE_WATER_FOAM);
            addToggle(3, 1, 1, "Estelas y salpicaduras", ModConfig.ENABLE_WATER_SPLASHES);
            addToggle(4, 0, 2, "Aros de lluvia", ModConfig.ENABLE_RAIN_RIPPLES);
            addToggle(5, 1, 2, "Niebla submarina", ModConfig.ENABLE_UNDERWATER_TINT);
            addValue(6, 0, 3, "Calidad del agua", qualityName(ModConfig.WATER_RENDER_QUALITY));
            addValue(7, 1, 3, "Radio del agua", ModConfig.WATER_EFFECT_RADIUS + " bloques");
            addToggle(8, 0, 4, "Superficie de lava", ModConfig.ENABLE_LAVA_EFFECTS);
            addToggle(9, 1, 4, "Humo y brasas", ModConfig.ENABLE_LAVA_EMBERS);
            addValue(10, 0, 5, "Calidad de lava", qualityName(ModConfig.LAVA_RENDER_QUALITY));
            addValue(11, 1, 5, "Radio de lava", ModConfig.LAVA_EFFECT_RADIUS + " bloques");
            addValue(12, 0, 6, "Intensidad de brillo", ModConfig.WATER_WAVE_STRENGTH + "%");
            addToggle(13, 1, 6, "Respuesta dinámica del agua", ModConfig.ENABLE_WATER_RIPPLES);
        } else if (page == PAGE_ITEMS) {
            addToggle(0, 0, 0, "Física de objetos", ModConfig.ENABLE_REALISTIC_ITEM_PHYSICS);
            addToggle(1, 1, 0, "Objetos tumbados", ModConfig.FLAT_GROUND_ITEMS);
            addToggle(2, 0, 1, "Recogida con clic derecho", ModConfig.MANUAL_ITEM_PICKUP);
            addToggle(3, 1, 1, "Lanzamiento al correr", ModConfig.ENABLE_SPRINT_TOSS);
            addToggle(4, 0, 2, "Flotar en agua y lava", ModConfig.FLOAT_ITEMS_IN_FLUIDS);
            addValue(5, 1, 2, "Fuerza de lanzamiento", ModConfig.ITEM_TOSS_STRENGTH + "%");
        } else if (page == PAGE_WORLD) {
            addToggle(0, 0, 0, "Generar mena de resonita", ModConfig.GENERATE_RESONITE_ORE);
            addValue(1, 1, 0, "Vetas por chunk", String.valueOf(ModConfig.ORE_VEINS_PER_CHUNK));
            addValue(2, 0, 1, "Tamaño de veta", String.valueOf(ModConfig.ORE_VEIN_SIZE));
            addValue(3, 1, 1, "Altura mínima", "Y " + ModConfig.ORE_MIN_Y);
            addValue(4, 0, 2, "Altura máxima", "Y " + ModConfig.ORE_MAX_Y);
        } else {
            addToggle(0, 0, 0, "Tema de interfaz Minefade", ModConfig.ENABLE_UI_THEME);
            addToggle(1, 1, 0, "Monitor de rendimiento", ModConfig.SHOW_OVERLAY);
            addToggle(2, 0, 1, "Mostrar memoria", ModConfig.SHOW_MEMORY);
            addToggle(3, 1, 1, "Contar chunks", ModConfig.SHOW_CHUNK_COUNT);
            addToggle(4, 0, 2, "Contar entidades", ModConfig.SHOW_ENTITY_COUNT);
            addToggle(5, 1, 2, "Contar bloques especiales", ModConfig.SHOW_TILE_ENTITY_COUNT);
        }
    }

    private static String densityName() {
        switch (ModConfig.BLOCK_DETAIL_DENSITY) {
            case 0: return "OFF";
            case 1: return "BAJA";
            case 3: return "ALTA";
            default: return "MEDIA";
        }
    }

    private static String qualityName(int quality) {
        return quality <= 0 ? "BAJA" : (quality == 1 ? "MEDIA" : "ALTA");
    }

    private String particleName() {
        return mc.gameSettings.particleSetting == 0 ? "TODAS"
                : (mc.gameSettings.particleSetting == 1 ? "REDUCIDAS" : "MÍNIMAS");
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == ID_CLOSE) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (button.id >= ID_NAV_BASE && button.id < ID_NAV_BASE + PAGE_NAMES.length) {
            page = button.id - ID_NAV_BASE;
            initGui();
            return;
        }
        if (button.id < ID_OPTION_BASE) return;

        int option = button.id - ID_OPTION_BASE;
        applyOption(option);
        ModConfig.save();
        mc.gameSettings.saveOptions();
        initGui();
    }

    private void applyOption(int option) {
        if (page == PAGE_QUALITY) {
            switch (option) {
                case 0: mc.gameSettings.fancyGraphics = !mc.gameSettings.fancyGraphics; break;
                case 1: mc.gameSettings.ambientOcclusion = (mc.gameSettings.ambientOcclusion + 1) % 3; break;
                case 2: ModConfig.ENABLE_BLOCK_SURFACE_DETAILS = !ModConfig.ENABLE_BLOCK_SURFACE_DETAILS; break;
                case 3: ModConfig.BLOCK_DETAIL_DENSITY = (ModConfig.BLOCK_DETAIL_DENSITY + 1) % 4; break;
                case 4: ModConfig.WATER_RENDER_QUALITY = (ModConfig.WATER_RENDER_QUALITY + 1) % 3; break;
                case 5: ModConfig.ENABLE_WATER_HIGHLIGHTS = !ModConfig.ENABLE_WATER_HIGHLIGHTS; break;
                case 6: ModConfig.ENABLE_WATER_FOAM = !ModConfig.ENABLE_WATER_FOAM; break;
                case 7: ModConfig.ENABLE_UNDERWATER_TINT = !ModConfig.ENABLE_UNDERWATER_TINT; break;
            }
        } else if (page == PAGE_DETAILS) {
            switch (option) {
                case 0: ModConfig.ENABLE_VEGETATION_CONTACT = !ModConfig.ENABLE_VEGETATION_CONTACT; break;
                case 1: ModConfig.ENABLE_SLIME_CONTACT = !ModConfig.ENABLE_SLIME_CONTACT; break;
                case 2: ModConfig.ENABLE_MATERIAL_FOOTSTEPS = !ModConfig.ENABLE_MATERIAL_FOOTSTEPS; break;
                case 3: ModConfig.ENABLE_WATER_SPLASHES = !ModConfig.ENABLE_WATER_SPLASHES; break;
                case 4: ModConfig.ENABLE_RAIN_RIPPLES = !ModConfig.ENABLE_RAIN_RIPPLES; break;
                case 5: ModConfig.ENABLE_WATERFALL_MIST = !ModConfig.ENABLE_WATERFALL_MIST; break;
                case 6: ModConfig.ENABLE_LAVA_EMBERS = !ModConfig.ENABLE_LAVA_EMBERS; break;
            }
        } else if (page == PAGE_PERFORMANCE) {
            switch (option) {
                case 0: ModConfig.WATER_EFFECT_RADIUS = ModConfig.WATER_EFFECT_RADIUS >= 64 ? 8 : ModConfig.WATER_EFFECT_RADIUS + 8; break;
                case 1: ModConfig.ENTITY_DISTANCE_PERCENT = ModConfig.ENTITY_DISTANCE_PERCENT >= 100 ? 50 : ModConfig.ENTITY_DISTANCE_PERCENT + 10; break;
                case 2: ModConfig.ITEM_RENDER_RADIUS = ModConfig.ITEM_RENDER_RADIUS >= 128 ? 16 : ModConfig.ITEM_RENDER_RADIUS + 16; break;
                case 3: mc.gameSettings.limitFramerate = mc.gameSettings.limitFramerate >= 260 ? 30 : mc.gameSettings.limitFramerate + 30; break;
                case 4: mc.gameSettings.renderDistanceChunks = mc.gameSettings.renderDistanceChunks >= 32 ? 4 : mc.gameSettings.renderDistanceChunks + 2; break;
                case 5: mc.gameSettings.particleSetting = (mc.gameSettings.particleSetting + 1) % 3; break;
                case 6: ModConfig.ENABLE_ADAPTIVE_WATER_QUALITY = !ModConfig.ENABLE_ADAPTIVE_WATER_QUALITY; break;
                case 7: ModConfig.FORCE_MINIMAL_PARTICLES = !ModConfig.FORCE_MINIMAL_PARTICLES; break;
                case 8: ModConfig.ENABLE_ENTITY_DISTANCE_CULLING = !ModConfig.ENABLE_ENTITY_DISTANCE_CULLING; break;
                case 9: ModConfig.ENABLE_ITEM_DISTANCE_CULLING = !ModConfig.ENABLE_ITEM_DISTANCE_CULLING; break;
                case 10: mc.gameSettings.enableVsync = !mc.gameSettings.enableVsync; break;
            }
        } else if (page == PAGE_ANIMATIONS) {
            switch (option) {
                case 0: ModConfig.ENABLE_PLAYER_ANIMATIONS = !ModConfig.ENABLE_PLAYER_ANIMATIONS; break;
                case 1: ModConfig.PLAYER_ANIMATION_STRENGTH = ModConfig.PLAYER_ANIMATION_STRENGTH >= 100 ? 0 : ModConfig.PLAYER_ANIMATION_STRENGTH + 10; break;
                case 2: ModConfig.ENABLE_WATER_HIGHLIGHTS = !ModConfig.ENABLE_WATER_HIGHLIGHTS; break;
                case 3: ModConfig.LAVA_RENDER_QUALITY = (ModConfig.LAVA_RENDER_QUALITY + 1) % 3; break;
                case 4: ModConfig.ENABLE_LAVA_EMBERS = !ModConfig.ENABLE_LAVA_EMBERS; break;
                case 5: ModConfig.ENABLE_WATER_SPLASHES = !ModConfig.ENABLE_WATER_SPLASHES; break;
            }
        } else if (page == PAGE_FLUIDS) {
            switch (option) {
                case 0: ModConfig.ENABLE_WATER_EFFECTS = !ModConfig.ENABLE_WATER_EFFECTS; break;
                case 1: ModConfig.ENABLE_WATER_HIGHLIGHTS = !ModConfig.ENABLE_WATER_HIGHLIGHTS; break;
                case 2: ModConfig.ENABLE_WATER_FOAM = !ModConfig.ENABLE_WATER_FOAM; break;
                case 3: ModConfig.ENABLE_WATER_SPLASHES = !ModConfig.ENABLE_WATER_SPLASHES; break;
                case 4: ModConfig.ENABLE_RAIN_RIPPLES = !ModConfig.ENABLE_RAIN_RIPPLES; break;
                case 5: ModConfig.ENABLE_UNDERWATER_TINT = !ModConfig.ENABLE_UNDERWATER_TINT; break;
                case 6: ModConfig.WATER_RENDER_QUALITY = (ModConfig.WATER_RENDER_QUALITY + 1) % 3; break;
                case 7: ModConfig.WATER_EFFECT_RADIUS = ModConfig.WATER_EFFECT_RADIUS >= 64 ? 8 : ModConfig.WATER_EFFECT_RADIUS + 8; break;
                case 8: ModConfig.ENABLE_LAVA_EFFECTS = !ModConfig.ENABLE_LAVA_EFFECTS; break;
                case 9: ModConfig.ENABLE_LAVA_EMBERS = !ModConfig.ENABLE_LAVA_EMBERS; break;
                case 10: ModConfig.LAVA_RENDER_QUALITY = (ModConfig.LAVA_RENDER_QUALITY + 1) % 3; break;
                case 11: ModConfig.LAVA_EFFECT_RADIUS = ModConfig.LAVA_EFFECT_RADIUS >= 32 ? 6 : ModConfig.LAVA_EFFECT_RADIUS + 4; break;
                case 12: ModConfig.WATER_WAVE_STRENGTH = ModConfig.WATER_WAVE_STRENGTH >= 100 ? 0 : ModConfig.WATER_WAVE_STRENGTH + 10; break;
                case 13: ModConfig.ENABLE_WATER_RIPPLES = !ModConfig.ENABLE_WATER_RIPPLES; break;
            }
        } else if (page == PAGE_ITEMS) {
            switch (option) {
                case 0: ModConfig.ENABLE_REALISTIC_ITEM_PHYSICS = !ModConfig.ENABLE_REALISTIC_ITEM_PHYSICS; break;
                case 1: ModConfig.FLAT_GROUND_ITEMS = !ModConfig.FLAT_GROUND_ITEMS; break;
                case 2: ModConfig.MANUAL_ITEM_PICKUP = !ModConfig.MANUAL_ITEM_PICKUP; break;
                case 3: ModConfig.ENABLE_SPRINT_TOSS = !ModConfig.ENABLE_SPRINT_TOSS; break;
                case 4: ModConfig.FLOAT_ITEMS_IN_FLUIDS = !ModConfig.FLOAT_ITEMS_IN_FLUIDS; break;
                case 5: ModConfig.ITEM_TOSS_STRENGTH = ModConfig.ITEM_TOSS_STRENGTH >= 140 ? 30 : ModConfig.ITEM_TOSS_STRENGTH + 10; break;
            }
        } else if (page == PAGE_WORLD) {
            switch (option) {
                case 0: ModConfig.GENERATE_RESONITE_ORE = !ModConfig.GENERATE_RESONITE_ORE; break;
                case 1: ModConfig.ORE_VEINS_PER_CHUNK = ModConfig.ORE_VEINS_PER_CHUNK >= 16 ? 0 : ModConfig.ORE_VEINS_PER_CHUNK + 1; break;
                case 2: ModConfig.ORE_VEIN_SIZE = ModConfig.ORE_VEIN_SIZE >= 12 ? 1 : ModConfig.ORE_VEIN_SIZE + 1; break;
                case 3: ModConfig.ORE_MIN_Y = ModConfig.ORE_MIN_Y >= ModConfig.ORE_MAX_Y - 1 ? 1 : ModConfig.ORE_MIN_Y + 1; break;
                case 4: ModConfig.ORE_MAX_Y = ModConfig.ORE_MAX_Y >= 255 ? Math.max(ModConfig.ORE_MIN_Y + 1, 2) : ModConfig.ORE_MAX_Y + 1; break;
            }
        } else {
            switch (option) {
                case 0: ModConfig.ENABLE_UI_THEME = !ModConfig.ENABLE_UI_THEME; break;
                case 1: ModConfig.SHOW_OVERLAY = !ModConfig.SHOW_OVERLAY; break;
                case 2: ModConfig.SHOW_MEMORY = !ModConfig.SHOW_MEMORY; break;
                case 3: ModConfig.SHOW_CHUNK_COUNT = !ModConfig.SHOW_CHUNK_COUNT; break;
                case 4: ModConfig.SHOW_ENTITY_COUNT = !ModConfig.SHOW_ENTITY_COUNT; break;
                case 5: ModConfig.SHOW_TILE_ENTITY_COUNT = !ModConfig.SHOW_TILE_ENTITY_COUNT; break;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(this.fontRenderer, "Video Settings", this.width / 2, 10, 0xFFFFFF);
        drawCenteredString(this.fontRenderer, PAGE_NAMES[page].replace("...", ""),
                this.width / 2, 23, 0xD0D0D0);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}