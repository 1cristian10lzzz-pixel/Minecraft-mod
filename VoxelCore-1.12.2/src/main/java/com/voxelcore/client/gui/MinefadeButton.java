package com.voxelcore.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/** A textured replacement for ordinary GuiButton instances. IDs and hitboxes stay unchanged. */
public class MinefadeButton extends GuiButton {
    private static final ResourceLocation BUTTON_TEXTURE =
            new ResourceLocation("voxelcore", "textures/gui/button_leather.png");
    private static final int ACCENT = 0xFFC99B61;
    private static final int ACCENT_HOVER = 0xFFE7C58A;

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

        int accent = this.hovered ? ACCENT_HOVER : ACCENT;
        int text = !this.enabled ? 0xFF77838D : (this.packedFGColour != 0 ? this.packedFGColour : 0xFFF1F0E9);
        mc.getTextureManager().bindTexture(BUTTON_TEXTURE);
        Gui.drawScaledCustomSizeModalRect(this.x, this.y, 0.0F, 0.0F, 64, 24,
                this.width, this.height, 64.0F, 24.0F);
        if (!this.enabled) Gui.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0x66707070);
        Gui.drawRect(this.x, this.y, this.x + 3, this.y + this.height, accent);
        if (this.hovered && this.enabled) {
            Gui.drawRect(this.x + 3, this.y + 1, this.x + this.width, this.y + 2, 0x55E8C58F);
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
