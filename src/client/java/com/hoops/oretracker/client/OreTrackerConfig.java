package com.hoops.oretracker.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class OreTrackerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private OreTrackerConfig() {}

    public static List<OreTrackerData.MineWorld> loadWorlds() {
        Path configPath = getConfigPath();

        try {
            if (Files.notExists(configPath)) {
                Files.createDirectories(configPath.getParent());
                Files.writeString(configPath, GSON.toJson(defaultRoot()), StandardCharsets.UTF_8);
            }

            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            ConfigRoot root = GSON.fromJson(json, ConfigRoot.class);

            if (root == null || root.worlds == null || root.worlds.isEmpty()) {
                return toDataWorlds(defaultRoot());
            }

            return toDataWorlds(root);
        } catch (Exception exception) {
            return toDataWorlds(defaultRoot());
        }
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("oretracker")
                .resolve("shops.json");
    }

    private static List<OreTrackerData.MineWorld> toDataWorlds(ConfigRoot root) {
        List<OreTrackerData.MineWorld> worlds = new ArrayList<>();

        for (WorldConfig worldConfig : root.worlds) {
            List<OreTrackerData.Mine> mines = new ArrayList<>();

            if (worldConfig.mines != null) {
                for (MineConfig mineConfig : worldConfig.mines) {
                    List<OreTrackerData.CategoryCost> categories = new ArrayList<>();

                    if (mineConfig.categories != null) {
                        for (CategoryConfig categoryConfig : mineConfig.categories) {
                            categories.add(new OreTrackerData.CategoryCost(
                                    safe(categoryConfig.id),
                                    safe(categoryConfig.displayName),
                                    toOreValue(categoryConfig.required)
                            ));
                        }
                    }

                    mines.add(new OreTrackerData.Mine(
                            safe(mineConfig.worldId),
                            safe(mineConfig.mineName),
                            item(mineConfig.icon),
                            safe(mineConfig.shopkeeperName),
                            safe(mineConfig.oreName),
                            toOreValue(mineConfig.required),
                            categories
                    ));
                }
            }

            worlds.add(new OreTrackerData.MineWorld(
                    safe(worldConfig.id),
                    safe(worldConfig.displayName),
                    item(worldConfig.icon),
                    mines
            ));
        }

        return worlds;
    }

    private static OreValue toOreValue(OreValueConfig config) {
        if (config == null) {
            return OreValue.empty();
        }

        return new OreValue(
                config.normal,
                config.compressed,
                config.superCompressed,
                config.ultraCompressed,
                config.megaCompressed,
                config.hyperCompressed
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static ConfigRoot defaultRoot() {
        return new ConfigRoot(List.of(
                world(
                        "spawn",
                        "Spawn",
                        "grass_block",
                        List.of(
                                fullMine("spawn", "Mine #1", "oak_log", "Wood Shop", "Wood", cost(32, 1, 1, 0, 0, 0)),

                                categoryMine(
                                        "spawn",
                                        "Mine #2",
                                        "cobblestone",
                                        "Stone Shop",
                                        "Stone",
                                        category("sword", "Sword", cost(32, 1, 0, 0, 0, 0)),
                                        category("tool", "Tool", cost(32, 1, 0, 0, 0, 0)),
                                        category("armor", "Armor", cost(0, 2, 0, 0, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 48, 0, 0, 0, 0))
                                ),

                                categoryMine(
                                        "spawn",
                                        "Mine #3",
                                        "coal_ore",
                                        "Coal Shop",
                                        "Coal",
                                        category("sword", "Sword", cost(0, 16, 1, 0, 0, 0)),
                                        category("tool", "Tool", cost(0, 15, 0, 0, 0, 0)),
                                        category("armor", "Armor", cost(0, 32, 1, 0, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 3, 0, 0, 0))
                                ),

                                categoryMine(
                                        "spawn",
                                        "Mine #4",
                                        "iron_ore",
                                        "Iron Shop",
                                        "Iron",
                                        category("sword", "Sword", cost(0, 0, 8, 0, 0, 0)),
                                        category("tool", "Tool", cost(0, 28, 1, 0, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 6, 0, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 8, 0, 0, 0))
                                ),

                                categoryMine(
                                        "spawn",
                                        "Mine #5",
                                        "gold_ore",
                                        "Gold Shop",
                                        "Gold",
                                        category("sword", "Sword", cost(0, 16, 11, 0, 0, 0)),
                                        category("tool", "Tool", cost(0, 6, 2, 0, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 18, 0, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 12, 0, 0, 0))
                                ),

                                categoryMine(
                                        "spawn",
                                        "Mine #6",
                                        "lapis_lazuli",
                                        "Lapis Shop",
                                        "Lapis",
                                        category("sword", "Sword", cost(0, 0, 15, 0, 0, 0)),
                                        category("tool", "Tool", cost(0, 38, 3, 0, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 24, 0, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 16, 0, 0, 0))
                                ),

                                categoryMine(
                                        "spawn",
                                        "Mine #7",
                                        "diamond_ore",
                                        "Diamond Shop",
                                        "Diamond",
                                        category("sword", "Sword", cost(0, 32, 20, 0, 0, 0)),
                                        category("tool", "Tool", cost(0, 32, 5, 0, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 36, 0, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 20, 0, 0, 0))
                                ),

                                categoryMine(
                                        "spawn",
                                        "Mine #8",
                                        "emerald_ore",
                                        "Emerald Shop",
                                        "Emerald",
                                        category("sword", "Sword", cost(0, 48, 33, 0, 0, 0)),
                                        category("tool", "Tool", cost(0, 16, 8, 0, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 54, 0, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 24, 0, 0, 0))
                                )
                        )
                ),

                world(
                        "nether",
                        "Nether",
                        "netherrack",
                        List.of(
                                categoryMine(
                                        "nether",
                                        "Mine #9",
                                        "magma_cream",
                                        "Magma Shop",
                                        "Magma",
                                        category("sword", "Sword", cost(0, 0, 62, 1, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 27, 0, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 24, 3, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 3, 0, 0))
                                ),

                                categoryMine(
                                        "nether",
                                        "Mine #10",
                                        "crimson_roots",
                                        "Crimson Shop",
                                        "Crimson",
                                        category("sword", "Sword", cost(0, 0, 0, 4, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 31, 0, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 6, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 6, 0, 0))
                                ),

                                categoryMine(
                                        "nether",
                                        "Mine #11",
                                        "nether_quartz_ore",
                                        "Quartz Shop",
                                        "Quartz",
                                        category("sword", "Sword", cost(0, 0, 32, 2, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 54, 0, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 32, 4, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 4, 0, 0))
                                ),

                                categoryMine(
                                        "nether",
                                        "Mine #12",
                                        "raw_gold",
                                        "Topaz Shop",
                                        "Topaz",
                                        category("sword", "Sword", cost(0, 0, 60, 3, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 17, 1, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 48, 6, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 6, 0, 0))
                                ),

                                categoryMine(
                                        "nether",
                                        "Mine #13",
                                        "warped_roots",
                                        "Warped Shop",
                                        "Warped",
                                        category("sword", "Sword", cost(0, 0, 0, 8, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 54, 0, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 12, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 10, 0, 0))
                                ),

                                categoryMine(
                                        "nether",
                                        "Mine #14",
                                        "prismarine_crystals",
                                        "Celestite Shop",
                                        "Celestite",
                                        category("sword", "Sword", cost(0, 0, 48, 10, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 44, 1, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 16, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 8, 0, 0))
                                ),

                                categoryMine(
                                        "nether",
                                        "Mine #15",
                                        "soul_sand",
                                        "Soul Sand Shop",
                                        "Soul Sand",
                                        category("sword", "Sword", cost(0, 0, 56, 4, 0, 0)),
                                        category("tool", "Tool", cost(0, 12, 4, 0, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 32, 6, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 8, 0, 0))
                                ),

                                categoryMine(
                                        "nether",
                                        "Mine #16",
                                        "netherite_ingot",
                                        "Netherite Shop",
                                        "Netherite",
                                        category("sword", "Sword", cost(0, 0, 0, 21, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 34, 2, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 28, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 12, 0, 0))
                                )
                        )
                ),

                world(
                        "end",
                        "End",
                        "end_stone",
                        List.of(
                                categoryMine(
                                        "end",
                                        "Mine #17",
                                        "end_stone",
                                        "Endstone Shop",
                                        "Endstone",
                                        category("sword", "Sword", cost(0, 0, 6, 6, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 24, 3, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 12, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 10, 0, 0))
                                ),

                                categoryMine(
                                        "end",
                                        "Mine #18",
                                        "wither_rose",
                                        "Death Shop",
                                        "Death",
                                        category("sword", "Sword", cost(0, 0, 0, 30, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 22, 4, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 48, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 20, 0, 0))
                                ),

                                categoryMine(
                                        "end",
                                        "Mine #19",
                                        "purple_dye",
                                        "Ethereal Shop",
                                        "Ethereal",
                                        category("sword", "Sword", cost(0, 0, 16, 11, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 44, 1, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 18, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 8, 0, 0))
                                ),

                                categoryMine(
                                        "end",
                                        "Mine #20",
                                        "end_crystal",
                                        "Nebula Shop",
                                        "Nebula",
                                        category("sword", "Sword", cost(0, 0, 40, 5, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 54, 0, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 9, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 4, 0, 0))
                                ),

                                categoryMine(
                                        "end",
                                        "Mine #21",
                                        "amethyst_shard",
                                        "Amethyst Shop",
                                        "Amethyst",
                                        category("sword", "Sword", cost(0, 0, 48, 33, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 6, 5, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 54, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 24, 0, 0))
                                ),

                                categoryMine(
                                        "end",
                                        "Mine #22",
                                        "blue_dye",
                                        "Azurite Shop",
                                        "Azurite",
                                        category("sword", "Sword", cost(0, 0, 0, 45, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 48, 6, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 8, 1, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 30, 0, 0))
                                ),

                                categoryMine(
                                        "end",
                                        "Mine #23",
                                        "ender_pearl",
                                        "Void Shop",
                                        "Void",
                                        category("sword", "Sword", cost(0, 0, 0, 60, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 44, 8, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 32, 1, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 40, 0, 0))
                                ),

                                categoryMine(
                                        "end",
                                        "Mine #24",
                                        "obsidian",
                                        "Nullscape Shop",
                                        "Nullscape",
                                        category("sword", "Sword", cost(0, 0, 32, 7, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 31, 0, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 12, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 6, 0, 0))
                                )
                        )
                ),

                world(
                        "heaven",
                        "Heaven",
                        "glowstone",
                        List.of(
                                categoryMine(
                                        "heaven",
                                        "Mine #25",
                                        "glowstone_dust",
                                        "Glowstone Shop",
                                        "Glowstone",
                                        category("sword", "Sword", cost(0, 0, 32, 3, 1, 0)),
                                        category("tool", "Tool", cost(0, 0, 8, 10, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 44, 1, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 0, 1, 0))
                                ),

                                categoryMine(
                                        "heaven",
                                        "Mine #26",
                                        "white_wool",
                                        "Cloudium Shop",
                                        "Cloudium",
                                        category("sword", "Sword", cost(0, 0, 16, 11, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 44, 1, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 18, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 16, 0, 0))
                                ),

                                categoryMine(
                                        "heaven",
                                        "Mine #27",
                                        "yellow_dye",
                                        "Sunstone Shop",
                                        "Sunstone",
                                        category("sword", "Sword", cost(0, 0, 0, 11, 1, 0)),
                                        category("tool", "Tool", cost(0, 0, 36, 11, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 56, 1, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 32, 1, 0))
                                ),

                                categoryMine(
                                        "heaven",
                                        "Mine #28",
                                        "pink_dye",
                                        "Cerasium Shop",
                                        "Cerasium",
                                        category("sword", "Sword", cost(0, 0, 32, 22, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 24, 3, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 36, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 32, 0, 0))
                                ),

                                categoryMine(
                                        "heaven",
                                        "Mine #29",
                                        "quartz",
                                        "Soranite Shop",
                                        "Soranite",
                                        category("sword", "Sword", cost(0, 0, 0, 26, 1, 0)),
                                        category("tool", "Tool", cost(0, 0, 32, 13, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 16, 2, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 0, 2, 0))
                                ),

                                categoryMine(
                                        "heaven",
                                        "Mine #30",
                                        "nether_star",
                                        "Starfall Shop",
                                        "Starfall",
                                        category("sword", "Sword", cost(0, 0, 28, 8, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 17, 1, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 32, 13, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 12, 0, 0))
                                ),

                                categoryMine(
                                        "heaven",
                                        "Mine #31",
                                        "amethyst_shard",
                                        "Crystite Shop",
                                        "Crystite",
                                        category("sword", "Sword", cost(0, 0, 0, 41, 1, 0)),
                                        category("tool", "Tool", cost(0, 0, 28, 17, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 40, 2, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 32, 2, 0))
                                ),

                                categoryMine(
                                        "heaven",
                                        "Mine #32",
                                        "feather",
                                        "Aetherite Shop",
                                        "Aetherite",
                                        category("sword", "Sword", cost(0, 0, 0, 56, 1, 0)),
                                        category("tool", "Tool", cost(0, 0, 24, 17, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 0, 3, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 0, 3, 0))
                                )
                        )
                ),

                world(
                        "ocean",
                        "Ocean",
                        "water_bucket",
                        List.of(
                                categoryMine(
                                        "ocean",
                                        "Mine #33",
                                        "sand",
                                        "Sand Shop",
                                        "Sand",
                                        category("sword", "Sword", cost(0, 0, 0, 54, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 24, 3, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 36, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 32, 0, 0))
                                ),

                                categoryMine(
                                        "ocean",
                                        "Mine #34",
                                        "prismarine_shard",
                                        "Prismarine Shop",
                                        "Prismarine",
                                        category("sword", "Sword", cost(0, 0, 0, 7, 2, 0)),
                                        category("tool", "Tool", cost(0, 0, 16, 20, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 24, 3, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 0, 3, 0))
                                ),

                                categoryMine(
                                        "ocean",
                                        "Mine #35",
                                        "cyan_dye",
                                        "Bathyal Shop",
                                        "Bathyal",
                                        category("sword", "Sword", cost(0, 0, 48, 33, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 4, 5, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 54, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 48, 0, 0))
                                ),

                                categoryMine(
                                        "ocean",
                                        "Mine #36",
                                        "sponge",
                                        "Sponge Shop",
                                        "Sponge",
                                        category("sword", "Sword", cost(0, 0, 48, 18, 0, 0)),
                                        category("tool", "Tool", cost(0, 0, 58, 2, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 30, 0, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 24, 0, 0))
                                ),

                                categoryMine(
                                        "ocean",
                                        "Mine #37",
                                        "dead_brain_coral",
                                        "Dead Coral Shop",
                                        "Dead Coral",
                                        category("sword", "Sword", cost(0, 0, 0, 22, 2, 0)),
                                        category("tool", "Tool", cost(0, 0, 8, 23, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 48, 3, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 0, 4, 0))
                                ),

                                categoryMine(
                                        "ocean",
                                        "Mine #38",
                                        "water_bucket",
                                        "Oxygen Shop",
                                        "Oxygen",
                                        category("sword", "Sword", cost(0, 0, 0, 52, 2, 0)),
                                        category("tool", "Tool", cost(0, 0, 0, 27, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 32, 4, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 0, 5, 0))
                                ),

                                categoryMine(
                                        "ocean",
                                        "Mine #39",
                                        "prismarine_crystals",
                                        "Thallasium Shop",
                                        "Thallasium",
                                        category("sword", "Sword", cost(0, 0, 0, 20, 3, 0)),
                                        category("tool", "Tool", cost(0, 0, 56, 30, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 16, 5, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 0, 6, 0))
                                ),

                                categoryMine(
                                        "ocean",
                                        "Mine #40",
                                        "heart_of_the_sea",
                                        "Atlantium Shop",
                                        "Atlantium",
                                        category("sword", "Sword", cost(0, 0, 0, 34, 3, 0)),
                                        category("tool", "Tool", cost(0, 0, 48, 33, 0, 0)),
                                        category("armor", "Armor", cost(0, 0, 0, 40, 5, 0)),
                                        category("talisman", "Talisman", cost(0, 0, 0, 0, 7, 0))
                                )
                        )
                )
        ));
    }

    private static WorldConfig world(String id, String displayName, String icon, List<MineConfig> mines) {
        return new WorldConfig(id, displayName, icon, mines);
    }

    private static MineConfig fullMine(
            String worldId,
            String mineName,
            String icon,
            String shopkeeperName,
            String oreName,
            OreValueConfig required
    ) {
        return new MineConfig(worldId, mineName, icon, shopkeeperName, oreName, required, List.of());
    }

    private static MineConfig categoryMine(
            String worldId,
            String mineName,
            String icon,
            String shopkeeperName,
            String oreName,
            CategoryConfig... categories
    ) {
        return new MineConfig(worldId, mineName, icon, shopkeeperName, oreName, cost(0, 0, 0, 0, 0, 0), List.of(categories));
    }

    private static CategoryConfig category(String id, String displayName, OreValueConfig required) {
        return new CategoryConfig(id, displayName, required);
    }

    private static OreValueConfig cost(
            int normal,
            int compressed,
            int superCompressed,
            int ultraCompressed,
            int megaCompressed,
            int hyperCompressed
    ) {
        return new OreValueConfig(
                normal,
                compressed,
                superCompressed,
                ultraCompressed,
                megaCompressed,
                hyperCompressed
        );
    }

    private static Item item(String id) {
        if (id == null) {
            return Items.PAPER;
        }

        return switch (id) {
            case "grass_block" -> Items.GRASS_BLOCK;
            case "oak_log" -> Items.OAK_LOG;
            case "cobblestone" -> Items.COBBLESTONE;
            case "coal_ore" -> Items.COAL_ORE;
            case "iron_ore" -> Items.IRON_ORE;
            case "gold_ore" -> Items.GOLD_ORE;
            case "lapis_lazuli" -> Items.LAPIS_LAZULI;
            case "diamond_ore" -> Items.DIAMOND_ORE;
            case "emerald_ore" -> Items.EMERALD_ORE;

            case "netherrack" -> Items.NETHERRACK;
            case "magma_cream" -> Items.MAGMA_CREAM;
            case "crimson_roots" -> Items.CRIMSON_ROOTS;
            case "nether_quartz_ore" -> Items.NETHER_QUARTZ_ORE;
            case "raw_gold" -> Items.RAW_GOLD;
            case "warped_roots" -> Items.WARPED_ROOTS;
            case "prismarine_crystals" -> Items.PRISMARINE_CRYSTALS;
            case "soul_sand" -> Items.SOUL_SAND;
            case "netherite_ingot" -> Items.NETHERITE_INGOT;

            case "end_stone" -> Items.END_STONE;
            case "wither_rose" -> Items.WITHER_ROSE;
            case "purple_dye" -> Items.PURPLE_DYE;
            case "end_crystal" -> Items.END_CRYSTAL;
            case "amethyst_shard" -> Items.AMETHYST_SHARD;
            case "blue_dye" -> Items.BLUE_DYE;
            case "ender_pearl" -> Items.ENDER_PEARL;
            case "obsidian" -> Items.OBSIDIAN;

            case "glowstone" -> Items.GLOWSTONE;
            case "glowstone_dust" -> Items.GLOWSTONE_DUST;
            case "white_wool" -> Items.WHITE_WOOL;
            case "yellow_dye" -> Items.YELLOW_DYE;
            case "pink_dye" -> Items.PINK_DYE;
            case "quartz" -> Items.QUARTZ;
            case "nether_star" -> Items.NETHER_STAR;
            case "feather" -> Items.FEATHER;

            case "water_bucket" -> Items.WATER_BUCKET;
            case "sand" -> Items.SAND;
            case "prismarine_shard" -> Items.PRISMARINE_SHARD;
            case "cyan_dye" -> Items.CYAN_DYE;
            case "sponge" -> Items.SPONGE;
            case "dead_brain_coral" -> Items.DEAD_BRAIN_CORAL;
            case "heart_of_the_sea" -> Items.HEART_OF_THE_SEA;

            default -> Items.PAPER;
        };
    }

    private static final class ConfigRoot {
        List<WorldConfig> worlds;

        ConfigRoot() {}

        ConfigRoot(List<WorldConfig> worlds) {
            this.worlds = worlds;
        }
    }

    private static final class WorldConfig {
        String id;
        String displayName;
        String icon;
        List<MineConfig> mines;

        WorldConfig() {}

        WorldConfig(String id, String displayName, String icon, List<MineConfig> mines) {
            this.id = id;
            this.displayName = displayName;
            this.icon = icon;
            this.mines = mines;
        }
    }

    private static final class MineConfig {
        String worldId;
        String mineName;
        String icon;
        String shopkeeperName;
        String oreName;
        OreValueConfig required;
        List<CategoryConfig> categories;

        MineConfig() {}

        MineConfig(
                String worldId,
                String mineName,
                String icon,
                String shopkeeperName,
                String oreName,
                OreValueConfig required,
                List<CategoryConfig> categories
        ) {
            this.worldId = worldId;
            this.mineName = mineName;
            this.icon = icon;
            this.shopkeeperName = shopkeeperName;
            this.oreName = oreName;
            this.required = required;
            this.categories = categories;
        }
    }

    private static final class CategoryConfig {
        String id;
        String displayName;
        OreValueConfig required;

        CategoryConfig() {}

        CategoryConfig(String id, String displayName, OreValueConfig required) {
            this.id = id;
            this.displayName = displayName;
            this.required = required;
        }
    }

    private static final class OreValueConfig {
        int normal;
        int compressed;
        int superCompressed;
        int ultraCompressed;
        int megaCompressed;
        int hyperCompressed;

        OreValueConfig() {}

        OreValueConfig(
                int normal,
                int compressed,
                int superCompressed,
                int ultraCompressed,
                int megaCompressed,
                int hyperCompressed
        ) {
            this.normal = normal;
            this.compressed = compressed;
            this.superCompressed = superCompressed;
            this.ultraCompressed = ultraCompressed;
            this.megaCompressed = megaCompressed;
            this.hyperCompressed = hyperCompressed;
        }
    }
}


