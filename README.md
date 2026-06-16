# Ore Tracker

Ore Tracker is a client-side Fabric mod for Minecraft 1.21.11 that tracks ore progress toward mine shop upgrades.

It includes a mine/shop selector, category tracking, a customizable HUD, and optional Discord webhook death tracking.

## Features

* Mine/shop selection GUI
* Category tracking for Sword, Tool, Armor, and Talisman
* Live HUD progress
* READY indicator when you have enough ore
* Detailed, compact, and minimal HUD modes
* HUD opacity, scale, position, and number-format settings
* Manual HUD drag positioning
* Saved client settings between Minecraft reloads
* Configurable shop prices through `shops.json`
* Discord webhook death tracking
* Death logs include start time, death time, killer, and inventory resources

## Commands

| Command         | Use                                     |
| --------------- | --------------------------------------- |
| `/ot`           | Open Ore Tracker                        |
| `/oretracker`   | Open Ore Tracker                        |
| `/otclear`      | Clear current tracker                   |
| `/ottoggle`     | Hide/show HUD                           |
| `/otcompact`    | Toggle compact HUD mode                 |
| `/otmove`       | Drag the HUD manually                   |
| `/otreload`     | Reload config                           |
| `/otdebug`      | Print detected inventory item names     |
| `/track`        | Open Discord webhook death-tracking GUI |
| `/stoptracking` | Stop death tracking manually            |

## HUD Commands

### HUD Mode

```txt
/otmode detailed
/otmode compact
/otmode minimal
```

Aliases:

```txt
/otmode detail
/otmode full
/otmode mini
```

### HUD Position

```txt
/otpos top_left
/otpos top_right
/otpos bottom_left
/otpos bottom_right
```

Aliases:

```txt
/otpos tl
/otpos tr
/otpos bl
/otpos br
```

### HUD Opacity

```txt
/otopacity 25-100
```

Example:

```txt
/otopacity 85
```

### HUD Scale

```txt
/otscale 75-150
```

Example:

```txt
/otscale 90
```

### Number Format

```txt
/otnumbers short
/otnumbers normal
/otnumbers full
```

Aliases:

```txt
/otnumbers s
/otnumbers n
/otnumbers f
```

Short labels:

```txt
HC = Hyper Compressed
MC = Mega Compressed
UC = Ultra Compressed
SC = Super Compressed
C  = Compressed
N  = Normal
```

## Discord Death Tracking

Use:

```txt
/track
```

This opens the death-tracking GUI. Paste a Discord webhook URL and press **Start Tracking**.

When tracking starts, Ore Tracker sends a Discord message with the start time.

When you die, Ore Tracker sends another Discord message with:

* Time of death
* Player who killed you, when detectable
* Inventory resources:

  * HC
  * MC
  * UC
  * SC
  * C
  * N

Tracking automatically stops after death.

Use this to stop manually:

```txt
/stoptracking
```

## Saved Settings

Ore Tracker saves client settings between Minecraft reloads, including:

* Discord webhook
* HUD visibility
* HUD mode
* HUD position
* HUD opacity
* HUD scale
* Number format

Settings are saved locally in the Minecraft config folder.

## Installation

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API.
3. Download the latest Ore Tracker `.jar` from the Releases page.
4. Put the `.jar` in your `.minecraft/mods` folder.
5. Launch the game.

## Config

Ore Tracker creates its config files inside your Minecraft config folder.

Shop prices are configurable through:

```txt
.minecraft/config/oretracker/shops.json
```

Client settings are saved through:

```txt
.minecraft/config/oretracker/client-settings.json
```

## Notes

Ore Tracker scans your inventory and hotbar only.

It does not scan:

* Shulkers
* Backpacks
* Ender chests
* Armor slots
* Offhand

Death-killer detection is client-side. On some servers, the server may not expose the exact killer to the client, so the killer may occasionally show as `Unknown`.

## License

MIT License.
