# Ore Tracker

Ore Tracker is a client-side Fabric mod for Minecraft 1.21.11 that tracks ore progress toward mine shop upgrades.

It includes a mine/shop selector, category tracking, a customizable HUD, saved client settings, Discord webhook tracking for deaths and damage warnings, a Discord invite prompt, join reporting, `/bossfarm`, and automatic update support for future versions.

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
* Disconnect handling while death tracking is active
* Recent chat context helps identify killers when Minecraft reports `Unknown`
* Discord invite prompt on multiplayer join
* Discord invite prompt only appears once per player
* Join reporting when a player joins a multiplayer server with the mod installed
* `/bossfarm` automatic right-click command
* `/bossfarm` automatically stops when manual input is detected
* Automatic update support for future Ore Tracker releases
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
| `/bossfarm`     | Toggle automatic right-click farming    |

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

## Disconnect Handling

If Discord death tracking is active and the player disconnects from the server, Ore Tracker automatically stops tracking.

When this happens, Ore Tracker sends a Discord webhook message with:

* Player name
* Disconnect time
* Stop reason

This prevents tracking from staying active after the player leaves the server.

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

## Boss Farm

Ore Tracker includes a `/bossfarm` command.

Use:

```txt
/bossfarm
```

When enabled, Boss Farm automatically right-clicks once every second.

Boss Farm is designed for server menus, boss farming setups, and repetitive right-click actions. If a GUI/menu opens, Ore Tracker automatically closes it so the process does not get stuck inside a menu.

Boss Farm automatically disables itself when the player:

* Moves their mouse/camera
* Presses any keyboard key
* Presses any mouse button
* Disconnects
* Leaves the world

Run the command again to toggle it off manually:

```txt
/bossfarm
```

## Discord Invite Prompt

When a player joins a multiplayer server with Ore Tracker installed, the mod may show a Discord invite prompt.

The prompt includes:

* A Discord invite button
* A “Maybe Later” button
* A custom Ore Tracker themed interface

The prompt only appears once per player. After it has been shown, Ore Tracker saves that locally and will not show it again unless the saved prompt config is deleted.

The prompt state is saved in:

```txt
.minecraft/config/oretracker-discord-prompt.properties
```

## Join Reporting Notice

Ore Tracker sends a Discord webhook notification to the developer when a player joins a multiplayer server with the mod installed.

The join report includes:

* Minecraft IGN
* Join time

The join report does not include:

* Server IP/address
* UUID
* access tokens
* location
* inventory
* chat messages

This feature is used to track mod usage and confirm that the mod is being used successfully.

## Automatic Updates

Ore Tracker includes automatic update support for future releases.

On launch, Ore Tracker checks a remote `update.json` manifest hosted on GitHub. If a newer compatible version is available, the mod can download the new `.jar`, verify it with SHA-256, and schedule it to replace the old jar after Minecraft closes.

Auto updates use:

* GitHub-hosted update manifest
* Version comparison
* Minecraft version compatibility check
* SHA-256 hash verification
* Background download
* Next-restart installation

Important:

```txt
Users must manually install v2.0.0 first.
Auto updates only work for versions released after the updater is installed.
```

Example:

```txt
1.0.0 -> 2.0.0 = manual install required
2.0.0 -> 2.0.1 = automatic update supported
2.0.1 -> 2.0.2 = automatic update supported
```

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
* Discord prompt seen-state

Settings are saved locally in the Minecraft config folder.

## Installation

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API.
3. Download the latest Ore Tracker `.jar` from the Releases page.
4. Put the `.jar` in your `.minecraft/mods` folder.
5. Launch the game.

## Updating

For v2.0.0, users should manually download and install the latest `.jar` from the Releases page.

After v2.0.0 is installed, future supported versions may be downloaded automatically by the built-in updater.

If an update is downloaded automatically, restart Minecraft to apply it.

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

Discord prompt seen-state is saved through:

```txt
.minecraft/config/oretracker-discord-prompt.properties
```

Auto updater files may temporarily appear in:

```txt
.minecraft/oretracker-updates/
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

Boss Farm is a client-side automation feature. Server rules may vary, so users are responsible for following the rules of the server they play on.

## Version

Current major release:

```txt
2.0.0
```

Minecraft version:

```txt
1.21.11
```

Loader:

```txt
Fabric
```

## License

MIT License.
