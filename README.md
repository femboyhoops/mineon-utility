# Ore Tracker

Ore Tracker is a client-side Fabric mod for Minecraft 1.21.11 that tracks ore progress toward mine shop upgrades.

It includes a mine/shop selector, category tracking, a customizable HUD, saved client settings, and optional Discord webhook tracking for deaths, damage warnings, inventory resources, and recent chat context.

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
* Optional Discord user ID ping support
* Damage warning webhook alerts
* Death logs include start time, death time, killer, inventory resources, and recent chat context
* Recent chat context helps identify killers when Minecraft reports `Unknown`
* Client-side only

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
| `/tracker`      | Open Discord webhook death-tracking GUI |
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

## Discord Tracking

Use:

```txt
/track
```

or:

```txt
/tracker
```

This opens the Discord tracking GUI.

Paste a Discord webhook URL and press **Start Tracking**.

You may also enter a Discord user ID. This is optional. If provided, Ore Tracker will mention that user in Discord webhook alerts.

Discord user IDs must be numeric, not usernames.

Example Discord mention format:

```txt
<@123456789012345678>
```

Ore Tracker handles the mention formatting automatically. You only need to paste the numeric user ID.

## Discord Death Reports

When tracking starts, Ore Tracker sends a Discord message with:

* Player name
* Start time
* Optional Discord user ping

When you die, Ore Tracker sends another Discord message with:

* Time of death
* Player who killed you, when detectable
* Inventory resources
* Recent chat context

Inventory resources are shown as:

```txt
HC = Hyper Compressed
MC = Mega Compressed
UC = Ultra Compressed
SC = Super Compressed
C  = Compressed
N  = Normal
```

Tracking automatically stops after death.

Use this to stop manually:

```txt
/stoptracking
```

## Damage Warnings

While tracking is active, Ore Tracker sends a Discord warning when you take damage.

This is useful for automining because damage can knock your aim or position off the block you were mining.

Damage warnings include:

* Player name
* Current health
* Time
* Optional Discord user ping

Damage warnings use a cooldown to avoid flooding the webhook from repeated damage sources like fire, lava, poison, or rapid hits.

## Recent Chat Context

Death reports include a small amount of recent chat context.

This helps when the client cannot directly detect the killer. Some servers send the actual death message through chat, while Minecraft’s client-side damage source may still report the killer as `Unknown`.

Ore Tracker keeps the chat context limited so Discord messages do not get flooded.

## Killer Detection

Ore Tracker attempts to detect the killer using:

1. Minecraft client-side damage source data
2. Minecraft death message data
3. Recent cached attacker data
4. Recent chat context

Ore Tracker avoids blindly blaming the nearest player, because that can create false reports.

If the killer cannot be detected reliably, Ore Tracker may show:

```txt
Unknown
```

This is expected on some servers because killer data is not always exposed cleanly to client-side mods.

## Saved Settings

Ore Tracker saves client settings between Minecraft reloads, including:

* Discord webhook
* Discord user ID
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

Recent chat context is included in death reports to help with those cases.

## License

MIT License.
