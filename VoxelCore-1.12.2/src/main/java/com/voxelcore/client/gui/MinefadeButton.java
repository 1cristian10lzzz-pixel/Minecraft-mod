package com.voxelcore.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

/** A skinned replacement for ordinary GuiButton instances. IDs and hitboxes stay unchanged. */
public class MinefadeButton extends GuiButton {
    private static final int PANEL = 0xE51A2531;
    private static final int PANEL_HOVER = 0xF02A3A49;
    private static final int ACCENT = 0xFF31C6B2;
    private static final int ACCENT_HOVER = 0xFF66F0D9;

    public MinefadeButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
    }

    public MinefadeButton(GuiButton original) {
        this(original.id, original.x, original.y, original.width, original.height, original.displayString);
        this.enabled = original.enabled;
        this.visible = original.visible;
        this.packedFGColour = original.packedFGColour;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;

        this.hovered = mouseX >= this.x && mouseY >= this.y
                && mouseX < this.x + this.width && mouseY < this.y + this.height;

        int background = !this.enabled ? 0xB51A2028 : (this.hovered ? PANEL_HOVER : PANEL);
        int accent = this.hovered ? ACCENT_HOVER : ACCENT;
        int text = !this.enabled ? 0xFF77838D : (this.packedFGColour != 0 ? this.packedFGColour : 0xFFF1F7FA);

        Gui.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, background);
        Gui.drawRect(this.x, this.y, this.x + this.width, this.y + 1, 0x55FFFFFF);
        Gui.drawRect(this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, 0xAA0D141B);
        Gui.drawRect(this.x, this.y, this.x + 3, this.y + this.height, accent);
        if (this.hovered && this.enabled) {
            Gui.drawRect(this.x + 3, this.y + 1, this.x + this.width, this.y + 2, 0x6631C6B2);
        }

        String label = this.displayString == null ? "" : this.displayString;
        int labelWidth = mc.fontRenderer.getStringWidth(label);
        float scale = labelWidth > this.width - 12 ? (float) (this.width - 12) / (float) Math.max(1, labelWidth) : 1.0F;
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0F);
        float centerX = (this.x + this.width / 2.0F) / scale;
        float centerY = (this.y + (this.height - 8) / 2.0F) / scale;
        mc.fontRenderer.drawString(label, (int) (centerX - labelWidth / 2.0F), (int) centerY, text, true);
        GlStateManager.popMatrix();
        this.mouseDragged(mc, mouseX, mouseY);
    }
}
