package com.hoops.oretracker.client;

import java.math.BigInteger;
import java.util.LinkedHashSet;
import java.util.Set;

public final class OreTrackerState {
    public enum HudPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CUSTOM
    }

    public enum HudMode {
        DETAILED,
        COMPACT,
        MINIMAL
    }

    public enum NumberFormatMode {
        SHORT,
        NORMAL,
        FULL
    }

    private static OreTrackerData.Mine selectedMine = null;
    private static final LinkedHashSet<String> selectedCategoryIds = new LinkedHashSet<>();

    private static boolean hudVisible = true;
    private static HudMode hudMode = HudMode.DETAILED;
    private static HudPosition hudPosition = HudPosition.TOP_LEFT;
    private static NumberFormatMode numberFormatMode = NumberFormatMode.NORMAL;

    private static int hudOpacityPercent = 95;
    private static int hudScalePercent = 100;

    private static int customHudX = 10;
    private static int customHudY = 10;

    private OreTrackerState() {}

    public static OreTrackerData.Mine getSelectedMine() {
        return selectedMine;
    }

    public static void setSelectedMine(OreTrackerData.Mine mine) {
        selectedMine = mine;
        selectedCategoryIds.clear();

        if (mine != null && mine.hasCategories()) {
            selectedCategoryIds.addAll(mine.allCategoryIds());
        }

        hudVisible = true;
    }

    public static void setSelectedMineWithCategories(OreTrackerData.Mine mine, Set<String> categoryIds) {
        selectedMine = mine;
        selectedCategoryIds.clear();

        if (categoryIds != null) {
            selectedCategoryIds.addAll(categoryIds);
        }

        hudVisible = true;
    }

    public static void clearSelectedMine() {
        selectedMine = null;
        selectedCategoryIds.clear();
    }

    public static Set<String> getSelectedCategoryIds() {
        return Set.copyOf(selectedCategoryIds);
    }

    public static BigInteger getSelectedRequiredValue() {
        if (selectedMine == null) {
            return BigInteger.ZERO;
        }

        if (selectedMine.hasCategories()) {
            return selectedMine.selectedCategoryValue(selectedCategoryIds);
        }

        if (selectedMine.required() == null) {
            return BigInteger.ZERO;
        }

        return selectedMine.required().toBaseValue();
    }

    public static String getSelectedCategoryLabel() {
        if (selectedMine == null) {
            return "";
        }

        if (!selectedMine.hasCategories()) {
            return "Tracking";
        }

        if (selectedCategoryIds.isEmpty()) {
            return "No categories";
        }

        if (selectedCategoryIds.size() == selectedMine.categories().size()) {
            return "All categories";
        }

        StringBuilder label = new StringBuilder();

        for (OreTrackerData.CategoryCost category : selectedMine.categories()) {
            if (!selectedCategoryIds.contains(category.id())) {
                continue;
            }

            if (label.length() > 0) {
                label.append(" + ");
            }

            label.append(category.displayName());
        }

        return label.toString();
    }

    public static boolean isHudVisible() {
        return hudVisible;
    }

    public static boolean toggleHudVisible() {
        hudVisible = !hudVisible;
        return hudVisible;
    }

    public static HudMode getHudMode() {
        return hudMode;
    }

    public static boolean setHudMode(String rawMode) {
        if (rawMode == null) {
            return false;
        }

        switch (rawMode.trim().toLowerCase()) {
            case "detailed", "detail", "full" -> hudMode = HudMode.DETAILED;
            case "compact" -> hudMode = HudMode.COMPACT;
            case "minimal", "mini" -> hudMode = HudMode.MINIMAL;
            default -> {
                return false;
            }
        }

        return true;
    }

    public static boolean isCompactHud() {
        return hudMode == HudMode.COMPACT;
    }

    public static boolean toggleCompactHud() {
        hudMode = hudMode == HudMode.COMPACT ? HudMode.DETAILED : HudMode.COMPACT;
        return hudMode == HudMode.COMPACT;
    }

    public static HudPosition getHudPosition() {
        return hudPosition;
    }

    public static boolean setHudPosition(String rawPosition) {
        if (rawPosition == null) {
            return false;
        }

        String position = rawPosition.trim().toLowerCase();

        switch (position) {
            case "top_left", "topleft", "tl" -> hudPosition = HudPosition.TOP_LEFT;
            case "top_right", "topright", "tr" -> hudPosition = HudPosition.TOP_RIGHT;
            case "bottom_left", "bottomleft", "bl" -> hudPosition = HudPosition.BOTTOM_LEFT;
            case "bottom_right", "bottomright", "br" -> hudPosition = HudPosition.BOTTOM_RIGHT;
            default -> {
                return false;
            }
        }

        return true;
    }

    public static void setCustomHudPosition(int x, int y) {
        customHudX = Math.max(0, x);
        customHudY = Math.max(0, y);
        hudPosition = HudPosition.CUSTOM;
    }

    public static int getCustomHudX() {
        return customHudX;
    }

    public static int getCustomHudY() {
        return customHudY;
    }

    public static int getHudOpacityPercent() {
        return hudOpacityPercent;
    }

    public static void setHudOpacityPercent(int percent) {
        hudOpacityPercent = Math.max(25, Math.min(100, percent));
    }

    public static int getHudScalePercent() {
        return hudScalePercent;
    }

    public static void setHudScalePercent(int percent) {
        hudScalePercent = Math.max(75, Math.min(150, percent));
    }

    public static NumberFormatMode getNumberFormatMode() {
        return numberFormatMode;
    }

    public static boolean setNumberFormatMode(String rawMode) {
        if (rawMode == null) {
            return false;
        }

        switch (rawMode.trim().toLowerCase()) {
            case "short", "s" -> numberFormatMode = NumberFormatMode.SHORT;
            case "normal", "default", "n" -> numberFormatMode = NumberFormatMode.NORMAL;
            case "full", "long", "f" -> numberFormatMode = NumberFormatMode.FULL;
            default -> {
                return false;
            }
        }

        return true;
    }
}