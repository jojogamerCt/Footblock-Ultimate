package net.footblock.footblockultimate.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * Borderless edit box whose text remains vertically centred in the custom
 * Score Manager field. Vanilla draws borderless edit-box text at the widget's
 * top edge instead of centring it.
 */
public final class ScoreManagerEditBox extends EditBox {
    private static final int TEXT_OFFSET_Y = 6;

    public ScoreManagerEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, TEXT_OFFSET_Y, 0.0F);
        super.renderWidget(graphics, mouseX, mouseY - TEXT_OFFSET_Y, partialTick);
        graphics.pose().popPose();
    }
}
