package com.voxelcore.client.gui;

import com.voxelcore.VoxelCore;
import com.voxelcore.config.ModConfig;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/** Responsive Minefade control centre with diagnostics, performance, water, world and item tabs. */
public class MinefadeSettingsScreen extends GuiScreen {
    private static final int TAB_DIAGNOSTICS = 0;
    private static final int TAB_PERFORMANCE = 1;
    private static final int TAB_WATER = 2;
    private static final int TAB_CONTENT = 3;
    private static final int TAB_ITEMS = 4;
    private static final int ID_DIAG = 10;
    private static final int ID_PERF = 11;
    private static final int ID_WATER = 12;
    private static final int ID_CONTENT = 13;
    private static final int ID_ITEMS = 14;
    private static final int ID_CLOSE = 99;
    private static final int ID_TOGGLE_BASE = 100;
    private static final int ID_ADJUST_BASE = 200;

    private final GuiScreen parent;
    private int tab = TAB_DIAGNOSTICS;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

    public MinefadeSettingsScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        panelWidth = Math.min(430, this.width - 24);
        panelHeight = Math.min(292, this.height - 24);
        panelLeft = (this.width - panelWidth) / 2;
        panelTop = (this.height - panelHeight) / 2;

        int tabY = panelTop + 48;
        int gap = 4;
        int tabWidth = (panelWidth - 30 - gap * 4) / 5;
        this.buttonList.add(new MinefadeButton(ID_DIAG, panelLeft + 10, tabY, tabWidth, 20, "INFO"));
        this.buttonList.add(new MinefadeButton(ID_PERF, panelLeft + 10 + tabWidth + gap, tabY, tabWidth, 20, "REND."));
        this.buttonList.add(new MinefadeButton(ID_WATER, panelLeft + 10 + (tabWidth + gap) * 2, tabY, tabWidth, 20, "AGUA"));
        this.buttonList.add(new MinefadeButton(ID_CONTENT, panelLeft + 10 + (tabWidth + gap) * 3, tabY, tabWidth, 20, "MUNDO"));
        this.buttonList.add(new MinefadeButton(ID_ITEMS, panelLeft + 10 + (tabWidth + gap) * 4, tabY, tabWidth, 20, "OBJETOS"));

        int closeY = panelTop + panelHeight - 30;
        this.buttonList.add(new MinefadeButton(ID_CLOSE, panelLeft + panelWidth - 104, closeY, 92, 20, "CERRAR"));
        buildTabButtons();
    }

    private void buildTabButtons() {
        int left = panelLeft + 14;
        int right = panelLeft + panelWidth / 2 + 2;
        int y = panelTop + 82;
        int colWidth = (panelWidth - 36) / 2;
        int rowGap = 30;

        if (tab == TAB_DIAGNOSTICS) {
            addToggle(0, left, y, colWidth, "Tema Minefade", ModConfig.ENABLE_UI_THEME);
            addToggle(1, right, y, colWidth, "Monitor de rendimiento", ModConfig.SHOW_OVERLAY);
            addToggle(2, left, y + rowGap, colWidth, "Mostrar memoria", ModConfig.SHOW_MEMORY);
            addToggle(3, right, y + rowGap, colWidth, "Contar chunks", ModConfig.SHOW_CHUNK_COUNT);
            addToggle(4, left, y + rowGap * 2, colWidth, "Contar entidades", ModConfig.SHOW_ENTITY_COUNT);
            addToggle(5, right, y + rowGap * 2, colWidth, "Contar bloques especiales", ModConfig.SHOW_TILE_ENTITY_COUNT);
        } else if (tab == TAB_PERFORMANCE) {
            addToggle(0, left, y, colWidth, "Recorte de criaturas lejanas", ModConfig.ENABLE_ENTITY_DISTANCE_CULLING);
            addAdjust(0, right, y, colWidth, "Distancia de criaturas: " + ModConfig.ENTITY_DISTANCE_PERCENT + "%");
            addToggle(1, left, y + rowGap, colWidth, "Partículas mínimas", ModConfig.FORCE_MINIMAL_PARTICLES);
            addToggle(2, left, y + rowGap * 2, colWidth, "Ocultar objetos lejanos", ModConfig.ENABLE_ITEM_DISTANCE_CULLING);\n            addToggle(3, right, y + rowGap * 2, colWidth, "Animaciones naturales", ModConfig.ENABLE_PLAYER_ANIMATIONS);
            addAdjust(1, right, y + rowGap * 2, colWidth, "Radio objetos: " + ModConfig.ITEM_RENDER_RADIUS);
        } else if (tab == TAB_WATER) {
            addToggle(0, left, y, colWidth, "Efectos de agua Minefade", ModConfig.ENABLE_WATER_EFFECTS);
            addToggle(1, right, y, colWidth, "Ondas y estelas", ModConfig.ENABLE_WATER_RIPPLES);
            addToggle(2, left, y + rowGap, colWidth, "Salpicaduras al nadar", ModConfig.ENABLE_WATER_SPLASHES);
            addToggle(3, right, y + rowGap, colWidth, "Aros de lluvia", ModConfig.ENABLE_RAIN_RIPPLES);
            addToggle(4, left, y + rowGap * 2, colWidth, "Neblina en cascadas", ModConfig.ENABLE_WATERFALL_MIST);
            addToggle(5, right, y + rowGap * 2, colWidth, "Color submarino por bioma", ModConfig.ENABLE_UNDERWATER_TINT);
            addAdjust(0, left, y + rowGap * 3, colWidth, "Intensidad de ondas: " + ModConfig.WATER_WAVE_STRENGTH + "%");
            addAdjust(1, right, y + rowGap * 3, colWidth, "Radio de efectos: " + ModConfig.WATER_EFFECT_RADIUS);
            addToggle(6, left, y + rowGap * 4, colWidth * 2 + 4,
                    "Calidad de agua automática", ModConfig.ENABLE_ADAPTIVE_WATER_QUALITY);
        } else if (tab == TAB_CONTENT) {
            addToggle(0, left, y, colWidth, "Generar mena de resonita", ModConfig.GENERATE_RESONITE_ORE);
            addAdjust(0, right, y, colWidth, "Vetas por chunk: " + ModConfig.ORE_VEINS_PER_CHUNK);
            addAdjust(1, left, y + rowGap, colWidth, "Tamaño de veta: " + ModConfig.ORE_VEIN_SIZE);
            addAdjust(2, right, y + rowGap, colWidth, "Altura mínima: " + ModConfig.ORE_MIN_Y);
            addAdjust(3, left, y + rowGap * 2, colWidth, "Altura máxima: " + ModConfig.ORE_MAX_Y);
        } else {
            addToggle(0, left, y, colWidth, "Física realista de objetos", ModConfig.ENABLE_REALISTIC_ITEM_PHYSICS);
            addToggle(1, right, y, colWidth, "Objetos tumbados", ModConfig.FLAT_GROUND_ITEMS);
            addToggle(2, left, y + rowGap, colWidth, "Recoger con clic derecho", ModConfig.MANUAL_ITEM_PICKUP);
            addToggle(3, right, y + rowGap, colWidth, "Lanzar al correr", ModConfig.ENABLE_SPRINT_TOSS);
            addToggle(4, left, y + rowGap * 2, colWidth, "Flotar en agua y lava", ModConfig.FLOAT_ITEMS_IN_FLUIDS);
            addAdjust(0, right, y + rowGap * 2, colWidth, "Fuerza de lanzamiento: " + ModConfig.ITEM_TOSS_STRENGTH + "%");
        }
    }

    private void addToggle(int index, int x, int y, int width, String label, boolean enabled) {
        String mark = enabled ? "[ON] " : "[OFF] ";
        this.buttonList.add(new MinefadeButton(ID_TOGGLE_BASE + index, x, y, width, 22, mark + label));
    }

    private void addAdjust(int index, int x, int y, int width, String label) {
        this.buttonList.add(new MinefadeButton(ID_ADJUST_BASE + index, x, y, width, 22, label + "  CAMBIAR"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == ID_CLOSE) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (button.id == ID_DIAG || button.id == ID_PERF || button.id == ID_WATER
                || button.id == ID_CONTENT || button.id == ID_ITEMS) {
            if (button.id == ID_DIAG) tab = TAB_DIAGNOSTICS;
            else if (button.id == ID_PERF) tab = TAB_PERFORMANCE;
            else if (button.id == ID_WATER) tab = TAB_WATER;
            else if (button.id == ID_CONTENT) tab = TAB_CONTENT;
            else tab = TAB_ITEMS;
            initGui();
            return;
        }

        if (tab == TAB_DIAGNOSTICS && button.id >= ID_TOGGLE_BASE && button.id < ID_TOGGLE_BASE + 6) {
            switch (button.id - ID_TOGGLE_BASE) {
                case 0: ModConfig.ENABLE_UI_THEME = !ModConfig.ENABLE_UI_THEME; break;
                case 1: ModConfig.SHOW_OVERLAY = !ModConfig.SHOW_OVERLAY; break;
                case 2: ModConfig.SHOW_MEMORY = !ModConfig.SHOW_MEMORY; break;
                case 3: ModConfig.SHOW_CHUNK_COUNT = !ModConfig.SHOW_CHUNK_COUNT; break;
                case 4: ModConfig.SHOW_ENTITY_COUNT = !ModConfig.SHOW_ENTITY_COUNT; break;
                case 5: ModConfig.SHOW_TILE_ENTITY_COUNT = !ModConfig.SHOW_TILE_ENTITY_COUNT; break;
            }
        } else if (tab == TAB_PERFORMANCE && button.id == ID_TOGGLE_BASE) {
            ModConfig.ENABLE_ENTITY_DISTANCE_CULLING = !ModConfig.ENABLE_ENTITY_DISTANCE_CULLING;
        } else if (tab == TAB_PERFORMANCE && button.id == ID_TOGGLE_BASE + 1) {
            ModConfig.FORCE_MINIMAL_PARTICLES = !ModConfig.FORCE_MINIMAL_PARTICLES;
        } else if (tab == TAB_PERFORMANCE && button.id == ID_TOGGLE_BASE + 2) {
            ModConfig.ENABLE_ITEM_DISTANCE_CULLING = !ModConfig.ENABLE_ITEM_DISTANCE_CULLING;
        } else if (tab == TAB_PERFORMANCE && button.id == ID_TOGGLE_BASE + 3) {\n            ModConfig.ENABLE_PLAYER_ANIMATIONS = !ModConfig.ENABLE_PLAYER_ANIMATIONS;\n        } else if (tab == TAB_PERFORMANCE && button.id == ID_ADJUST_BASE) {
            ModConfig.ENTITY_DISTANCE_PERCENT = ModConfig.ENTITY_DISTANCE_PERCENT >= 100 ? 50 : ModConfig.ENTITY_DISTANCE_PERCENT + 10;
        } else if (tab == TAB_PERFORMANCE && button.id == ID_ADJUST_BASE + 1) {
            ModConfig.ITEM_RENDER_RADIUS = ModConfig.ITEM_RENDER_RADIUS >= 128 ? 16 : ModConfig.ITEM_RENDER_RADIUS + 16;
        } else if (tab == TAB_WATER) {
            if (button.id == ID_TOGGLE_BASE) ModConfig.ENABLE_WATER_EFFECTS = !ModConfig.ENABLE_WATER_EFFECTS;
            else if (button.id == ID_TOGGLE_BASE + 1) ModConfig.ENABLE_WATER_RIPPLES = !ModConfig.ENABLE_WATER_RIPPLES;
            else if (button.id == ID_TOGGLE_BASE + 2) ModConfig.ENABLE_WATER_SPLASHES = !ModConfig.ENABLE_WATER_SPLASHES;
            else if (button.id == ID_TOGGLE_BASE + 3) ModConfig.ENABLE_RAIN_RIPPLES = !ModConfig.ENABLE_RAIN_RIPPLES;
            else if (button.id == ID_TOGGLE_BASE + 4) ModConfig.ENABLE_WATERFALL_MIST = !ModConfig.ENABLE_WATERFALL_MIST;
            else if (button.id == ID_TOGGLE_BASE + 5) ModConfig.ENABLE_UNDERWATER_TINT = !ModConfig.ENABLE_UNDERWATER_TINT;
            else if (button.id == ID_TOGGLE_BASE + 6) ModConfig.ENABLE_ADAPTIVE_WATER_QUALITY = !ModConfig.ENABLE_ADAPTIVE_WATER_QUALITY;
            else if (button.id == ID_ADJUST_BASE) ModConfig.WATER_WAVE_STRENGTH = ModConfig.WATER_WAVE_STRENGTH >= 100 ? 0 : ModConfig.WATER_WAVE_STRENGTH + 10;
            else if (button.id == ID_ADJUST_BASE + 1) ModConfig.WATER_EFFECT_RADIUS = ModConfig.WATER_EFFECT_RADIUS >= 64 ? 8 : ModConfig.WATER_EFFECT_RADIUS + 8;
        } else if (tab == TAB_CONTENT) {
            if (button.id == ID_TOGGLE_BASE) ModConfig.GENERATE_RESONITE_ORE = !ModConfig.GENERATE_RESONITE_ORE;
            else if (button.id == ID_ADJUST_BASE) ModConfig.ORE_VEINS_PER_CHUNK = ModConfig.ORE_VEINS_PER_CHUNK >= 16 ? 0 : ModConfig.ORE_VEINS_PER_CHUNK + 1;
            else if (button.id == ID_ADJUST_BASE + 1) ModConfig.ORE_VEIN_SIZE = ModConfig.ORE_VEIN_SIZE >= 12 ? 1 : ModConfig.ORE_VEIN_SIZE + 1;
            else if (button.id == ID_ADJUST_BASE + 2) ModConfig.ORE_MIN_Y = ModConfig.ORE_MIN_Y >= ModConfig.ORE_MAX_Y - 1 ? 1 : ModConfig.ORE_MIN_Y + 1;
            else if (button.id == ID_ADJUST_BASE + 3) ModConfig.ORE_MAX_Y = ModConfig.ORE_MAX_Y >= 255 ? Math.max(ModConfig.ORE_MIN_Y + 1, 2) : ModConfig.ORE_MAX_Y + 1;
        } else if (tab == TAB_ITEMS) {
            if (button.id == ID_TOGGLE_BASE) ModConfig.ENABLE_REALISTIC_ITEM_PHYSICS = !ModConfig.ENABLE_REALISTIC_ITEM_PHYSICS;
            else if (button.id == ID_TOGGLE_BASE + 1) ModConfig.FLAT_GROUND_ITEMS = !ModConfig.FLAT_GROUND_ITEMS;
            else if (button.id == ID_TOGGLE_BASE + 2) ModConfig.MANUAL_ITEM_PICKUP = !ModConfig.MANUAL_ITEM_PICKUP;
            else if (button.id == ID_TOGGLE_BASE + 3) ModConfig.ENABLE_SPRINT_TOSS = !ModConfig.ENABLE_SPRINT_TOSS;
            else if (button.id == ID_TOGGLE_BASE + 4) ModConfig.FLOAT_ITEMS_IN_FLUIDS = !ModConfig.FLOAT_ITEMS_IN_FLUIDS;
            else if (button.id == ID_ADJUST_BASE) ModConfig.ITEM_TOSS_STRENGTH = ModConfig.ITEM_TOSS_STRENGTH >= 140 ? 30 : ModConfig.ITEM_TOSS_STRENGTH + 10;
        }

        ModConfig.save();
        initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawGradientRect(0, 0, width, height, 0xD9081118, 0xF0080C12);

        mc.getTextureManager().bindTexture(new net.minecraft.util.ResourceLocation(VoxelCore.MODID, "textures/gui/panel_leather.png"));
        drawModalRectWithCustomSizedTexture(panelLeft, panelTop, 0.0F, 0.0F,
                panelWidth, panelHeight, 128.0F, 128.0F);
        Gui.drawRect(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 2, 0xFFD4AD7A);
        Gui.drawRect(panelLeft + 1, panelTop + 1, panelLeft + panelWidth - 1, panelTop + panelHeight - 1, 0x221FFFFF);
        Gui.drawRect(panelLeft + 10, panelTop + 37, panelLeft + panelWidth - 10, panelTop + 38, 0x663D5B69);

        this.fontRenderer.drawString("MINEFADE", panelLeft + 14, panelTop + 12, 0xFFE3C18B, false);
        this.fontRenderer.drawString("Centro de control", panelLeft + 14, panelTop + 25, 0xFFB6C6D0, false);
        String version = "1.12.2  •  " + VoxelCore.VERSION;
        this.fontRenderer.drawString(version, panelLeft + panelWidth - this.fontRenderer.getStringWidth(version) - 14,
                panelTop + 17, 0xFF8195A3, false);

        String hint;
        if (tab == TAB_DIAGNOSTICS) hint = "Indicadores y aspecto de las pantallas.";
        else if (tab == TAB_PERFORMANCE) hint = "Optimización visual opcional y reversible.";
        else if (tab == TAB_WATER) hint = "Efectos locales; el agua vanilla conserva su física.";
        else if (tab == TAB_ITEMS) hint = "Objetos tumbados, flotación, lanzamiento y recogida manual.";
        else hint = "La generación solo afecta a chunks nuevos del mundo.";
        this.fontRenderer.drawString(hint, panelLeft + 14, panelTop + panelHeight - 47, 0xFFB9A78D, false);

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
