package com.hoops.oretracker.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class OreCategoryScreen extends Screen {
    private static final int ACCENT = 0xFFB56BFF;
    private static final int ACCENT_SOFT = 0xAAB56BFF;
    private static final int TEXT_MUTED = 0xFF9A9AA4;

    private final OreTrackerData.Mine mine;
    private final LinkedHashSet<String> selectedCategoryIds = new LinkedHashSet<>();

    private final List<CategoryHitbox> categoryHitboxes = new ArrayList<>();

    private Hitbox startButton = Hitbox.empty();
    private Hitbox backButton = Hitbox.empty();
    private Hitbox allButton = Hitbox.empty();
    private Hitbox noneButton = Hitbox.empty();

    public OreCategoryScreen(OreTrackerData.Mine mine) {
        super(Component.literal("Ore Tracker Categories"));
        this.mine = mine;

        // Intentionally empty by default.
        // User must choose Sword / Tool / Armor / Talisman manually.
    }

    @Override
    protected void init() {
        // Fully custom screen.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        categoryHitboxes.clear();

        graphics.fill(0, 0, this.width, this.height, 0xAA000000);

        int centerX = this.width / 2;

        int panelW = Math.min(520, this.width - 32);
        int panelH = 330;
        int panelX = centerX - panelW / 2;
        int panelY = 28;

        drawShell(graphics, panelX, panelY, panelW, panelH);

        graphics.fill(panelX + 12, panelY + 12, panelX + panelW - 12, panelY + 76, 0xCC101018);
        graphics.fill(panelX + 12, panelY + 12, panelX + panelW - 12, panelY + 13, 0x55FFFFFF);

        graphics.fill(centerX - 26, panelY + 42, centerX - 4, panelY + 64, 0xFF1A1024);
        graphics.renderItem(new ItemStack(mine.icon()), centerX - 23, panelY + 45);

        drawCenteredString(graphics, "Select Categories", centerX, panelY + 22, 0xFFFFFFFF, true);
        drawCenteredString(graphics, mine.mineName() + " / " + mine.shopkeeperName(), centerX, panelY + 34, TEXT_MUTED, false);

        graphics.drawString(
                this.font,
                mine.oreName(),
                centerX + 2,
                panelY + 49,
                ACCENT,
                true
        );

        int utilityY = panelY + 88;
        allButton = new Hitbox(panelX + 22, utilityY, 76, 22);
        noneButton = new Hitbox(panelX + 106, utilityY, 76, 22);

        drawSmallButton(graphics, allButton, "All", allButton.contains(mouseX, mouseY), true);
        drawSmallButton(graphics, noneButton, "None", noneButton.contains(mouseX, mouseY), true);

        String selectedText = selectedCategoryIds.size() + " / " + mine.categories().size() + " selected";
        graphics.drawString(
                this.font,
                selectedText,
                panelX + panelW - 22 - this.font.width(selectedText),
                utilityY + 7,
                0xFFAAAAAA,
                false
        );

        int gridX = panelX + 22;
        int gridY = panelY + 124;
        int gap = 12;
        int tileW = (panelW - 44 - gap) / 2;
        int tileH = 56;

        for (int i = 0; i < mine.categories().size(); i++) {
            OreTrackerData.CategoryCost category = mine.categories().get(i);

            int col = i % 2;
            int row = i / 2;

            int tileX = gridX + col * (tileW + gap);
            int tileY = gridY + row * (tileH + 10);

            Hitbox hitbox = new Hitbox(tileX, tileY, tileW, tileH);
            categoryHitboxes.add(new CategoryHitbox(hitbox, category));

            boolean selected = selectedCategoryIds.contains(category.id());
            boolean hovered = hitbox.contains(mouseX, mouseY);

            drawCategoryTile(graphics, category, hitbox, selected, hovered);
        }

        BigInteger total = getSelectedTotal();

        String totalText = "Tracking total: " + formatOreParts(total);
        totalText = fitText(this.font, totalText, panelW - 44);

        graphics.drawString(this.font, totalText, panelX + 22, panelY + panelH - 58, ACCENT, false);

        backButton = new Hitbox(panelX + 22, panelY + panelH - 34, 92, 24);
        startButton = new Hitbox(panelX + panelW - 146, panelY + panelH - 34, 124, 24);

        drawSmallButton(graphics, backButton, "Back", backButton.contains(mouseX, mouseY), true);
        drawSmallButton(graphics, startButton, "Start Tracking", startButton.contains(mouseX, mouseY), !selectedCategoryIds.isEmpty());
    }

    private void drawShell(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xEE050509);

        graphics.fill(x, y, x + width, y + 2, ACCENT);
        graphics.fill(x, y + 2, x + 2, y + height, ACCENT_SOFT);
        graphics.fill(x + width - 2, y + 2, x + width, y + height, 0x331B1B22);
        graphics.fill(x, y + height - 2, x + width, y + height, 0x331B1B22);
    }

    private void drawCategoryTile(GuiGraphics graphics, OreTrackerData.CategoryCost category, Hitbox box, boolean selected, boolean hovered) {
        int background = selected
                ? hovered ? 0xFF241430 : 0xFF1A1024
                : hovered ? 0xFF181820 : 0xFF101017;

        int border = selected ? ACCENT_SOFT : hovered ? 0x6633333A : 0x3333333A;
        int accent = selected ? ACCENT : 0xFF55555F;

        graphics.fill(box.x, box.y, box.x + box.width, box.y + box.height, background);

        graphics.fill(box.x, box.y, box.x + box.width, box.y + 1, border);
        graphics.fill(box.x, box.y + box.height - 1, box.x + box.width, box.y + box.height, 0x22222228);
        graphics.fill(box.x, box.y, box.x + 2, box.y + box.height, accent);

        String check = selected ? "✓" : " ";
        graphics.fill(box.x + 10, box.y + 16, box.x + 28, box.y + 34, selected ? 0xFF2A1836 : 0xFF050509);
        graphics.drawString(this.font, check, box.x + 15, box.y + 21, selected ? ACCENT : 0xFF555555, true);

        String name = fitText(this.font, category.displayName(), box.width - 48);
        String price = fitText(this.font, formatOreParts(category.required().toBaseValue()), box.width - 48);

        graphics.drawString(this.font, name, box.x + 38, box.y + 13, 0xFFFFFFFF, true);
        graphics.drawString(this.font, price, box.x + 38, box.y + 29, TEXT_MUTED, false);
    }

    private void drawSmallButton(GuiGraphics graphics, Hitbox box, String label, boolean hovered, boolean enabled) {
        int background;

        if (!enabled) {
            background = 0xFF101014;
        } else {
            background = hovered ? 0xFF21172C : 0xFF15151D;
        }

        int textColor = enabled ? hovered ? 0xFFFFFFFF : ACCENT : 0xFF555555;

        graphics.fill(box.x, box.y, box.x + box.width, box.y + box.height, background);
        graphics.fill(box.x, box.y, box.x + box.width, box.y + 1, enabled ? 0x55FFFFFF : 0x22333333);
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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubled);
        }

        double mouseX = event.x();
        double mouseY = event.y();

        if (backButton.contains(mouseX, mouseY)) {
            Minecraft.getInstance().setScreen(new OreTrackerScreen());
            return true;
        }

        if (allButton.contains(mouseX, mouseY)) {
            selectedCategoryIds.clear();

            for (OreTrackerData.CategoryCost category : mine.categories()) {
                selectedCategoryIds.add(category.id());
            }

            return true;
        }

        if (noneButton.contains(mouseX, mouseY)) {
            selectedCategoryIds.clear();
            return true;
        }

        if (startButton.contains(mouseX, mouseY) && !selectedCategoryIds.isEmpty()) {
            OreTrackerState.setSelectedMineWithCategories(mine, selectedCategoryIds);
            Minecraft.getInstance().setScreen(null);
            return true;
        }

        for (CategoryHitbox categoryHitbox : categoryHitboxes) {
            if (categoryHitbox.hitbox.contains(mouseX, mouseY)) {
                String id = categoryHitbox.category.id();

                if (selectedCategoryIds.contains(id)) {
                    selectedCategoryIds.remove(id);
                } else {
                    selectedCategoryIds.add(id);
                }

                return true;
            }
        }

        return super.mouseClicked(event, doubled);
    }

    private BigInteger getSelectedTotal() {
        return mine.selectedCategoryValue(selectedCategoryIds);
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

    private static String formatOreParts(BigInteger value) {
        if (value == null || value.signum() <= 0) {
            return "0";
        }

        BigInteger remaining = value;
        StringBuilder result = new StringBuilder();

        remaining = appendPart(result, remaining, OreValue.HYPER_COMPRESSED_VALUE, "Hyper");
        remaining = appendPart(result, remaining, OreValue.MEGA_COMPRESSED_VALUE, "Mega");
        remaining = appendPart(result, remaining, OreValue.ULTRA_COMPRESSED_VALUE, "Ultra");
        remaining = appendPart(result, remaining, OreValue.SUPER_COMPRESSED_VALUE, "Super");
        remaining = appendPart(result, remaining, OreValue.COMPRESSED_VALUE, "Compressed");
        appendPart(result, remaining, OreValue.NORMAL_VALUE, "Normal");

        return result.length() == 0 ? "0" : result.toString();
    }

    private static BigInteger appendPart(StringBuilder result, BigInteger value, BigInteger unit, String label) {
        BigInteger[] divided = value.divideAndRemainder(unit);
        BigInteger amount = divided[0];

        if (amount.signum() > 0) {
            if (!result.isEmpty()) {
                result.append(" + ");
            }

            result.append(amount).append(" ").append(label);
        }

        return divided[1];
    }

    private record CategoryHitbox(Hitbox hitbox, OreTrackerData.CategoryCost category) {}

    private record Hitbox(int x, int y, int width, int height) {
        static Hitbox empty() {
            return new Hitbox(0, 0, 0, 0);
        }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}