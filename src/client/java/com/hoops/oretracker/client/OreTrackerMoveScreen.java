package com.hoops.oretracker.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class OreTrackerMoveScreen extends Screen {
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    private Hitbox hudBox = Hitbox.empty();

    public OreTrackerMoveScreen() {
        super(Component.literal("Move Ore Tracker HUD"));
    }

    @Override
    protected void init() {
        // Fully custom screen.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0xAA000000);

        int previewW = scaled(248);
        int previewH = scaled(72);

        int x = OreTrackerState.getCustomHudX();
        int y = OreTrackerState.getCustomHudY();

        if (OreTrackerState.getHudPosition() != OreTrackerState.HudPosition.CUSTOM) {
            x = 10;
            y = 10;
            OreTrackerState.setCustomHudPosition(x, y);
        }

        x = Math.max(0, Math.min(this.width - previewW, x));
        y = Math.max(0, Math.min(this.height - previewH, y));

        OreTrackerState.setCustomHudPosition(x, y);

        hudBox = new Hitbox(x, y, previewW, previewH);

        drawPreviewHud(graphics, x, y, previewW, previewH, hudBox.contains(mouseX, mouseY));

        String title = "Move Ore Tracker HUD";
        String line1 = "Drag the preview box to place the HUD.";
        String line2 = "Press ESC when done. Use /otpos top_left to reset to a corner.";

        drawCentered(graphics, title, this.width / 2, this.height - 46, 0xFFFFFFFF, true);
        drawCentered(graphics, line1, this.width / 2, this.height - 32, 0xFFB56BFF, false);
        drawCentered(graphics, line2, this.width / 2, this.height - 20, 0xFF9A9AA4, false);
    }

    private void drawPreviewHud(GuiGraphics graphics, int x, int y, int width, int height, boolean hovered) {
        int accent = hovered || dragging ? 0xFFFFFFFF : 0xFFB56BFF;

        graphics.fill(x, y, x + width, y + height, 0xEF050508);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 32, 0xAA101018);

        graphics.fill(x, y, x + width, y + 2, accent);
        graphics.fill(x, y + 2, x + 2, y + height, 0xAAB56BFF);
        graphics.fill(x + width - 1, y + 2, x + width, y + height, 0x441B1B22);
        graphics.fill(x + 2, y + height - 1, x + width, y + height, 0x441B1B22);

        graphics.drawString(this.font, "Ore Tracker HUD", x + 10, y + 9, 0xFFFFFFFF, true);
        graphics.drawString(this.font, dragging ? "Dragging..." : "Drag me", x + 10, y + 22, 0xFFB56BFF, false);

        int barX = x + 10;
        int barY = y + height - 22;
        int barW = width - 20;

        graphics.fill(barX, barY, barX + barW, barY + 8, 0xFF15151D);
        graphics.fill(barX, barY, barX + (int) (barW * 0.66), barY + 8, 0xFFB56BFF);
    }

    private void drawCentered(GuiGraphics graphics, String text, int centerX, int y, int color, boolean shadow) {
        graphics.drawString(
                this.font,
                text,
                centerX - this.font.width(text) / 2,
                y,
                color,
                shadow
        );
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubled);
        }

        double mouseX = event.x();
        double mouseY = event.y();

        if (hudBox.contains(mouseX, mouseY)) {
            dragging = true;
            dragOffsetX = (int) mouseX - hudBox.x;
            dragOffsetY = (int) mouseY - hudBox.y;
            return true;
        }

        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double offsetX, double offsetY) {
        if (!dragging || event.button() != 0) {
            return super.mouseDragged(event, offsetX, offsetY);
        }

        int previewW = scaled(248);
        int previewH = scaled(72);

        int newX = (int) event.x() - dragOffsetX;
        int newY = (int) event.y() - dragOffsetY;

        newX = Math.max(0, Math.min(this.width - previewW, newX));
        newY = Math.max(0, Math.min(this.height - previewH, newY));

        OreTrackerState.setCustomHudPosition(newX, newY);

        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && dragging) {
            dragging = false;
            return true;
        }

        return super.mouseReleased(event);
    }

    private static int scaled(int value) {
        return Math.max(1, Math.round(value * OreTrackerState.getHudScalePercent() / 100.0f));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Hitbox(int x, int y, int width, int height) {
        static Hitbox empty() {
            return new Hitbox(0, 0, 0, 0);
        }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}


