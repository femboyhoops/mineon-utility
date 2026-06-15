package com.hoops.oretracker.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class OreTrackerHud {
    private static final int DETAILED_MIN_WIDTH = 258;
    private static final int DETAILED_MAX_WIDTH = 440;
    private static final int COMPACT_MIN_WIDTH = 208;
    private static final int COMPACT_MAX_WIDTH = 340;
    private static final int MINIMAL_MIN_WIDTH = 158;
    private static final int MINIMAL_MAX_WIDTH = 260;

    private static final int MARGIN = 10;

    private static final int PURPLE = 0xFFB56BFF;
    private static final int PURPLE_SOFT = 0xAAB56BFF;
    private static final int PURPLE_DARK = 0xFF1A1024;
    private static final int READY = 0xFF55FF99;
    private static final int READY_SOFT = 0xAA55FF99;
    private static final int READY_DARK = 0xFF0D2418;
    private static final int TEXT_MUTED = 0xFF85858F;

    private OreTrackerHud() {}

    public static void register() {
        HudRenderCallback.EVENT.register(OreTrackerHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || !OreTrackerState.isHudVisible()) {
            return;
        }

        OreTrackerData.Mine mine = OreTrackerState.getSelectedMine();

        if (mine == null) {
            return;
        }

        HudData data = createHudData(client, mine);

        switch (OreTrackerState.getHudMode()) {
            case MINIMAL -> renderMinimal(graphics, client, mine, data);
            case COMPACT -> renderCompact(graphics, client, mine, data);
            case DETAILED -> renderDetailed(graphics, client, mine, data);
        }
    }

    private static HudData createHudData(Minecraft client, OreTrackerData.Mine mine) {
        BigInteger current = scanInventoryForOreValue(client, mine.oreName());
        BigInteger required = OreTrackerState.getSelectedRequiredValue();
        BigInteger remaining = required.subtract(current).max(BigInteger.ZERO);

        boolean ready = required.signum() > 0 && current.compareTo(required) >= 0;

        return new HudData(
                current,
                required,
                remaining,
                getPercentText(current, required),
                formatOreParts(current),
                formatOreParts(required),
                formatOreParts(remaining),
                OreTrackerState.getSelectedCategoryLabel(),
                ready
        );
    }

    private static void renderDetailed(GuiGraphics graphics, Minecraft client, OreTrackerData.Mine mine, HudData data) {
        int width = measureDetailed(client.font, mine, data).width;

        List<String> haveLines = wrapOreText(client.font, data.haveText, width - scaled(78));
        List<String> needLines = wrapOreText(client.font, data.needText, width - scaled(78));
        List<String> leftLines = wrapOreText(client.font, data.leftText, width - scaled(78));

        int rowHeight = getRowHeight(haveLines) + getRowHeight(needLines) + getRowHeight(leftLines);
        int height = scaled(88) + rowHeight;

        Position position = getPosition(width, height, client);

        int x = position.x;
        int y = position.y;

        Accent accent = getAccent(data);

        drawCard(graphics, x, y, width, height, accent.main, accent.soft, scaled(48));

        int iconBoxX = x + scaled(9);
        int iconBoxY = y + scaled(8);
        int iconBoxSize = scaled(24);

        drawIconBox(graphics, mine, iconBoxX, iconBoxY, iconBoxSize);

        String badgeText = data.ready ? "READY" : data.percentText;
        int badgeWidth = client.font.width(badgeText) + scaled(16);
        int badgeX = x + width - badgeWidth - scaled(8);
        int badgeY = y + scaled(8);

        drawBadge(graphics, client.font, badgeText, badgeX, badgeY, badgeWidth, data.ready, accent);

        int titleMaxWidth = width - scaled(52) - badgeWidth;

        String title = fitText(client.font, mine.shopkeeperName(), titleMaxWidth);
        String subtitle = fitText(client.font, mine.mineName() + " / " + mine.oreName(), width - scaled(46));
        String category = fitText(client.font, data.categoryLabel, width - scaled(46));

        graphics.drawString(client.font, title, x + scaled(38), y + scaled(8), 0xFFFFFFFF, true);
        graphics.drawString(client.font, subtitle, x + scaled(38), y + scaled(19), 0xFF9A9AA4, false);
        graphics.drawString(client.font, category, x + scaled(38), y + scaled(30), accent.main, false);

        double barPercent = getBarPercent(data.current, data.required);

        int barX = x + scaled(10);
        int barY = y + scaled(62);
        int barWidth = width - scaled(20);
        int barHeight = scaled(9);
        int filledWidth = (int) Math.round(barWidth * barPercent);

        graphics.drawString(
                client.font,
                data.ready ? "Complete" : progressStateLabel(barPercent),
                x + scaled(10),
                y + scaled(52),
                TEXT_MUTED,
                false
        );

        drawProgressBar(graphics, barX, barY, barWidth, barHeight, filledWidth, accent.main);
        drawMilestones(graphics, barX, barY, barWidth, barHeight);

        int rowY = y + scaled(78);
        rowY = drawWrappedValueRow(graphics, client.font, "Have", haveLines, x + scaled(10), rowY, 0xFFDDDDFF);
        rowY = drawWrappedValueRow(graphics, client.font, "Need", needLines, x + scaled(10), rowY, 0xFFE8C8FF);
        drawWrappedValueRow(graphics, client.font, "Left", leftLines, x + scaled(10), rowY, data.ready ? READY : 0xFFFF88CC);
    }

    private static void renderCompact(GuiGraphics graphics, Minecraft client, OreTrackerData.Mine mine, HudData data) {
        Layout layout = measureCompact(client.font, mine, data);
        Position position = getPosition(layout.width, layout.height, client);

        int x = position.x;
        int y = position.y;
        int width = layout.width;
        int height = layout.height;

        Accent accent = getAccent(data);

        drawCard(graphics, x, y, width, height, accent.main, accent.soft, scaled(30));

        int iconBoxX = x + scaled(7);
        int iconBoxY = y + scaled(7);
        int iconBoxSize = scaled(22);

        drawIconBox(graphics, mine, iconBoxX, iconBoxY, iconBoxSize);

        String badgeText = data.ready ? "READY" : data.percentText;
        int badgeWidth = client.font.width(badgeText) + scaled(14);
        int badgeX = x + width - badgeWidth - scaled(8);
        int badgeY = y + scaled(7);

        drawBadge(graphics, client.font, badgeText, badgeX, badgeY, badgeWidth, data.ready, accent);

        String title = fitText(client.font, mine.shopkeeperName(), width - scaled(40) - badgeWidth);
        String subtitle = fitText(client.font, data.categoryLabel, width - scaled(38));
        String bottom = data.ready ? "Ready to buy" : "Left: " + data.leftText;

        graphics.drawString(client.font, title, x + scaled(30), y + scaled(7), 0xFFFFFFFF, true);
        graphics.drawString(client.font, subtitle, x + scaled(30), y + scaled(18), accent.main, false);

        int barX = x + scaled(8);
        int barY = y + scaled(36);
        int barWidth = width - scaled(16);
        int barHeight = scaled(7);
        int filledWidth = (int) Math.round(barWidth * getBarPercent(data.current, data.required));

        drawProgressBar(graphics, barX, barY, barWidth, barHeight, filledWidth, accent.main);

        graphics.drawString(
                client.font,
                fitText(client.font, bottom, width - scaled(16)),
                x + scaled(8),
                y + scaled(48),
                data.ready ? READY : 0xFFDDDDFF,
                false
        );
    }

    private static void renderMinimal(GuiGraphics graphics, Minecraft client, OreTrackerData.Mine mine, HudData data) {
        Layout layout = measureMinimal(client.font, mine, data);
        Position position = getPosition(layout.width, layout.height, client);

        int x = position.x;
        int y = position.y;
        int width = layout.width;
        int height = layout.height;

        Accent accent = getAccent(data);

        drawCard(graphics, x, y, width, height, accent.main, accent.soft, scaled(24));

        graphics.renderItem(new ItemStack(mine.icon()), x + scaled(7), y + scaled(7));

        String badgeText = data.ready ? "READY" : data.percentText;
        String title = fitText(client.font, mine.oreName(), width - scaled(72));

        graphics.drawString(client.font, title, x + scaled(28), y + scaled(7), 0xFFFFFFFF, true);
        graphics.drawString(client.font, badgeText, x + width - scaled(8) - client.font.width(badgeText), y + scaled(7), accent.main, true);

        int barX = x + scaled(8);
        int barY = y + scaled(27);
        int barWidth = width - scaled(16);
        int barHeight = scaled(6);
        int filledWidth = (int) Math.round(barWidth * getBarPercent(data.current, data.required));

        drawProgressBar(graphics, barX, barY, barWidth, barHeight, filledWidth, accent.main);
    }

    private static Layout measureDetailed(Font font, OreTrackerData.Mine mine, HudData data) {
        int minWidth = scaled(DETAILED_MIN_WIDTH);
        int maxWidth = scaled(DETAILED_MAX_WIDTH);
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();

        int desired = minWidth;

        desired = Math.max(desired, font.width(mine.shopkeeperName()) + font.width(data.percentText) + scaled(84));
        desired = Math.max(desired, font.width(mine.mineName() + " / " + mine.oreName()) + scaled(58));
        desired = Math.max(desired, font.width(data.categoryLabel) + scaled(58));
        desired = Math.max(desired, font.width("Have") + font.width(data.haveText) + scaled(84));
        desired = Math.max(desired, font.width("Need") + font.width(data.needText) + scaled(84));
        desired = Math.max(desired, font.width("Left") + font.width(data.leftText) + scaled(84));

        int maxAvailable = Math.max(minWidth, Math.min(maxWidth, screenWidth - scaled(MARGIN * 2)));
        int width = Math.min(Math.max(minWidth, desired), maxAvailable);

        return new Layout(width, scaled(100));
    }

    private static Layout measureCompact(Font font, OreTrackerData.Mine mine, HudData data) {
        int minWidth = scaled(COMPACT_MIN_WIDTH);
        int maxWidth = scaled(COMPACT_MAX_WIDTH);
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();

        String badgeText = data.ready ? "READY" : data.percentText;
        String bottom = data.ready ? "Ready to buy" : "Left: " + data.leftText;

        int desired = minWidth;
        desired = Math.max(desired, font.width(mine.shopkeeperName()) + font.width(badgeText) + scaled(62));
        desired = Math.max(desired, font.width(data.categoryLabel) + scaled(46));
        desired = Math.max(desired, font.width(bottom) + scaled(26));

        int maxAvailable = Math.max(minWidth, Math.min(maxWidth, screenWidth - scaled(MARGIN * 2)));
        int width = Math.min(Math.max(minWidth, desired), maxAvailable);

        return new Layout(width, scaled(62));
    }

    private static Layout measureMinimal(Font font, OreTrackerData.Mine mine, HudData data) {
        int minWidth = scaled(MINIMAL_MIN_WIDTH);
        int maxWidth = scaled(MINIMAL_MAX_WIDTH);
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();

        String badgeText = data.ready ? "READY" : data.percentText;

        int desired = minWidth;
        desired = Math.max(desired, font.width(mine.oreName()) + font.width(badgeText) + scaled(78));

        int maxAvailable = Math.max(minWidth, Math.min(maxWidth, screenWidth - scaled(MARGIN * 2)));
        int width = Math.min(Math.max(minWidth, desired), maxAvailable);

        return new Layout(width, scaled(40));
    }

    private static void drawCard(GuiGraphics graphics, int x, int y, int width, int height, int accent, int accentSoft, int headerHeight) {
        graphics.fill(x, y, x + width, y + height, withOpacity(0xEF050508));
        graphics.fill(x + scaled(2), y + scaled(2), x + width - scaled(2), y + headerHeight, withOpacity(0xAA101018));

        graphics.fill(x, y, x + width, y + scaled(2), withOpacity(accent));
        graphics.fill(x, y + scaled(2), x + scaled(2), y + height, withOpacity(accentSoft));

        graphics.fill(x + width - scaled(1), y + scaled(2), x + width, y + height, withOpacity(0x441B1B22));
        graphics.fill(x + scaled(2), y + height - scaled(1), x + width, y + height, withOpacity(0x441B1B22));

        graphics.fill(x + scaled(2), y + headerHeight, x + width - scaled(2), y + headerHeight + scaled(1), withOpacity(0x3333333A));
        graphics.fill(x + scaled(8), y + height - scaled(8), x + scaled(30), y + height - scaled(6), withOpacity(accentSoft));
    }

    private static void drawBadge(GuiGraphics graphics, Font font, String text, int x, int y, int width, boolean ready, Accent accent) {
        graphics.fill(x, y, x + width, y + scaled(16), withOpacity(ready ? READY_DARK : PURPLE_DARK));
        graphics.fill(x, y, x + width, y + scaled(1), withOpacity(ready ? 0x6655FF99 : 0x66B56BFF));
        graphics.drawString(font, text, x + scaled(8), y + scaled(5), accent.main, true);
    }

    private static void drawIconBox(GuiGraphics graphics, OreTrackerData.Mine mine, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, withOpacity(0xFF111119));
        graphics.fill(x, y, x + size, y + scaled(1), withOpacity(0x33FFFFFF));

        graphics.renderItem(
                new ItemStack(mine.icon()),
                x + size / 2 - 8,
                y + size / 2 - 8
        );
    }

    private static void drawProgressBar(GuiGraphics graphics, int x, int y, int width, int height, int filledWidth, int accent) {
        graphics.fill(x, y, x + width, y + height, withOpacity(0xFF15151D));
        graphics.fill(x, y, x + filledWidth, y + height, withOpacity(accent));
        graphics.fill(x, y, x + width, y + scaled(1), withOpacity(0x55FFFFFF));
        graphics.fill(x, y + height - scaled(1), x + width, y + height, withOpacity(0x66000000));
    }

    private static void drawMilestones(GuiGraphics graphics, int x, int y, int width, int height) {
        for (int i = 1; i <= 3; i++) {
            int tickX = x + (width * i / 4);
            graphics.fill(tickX, y - scaled(1), tickX + scaled(1), y + height + scaled(1), withOpacity(0x99FFFFFF));
        }
    }

    private static int drawWrappedValueRow(GuiGraphics graphics, Font font, String label, List<String> lines, int x, int y, int valueColor) {
        int valueX = x + scaled(44);

        graphics.drawString(font, label, x, y, TEXT_MUTED, false);

        if (lines.isEmpty()) {
            graphics.drawString(font, "0", valueX, y, valueColor, false);
            return y + scaled(13);
        }

        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), valueX, y + i * scaled(10), valueColor, false);
        }

        return y + getRowHeight(lines);
    }

    private static int getRowHeight(List<String> lines) {
        return Math.max(scaled(13), lines.size() * scaled(10) + scaled(3));
    }

    private static List<String> wrapOreText(Font font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();

        if (text == null || text.isBlank()) {
            lines.add("0");
            return lines;
        }

        if (font.width(text) <= maxWidth) {
            lines.add(text);
            return lines;
        }

        String[] parts = text.split(" \\+ ");
        StringBuilder currentLine = new StringBuilder();

        for (String part : parts) {
            String candidate = currentLine.length() == 0
                    ? part
                    : currentLine + " + " + part;

            if (font.width(candidate) <= maxWidth) {
                currentLine = new StringBuilder(candidate);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(part);
                } else {
                    lines.add(fitText(font, part, maxWidth));
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
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

    private static Position getPosition(int width, int height, Minecraft client) {
        int margin = scaled(MARGIN);

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        int x = margin;
        int y = margin;

        switch (OreTrackerState.getHudPosition()) {
            case TOP_RIGHT -> {
                x = screenWidth - width - margin;
                y = margin;
            }
            case BOTTOM_LEFT -> {
                x = margin;
                y = screenHeight - height - margin;
            }
            case BOTTOM_RIGHT -> {
                x = screenWidth - width - margin;
                y = screenHeight - height - margin;
            }
            case CUSTOM -> {
                x = Math.max(0, Math.min(screenWidth - width, OreTrackerState.getCustomHudX()));
                y = Math.max(0, Math.min(screenHeight - height, OreTrackerState.getCustomHudY()));
            }
            case TOP_LEFT -> {
                x = margin;
                y = margin;
            }
        }

        return new Position(x, y);
    }

    private static Accent getAccent(HudData data) {
        if (data.ready) {
            return new Accent(pulseColor(READY, 0xFFFFFFFF), READY_SOFT);
        }

        double percent = getBarPercent(data.current, data.required);

        if (percent >= 0.75) {
            return new Accent(0xFFD9A6FF, 0xAAD9A6FF);
        }

        if (percent >= 0.50) {
            return new Accent(0xFFC789FF, 0xAAC789FF);
        }

        if (percent >= 0.25) {
            return new Accent(PURPLE, PURPLE_SOFT);
        }

        return new Accent(0xFF8F4FD6, 0xAA8F4FD6);
    }

    private static String progressStateLabel(double percent) {
        if (percent >= 0.75) {
            return "Almost ready";
        }

        if (percent >= 0.50) {
            return "Halfway";
        }

        if (percent >= 0.25) {
            return "Progressing";
        }

        return "Starting";
    }

    private static int pulseColor(int base, int highlight) {
        double pulse = (Math.sin(System.currentTimeMillis() / 180.0) + 1.0) / 2.0;

        int br = (base >> 16) & 255;
        int bg = (base >> 8) & 255;
        int bb = base & 255;

        int hr = (highlight >> 16) & 255;
        int hg = (highlight >> 8) & 255;
        int hb = highlight & 255;

        int r = (int) Math.round(br + (hr - br) * pulse * 0.35);
        int g = (int) Math.round(bg + (hg - bg) * pulse * 0.35);
        int b = (int) Math.round(bb + (hb - bb) * pulse * 0.35);

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int withOpacity(int argb) {
        int baseAlpha = (argb >>> 24) & 255;
        int newAlpha = baseAlpha * OreTrackerState.getHudOpacityPercent() / 100;
        return (argb & 0x00FFFFFF) | (newAlpha << 24);
    }

    private static int scaled(int value) {
        return Math.max(1, Math.round(value * OreTrackerState.getHudScalePercent() / 100.0f));
    }

    private static BigInteger scanInventoryForOreValue(Minecraft client, String oreName) {
        BigInteger total = BigInteger.ZERO;

        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);

            if (stack.isEmpty()) {
                continue;
            }

            String rawName = normalizeName(stack.getHoverName().getString());
            int count = stack.getCount();

            if (rawName.equals(oreName)) {
                total = total.add(OreValue.NORMAL_VALUE.multiply(BigInteger.valueOf(count)));
            } else if (rawName.equals("Compressed " + oreName)) {
                total = total.add(OreValue.COMPRESSED_VALUE.multiply(BigInteger.valueOf(count)));
            } else if (rawName.equals("Super Compressed " + oreName)) {
                total = total.add(OreValue.SUPER_COMPRESSED_VALUE.multiply(BigInteger.valueOf(count)));
            } else if (rawName.equals("Ultra Compressed " + oreName)) {
                total = total.add(OreValue.ULTRA_COMPRESSED_VALUE.multiply(BigInteger.valueOf(count)));
            } else if (rawName.equals("Mega Compressed " + oreName)) {
                total = total.add(OreValue.MEGA_COMPRESSED_VALUE.multiply(BigInteger.valueOf(count)));
            } else if (rawName.equals("Hyper Compressed " + oreName)) {
                total = total.add(OreValue.HYPER_COMPRESSED_VALUE.multiply(BigInteger.valueOf(count)));
            }
        }

        return total;
    }

    private static String normalizeName(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replaceAll("(?i)§[0-9A-FK-OR]", "")
                .replaceAll("(?i)&[0-9A-FK-OR]", "")
                .trim();
    }

    private static String getPercentText(BigInteger current, BigInteger required) {
        if (required.signum() <= 0) {
            return "0.0%";
        }

        BigDecimal percent = new BigDecimal(current)
                .multiply(BigDecimal.valueOf(100))
                .divide(new BigDecimal(required), 1, RoundingMode.HALF_UP);

        if (percent.compareTo(BigDecimal.valueOf(100)) > 0) {
            percent = BigDecimal.valueOf(100).setScale(1);
        }

        return percent + "%";
    }

    private static double getBarPercent(BigInteger current, BigInteger required) {
        if (required.signum() <= 0) {
            return 0.0;
        }

        BigDecimal ratio = new BigDecimal(current)
                .divide(new BigDecimal(required), 4, RoundingMode.HALF_UP);

        return Math.min(1.0, ratio.doubleValue());
    }

    private static String formatOreParts(BigInteger value) {
        if (value == null || value.signum() <= 0) {
            return "0";
        }

        OreParts parts = decompose(value);
        List<OreTierLine> lines = getNonZeroTierLines(parts);

        if (lines.isEmpty()) {
            return "0";
        }

        StringBuilder result = new StringBuilder();

        for (OreTierLine line : lines) {
            if (result.length() > 0) {
                result.append(" + ");
            }

            result.append(line.amount).append(" ").append(line.displayLabel);
        }

        return result.toString();
    }

    private static OreParts decompose(BigInteger value) {
        if (value == null || value.signum() <= 0) {
            return new OreParts(
                    BigInteger.ZERO,
                    BigInteger.ZERO,
                    BigInteger.ZERO,
                    BigInteger.ZERO,
                    BigInteger.ZERO,
                    BigInteger.ZERO
            );
        }

        BigInteger remaining = value;

        BigInteger[] hyper = remaining.divideAndRemainder(OreValue.HYPER_COMPRESSED_VALUE);
        remaining = hyper[1];

        BigInteger[] mega = remaining.divideAndRemainder(OreValue.MEGA_COMPRESSED_VALUE);
        remaining = mega[1];

        BigInteger[] ultra = remaining.divideAndRemainder(OreValue.ULTRA_COMPRESSED_VALUE);
        remaining = ultra[1];

        BigInteger[] superCompressed = remaining.divideAndRemainder(OreValue.SUPER_COMPRESSED_VALUE);
        remaining = superCompressed[1];

        BigInteger[] compressed = remaining.divideAndRemainder(OreValue.COMPRESSED_VALUE);
        remaining = compressed[1];

        return new OreParts(
                hyper[0],
                mega[0],
                ultra[0],
                superCompressed[0],
                compressed[0],
                remaining
        );
    }

    private static List<OreTierLine> getNonZeroTierLines(OreParts parts) {
        List<OreTierLine> lines = new ArrayList<>();

        addTierLine(lines, "HC", label("Hyper"), parts.hyper);
        addTierLine(lines, "MC", label("Mega"), parts.mega);
        addTierLine(lines, "UC", label("Ultra"), parts.ultra);
        addTierLine(lines, "SC", label("Super"), parts.superCompressed);
        addTierLine(lines, "C", label("Compressed"), parts.compressed);
        addTierLine(lines, "N", label("Normal"), parts.normal);

        return lines;
    }

    private static void addTierLine(List<OreTierLine> lines, String shortLabel, String displayLabel, BigInteger amount) {
        if (amount != null && amount.signum() > 0) {
            lines.add(new OreTierLine(shortLabel, displayLabel, amount));
        }
    }

    private static String label(String tier) {
        return switch (OreTrackerState.getNumberFormatMode()) {
            case SHORT -> switch (tier) {
                case "Hyper" -> "HC";
                case "Mega" -> "MC";
                case "Ultra" -> "UC";
                case "Super" -> "SC";
                case "Compressed" -> "C";
                case "Normal" -> "N";
                default -> tier;
            };
            case FULL -> switch (tier) {
                case "Hyper" -> "Hyper Compressed";
                case "Mega" -> "Mega Compressed";
                case "Ultra" -> "Ultra Compressed";
                case "Super" -> "Super Compressed";
                case "Compressed" -> "Compressed";
                case "Normal" -> "Normal";
                default -> tier;
            };
            case NORMAL -> tier;
        };
    }

    private record HudData(
            BigInteger current,
            BigInteger required,
            BigInteger remaining,
            String percentText,
            String haveText,
            String needText,
            String leftText,
            String categoryLabel,
            boolean ready
    ) {}

    private record OreParts(
            BigInteger hyper,
            BigInteger mega,
            BigInteger ultra,
            BigInteger superCompressed,
            BigInteger compressed,
            BigInteger normal
    ) {}

    private record OreTierLine(String shortLabel, String displayLabel, BigInteger amount) {}

    private record Accent(int main, int soft) {}

    private record Layout(int width, int height) {}

    private record Position(int x, int y) {}
}