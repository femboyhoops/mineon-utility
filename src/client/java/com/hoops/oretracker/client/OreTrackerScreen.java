package com.hoops.oretracker.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class OreTrackerScreen extends Screen {
    private static final int ACCENT = 0xFFB56BFF;
    private static final int ACCENT_SOFT = 0xAAB56BFF;
    private static final int ACCENT_DARK = 0xFF1A1024;
    private static final int TEXT_MUTED = 0xFF9A9AA4;

    private int worldIndex = 0;

    private Hitbox previousWorldButton = Hitbox.empty();
    private Hitbox nextWorldButton = Hitbox.empty();
    private final List<MineHitbox> mineHitboxes = new ArrayList<>();

    public OreTrackerScreen() {
        super(Component.literal("Ore Tracker"));
    }

    @Override
    protected void init() {
        // Fully custom screen.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        List<OreTrackerData.MineWorld> worlds = OreTrackerData.worlds();

        graphics.fill(0, 0, this.width, this.height, 0xAA000000);

        if (worlds.isEmpty()) {
            drawCenteredString(graphics, "Ore Tracker config failed to load.", this.width / 2, 28, 0xFFFF5555, true);
            return;
        }

        if (worldIndex >= worlds.size()) {
            worldIndex = 0;
        }

        OreTrackerData.MineWorld world = worlds.get(worldIndex);

        mineHitboxes.clear();

        int centerX = this.width / 2;

        int panelW = Math.min(560, this.width - 32);
        int panelH = 384;
        int panelX = centerX - panelW / 2;
        int panelY = 18;

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xEE050509);

        graphics.fill(panelX, panelY, panelX + panelW, panelY + 2, ACCENT);
        graphics.fill(panelX, panelY + 2, panelX + 2, panelY + panelH, ACCENT_SOFT);
        graphics.fill(panelX + panelW - 2, panelY + 2, panelX + panelW, panelY + panelH, 0x331B1B22);
        graphics.fill(panelX, panelY + panelH - 2, panelX + panelW, panelY + panelH, 0x331B1B22);

        graphics.fill(panelX + 12, panelY + 12, panelX + panelW - 12, panelY + 82, 0xCC101018);
        graphics.fill(panelX + 12, panelY + 12, panelX + panelW - 12, panelY + 13, 0x55FFFFFF);

        graphics.fill(centerX - 26, panelY + 42, centerX - 4, panelY + 64, ACCENT_DARK);
        graphics.renderItem(new ItemStack(world.icon()), centerX - 23, panelY + 45);

        String title = "Ore Tracker";
        String subtitle = "Select a mine to track shop completion";

        drawCenteredString(graphics, title, centerX, panelY + 22, 0xFFFFFFFF, true);
        drawCenteredString(graphics, subtitle, centerX, panelY + 32, TEXT_MUTED, false);

        String worldText = toTitleCase(world.displayName());
        graphics.drawString(
                this.font,
                worldText,
                centerX + 2,
                panelY + 49,
                ACCENT,
                true
        );

        int arrowY = panelY + 48;
        previousWorldButton = new Hitbox(centerX - 156, arrowY - 5, 42, 26);
        nextWorldButton = new Hitbox(centerX + 114, arrowY - 5, 42, 26);

        drawArrowButton(graphics, previousWorldButton, "<", previousWorldButton.contains(mouseX, mouseY));
        drawArrowButton(graphics, nextWorldButton, ">", nextWorldButton.contains(mouseX, mouseY));

        String countText = (worldIndex + 1) + " / " + worlds.size();
        int pillW = this.font.width(countText) + 18;
        graphics.fill(centerX - pillW / 2, panelY + 68, centerX + pillW / 2, panelY + 84, 0xFF171720);
        drawCenteredString(graphics, countText, centerX, panelY + 72, 0xFFAAAAAA, false);

        int gridX = panelX + 22;
        int gridY = panelY + 104;
        int gap = 12;
        int tileW = (panelW - 44 - gap) / 2;
        int tileH = 48;

        for (int i = 0; i < world.mines().size(); i++) {
            OreTrackerData.Mine mine = world.mines().get(i);

            int col = i % 2;
            int row = i / 2;

            int tileX = gridX + col * (tileW + gap);
            int tileY = gridY + row * (tileH + 10);

            Hitbox hitbox = new Hitbox(tileX, tileY, tileW, tileH);
            mineHitboxes.add(new MineHitbox(hitbox, mine));

            drawMineTile(graphics, mine, hitbox, hitbox.contains(mouseX, mouseY));
        }

        String bugLine = "Contact 1hoopss. on discord to report bugs or mismatched prices.";
        String commandLine = "Useful Commands: /ot, /oretracker, /otclear";

        drawCenteredString(graphics, bugLine, centerX, panelY + panelH - 28, 0xFF8D8D98, false);
        drawCenteredString(graphics, commandLine, centerX, panelY + panelH - 16, 0xFF777782, false);
    }

    private void drawMineTile(GuiGraphics graphics, OreTrackerData.Mine mine, Hitbox box, boolean hovered) {
        int background = hovered ? 0xFF181820 : 0xFF101017;
        int border = hovered ? ACCENT_SOFT : 0x3333333A;
        int accent = mine.hasCostData() ? ACCENT : 0xFFFF5555;

        graphics.fill(box.x, box.y, box.x + box.width, box.y + box.height, background);

        graphics.fill(box.x, box.y, box.x + box.width, box.y + 1, border);
        graphics.fill(box.x, box.y + box.height - 1, box.x + box.width, box.y + box.height, 0x22222228);
        graphics.fill(box.x, box.y, box.x + 2, box.y + box.height, accent);

        graphics.fill(box.x + 10, box.y + 13, box.x + 32, box.y + 35, 0xFF050509);
        graphics.renderItem(new ItemStack(mine.icon()), box.x + 13, box.y + 16);

        String shop = fitText(this.font, mine.shopkeeperName(), box.width - 50);
        String meta = fitText(this.font, mine.mineName() + " / " + mine.oreName(), box.width - 50);

        graphics.drawString(this.font, shop, box.x + 42, box.y + 11, 0xFFFFFFFF, true);
        graphics.drawString(this.font, meta, box.x + 42, box.y + 25, TEXT_MUTED, false);
    }

    private void drawArrowButton(GuiGraphics graphics, Hitbox box, String label, boolean hovered) {
        int background = hovered ? 0xFF21172C : 0xFF13131A;
        int textColor = hovered ? 0xFFFFFFFF : ACCENT;

        graphics.fill(box.x, box.y, box.x + box.width, box.y + box.height, background);
        graphics.fill(box.x, box.y, box.x + box.width, box.y + 1, 0x55FFFFFF);
        graphics.fill(box.x, box.y + box.height - 1, box.x + box.width, box.y + box.height, 0x33111111);

        drawCenteredString(graphics, label, box.x + box.width / 2, box.y + 8, textColor, true);
    }

    private void drawCenteredString(GuiGraphics graphics, String text, int centerX, int y, int color, boolean shadow) {
        graphics.drawString(
                this.font,
                text,
                centerX - this.font.width(text) / 2,
                y,
                color,
                shadow
        );
    }

    private static String toTitleCase(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String trimmed = input.trim();

        if (trimmed.length() == 1) {
            return trimmed.toUpperCase();
        }

        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }

    private static String fitText(Font font, String text, int maxWidth) {
        if (text == null) {
            return "";
        }

        if (font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";

        for (int i = text.length(); i >= 0; i--) {
            String shortened = text.substring(0, i).trim() + ellipsis;

            if (font.width(shortened) <= maxWidth) {
                return shortened;
            }
        }

        return ellipsis;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubled);
        }

        double mouseX = event.x();
        double mouseY = event.y();

        List<OreTrackerData.MineWorld> worlds = OreTrackerData.worlds();

        if (worlds.isEmpty()) {
            return true;
        }

        if (previousWorldButton.contains(mouseX, mouseY)) {
            worldIndex--;

            if (worldIndex < 0) {
                worldIndex = worlds.size() - 1;
            }

            return true;
        }

        if (nextWorldButton.contains(mouseX, mouseY)) {
            worldIndex++;

            if (worldIndex >= worlds.size()) {
                worldIndex = 0;
            }

            return true;
        }

        for (MineHitbox mineHitbox : mineHitboxes) {
            if (mineHitbox.hitbox.contains(mouseX, mouseY)) {
                OreTrackerData.Mine mine = mineHitbox.mine;

                if (mine.hasCategories()) {
                    Minecraft.getInstance().setScreen(new OreCategoryScreen(mine));
                } else {
                    OreTrackerState.setSelectedMine(mine);
                    Minecraft.getInstance().setScreen(null);
                }

                return true;
            }
        }

        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record MineHitbox(Hitbox hitbox, OreTrackerData.Mine mine) {}

    private record Hitbox(int x, int y, int width, int height) {
        static Hitbox empty() {
            return new Hitbox(0, 0, 0, 0);
        }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}


