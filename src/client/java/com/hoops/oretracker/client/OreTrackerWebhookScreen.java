package com.hoops.oretracker.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class OreTrackerWebhookScreen extends Screen {
    private static final int BACKDROP = 0xD0100718;
    private static final int PANEL_BG = 0xF0140A22;
    private static final int PANEL_BORDER = 0xFF7C3AED;
    private static final int PANEL_BORDER_DARK = 0xFF3B1A68;
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_MUTED = 0xFFC9B8FF;
    private static final int TEXT_DIM = 0xFF9D8BC7;
    private static final int STATUS_OK = 0xFFA7F3D0;
    private static final int STATUS_ERROR = 0xFFFCA5A5;

    private EditBox webhookBox;
    private EditBox discordUserIdBox;
    private String status = "";
    private int statusColor = STATUS_OK;

    public OreTrackerWebhookScreen() {
        super(Component.literal("Ore Tracker Discord Alerts"));
    }

    @Override
    protected void init() {
        int boxWidth = Math.min(430, this.width - 40);
        int x = (this.width - boxWidth) / 2;
        int y = Math.max(42, this.height / 2 - 92);

        this.webhookBox = new EditBox(
                this.font,
                x + 12,
                y + 52,
                boxWidth - 24,
                20,
                Component.literal("Discord webhook URL")
        );
        this.webhookBox.setMaxLength(2048);
        this.webhookBox.setValue(nullToEmpty(OreTrackerSavedSettings.getDiscordWebhook()));
        this.webhookBox.setHint(Component.literal("Discord webhook URL"));
        this.webhookBox.setTextColor(0xFFFFFFFF);
        this.webhookBox.setTextColorUneditable(0xFF9CA3AF);
        this.addRenderableWidget(this.webhookBox);

        this.discordUserIdBox = new EditBox(
                this.font,
                x + 12,
                y + 106,
                boxWidth - 24,
                20,
                Component.literal("Discord user ID optional")
        );
        this.discordUserIdBox.setMaxLength(20);
        this.discordUserIdBox.setValue(getSavedDiscordUserIdSafely());
        this.discordUserIdBox.setHint(Component.literal("Optional Discord user ID for pings"));
        this.discordUserIdBox.setTextColor(0xFFFFFFFF);
        this.discordUserIdBox.setTextColorUneditable(0xFF9CA3AF);
        this.addRenderableWidget(this.discordUserIdBox);

        this.addRenderableWidget(Button.builder(Component.literal("Start Tracking"), button -> startTracking())
                .bounds(x + 12, y + 150, boxWidth - 24, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Stop Tracking"), button -> {
                    OreTrackerDeathTracker.stop(true);
                    setStatus("Tracking stopped.", false);
                })
                .bounds(x + 12, y + 176, (boxWidth - 32) / 2, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(x + 20 + (boxWidth - 32) / 2, y + 176, (boxWidth - 32) / 2, 20)
                .build());
    }

    private void startTracking() {
        String webhook = nullToEmpty(this.webhookBox.getValue()).trim();
        String rawDiscordUserId = nullToEmpty(this.discordUserIdBox.getValue()).trim();
        String discordUserId = cleanDiscordUserId(rawDiscordUserId);

        if (webhook.isBlank()) {
            setStatus("Paste a Discord webhook URL first.", true);
            return;
        }

        if (!rawDiscordUserId.isBlank() && discordUserId.isBlank()) {
            setStatus("Discord user ID must be numeric and 17-20 digits, or blank.", true);
            return;
        }

        OreTrackerDeathTracker.start(webhook, discordUserId);
        setStatus(discordUserId.isBlank()
                ? "Tracking started. Pings are disabled."
                : "Tracking started. Discord pings are enabled.", false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Do not call renderBackground() here on Feather/1.21.11.
        // Feather can already apply blur in the same frame, and calling Minecraft's
        // blurred background again can crash with "Can only blur once per frame".
        graphics.fill(0, 0, this.width, this.height, BACKDROP);

        int boxWidth = Math.min(430, this.width - 40);
        int x = (this.width - boxWidth) / 2;
        int y = Math.max(42, this.height / 2 - 92);
        int panelHeight = 218;

        drawPanel(graphics, x, y, boxWidth, panelHeight);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, y + 14, TEXT_PRIMARY);
        graphics.drawCenteredString(this.font, "Webhook alerts, optional Discord pings, damage warnings, and death chat context.", this.width / 2, y + 28, TEXT_DIM);

        graphics.drawString(this.font, "Discord Webhook", x + 12, y + 42, TEXT_MUTED, false);
        graphics.drawString(this.font, "Discord User ID (optional)", x + 12, y + 96, TEXT_MUTED, false);
        graphics.drawString(this.font, "Leave blank if you do not want <@user> pings.", x + 12, y + 130, TEXT_DIM, false);

        if (this.status != null && !this.status.isBlank()) {
            graphics.drawCenteredString(this.font, this.status, this.width / 2, y + 202, this.statusColor);
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        Minecraft client = Minecraft.getInstance();
        client.setScreen(null);
    }

    private void setStatus(String value, boolean error) {
        this.status = value == null ? "" : value;
        this.statusColor = error ? STATUS_ERROR : STATUS_OK;
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0x88000000);
        graphics.fill(x, y, x + width, y + height, PANEL_BORDER_DARK);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, PANEL_BG);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 3, PANEL_BORDER);
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xAA000000);
    }

    private static String getSavedDiscordUserIdSafely() {
        try {
            return OreTrackerDeathTracker.getSavedDiscordUserIdForUi();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String cleanDiscordUserId(String input) {
        if (input == null) {
            return "";
        }

        String cleaned = input.trim().replaceAll("[^0-9]", "");

        if (cleaned.length() < 17 || cleaned.length() > 20) {
            return "";
        }

        return cleaned;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
