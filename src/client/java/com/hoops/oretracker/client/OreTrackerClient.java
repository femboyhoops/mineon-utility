package com.hoops.oretracker.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class OreTrackerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OreTrackerData.reload();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("oretracker").executes(context -> {
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new OreTrackerScreen())
                );
                return 1;
            }));

            dispatcher.register(literal("ot").executes(context -> {
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new OreTrackerScreen())
                );
                return 1;
            }));

            dispatcher.register(literal("otclear").executes(context -> {
                Minecraft.getInstance().execute(() -> {
                    OreTrackerState.clearSelectedMine();
                    sendClientMessage("Ore Tracker HUD cleared.");
                });
                return 1;
            }));

            dispatcher.register(literal("ottoggle").executes(context -> {
                Minecraft.getInstance().execute(() -> {
                    boolean visible = OreTrackerState.toggleHudVisible();
                    sendClientMessage("Ore Tracker HUD " + (visible ? "shown." : "hidden."));
                });
                return 1;
            }));

            dispatcher.register(literal("otcompact").executes(context -> {
                Minecraft.getInstance().execute(() -> {
                    boolean compact = OreTrackerState.toggleCompactHud();
                    sendClientMessage("Ore Tracker compact HUD " + (compact ? "enabled." : "disabled."));
                });
                return 1;
            }));

            dispatcher.register(literal("otmode")
                    .then(argument("mode", StringArgumentType.word())
                            .executes(context -> {
                                String mode = StringArgumentType.getString(context, "mode");

                                Minecraft.getInstance().execute(() -> {
                                    boolean changed = OreTrackerState.setHudMode(mode);

                                    if (changed) {
                                        sendClientMessage("Ore Tracker HUD mode set to " + mode + ".");
                                    } else {
                                        sendClientMessage("Invalid mode. Use: detailed, compact, minimal.");
                                    }
                                });

                                return 1;
                            }))
            );

            dispatcher.register(literal("otopacity")
                    .then(argument("percent", IntegerArgumentType.integer(25, 100))
                            .executes(context -> {
                                int percent = IntegerArgumentType.getInteger(context, "percent");

                                Minecraft.getInstance().execute(() -> {
                                    OreTrackerState.setHudOpacityPercent(percent);
                                    sendClientMessage("Ore Tracker HUD opacity set to " + percent + "%.");
                                });

                                return 1;
                            }))
            );

            dispatcher.register(literal("otscale")
                    .then(argument("percent", IntegerArgumentType.integer(75, 150))
                            .executes(context -> {
                                int percent = IntegerArgumentType.getInteger(context, "percent");

                                Minecraft.getInstance().execute(() -> {
                                    OreTrackerState.setHudScalePercent(percent);
                                    sendClientMessage("Ore Tracker HUD scale set to " + percent + "%.");
                                });

                                return 1;
                            }))
            );

            dispatcher.register(literal("otnumbers")
                    .then(argument("mode", StringArgumentType.word())
                            .executes(context -> {
                                String mode = StringArgumentType.getString(context, "mode");

                                Minecraft.getInstance().execute(() -> {
                                    boolean changed = OreTrackerState.setNumberFormatMode(mode);

                                    if (changed) {
                                        sendClientMessage("Ore Tracker number format set to " + mode + ".");
                                    } else {
                                        sendClientMessage("Invalid number format. Use: short, normal, full.");
                                    }
                                });

                                return 1;
                            }))
            );

            dispatcher.register(literal("otmove").executes(context -> {
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new OreTrackerMoveScreen())
                );
                return 1;
            }));

            dispatcher.register(literal("otreload").executes(context -> {
                Minecraft.getInstance().execute(() -> {
                    OreTrackerData.reload();
                    OreTrackerState.clearSelectedMine();
                    sendClientMessage("Ore Tracker config reloaded. HUD selection cleared.");
                });
                return 1;
            }));

            dispatcher.register(literal("otpos")
                    .then(argument("position", StringArgumentType.word())
                            .executes(context -> {
                                String position = StringArgumentType.getString(context, "position");

                                Minecraft.getInstance().execute(() -> {
                                    boolean changed = OreTrackerState.setHudPosition(position);

                                    if (changed) {
                                        sendClientMessage("Ore Tracker HUD position set to " + position + ".");
                                    } else {
                                        sendClientMessage("Invalid position. Use: top_left, top_right, bottom_left, bottom_right.");
                                    }
                                });

                                return 1;
                            }))
            );

            dispatcher.register(literal("otdebug").executes(context -> {
                Minecraft.getInstance().execute(OreTrackerClient::debugInventoryNames);
                return 1;
            }));
        });

        OreTrackerHud.register();
    }

    private static void debugInventoryNames() {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null) {
            return;
        }

        sendClientMessage("---- Ore Tracker Inventory Debug ----");

        boolean foundAny = false;

        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);

            if (stack.isEmpty()) {
                continue;
            }

            foundAny = true;

            String rawName = stack.getHoverName().getString();
            int count = stack.getCount();

            sendClientMessage("Slot " + slot + " x" + count + ": [" + rawName + "]");
        }

        if (!foundAny) {
            sendClientMessage("No items found in inventory/hotbar.");
        }

        sendClientMessage("-------------------------------------");
    }

    private static void sendClientMessage(String message) {
        Minecraft client = Minecraft.getInstance();

        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(message), false);
        }
    }
}