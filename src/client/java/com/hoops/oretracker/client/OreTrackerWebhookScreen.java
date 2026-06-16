package com.hoops.oretracker.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class OreTrackerWebhookScreen extends Screen {
    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 246;

    private EditBox webhookBox;
    private Hitbox startButton;
    private Hitbox cancelButton;

    public OreTrackerWebhookScreen() {
        super(Component.literal("Ore Tracker Death Tracking"));
    }

    @Override
    protected void init() {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        webhookBox = new EditBox(
                this.font,
                panelX + 32,
                panelY + 103,
                PANEL_WIDTH - 64,
                24,
                Component.literal("Discord Webhook")
        );

        webhookBox.setMaxLength(512);
        webhookBox.setValue(OreTrackerSavedSettings.getDiscordWebhook());
        webhookBox.setHint(Component.literal("https://discord.com/api/webhooks/..."));

        this.addRenderableWidget(webhookBox);

        startButton = new Hitbox(panelX + 32, panelY + 165, 186, 34);
        cancelButton = new Hitbox(panelX + PANEL_WIDTH - 218, panelY + 165, 186, 34);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0xD9040208);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        drawPanel(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.drawString(this.font, "ORE TRACKER", panelX + 32, panelY + 24, 0xFFF8F2FF, false);
        graphics.drawString(this.font, "Death Webhook Tracking", panelX + 32, panelY + 40, 0xFFC7A7FF, false);

        drawStatusPill(graphics, panelX + PANEL_WIDTH - 143, panelY + 25);

        graphics.drawString(this.font, "Paste your Discord webhook below.", panelX + 32, panelY + 70, 0xFFE6DBFF, false);
        graphics.drawString(this.font, "The webhook is saved locally and reused next time.", panelX + 32, panelY + 83, 0xFF9F91B8, false);

        graphics.drawString(this.font, "Webhook URL", panelX + 32, panelY + 92, 0xFFEDE3FF, false);

        drawButton(graphics, startButton, OreTrackerDeathTracker.isTracking() ? "Restart Tracking" : "Start Tracking", startButton.contains(mouseX, mouseY), true, true);
        drawButton(graphics, cancelButton, "Cancel", cancelButton.contains(mouseX, mouseY), true, false);

        graphics.drawString(this.font, "Sends start time, death time, killer, and HC/MC/UC/SC/C/N inventory counts.", panelX + 32, panelY + 214, 0xFF9E90B8, false);

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        if (startButton != null && startButton.contains(mouseX, mouseY)) {
            String webhook = webhookBox.getValue().trim();

            if (!isValidWebhook(webhook)) {
                sendClientMessage("Paste a valid Discord webhook first.");
                return true;
            }

            OreTrackerDeathTracker.start(webhook);
            Minecraft.getInstance().setScreen(null);
            return true;
        }

        if (cancelButton != null && cancelButton.contains(mouseX, mouseY)) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }

        return super.mouseClicked(event, doubled);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x - 3, y - 3, x + width + 3, y + height + 3, 0xFF5F32B6);
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0xFFB68CFF);
        graphics.fill(x, y, x + width, y + height, 0xFF0D0814);

        graphics.fill(x, y, x + width, y + 4, 0xFFA855F7);
        graphics.fill(x, y + 4, x + width, y + 44, 0xFF130B20);

        graphics.fill(x + 24, y + 58, x + width - 24, y + 59, 0xFF372252);
        graphics.fill(x + 24, y + 145, x + width - 24, y + 146, 0xFF2B1A3F);
    }

    private void drawStatusPill(GuiGraphics graphics, int x, int y) {
        boolean active = OreTrackerDeathTracker.isTracking();

        String label = active ? "TRACKING" : "INACTIVE";
        int bg = active ? 0xFF301D46 : 0xFF191120;
        int border = active ? 0xFFA855F7 : 0xFF5B4A70;
        int text = active ? 0xFFEEDBFF : 0xFFB8A9C9;

        graphics.fill(x, y, x + 111, y + 20, border);
        graphics.fill(x + 1, y + 1, x + 110, y + 19, bg);

        graphics.drawString(this.font, label, x + (111 - this.font.width(label)) / 2, y + 6, text, false);
    }

    private void drawButton(GuiGraphics graphics, Hitbox box, String label, boolean hovered, boolean enabled, boolean primary) {
        int bg;

        if (!enabled) {
            bg = 0xFF17111F;
        } else if (primary) {
            bg = hovered ? 0xFFA855F7 : 0xFF7C3AED;
        } else {
            bg = hovered ? 0xFF35214C : 0xFF21172E;
        }

        int border = hovered ? 0xFFE4CCFF : primary ? 0xFFC4A0FF : 0xFF5D4A73;
        int text = enabled ? 0xFFFFFFFF : 0xFF7D718F;

        graphics.fill(box.x, box.y, box.x + box.width, box.y + box.height, border);
        graphics.fill(box.x + 1, box.y + 1, box.x + box.width - 1, box.y + box.height - 1, bg);

        int textX = box.x + (box.width - this.font.width(label)) / 2;
        int textY = box.y + 12;

        graphics.drawString(this.font, label, textX, textY, text, false);
    }

    private boolean isValidWebhook(String webhook) {
        return webhook != null
                && (webhook.startsWith("https://discord.com/api/webhooks/")
                || webhook.startsWith("https://discordapp.com/api/webhooks/"));
    }

    private void sendClientMessage(String message) {
        Minecraft client = Minecraft.getInstance();

        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(message), false);
        }
    }

    private static final class Hitbox {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private Hitbox(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}