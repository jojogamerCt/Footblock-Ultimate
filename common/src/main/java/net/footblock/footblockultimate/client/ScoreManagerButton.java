package net.footblock.footblockultimate.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public final class ScoreManagerButton extends AbstractButton {
    private final Runnable action;
    private final int accentColor;
    private final BooleanSupplier selected;

    public ScoreManagerButton(int x, int y, int width, int height, Component message,
                              int accentColor, BooleanSupplier selected, Runnable action) {
        super(x, y, width, height, message);
        this.action = action;
        this.accentColor = accentColor;
        this.selected = selected;
    }

    @Override
    public void onPress() {
        this.action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean highlighted = this.isHoveredOrFocused();
        boolean selectedNow = this.selected.getAsBoolean();
        int x = this.getX();
        int y = this.getY();
        int background = selectedNow ? 0xFF18343A : highlighted ? 0xFF1B3037 : 0xFF111D22;
        int border = selectedNow || highlighted ? this.accentColor : 0xFF42606A;
        int textColor = this.active ? 0xFFF2FBFF : 0xFF6F7B80;

        graphics.fill(x + 2, y + 2, x + this.width + 2, y + this.height + 2, 0x90000000);
        graphics.fill(x, y, x + this.width, y + this.height, background);
        graphics.fill(x, y, x + this.width, y + 1, border);
        graphics.fill(x, y + this.height - 1, x + this.width, y + this.height, border);
        graphics.fill(x, y, x + 1, y + this.height, border);
        graphics.fill(x + this.width - 1, y, x + this.width, y + this.height, border);
        if (selectedNow) {
            graphics.fill(x + 3, y + 3, x + 6, y + this.height - 3, this.accentColor);
        }

        graphics.drawCenteredString(
                Minecraft.getInstance().font,
                this.getMessage(),
                x + this.width / 2,
                y + (this.height - 8) / 2,
                textColor
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
