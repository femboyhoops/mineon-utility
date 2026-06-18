package com.hoops.oretracker.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Desktop;
import java.net.URI;

public final class OreTrackerDiscordPromptScreen extends Screen {
    private final String discordInvite;

    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 220;

    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 24;

    private int panelX;
    private int panelY;

    private int joinButtonX;
    private int joinButtonY;

    private int laterButtonX;
    private int laterButtonY;

    public OreTrackerDiscordPromptScreen(String discordInvite) {
        super(Component.literal("Join Our Discord"));
        this.discordInvite = discordInvite;
    }

    protected void init() {
        this.panelX = (this.width - PANEL_WIDTH) / 2;
        this.panelY = (this.height - PANEL_HEIGHT) / 2;

        int centerX = this.width / 2;

        this.joinButtonX = centerX - BUTTON_WIDTH / 2;
        this.joinButtonY = this.panelY + 145;

        this.laterButtonX = centerX - BUTTON_WIDTH / 2;
        this.laterButtonY = this.panelY + 175;

        this.addRenderableWidget(
                Button.builder(Component.literal("Join Discord"), button -> openDiscord())
                        .bounds(this.joinButtonX, this.joinButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("Maybe Later"), button -> closePrompt())
                        .bounds(this.laterButtonX, this.laterButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );
    }

    private void openDiscord() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(discordInvite));
            }
        } catch (Exception ignored) {
        }

        closePrompt();
    }

    private void closePrompt() {
        Minecraft.getInstance().setScreen(null);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        /*
         * Do NOT call renderBackground().
         * Feather/Essential-style clients can crash with:
         * "Can only blur once per frame"
         */

        this.panelX = (this.width - PANEL_WIDTH) / 2;
        this.panelY = (this.height - PANEL_HEIGHT) / 2;

        int centerX = this.width / 2;

        // Fullscreen dark purple overlay.
        graphics.fill(0, 0, this.width, this.height, 0xD00B0613);

        // Big soft shadow.
        graphics.fill(
                this.panelX + 6,
                this.panelY + 6,
                this.panelX + PANEL_WIDTH + 6,
                this.panelY + PANEL_HEIGHT + 6,
                0x85000000
        );

        // Outer panel.
        graphics.fill(
                this.panelX,
                this.panelY,
                this.panelX + PANEL_WIDTH,
                this.panelY + PANEL_HEIGHT,
                0xF0140924
        );

        // Inner panel.
        graphics.fill(
                this.panelX + 5,
                this.panelY + 5,
                this.panelX + PANEL_WIDTH - 5,
                this.panelY + PANEL_HEIGHT - 5,
                0xF0201238
        );

        // Top glow strip.
        graphics.fill(
                this.panelX + 2,
                this.panelY + 2,
                this.panelX + PANEL_WIDTH - 2,
                this.panelY + 5,
                0xFFB84DFF
        );

        // Border.
        drawBorder(graphics, this.panelX, this.panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF9D4EDD);
        drawBorder(graphics, this.panelX + 3, this.panelY + 3, PANEL_WIDTH - 6, PANEL_HEIGHT - 6, 0xFF3C1361);

        // Small decorative accent line.
        graphics.fill(
                this.panelX + 34,
                this.panelY + 48,
                this.panelX + PANEL_WIDTH - 34,
                this.panelY + 50,
                0xFF7C3AED
        );

        // Header.
        graphics.drawCenteredString(
                this.font,
                Component.literal("Ore Tracker"),
                centerX,
                this.panelY + 18,
                0xFFE9D5FF
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal("Enjoying the mod?"),
                centerX,
                this.panelY + 65,
                0xFFFFFFFF
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal("Join the discord for updates!"),
                centerX,
                this.panelY + 82,
                0xFFD8B4FE
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal(discordInvite),
                centerX,
                this.panelY + 108,
                0xFFBDA5D8
        );

        // Render actual clickable Minecraft buttons first.
        super.render(graphics, mouseX, mouseY, partialTick);

        /*
         * Draw purple visuals over the vanilla buttons.
         * The real Button widgets underneath still handle clicking.
         */
        drawThemedButton(
                graphics,
                mouseX,
                mouseY,
                this.joinButtonX,
                this.joinButtonY,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                "Join Discord",
                true
        );

        drawThemedButton(
                graphics,
                mouseX,
                mouseY,
                this.laterButtonX,
                this.laterButtonY,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                "Maybe Later",
                false
        );
    }

    private void drawThemedButton(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int height,
            String label,
            boolean primary
    ) {
        boolean hovered = isInside(mouseX, mouseY, x, y, width, height);

        int backgroundColor;
        int topColor;
        int borderColor;
        int textColor;

        if (primary) {
            backgroundColor = hovered ? 0xFF9D4EDD : 0xFF7B2CBF;
            topColor = hovered ? 0xFFC77DFF : 0xFF9D4EDD;
            borderColor = hovered ? 0xFFE9D5FF : 0xFFC77DFF;
            textColor = 0xFFFFFFFF;
        } else {
            backgroundColor = hovered ? 0xFF3B245C : 0xFF241333;
            topColor = hovered ? 0xFF5B3486 : 0xFF38204F;
            borderColor = hovered ? 0xFFC77DFF : 0xFF6D4C91;
            textColor = 0xFFD8B4FE;
        }

        // Shadow.
        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x80000000);

        // Main fill.
        graphics.fill(x, y, x + width, y + height, backgroundColor);

        // Top highlight.
        graphics.fill(x + 2, y + 2, x + width - 2, y + 7, topColor);

        // Border.
        drawBorder(graphics, x, y, width, height, borderColor);

        graphics.drawCenteredString(
                this.font,
                Component.literal(label),
                x + width / 2,
                y + 8,
                textColor
        );
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x
                && mouseX <= x + width
                && mouseY >= y
                && mouseY <= y + height;
    }
}