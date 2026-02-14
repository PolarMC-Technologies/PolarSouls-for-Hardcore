---
layout: default
title: Configuration Reference
---

# Configuration Reference

Complete guide to configuring PolarSouls to match your server's needs.

## Table of Contents

1. [Overview](#overview)
2. [Server Role](#server-role)
3. [Database Configuration](#database-configuration)
4. [Lives System](#lives-system)
5. [Death Modes](#death-modes)
6. [Limbo Settings](#limbo-settings)
7. [HRM Features](#hrm-features)
8. [Extra Life Item](#extra-life-item)
9. [Messages & Colors](#messages--colors)
10. [Advanced Options](#advanced-options)

## Overview

PolarSouls uses a `config.yml` file that is automatically generated when you first run the plugin. This file is highly customizable and includes detailed comments explaining each setting.

**Location:** `plugins/PolarSouls/config.yml` on both servers

> **Important:** After editing the config file, either:
> - Restart both servers, or
> - Run `/psadmin reload` to reload without restart (some changes may require restart)

---

## Server Role

These settings identify what role each server plays in your network.

### Main Server Configuration

```yaml
# CRITICAL: Set this correctly!
is-limbo-server: false    # This is the Main server (survival)

# Must match your Velocity/proxy configuration
main-server-name: "main"     # Name of Main server in proxy config
limbo-server-name: "limbo"   # Name of Limbo server in proxy config
```

### Limbo Server Configuration

```yaml
# CRITICAL: Set this correctly!
is-limbo-server: true     # This is the Limbo server (purgatory)

# Must match your Velocity/proxy configuration
main-server-name: "main"     # Name of Main server in proxy config
limbo-server-name: "limbo"   # Name of Limbo server in proxy config
```

> **Tip:** The server names must exactly match the server names in your `velocity.toml` file.

---

## Database Configuration

Both servers must use **identical** database credentials.

```yaml
database:
  host: "localhost"              # Database host
  port: 3306                     # Database port (default: 3306)
  name: "polarsouls"             # Database name
  username: "root"               # MySQL username
  password: "changeme"           # MySQL password - CHANGE THIS!
  pool-size: 5                   # Connection pool size
  table-name: "hardcore_players" # Table name (default is fine)
```

### Pool Size Recommendations

- **Small servers (1-20 players):** 5
- **Medium servers (20-50 players):** 10
- **Large servers (50+ players):** 15-20

### For Pterodactyl Hosting

Use the database host provided by your hosting panel, not "localhost":

```yaml
database:
  host: "db-pterodactyl.c9akciq32cpl.us-east-1.rds.amazonaws.com"
  port: 3306
  name: "s123_polarsouls"
  username: "u123_admin"
  password: "your_secure_password"
```

---

## Lives System

Configure how the lives mechanic works.

```yaml
lives:
  default: 2                      # Starting lives for new players
  max-lives: 5                    # Maximum lives cap
  on-revive: 1                    # Lives restored when revived
  grace-period: "24h"             # New player protection period
  revive-cooldown-seconds: 30     # Post-revival death protection
```

### Default Lives

```yaml
default: 2
```

Starting number of lives for newly joined players. Common values:
- `1` - Hardcore (one life)
- `2` - Standard (two lives)
- `3` - Casual (three lives)

### Maximum Lives

```yaml
max-lives: 5
```

The highest number of lives a player can have. They cannot use extra life items or commands to exceed this cap.

### Lives on Revival

```yaml
on-revive: 1
```

How many lives a dead player gets back when revived. Options:
- `1` - Revived players get 1 life (requires teamwork to get more)
- `2` - Revived players get 2 lives (more forgiving)
- Set to any value that makes sense for your server

### Grace Period

```yaml
grace-period: "24h"
```

New player protection that prevents losing lives during the grace period. The timer counts **only when the player is online** (pauses when offline).

**Format Options:**
- `"24h"` = 24 hours
- `"2h30m"` = 2 hours 30 minutes
- `"90m"` = 90 minutes
- `"0"` = Disabled (no grace period)

**Example:** A new player with "24h" grace who plays 3 hours, logs off, then returns will have 21 hours remaining.

### Revive Cooldown

```yaml
revive-cooldown-seconds: 30
```

After being revived, a player is protected for this duration and cannot lose lives from deaths. Gives them time to get situated.

---

## Death Modes

Choose how your server handles player death.

```yaml
main:
  death-mode: "hybrid"            # hybrid | spectator | limbo
  hybrid-timeout-seconds: 300     # Timeout for hybrid mode (5 min)
  spectator-on-death: false       # Put in spectator before Limbo
  detect-hrm-revive: true         # Auto-detect ritual structure revivals
  send-to-limbo-delay-ticks: 20   # Delay before Limbo transfer (1 sec)
```

### Hybrid Mode (Default - Recommended)

```yaml
death-mode: "hybrid"
hybrid-timeout-seconds: 300
```

**Behavior:**
- Player loses a life and enters spectator mode on Main
- Team has 5 minutes (configurable) to revive them
- If not revived within timeout, player transfers to Limbo
- If player disconnects while dead, they skip spectator and go straight to Limbo

**Best For:** Servers wanting tension and urgency - gives teams a limited rescue window.

**Timeout Options:**
- `300` = 5 minutes (default)
- `180` = 3 minutes (faster)
- `600` = 10 minutes (more generous)

### Spectator Mode

```yaml
death-mode: "spectator"
spectator-on-death: true
```

**Behavior:**
- Player loses a life and enters spectator mode
- Player stays on Main server indefinitely
- Never auto-transfers to Limbo
- Can only be sent to Limbo by dying without lives or admin command

**Best For:** Servers preferring dead players to spectate their team constantly without forced exile.

### Limbo Mode (Strict)

```yaml
death-mode: "limbo"
```

**Behavior:**
- Player loses a life
- Upon reaching 0 lives, player immediately transfers to Limbo
- No spectator window

**Best For:** Hardcore servers enforcing strict separation between living and dead.

### HRM Revival Detection

```yaml
detect-hrm-revive: true
```

When enabled, the plugin automatically detects when a ritual structure is completed and performs the revival. Leave this enabled unless troubleshooting.

### Limbo Transfer Delay

```yaml
send-to-limbo-delay-ticks: 20
```

Delay in ticks before sending a player to Limbo (20 ticks = 1 second). Gives time for other events to process. Usually no need to change.

---

## Limbo Settings

Configure the Limbo server (only applies on Limbo server).

```yaml
limbo:
  check-interval-seconds: 3       # How often to check for revivals
  spawn:
    world: "world"
    x: 0.5
    y: 65.0
    z: 0.5
    yaw: 0.0
    pitch: 0.0
```

### Check Interval

```yaml
check-interval-seconds: 3
```

How frequently the Limbo server checks the database for revival requests. Lower = more responsive, higher = less database load.

- `1` - Very responsive (high database load)
- `3` - Balanced (default, recommended)
- `5` - Less responsive (low database load)

### Spawn Location

```yaml
spawn:
  world: "world"    # World name
  x: 0.5            # X coordinate
  y: 65.0           # Y coordinate
  z: 0.5            # Z coordinate
  yaw: 0.0          # Horizontal rotation (0 = north)
  pitch: 0.0        # Vertical rotation (0 = level, 90 = down)
```

Set the spawn location where dead players appear on Limbo. **Use the `/setlimbospawn` command** to set this automatically:

1. Stand at desired spawn location on Limbo
2. Run `/setlimbospawn`
3. Coordinates are automatically saved

---

## HRM Features

Configure the built-in Hardcore Revive Mode system.

```yaml
hrm:
  enabled: true                   # Master HRM toggle
  drop-heads: true                # Drop player heads on death
  death-location-message: true    # Send death coords to player
  structure-revive: true          # Enable 3x3x3 ritual structures
  leave-structure-base: true      # Don't destroy structure after revive
  head-wearing-effects: true      # Speed/night vision when wearing heads
  revive-skull-recipe: true       # Enable Revive Skull crafting
```

### Enable/Disable HRM

```yaml
enabled: true
```

Master toggle for all HRM features. Set to `false` to disable everything related to HRM.

### Player Head Drops

```yaml
drop-heads: true
```

When a player loses all lives, their head drops at the death location. These heads are used in ritual structures for revival.

### Death Location Messages

```yaml
death-location-message: true
```

When a player dies, send them a message with the death coordinates to help their teammates locate their dropped head.

**Example Message:**
```
Death coordinates: X: 1234 Y: 64 Z: -5678
```

### Structure-Based Revival

```yaml
structure-revive: true
```

Enable the 3x3x3 ritual structure revival system. When disabled, ritual structures won't trigger revivals.

### Leave Structure Base

```yaml
leave-structure-base: true
```

If true, the base structure remains after revival (only the player head is removed).  
If false, the entire structure is destroyed upon successful revival.

### Head Wearing Effects

```yaml
head-wearing-effects: true
```

When a player wears a dead player's head, they receive:
- **Speed II** effect
- **Night Vision** effect

Tactical advantage while carrying heads to revival structures.

### Revive Skull Recipe

```yaml
revive-skull-recipe: true
```

Enable crafting of Revive Skull items. The recipe is:

| | | |
|---|---|---|
| Obsidian | Ghast Tear | Obsidian |
| Totem of Undying | Any Skull/Head | Totem of Undying |
| Obsidian | Ghast Tear | Obsidian |

---

## Extra Life Item

Configure the craftable item that grants additional lives.

```yaml
extra-life:
  enabled: true
  item-material: "NETHER_STAR"    # Icon displayed in menus
  recipe:
    row1: "DED"    # Row 1: Diamond, Emerald, Diamond
    row2: "INI"    # Row 2: Netherite Ingot, Nether Star, Netherite Ingot
    row3: "GEG"    # Row 3: Gold Block, Emerald Block, Gold Block
    ingredients:
      G: "GOLD_BLOCK"
      E: "EMERALD_BLOCK"
      N: "NETHER_STAR"
      D: "DIAMOND_BLOCK"
      I: "NETHERITE_INGOT"
```

### Enable/Disable

```yaml
enabled: true
```

Set to `false` to disable extra life crafting entirely.

### Item Icon

```yaml
item-material: "NETHER_STAR"
```

The item used as the icon in menus and crafting results. Any valid Minecraft material works (common choices: `NETHER_STAR`, `GOLDEN_APPLE`, `DIAMOND`, `EMERALD`).

### Custom Recipe

To create a custom recipe:

1. Define the layout in `row1`, `row2`, `row3` using letters
2. Map each letter to a material in `ingredients`

**Example: Simple Diamond Recipe**

```yaml
recipe:
  row1: "DDD"
  row2: "DED"
  row3: "DDD"
  ingredients:
    D: "DIAMOND_BLOCK"
    E: "EMERALD_BLOCK"
```

Result: 9 Diamond Blocks with 1 Emerald Block in the center.

**Valid Material Names:**

Any Minecraft material name in SCREAMING_SNAKE_CASE:
- Blocks: `DIAMOND_BLOCK`, `GOLD_BLOCK`, `EMERALD_BLOCK`, `OBSIDIAN`, etc.
- Items: `NETHER_STAR`, `GOLDEN_APPLE`, `DRAGON_EGG`, etc.

**Finding Material Names:**

Check [Minecraft Material Constants](https://papermc.io/javadocs/paper/latest/) or look at `org.bukkit.Material` enum.

---

## Messages & Colors

Customize all player-facing messages and notification colors.

```yaml
messages:
  prefix: "&8[&4☠&8] &r"
  death-life-lost: "&cYou lost a life! &7Remaining: &e%lives%"
  death-last-life: "&c&l⚠ FINAL WARNING! &cYou are on your last life. Be careful!"
  revive-success: "&a&l✦ REVIVED! &aReturning to the world of the living..."
  # ... many more messages
```

### Color Codes

- `&0` = Black
- `&c` = Red
- `&a` = Green
- `&e` = Yellow
- `&9` = Blue
- `&d` = Magenta
- `&b` = Cyan
- `&f` = White
- `&7` = Gray
- `&8` = Dark Gray

### Formatting Codes

- `&l` = Bold
- `&m` = Strikethrough
- `&n` = Underline
- `&o` = Italic
- `&r` = Reset

### Available Variables

- `%lives%` - Player's current lives
- `%player%` - Player name
- `%timeout%` - Timeout duration (hybrid mode)
- `%time_remaining%` - Grace period remaining
- `%max%` - Maximum lives

### Example: Custom Messages

```yaml
messages:
  prefix: "&8[&cDEATH&8] &r"
  death-life-lost: "&cOh no! &7You lost a life. &eRemaining: %lives%/%max%"
  death-last-life: "&c&l💀 CRITICAL! &cThis is your LAST life!"
  revive-success: "&a&l✓ &aYou have been revived! Stay alive this time!"
  grace-remaining: "&eYou are protected for &a%time_remaining%"
```

---

## Advanced Options

```yaml
check-for-updates: true          # Check for new versions on Modrinth
hardcore-hearts: true            # Display hardcore hearts cosmetically
debug: false                     # Enable debug logging (dev only)
```

### Update Checking

```yaml
check-for-updates: true
```

When enabled, the plugin checks Modrinth on startup for newer versions and displays update notices in console.

### Hardcore Hearts

```yaml
hardcore-hearts: true
```

Display Minecraft's hardcore-style hearts (half-hearts instead of full hearts). This is **cosmetic only** and doesn't affect gameplay.

> Requires client mod support or resource pack to display properly

### Debug Logging

```yaml
debug: false
```

Enable detailed debug logging to console. Only useful for troubleshooting. Turn on if you're having issues and need more information.

---

## Configuration Examples

### Hardcore Server (One Life)

```yaml
lives:
  default: 1
  max-lives: 1
  on-revive: 0  # No lives on revive (need to craft extra life)

main:
  death-mode: "limbo"  # Immediate exile
```

### Casual Server (Three Lives)

```yaml
lives:
  default: 3
  max-lives: 5
  on-revive: 2
  grace-period: "48h"  # Extended grace for new players

main:
  death-mode: "hybrid"
  hybrid-timeout-seconds: 600  # 10 minute timeout
```

### Competitive Server (Spectator Focus)

```yaml
lives:
  default: 2
  max-lives: 3
  grace-period: "0"  # No grace period

main:
  death-mode: "spectator"  # Dead players watch forever
```

---

## Reloading Configuration

To reload configuration without restarting:

```bash
/psadmin reload
```

**Note:** Some changes take effect immediately, others may require:
- Player rejoin
- Server restart
- Plugin reload

---

## Need Help?

- [Installation Guide](installation)
- [Troubleshooting Guide](troubleshooting)
- 📋 [Commands Reference](commands)
- [FAQ](faq)

---

[← Commands Reference](commands) | [Back to Home](index) | [Revival System →](revival-system)
