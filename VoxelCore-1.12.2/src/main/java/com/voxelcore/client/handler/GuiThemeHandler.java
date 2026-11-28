package com.voxelcore.client.handler;

import com.voxelcore.client.gui.MinefadeButton;
import com.voxelcore.client.gui.MinefadeSettingsScreen;
import com.voxelcore.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.ListIterator;

/** Applies a consistent Minefade skin to ordinary GUI buttons without changing their IDs or actions. */
@SideOnly(Side.CLIENT)
public class GuiThemeHandler {
    private static final int MINEFADE_BUTTON_ID = 0x4D46;

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!ModConfig.ENABLE_UI_THEME) return;
        GuiScreen screen = event.getGui();
        if (screen instanceof MinefadeSettingsScreen) return;
        List<GuiButton> buttons = event.getButtonList();
        ListIterator<GuiButton> iterator = buttons.listIterator();

        while (iterator.hasNext()) {
            GuiButton button = iterator.next();
            // Keep sliders, image buttons, toggles, and other specialized controls intact.
            if (button != null && (button.getClass() == GuiButton.class || button.getClass() == GuiOptionButton.class)) {
                iterator.set(new MinefadeButton(button));
            }
        }

        if (screen instanceof GuiMainMenu || screen instanceof GuiIngameMenu || screen instanceof GuiOptions) {
            boolean exists = false;
            for (GuiButton button : buttons) {
                if (button.id == MINEFADE_BUTTON_ID) exists = true;
            }
            if (!exists && screen.width >= 280) {
                buttons.add(new MinefadeButton(MINEFADE_BUTTON_ID, screen.width - 100, screen.height - 27,
                        92, 20, "MINEFADE"));
            }
        }
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.getButton() == null || event.getButton().id != MINEFADE_BUTTON_ID) return;
        event.setCanceled(true);
        Minecraft.getMinecraft().displayGuiScreen(new MinefadeSettingsScreen(event.getGui()));
    }

    @SubscribeEvent
    public void onGuiDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!ModConfig.ENABLE_UI_THEME) return;
        GuiScreen screen = event.getGui();
        if (screen == null || screen instanceof GuiChat || screen instanceof MinefadeSettingsScreen) return;

        // A very small identity strip stays in the outer margin and does not cover container slots.
        int width = screen.width;
        if (width < 300) return;
        GlStateManager.pushMatrix();
        net.minecraft.client.gui.Gui.drawRect(0, 0, width, 2, 0xFF31C6B2);
        int badgeWidth = 88;
        int x = width - badgeWidth - 7;
        net.minecraft.client.gui.Gui.drawRect(x, 5, x + badgeWidth, 17, 0xA9121D27);
        Minecraft.getMinecraft().fontRenderer.drawString("MINEFADE UI", x + 5, 7, 0xFF85EBDD, false);
        GlStateManager.popMatrix();
    }
}
