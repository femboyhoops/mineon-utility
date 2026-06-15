package com.hoops.oretracker.client;

import net.minecraft.world.item.Item;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

public final class OreTrackerData {
    private OreTrackerData() {}

    private static List<MineWorld> worlds = List.of();

    public record MineWorld(
            String id,
            String displayName,
            Item icon,
            List<Mine> mines
    ) {}

    public record CategoryCost(
            String id,
            String displayName,
            OreValue required
    ) {}

    public record Mine(
            String worldId,
            String mineName,
            Item icon,
            String shopkeeperName,
            String oreName,
            OreValue required,
            List<CategoryCost> categories
    ) {
        public Mine {
            if (categories == null) {
                categories = List.of();
            }
        }

        public boolean hasCategories() {
            return !categories.isEmpty();
        }

        public boolean hasCostData() {
            if (hasCategories()) {
                return selectedCategoryValue(allCategoryIds()).signum() > 0;
            }

            return required != null && required.toBaseValue().signum() > 0;
        }

        public BigInteger selectedCategoryValue(Set<String> selectedCategoryIds) {
            if (!hasCategories() || selectedCategoryIds == null || selectedCategoryIds.isEmpty()) {
                return BigInteger.ZERO;
            }

            BigInteger total = BigInteger.ZERO;

            for (CategoryCost category : categories) {
                if (selectedCategoryIds.contains(category.id()) && category.required() != null) {
                    total = total.add(category.required().toBaseValue());
                }
            }

            return total;
        }

        public Set<String> allCategoryIds() {
            java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();

            for (CategoryCost category : categories) {
                ids.add(category.id());
            }

            return ids;
        }
    }

    public static void reload() {
        worlds = OreTrackerConfig.loadWorlds();
    }

    public static List<MineWorld> worlds() {
        if (worlds.isEmpty()) {
            reload();
        }

        return worlds;
    }
}