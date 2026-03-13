---
layout: default
title: Revival System Guide
---

# Revival System Guide

Complete guide to the multiple revival methods available in PolarSouls, including ritual structures, items, and commands.

## Table of Contents

1. [Revival Methods Overview](#revival-methods-overview)
2. [Ritual Structure Revival](#ritual-structure-revival)
3. [Revive Skull Item](#revive-skull-item)
4. [Extra Life Item](#extra-life-item)
5. [Command-Based Revival](#command-based-revival)
6. [Head Mechanics](#head-mechanics)
7. [Revival Configuration](#revival-configuration)

## Revival Methods Overview

PolarSouls supports **four ways** to revive dead players:

| Method | User | Requirement | Speed | Risk |
|--------|------|-------------|-------|------|
| **Ritual Structure** | Any player | Build 3x3x3 structure + head | Slow | None (repeatable) |
| **Revive Skull** | Any player | Craft item + dead player head | Medium | None (repeatable) |
| **Revive Command** | Mod/Admin | Permission + command | Instant | None (authority needed) |
| **Admin Command** | Admin | `polarsouls.admin` permission | Instant | Highest privilege |

All methods work together - you can use any combination!

---

## Ritual Structure Revival

A ritual structure is a 3x3x3 beacon-style structure that automatically revives a dead player when completed.

### Structure Layout

The complete structure has three layers:

#### Layer 1: Base (3x3 grid)

```
[Soul Sand]  [Stair]       [Soul Sand]
[Stair]      [Ore Block]   [Stair]
[Soul Sand]  [Stair]       [Soul Sand]
```

**Components:**
- **4 Soul Sand blocks** at the corners
- **4 Stair blocks** at the edges (any stairs work: oak, stone, etc.)
- **1 Ore block** in the center (Gold/Diamond/Emerald/Iron/etc. - any ore works!)

#### Layer 2: Middle

```
[Wither Rose]  [Empty]  [Wither Rose]
[Empty]        [Fence]  [Empty]
[Wither Rose]  [Empty]  [Wither Rose]
```

**Components:**
- **4 Wither Roses** placed on top of the Soul Sand corners
- **1 Fence** placed on top of the center ore block
- Rest is empty air

#### Layer 3: Top

```
[Empty]  [Empty]  [Empty]
[Empty]  [Head]   [Empty]
[Empty]  [Empty]  [Empty]
```

**Components:**
- **Dead player's head** placed on top of the fence

### Step-by-Step Building

**Step 1: Build the Base Layer**

1. Dig a 3x3 pit or find a flat area
2. Place Soul Sand in all 4 corners (positions 1, 3, 7, 9 if you number them)
3. Place any Stair blocks in the 4 edge positions (2, 4, 6, 8)
4. Place any Ore block in the center (5)

Visual (from top, numbers 1-9):
```
1 2 3
4 5 6
7 8 9
```

Placement:
```
Soul Sand    Stair       Soul Sand
Stair        Ore Block   Stair
Soul Sand    Stair       Soul Sand
```

**Step 2: Add Middle Layer**

1. Look up at the Soul Sand blocks in the 4 corners
2. Place Wither Roses on top of each Soul Sand corner
3. Look up at the ore block in the center
4. Place a Fence on top of it

**Step 3: Place the Player Head**

1. Get the dead player's head (find it on the ground or from Revive Skull)
2. Look up at the fence in the center of layer 2
3. Place the head on top of the fence

### Automatic Revision

Once the head is placed on the fence, the plugin **automatically detects** the completed structure and:

1. - Revives the dead player (database updated)
2. - Teleports the player back to Main server
3. - Restores lives (default: 1)
4. - Optionally destroys the structure (configurable)

### Configuration Options

```yaml
hrm:
  structure-revive: true          # Enable/disable ritual structures
  leave-structure-base: true      # Keep base after revival?
  drop-heads: true                # Players drop heads on death
  detect-hrm-revive: true         # Auto-detect completed structures
```

**leave-structure-base:** If true, the base structure stays (only head removed). If false, the entire structure is destroyed.

### Example: Complete Structure

Here's a complete visual example from the side:

```
Layer 3:          [Head]
                    |
Layer 2:   [Rose]- [Fence] -[Rose]
                    |
Layer 1:  [Soul]- [Ore] -[Stair]
          [Sand]       [Soul Sand]

Full 3D View:
     Head
   W  F  W
S  O  O  S
  W     W
Soul  Stair  Soul
Stair Ore   Stair
```

---

## Revive Skull Item

A special crafted item that provides an easy way to get dead player heads and revive them.

### Crafting Recipe

**Shapeless recipe:**

| | | |
|---|---|---|
| Obsidian | Ghast Tear | Obsidian |
| Totem of Undying | Any Skull/Head | Totem of Undying |
| Obsidian | Ghast Tear | Obsidian |

**Materials needed:**
- 4× Obsidian
- 2× Ghast Tear (from Ghasts in the Nether)
- 2× Totem of Undying (from Evokers)
- 1× Any Skull or Head

**Result:** 1 Revive Skull item

### Using the Revive Skull

1. Right-click the Revive Skull in your inventory
2. A GUI menu opens showing all currently dead players
3. Click on a player's name in the menu
4. You receive their player head (item drops in your inventory or on ground)
5. Use the head in the ritual structure to revive them

### GUI Menu

When you right-click a Revive Skull, you see:

```
═══════════════════════════════════════
        DEAD PLAYERS - REVIVE MENU
═══════════════════════════════════════
  👤 DeadPlayer1
  👤 DeadPlayer2
  👤 DeadPlayer3
═══════════════════════════════════════
```

Click on any player to get their head.

### Features

- - Can be used multiple times (doesn't break)
- - Opens a clean GUI
- - No confusing items lying around
- - Can only be used by living players
- - Shows all currently dead players

### Configuration

```yaml
hrm:
  revive-skull-recipe: true  # Enable/disable crafting
```

---

## Extra Life Item

A craftable item that grants players +1 life when used.

### Default Recipe

| | | |
|---|---|---|
| Diamond Block | Emerald Block | Diamond Block |
| Netherite Ingot | Nether Star | Netherite Ingot |
| Gold Block | Emerald Block | Gold Block |

**Materials needed:**
- 2× Diamond Block
- 2× Emerald Block
- 2× Netherite Ingot
- 1× Nether Star
- 1× Gold Block

**Result:** 1 Extra Life item (displays as Nether Star)

### Using the Extra Life Item

1. Craft or obtain an Extra Life item
2. Right-click it in your inventory
3. **Success:** You gain +1 life (if not at maximum)
4. **Failure:** Message if already at max lives

### Features

- - Works for any player (not just newly revived)
- - Respects maximum lives cap (can't exceed max-lives setting)
- - Cannot be used while dead
- - Shows clear success/failure message

### Customizing the Recipe

You can completely customize the recipe by editing:

```yaml
extra-life:
  enabled: true
  item-material: "NETHER_STAR"    # Icon in crafting
  recipe:
    row1: "DED"   # Define row 1: D=Diamond Block, E=Emerald Block, D=Diamond Block
    row2: "INI"   # Define row 2: I=Netherite Ingot, N=Nether Star, I=Netherite Ingot
    row3: "GEG"   # Define row 3: G=Gold Block, E=Emerald Block, G=Gold Block
    ingredients:
      G: "GOLD_BLOCK"           # Define what letter G means
      E: "EMERALD_BLOCK"        # Define what letter E means
      N: "NETHER_STAR"          # Define what letter N means
      D: "DIAMOND_BLOCK"        # Define what letter D means
      I: "NETHERITE_INGOT"      # Define what letter I means
```

### Example: Simple Diamond Recipe

```yaml
extra-life:
  recipe:
    row1: "DDD"
    row2: "DED"
    row3: "DDD"
    ingredients:
      D: "DIAMOND_BLOCK"
      E: "EMERALD_BLOCK"
```

This creates a diamond shape with emerald in the center.

### Example: Emerald-Only Recipe

```yaml
extra-life:
  recipe:
    row1: "EEE"
    row2: "EEE"
    row3: "EEE"
    ingredients:
      E: "EMERALD_BLOCK"
```

Simply 9 Emerald Blocks (easiest recipe).

### Disabling the Extra Life Item

```yaml
extra-life:
  enabled: false
```

---

## Command-Based Revival

Two permission-based commands for reviving players.

### `/revive <player>`

**Permission:** `polarsouls.revive` (default: op)

Regular command for reviving a dead player. Less powerful than admin version.

```bash
/revive DeadPlayer
```

### `/psadmin revive <player>`

**Permission:** `polarsouls.admin` (op only)

Admin command for reviving. Same result as `/revive` but requires admin permission.

```bash
/psadmin revive DeadPlayer
```

### How Command Revival Works

1. Player must exist in database
2. Player must be marked as dead
3. Command instantly marks player as alive
4. Player is teleported to Main server spawn
5. Lives are restored (configured in `on-revive` setting)
6. Revive cooldown starts (30 seconds default)

### Command Revival Benefits

- ⚡ Instant (no structure building required)
- Precise (revive specific player)
- 🔑 Permission-based (can be given to moderators)
- Works even if player is offline
- - Works from console

### Example Usage

```bash
# Revive offline player
/psadmin revive Steve

# Revive from console
> psadmin revive PlayerName

# Revive multiple players
/psadmin revive Player1
/psadmin revive Player2
```

---

## Head Mechanics

Dead players' heads are central to the HRM revival system.

### Player Head Drops

When a player loses all lives, the plugin spawns their head near the death location. There are two modes for how this works, controlled by `hrm.head-place-as-block` in `config.yml`.

---

#### Block Mode (`head-place-as-block: true`, default)

The head is placed as a **permanent skull block** in the world.

- The plugin scans **upward** from the death Y coordinate to find the first open air block sitting on solid ground. If you die inside lava it emerges above the lava surface — always accessible.
- The block persists forever. It can't burn, can't despawn, and can't be washed away by water.
- Teammates **mine/break the block** to pick up the skull item as normal, then carry it to the revival structure.
- On revival the plugin **removes the block automatically** (see Cleanup below).

**Configuration:**
```yaml
hrm:
  head-place-as-block: true
```

---

#### Item Entity Mode (`head-place-as-block: false`)

The head is dropped as a standard **item entity** on the ground — the traditional Minecraft approach.

Two extra options control its survival:

| Option | What it does |
|---|---|
| `head-no-despawn: true` | Cancels the natural 5-minute despawn timer. The item stays until picked up. |
| `head-fireproof: true` | Marks the item as invulnerable. Fire, lava, and explosions can't destroy it. |

**Configuration:**
```yaml
hrm:
  head-place-as-block: false
  head-no-despawn: true    # item never despawns
  head-fireproof: true     # item survives lava/fire
```

---

### Head Cleanup on Revival

When a player is revived (any method — ritual, skull, command, admin), the plugin **removes all copies of their head** from the world. This runs in two passes:

**Pass 1 — Targeted removal (instant)**
If the server placed the head as a block, it stored the exact location in memory. On revive it looks up those coordinates directly, force-loads the chunk if it isn't currently loaded, removes the block, and releases the chunk. This is always correct regardless of whether the chunk is loaded or not.

> **Note:** The location map is in-memory only. If the server restarts between a player's death and their revival, Pass 1 has no data and Pass 2 acts as the safety net.

**Pass 2 — Tick-spread scan (fallback)**
Scans across all worlds to catch anything Pass 1 missed: item entities, item frames, player inventories, ender chests, shulker boxes, and any stale skull blocks left from before location tracking was introduced. Work is spread across multiple server ticks to avoid lag spikes on large servers.

In item-entity mode the chunk block scan is skipped entirely (no skull blocks to find), making cleanup faster.

---

### Wearing Head Effects

When a living player **wears a dead player's head**, they receive:

- **Speed II** — faster movement to the revival structure
- **Night Vision** — see in darkness en route

**Configuration:**
```yaml
hrm:
  head-wearing-effects: true
```

### Getting Heads Without a Physical Drop

If the original head was lost, burned, or simply too far away, use the **Revive Skull** to get a fresh copy:

1. Right-click a Revive Skull
2. Select the dead player from the menu
3. Receive their head instantly

The database always holds a permanent record of which players are dead, so a new head can be obtained at any time regardless of what happened to the original.

---

## Revival Configuration

### Enabling/Disabling Features

**Master HRM Toggle:**
```yaml
hrm:
  enabled: true  # Master on/off for all HRM features
```

**Individual Toggles:**
```yaml
hrm:
  drop-heads: true              # Drop heads on death
  structure-revive: true        # Enable ritual structures
  revive-skull-recipe: true     # Enable Revive Skull crafting
  head-wearing-effects: true    # Speed/night vision on heads
  detect-hrm-revive: true       # Auto-detect completed structures
  leave-structure-base: true    # Keep structure after revival
  head-place-as-block: true     # Place head as permanent block (recommended)
  head-no-despawn: true         # (item-entity mode) head item never despawns
  head-fireproof: true          # (item-entity mode) head item survives fire/lava
```

### Lives on Revival

```yaml
lives:
  on-revive: 1  # How many lives restored per revival
```

Options:
- `0` = Revived with 0 lives (must use extra life item immediately)
- `1` = Revived with 1 life (standard)
- `2+` = More forgiving

### Revive Cooldown

```yaml
lives:
  revive-cooldown-seconds: 30  # Post-revival death protection
```

After revival, player has this many seconds of protection where they cannot lose lives.

---

## Revival Flow Examples

### Scenario 1: Using Ritual Structure

```
1. Player dies
   ↓
2. Head drops at death location
   ↓
3. Teammate collects head
   ↓
4. Teammate builds 3x3x3 ritual structure
   ↓
5. Teammate places head on top
   ↓
6. Plugin detects structure and revives player
   ↓
7. Player appears on Main server with 1 life restored
```

### Scenario 2: Using Revive Skull

```
1. Player dies
   ↓
2. Head drops at death location
   ↓
3. Teammate uses Revive Skull (right-click)
   ↓
4. Teammate selects dead player from menu
   ↓
5. Teammate receives player's head
   ↓
6. Teammate builds ritual structure and places head
   ↓
7. Plugin revives player
   ↓
8. Player appears on Main server
```

### Scenario 3: Using Command

```
1. Player dies
   ↓
2. Moderator/admin runs /revive PlayerName
   ↓
3. Player instantly revives (no structure needed)
   ↓
4. Player teleported to Main server
   ↓
5. Lives restored to 1
```

---

## Tips & Tricks

### Efficient Revival Teams

- 👷 **Designate a gatherer** - Collects heads and materials
- 👷 **Designate a builder** - Constructs revival structures
- 👷 **Designate a guard** - Protects team while reviving

### Ritual Structure Locations

- Build near your base for quick revivals
- 🔒 Build in a secure location (protected from mobs)
- 🎨 Customize ore block color for aesthetic (any ore works)

### Revive Skull Strategies

- 🏪 Keep Revive Skull in shared chest
- Duplicate recipe components for multiple skulls
- Use when original head is lost or too far away

### Extra Life Farming

- ⛏️ Farm required materials (Nether stars, netherite, etc.)
- Create a dedicated farm for extra life materials
- 🤝 Distribute extra life items to team members

---

## Troubleshooting Revival

### Structure Not Triggering

**Check:**
- [ ] Structure is exactly 3x3x3 (verify with blocks)
- [ ] Head is placed on fence (top layer center)
- [ ] Correct block types used (soul sand, wither roses, etc.)
- [ ] `hrm.structure-revive: true` in config
- [ ] `detect-hrm-revive: true` in config

**Solution:** Try using `/revive <player>` command instead to verify database is working.

### Revive Skull Not Opening Menu

**Check:**
- [ ] `hrm.revive-skull-recipe: true` in config
- [ ] You have the right item (Nether Star with "Revive Skull" name)
- [ ] Dead players exist in database

**Solution:** Craft a new Revive Skull or verify crafting recipe.

### Heads Not Dropping

**Check:**
- [ ] `hrm.drop-heads: true` in config
- [ ] Player actually lost all lives (check with `/pstatus`)
- [ ] Check ground at death location

**Solution:** Use Revive Skull menu instead.

### Extra Life Not Working

**Check:**
- [ ] `extra-life.enabled: true` in config
- [ ] You're not already at maximum lives
- [ ] You're not dead (can't use while dead)
- [ ] Recipe is correct (verify crafting)

**Solution:** Check console for errors, verify recipe materials.

---

## See Also

- 📋 [Commands Reference](commands)
- [Configuration Reference](configuration)
- [FAQ](faq)

---

[← Configuration Reference](configuration) | [Back to Home](index) | [FAQ →](faq)
