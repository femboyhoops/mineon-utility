# Ore Tracker

Ore Tracker is a client-side Fabric mod for Minecraft 1.21.11 that tracks ore progress toward mine shop upgrades.

## Features

- Mine/shop selection GUI
- Category tracking for Sword, Tool, Armor, and Talisman
- Live HUD progress
- READY indicator when you have enough ore
- Detailed, compact, and minimal HUD modes
- HUD opacity, scale, position, and number-format settings
- Configurable shop prices through `shops.json`

## Commands

- `/ot` - Open Ore Tracker
- `/oretracker` - Open Ore Tracker
- `/otclear` - Clear current tracker
- `/ottoggle` - Hide/show HUD
- `/otcompact` - Toggle compact HUD
- `/otmode detailed|compact|minimal` - Change HUD mode
- `/otpos top_left|top_right|bottom_left|bottom_right` - Move HUD to a corner
- `/otmove` - Drag HUD manually
- `/otopacity 25-100` - Set HUD opacity
- `/otscale 75-150` - Set HUD scale
- `/otnumbers short|normal|full` - Change number format
- `/otreload` - Reload config
- `/otdebug` - Print detected inventory item names

## Installation

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API.
3. Download the latest Ore Tracker `.jar` from the Releases page.
4. Put the `.jar` in your `.minecraft/mods` folder.
5. Launch the game.

## Notes

Ore Tracker scans your inventory and hotbar only. It does not scan shulkers, backpacks, ender chests, armor slots, or offhand.

## License

MIT License.