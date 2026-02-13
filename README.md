# PolarSouls

![PolarSouls Banner](https://cdn.modrinth.com/data/Pb03qu6T/images/70ce5f45786d4716bb6d47d242ee3238a2b4ec4a.jpeg)

**Version 1.3.6** | [Modrinth](https://modrinth.com/plugin/polarsouls) | [GitHub](https://github.com/SSoggyTacoBlud/PolarSouls-for-Hardcore)

A comprehensive hardcore lives system plugin for Minecraft 1.21.X (Spigot/Paper/Purpur) designed for Velocity proxy networks. Features a sophisticated lives-based death mechanic where players are exiled to a Limbo server upon losing all lives, with multiple revival methods to bring them back.

## Features Overview

**Lives System** - Configurable starting lives (default: 2), with maximum cap (default: 5)

**Three Death Modes** - Choose between immediate Limbo exile, permanent spectator, or hybrid timeout

**Multiple Revival Methods** - Ritual structures, Revive Skull item, or admin commands

**Grace Period Protection** - New players get protected time to learn (counts only online time)

**Extra Life Items** - Craftable items to gain additional lives (fully customizable recipe)

**Cross-Server Architecture** - MySQL-backed persistence across Main and Limbo servers

**Automatic Transfer** - Dead players sent to Limbo, revived players return to Main automatically

**Limbo Visiting** - Alive players can visit Limbo to interact with dead teammates

## How It Works

The plugin runs on two backend servers behind a Velocity proxy (might work on BungeeCord/Waterfall, not tested yet - please report if you test it): **Main** (survival) and **Limbo** (purgatory). Both servers share a MySQL database for synchronized player state.

**Basic Flow:**
1. Players start with configurable lives (default: 2)
2. Each death costs one life
3. **Grace period** protects new players from losing lives (counts only online time, pauses offline)
4. At 0 lives, behavior depends on **death mode** (see below)
5. Teammates can revive dead players using:
   - **Ritual structure** (3x3x3 beacon with player head)
   - **Revive Skull** menu (craftable item to select dead player)
   - **`/revive <player>`** command (requires permission)
   - **Admin commands** (`/psadmin revive`)
6. Revived players automatically return to Main with restored lives (default: 1)
7. **Revive cooldown** (default: 30 seconds) protects freshly revived players from accidental death
8. Alive players can visit Limbo with `/limbo` and return with `/leavelimbo`

## Death Modes

PolarSouls offers three death mode configurations to match your server's gameplay style:

| Mode | Behavior | Best For |
|------|----------|----------|
| **`hybrid`** (default) | Dead players enter spectator mode on Main for a configurable timeout window (default: 5 minutes). Teammates must revive them during this window, or they're automatically transferred to Limbo. If a dead player disconnects and reconnects, they skip spectator and go straight to Limbo. | Servers wanting tension and urgency - gives teams a limited rescue window |
| **`spectator`** | Dead players stay on Main server in spectator mode indefinitely until revived. They never auto-transfer to Limbo. | Servers preferring dead players to spectate their team constantly without forced exile |
| **`limbo`** | Dead players are immediately transferred to Limbo upon losing all lives. No spectator window. | Hardcore servers enforcing strict separation between living and dead |

All modes support all revival methods:
- ✅ Hardcore Revive Mode (HRM) ritual structures
- ✅ Revive Skull item menu
- ✅ `/revive <player>` command
- ✅ Admin commands (`/psadmin revive`)

## Built-in HRM (Hardcore Revive Mode) Features
*(Revival system inspired by [Hardcore Revive Mod](https://modrinth.com/plugin/hardcore-revive-mod))*

PolarSouls includes a complete built-in revival system with multiple mechanics:

### Player Head Drops
- When a player loses all lives, their **player head drops** at the death location
- The dead player receives a **death coordinates message** to help teammates find the head
- Heads are required for the revival ritual structure
- **Configurable:** Can be disabled in `config.yml` (`hrm.drop-heads: false`)

### Revival Ritual Structure
Build a 3x3x3 beacon-style structure to revive dead players:

**Structure Layout:**
- **Bottom layer (3x3):**
  - 4 **Soul Sand** blocks at the corners
  - 4 **Stair blocks** at the edges (any stairs work)
  - 1 **Ore block** in the center (Gold/Diamond/Emerald/etc.)
- **Middle layer:**
  - 4 **Wither Roses** placed on top of the Soul Sand corners
  - 1 **Fence** placed on top of the center ore block
- **Top layer:**
  - Place the **dead player's head** on top of the fence to trigger revival

**Features:**
- ✅ Auto-detects when structure is completed (plugin automatically saves revival to database)
- ✅ Configurable structure preservation (`hrm.leave-structure-base: true/false`)
- ✅ Supports any ore block type for the base (customize for difficulty/aesthetics)
- ✅ Visual feedback when revival triggers

**Configurable:** Can be disabled entirely in `config.yml` (`hrm.structure-revive: false`)

### Craftable Items

#### **Revive Skull**
A special item to simplify obtaining dead player heads for rituals.

**Recipe (shapeless):**

| | | |
|---|---|---|
| Obsidian | Ghast Tear | Obsidian |
| Totem of Undying | Any Skull/Head | Totem of Undying |
| Obsidian | Ghast Tear | Obsidian |

**Usage:**
- Right-click the Revive Skull to open a GUI menu
- Menu shows all currently dead players
- Click a player to receive their head
- Use the head in the revival ritual structure

**Configurable:** Recipe can be disabled in `config.yml` (`hrm.revive-skull-recipe: false`)

#### **Extra Life Item**
Craftable item that grants +1 life when used (respects maximum lives cap).

**Default Recipe:**

| | | |
|---|---|---|
| Diamond Block | Emerald Block | Diamond Block |
| Netherite Ingot | Nether Star | Netherite Ingot |
| Gold Block | Emerald Block | Gold Block |

**Usage:**
- Right-click to consume and gain +1 life
- Cannot be used while dead
- Cannot exceed maximum lives (default: 5)
- Success/failure messages sent to player

**Fully Customizable Recipe:**
```yaml
extra-life:
  enabled: true
  item-material: "NETHER_STAR"  # Icon displayed in menus
  recipe:
    row1: "DED"  # D = Diamond Block, E = Emerald Block
    row2: "INI"  # I = Netherite Ingot, N = Nether Star
    row3: "GEG"  # G = Gold Block
    ingredients:
      G: "GOLD_BLOCK"
      E: "EMERALD_BLOCK"
      N: "NETHER_STAR"
      D: "DIAMOND_BLOCK"
      I: "NETHERITE_INGOT"
```

Change the letters in `row1/row2/row3` and define custom materials in `ingredients` to create your own recipe!

### Head Wearing Effects
- Players wearing a dead player's head receive **Speed II** and **Night Vision** effects
- Tactical advantage while carrying heads to revival structures
- **Configurable:** Can be disabled in `config.yml` (`hrm.head-wearing-effects: false`)

## Grace Period System

The **grace period** protects new players from losing lives while they learn the server mechanics.

**How it Works:**
- New players get a configurable grace period (default: 24 hours, can be disabled with `"0"`)
- Timer counts **only when player is online** (pauses when offline)
- Deaths during grace period do not cost lives
- Custom messages notify player of remaining grace time
- Admins can set custom grace periods per player with `/psadmin grace <player> <hours>`

**Format Options:**
- `"24h"` = 24 hours
- `"2h30m"` = 2 hours 30 minutes
- `"90m"` = 90 minutes
- `"0"` = Disabled (no grace period)

**Example:** A player with "24h" grace period who plays 3 hours, logs off, then returns later will have 21 hours of grace remaining.

## Limbo Visiting

Alive players can visit Limbo using `/limbo` to interact with dead teammates.

**Features:**
- ✅ `/limbo` - Teleport to Limbo server (requires `polarsouls.visit` permission)
- ✅ `/leavelimbo` (or `/hub`) - Return to Main server
- ✅ Alive players can freely come and go
- ❌ Dead players cannot leave until revived
- Custom welcome messages for visitors vs. dead players

## Requirements

- **Minecraft:** 1.21.X (Spigot, Paper, or Purpur)
- **Proxy:** Velocity (BungeeCord/Waterfall might work but untested)
- **Database:** MySQL 5.7+ or MariaDB 10.2+ (can be shared with other plugins like CoreProtect)
- **Java:** 21 or higher
- **Servers:** Two backend servers required:
  - **Main** - Survival/gameplay server
  - **Limbo** - Purgatory server (optional if using `spectator` death mode)

> **⚠️ IMPORTANT:** Do **NOT** enable `hardcore=true` in `server.properties` on either server. Leave it as `false`. The plugin manages hardcore mechanics internally - enabling Minecraft's built-in hardcore mode will break the plugin. If you already have it on, either delete your world or search up how you edit the game files.

> **💡 Cosmetic Hearts:** The plugin can display hardcore-style hearts on clients (`hardcore-hearts: true` in config) without enabling actual hardcore mode.
> **This is inspired by https://github.com/cerus-mc/hardcore-hearts.**

## Proxy / Backend Installation

### Critical Setup Rules

**❌ DO NOT install on proxy:**
- PolarSouls is NOT a proxy plugin
- Install ONLY on backend servers (Main and Limbo)
- Do NOT place in Velocity/BungeeCord/Waterfall plugins folder

**✅ Install on both backend servers:**
- Main server (survival)
- Limbo server (purgatory)

### Player Information Forwarding

The proxy must forward player information to backends so UUIDs and addresses remain consistent:

**For Velocity:**
```toml
# In velocity.toml
player-info-forwarding-mode = "modern"
```

**For BungeeCord/Waterfall:**
- Enable IP forwarding in BungeeCord's `config.yml`
- Set `bungeecord: true` in `spigot.yml` on both backends
- Configure Paper forwarding if using Paper

### Network Configuration

1. Both backend servers must connect to the **same MySQL database**
2. Database credentials must be **identical** in both configs
3. Server names in config (`main-server-name` and `limbo-server-name`) must match your proxy configuration
4. Ensure firewalls allow backend-to-database connections

### Testing Your Setup

After configuration, test the complete flow:
1. Join Main server with a test account
2. Die enough times to lose all lives
3. Verify transfer to Limbo server
4. Use `/psadmin revive <player>` from Main server console
5. Verify automatic return to Main server

> **🐛 BungeeCord/Waterfall Status:** Untested but may work. Please [open an issue](https://github.com/SSoggyTacoBlud/PolarSouls-for-Hardcore/issues) if you test it!

## Installation

### Step 1: Download
Download the latest release (`PolarSouls-1.3.6.jar`) from the [Releases page](https://github.com/SSoggyTacoBlud/PolarSouls-for-Hardcore/releases).

### Step 2: Install Plugin
Place `PolarSouls-1.3.6.jar` in the `plugins/` folder of **both** servers:
- Main server: `/plugins/PolarSouls-1.3.6.jar`
- Limbo server: `/plugins/PolarSouls-1.3.6.jar`

### Step 3: Generate Config
Start both servers to generate default `config.yml` files. Stop them after generation.

### Step 4: Configure Database (BOTH servers)
Edit `config.yml` on **both servers** with **identical** database credentials:

```yaml
database:
  host: "localhost"
  port: 3306
  name: "polarsouls"        # Your database name
  username: "root"           # Your MySQL username
  password: "your_password"  # Your MySQL password
  pool-size: 5
  table-name: "hardcore_players"
```

**For Pterodactyl users:**
1. Go to your panel → Databases tab
2. Create a new database
3. Use the provided host, port, username, and password in config

### Step 5: Configure Server Roles

**On Main server (`config.yml`):**
```yaml
is-limbo-server: false
main-server-name: "main"    # Must match your Velocity config
limbo-server-name: "limbo"  # Must match your Velocity config
```

**On Limbo server (`config.yml`):**
```yaml
is-limbo-server: true
main-server-name: "main"    # Must match your Velocity config
limbo-server-name: "limbo"  # Must match your Velocity config
```

### Step 6: Set Limbo Spawn
1. Start the Limbo server
2. Join the Limbo server in-game
3. Stand where you want dead players to spawn
4. Run `/setlimbospawn`
5. Verify spawn location is saved in config

### Step 7: Restart & Test
1. Restart both servers
2. Check console for successful database connection
3. Test the complete death → Limbo → revival flow
4. Check console for any version mismatch warnings

### Step 8: Customize (Optional)
Adjust settings in `config.yml` to match your server's needs:
- Lives settings (`default`, `max-lives`, `on-revive`)
- Death mode (`limbo`, `spectator`, or `hybrid`)
- Grace period duration
- Extra Life recipe
- Messages and colors

The `config.yml` includes detailed comments explaining each setting. If you need help, feel free to reach out via [GitHub Issues](https://github.com/SSoggyTacoBlud/PolarSouls-for-Hardcore/issues).

## Configuration

PolarSouls is highly customizable. See the generated `config.yml` for extensive documentation and all options.

### Key Configuration Sections

#### Server Role
```yaml
# CRITICAL: Set this correctly!
is-limbo-server: false    # false on Main, true on Limbo

# Must match your Velocity/proxy config
main-server-name: "main"
limbo-server-name: "limbo"
```

#### Database
```yaml
database:
  host: "localhost"
  port: 3306
  name: "polarsouls"
  username: "root"
  password: "changeme"    # CHANGE THIS!
  pool-size: 5
  table-name: "hardcore_players"
```

#### Lives System
```yaml
lives:
  default: 2                      # Starting lives for new players
  max-lives: 5                    # Maximum lives cap
  on-revive: 1                    # Lives restored on revival
  grace-period: "24h"             # New player protection (0 to disable)
  revive-cooldown-seconds: 30     # Post-revival death protection
```

#### Death Mode
```yaml
main:
  death-mode: "hybrid"            # hybrid | spectator | limbo
  hybrid-timeout-seconds: 300     # Timeout for hybrid mode (5 min)
  spectator-on-death: false       # Put in spectator before Limbo
  detect-hrm-revive: true         # Auto-detect ritual structure revivals
  send-to-limbo-delay-ticks: 20   # Delay before Limbo transfer (1 sec)
```

#### HRM Features
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

#### Extra Life Item
```yaml
extra-life:
  enabled: true
  item-material: "NETHER_STAR"    # Icon in menus
  recipe:
    row1: "DED"    # D=Diamond, E=Emerald, D=Diamond
    row2: "INI"    # I=Netherite, N=Nether Star, I=Netherite
    row3: "GEG"    # G=Gold, E=Emerald, G=Gold
    ingredients:
      G: "GOLD_BLOCK"
      E: "EMERALD_BLOCK"
      N: "NETHER_STAR"
      D: "DIAMOND_BLOCK"
      I: "NETHERITE_INGOT"
```
**To customize:** Change letters in rows and define materials in ingredients. All Minecraft material names work!

#### Limbo Spawn
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
Use `/setlimbospawn` in-game to set this automatically.

#### Additional Options
```yaml
check-for-updates: true          # Check for new versions on Modrinth
hardcore-hearts: true            # Display hardcore hearts cosmetically
debug: false                     # Enable debug logging (dev only)
```

### Messages Customization

All messages support Minecraft color codes (`&a`, `&c`, `&l`, etc.):

```yaml
messages:
  prefix: "&8[&4☠&8] &r"
  death-life-lost: "&cYou lost a life! &7Remaining: &e%lives%"
  death-last-life: "&c&l⚠ FINAL WARNING! &cYou are on your last life. Be careful!"
  revive-success: "&a&l✦ REVIVED! &aReturning to the world of the living..."
  # ... many more customizable messages
```

Variables you can use:
- `%lives%` - Player's current lives
- `%player%` - Player name
- `%timeout%` - Timeout duration (hybrid mode)
- `%time_remaining%` - Grace period remaining
- `%max%` - Maximum lives


## Commands

### Player Commands

| Command | Description | Permission | Aliases |
|---------|-------------|------------|---------|
| `/pstatus [player]` | Check your or another player's lives, death status, and grace period | `polarsouls.status` (default: true) | - |
| `/revive <player>` | Revive a dead player and return them from Limbo | `polarsouls.revive` (default: op) | - |
| `/limbo` | Visit the Limbo server as a living player | `polarsouls.visit` (default: true) | `/visitlimbo` |
| `/leavelimbo` | Return from Limbo to Main (visitors only) | `polarsouls.visit` (default: true) | `/hub` |

### Admin Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/psadmin lives <player> <amount>` | Set a player's life count | `polarsouls.admin` |
| `/psadmin revive <player>` | Revive a dead player (same as `/revive`) | `polarsouls.admin` |
| `/psadmin kill <player>` | Force-kill a player (sets lives to 0, sends to Limbo) | `polarsouls.admin` |
| `/psadmin grace <player> <hours>` | Set custom grace period for a player | `polarsouls.admin` |
| `/psadmin reset <player>` | Reset player to defaults (default lives, clear grace) | `polarsouls.admin` |
| `/psadmin info <player>` | View detailed player data (UUID, lives, death state, timestamps) | `polarsouls.admin` |
| `/psadmin reload` | Reload configuration from disk | `polarsouls.admin` |
| `/psetlives <player> <amount>` | Legacy command - set player's lives (use `/psadmin lives` instead) | `polarsouls.admin` |
| `/setlimbospawn` | Set Limbo spawn to your current location | `polarsouls.admin` |

**Aliases:** `/psadmin` can also be used as `/psa`

### Command Examples

```bash
# Check a player's status
/pstatus SSoggyTacoBlud
# Output: SSoggyTacoBlud - Lives: 2 - Status: Alive

# Revive a dead player
/revive SSoggyTacoBlud
# or
/psadmin revive SSoggyTacoBlud

# Give a player extra lives
/psadmin lives SSoggyTacoBlud 5

# Set a 48-hour grace period
/psadmin grace NewPlayer 48

# Force-kill a player for testing
/psadmin kill TestPlayer

# View detailed player information
/psadmin info SSoggyTacoBlud
# Shows: UUID, username, lives, death status, join time, grace period, last seen

# Reset a player's data
/psadmin reset SSoggyTacoBlud
# Resets to default lives, clears grace period

# Reload configuration
/psadmin reload
```

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `polarsouls.admin` | Full admin access | op |
| `polarsouls.revive` | Can revive dead players | op |
| `polarsouls.status` | Can check player status | true |
| `polarsouls.visit` | Can visit Limbo as a living player | true |
| `polarsouls.bypass` | Bypass all death mechanics | false |


## Troubleshooting

### Players aren't being transferred to Limbo
- ✅ Check that `is-limbo-server` is set correctly on both servers (false on Main, true on Limbo)
- ✅ Verify both servers use identical database credentials
- ✅ Confirm `main-server-name` and `limbo-server-name` match your proxy config
- ✅ Check Velocity/proxy player forwarding is enabled
- ✅ Look for errors in console related to database connection or BungeeCord messaging

### Revivals aren't working
- ✅ Ensure HRM is enabled: `hrm.enabled: true` and `hrm.structure-revive: true`
- ✅ Check revival structure is built correctly (see structure guide above)
- ✅ Verify `detect-hrm-revive: true` in Main server config
- ✅ Try manual revival with `/revive <player>` to test database connectivity
- ✅ Check both servers can access the shared MySQL database

### Players lose lives during grace period
- ✅ Check grace period is configured: `grace-period: "24h"` (not `"0"`)
- ✅ Grace period counts only online time - verify with `/pstatus <player>`
- ✅ If player joined before enabling grace, use `/psadmin grace <player> <hours>` to set manually

### Version mismatch warnings
- ✅ Both servers MUST run the same PolarSouls version
- ✅ Download the same `.jar` file for both Main and Limbo
- ✅ Check console logs for version numbers
- ✅ Update both servers simultaneously

### Database connection errors
- ✅ Verify MySQL/MariaDB is running and accessible
- ✅ Test database credentials with MySQL client
- ✅ Check firewall rules allow backend servers to reach database
- ✅ For Pterodactyl: Use the database host from the panel, not "localhost"
- ✅ Ensure database exists (create it if needed)

### Players reconnecting go straight to Limbo (hybrid mode)
- ✅ This is intended behavior! In hybrid mode, disconnecting while dead skips spectator timeout
- ✅ If you want dead players to stay as spectators forever, use `death-mode: "spectator"`

### Extra Life items not working
- ✅ Confirm `extra-life.enabled: true`
- ✅ Check recipe is valid (all material names must be correct)
- ✅ Verify player doesn't have max lives already
- ✅ Can't use while dead, player must be alive

### Hardcore hearts not showing
- ✅ Enable in config: `hardcore-hearts: true`
- ✅ Requires client mod support or resource pack (cosmetic only)
- ✅ Does not affect gameplay - lives system works regardless

### Getting help
If issues persist:
1. Enable debug mode: `debug: true` in config
2. Restart servers and reproduce the issue
3. Check console logs for errors
4. [Open an issue](https://github.com/SSoggyTacoBlud/PolarSouls-for-Hardcore/issues) with:
   - PolarSouls version
   - Minecraft version
   - Proxy type (Velocity/BungeeCord)
   - Relevant config sections
   - Console errors/logs

## Update Checking

PolarSouls includes automatic update checking via Modrinth:
- Checks for new versions on plugin startup
- Displays update notifications in console
- Can be disabled: `check-for-updates: false`
- Does not auto-update - manual download required

## Credits

- **Revival mechanics** "inspired" by [Hardcore Revive Mod](https://modrinth.com/plugin/hardcore-revive-mod)
- **Author:** SSoggyTacoMan and GitHub Copilot for some bug fixes
- **License:** GPL-3.0

## Contributing

Issues, suggestions, and pull requests are welcome!
- Report bugs: [GitHub Issues](https://github.com/SSoggyTacoBlud/PolarSouls-for-Hardcore/issues)
- Feature requests: [GitHub Issues](https://github.com/SSoggyTacoBlud/PolarSouls-for-Hardcore/issues)

## License

This project is licensed under the GNU General Public License v3.0 (GPL-3.0).

See the [LICENSE](LICENSE) file for full details.
