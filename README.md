# PolarSouls

![PolarSouls Banner](https://cdn.modrinth.com/data/Pb03qu6T/images/70ce5f45786d4716bb6d47d242ee3238a2b4ec4a.jpeg)

**Version 1.3.6** | [Modrinth](https://modrinth.com/project/Pb03qu6T) | [GitHub](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore)

A hardcore lives system plugin for Minecraft 1.21.X (Spigot/Paper/Purpur) designed for Velocity proxy networks. When you die enough times, you get sent to a Limbo server until your teammates bring you back.

> **[Complete Documentation Wiki →](https://polarmc-technologies.github.io/PolarSouls-for-Hardcore/)** - Installation guides, configuration reference, commands, troubleshooting, and more!
>
> **Quick Links:**
> - [Quick Start Guide](https://polarmc-technologies.github.io/PolarSouls-for-Hardcore/quick-start.html)
> - [Installation Guide](https://polarmc-technologies.github.io/PolarSouls-for-Hardcore/installation.html)
> - [Configuration Reference](https://polarmc-technologies.github.io/PolarSouls-for-Hardcore/configuration.html)
> - [Troubleshooting](https://polarmc-technologies.github.io/PolarSouls-for-Hardcore/troubleshooting.html)
> - [FAQ](https://polarmc-technologies.github.io/PolarSouls-for-Hardcore/faq.html)

## Features

**Lives System** - start with 2 lives (configurable), max out at 5

**Three Death Modes** - pick between instant Limbo exile, permanent spectator, or hybrid timeout

**Multiple Revival Methods** - ritual structures, Revive Skull item, or admin commands

**Grace Period** - new players get some breathing room to learn the ropes (only counts online time)

**Extra Life Items** - craft items to gain more lives (fully customizable recipe)

**Cross-Server Setup** - MySQL syncs everything between Main and Limbo servers

**Auto Transfer** - dead players go to Limbo, revived players come back to Main

**Limbo Visiting** - living players can visit Limbo to hang with dead teammates

## How It Works

Runs on two servers behind a Velocity proxy (might work on BungeeCord/Waterfall but not tested - let us know if you try it): **Main** (where you play) and **Limbo** (where you go when dead). Both servers talk to the same MySQL database.

**Basic flow:**
1. Start with 2 lives (configurable)
2. Each death = -1 life
3. Grace period protects newbies (only ticks down while online)
4. At 0 lives, what happens depends on your death mode (see below)
5. Teammates can revive you with:
   - Ritual structure (3x3x3 beacon with player head)
   - Revive Skull menu (craftable item to pick dead players)
   - `/revive <player>` command (needs permission)
   - Admin commands (`/psadmin revive`)
6. When revived, you auto-teleport back to Main with 1 life (configurable)
7. Revive cooldown (default 30 seconds) stops you from immediately dying again
8. Living players can `/limbo` to visit and `/leavelimbo` to return

## Death Modes

Pick the one that fits your server's vibe:

| Mode | What happens | Good for |
|------|----------|----------|
| **`hybrid`** (default) | Dead players chill in spectator on Main for 5 minutes. Team has that long to revive them or they get yeeted to Limbo. If they log out and back in, they skip spectator and go straight to Limbo. | Servers that want some tension - race against the clock vibes |
| **`spectator`** | Dead players stay on Main as spectators forever until revived. Never get sent to Limbo. | Servers where you want dead people watching their team 24/7 |
| **`limbo`** | Dead? Straight to Limbo. No spectator time. | Hardcore servers that want complete separation |

All modes work with all revival methods:
- HRM ritual structures
- Revive Skull item menu
- `/revive <player>` command
- Admin commands (`/psadmin revive`)

## Built-in HRM (Hardcore Revive Mode)
*(revival mechanics "borrowed" from [Hardcore Revive Mod](https://modrinth.com/plugin/hardcore-revive-mod))*

Comes with a full revival system:

### Player Head Drops
- When you lose all your lives, your head drops where you died
- You get coords in chat so your team knows where to look
- Heads are needed for the revival ritual
- Can disable this in `config.yml` (`hrm.drop-heads: false`)

### Revival Ritual Structure
Build a 3x3x3 beacon-ish structure to bring people back:

**How to build:**
- **Bottom (3x3):**
  - 4 Soul Sand at corners
  - 4 Stairs at edges (any stairs)
  - 1 Ore block in middle (Gold/Diamond/Emerald/whatever)
- **Middle:**
  - 4 Wither Roses on the Soul Sand corners
  - 1 Fence on the ore block
- **Top:**
  - Stick the dead player's head on the fence = revival triggered

**Features:**
- Auto-detects when done (saves to database)
- Can keep or destroy the structure after (`hrm.leave-structure-base`)
- Works with any ore block (make it harder/easier/prettier)
- Shows feedback when it works

Can disable in `config.yml` (`hrm.structure-revive: false`)

### Craftable Items

#### Revive Skull
Makes getting dead player heads way easier.

**Recipe (shapeless):**

| | | |
|---|---|---|
| Obsidian | Ghast Tear | Obsidian |
| Totem of Undying | Any Skull/Head | Totem of Undying |
| Obsidian | Ghast Tear | Obsidian |

**How to use:**
- Right-click to open a menu
- Shows all dead players
- Click one to get their head
- Use it in the revival ritual

Can disable in `config.yml` (`hrm.revive-skull-recipe: false`)

#### Extra Life Item
Craft to get +1 life (respects max cap).

**Default recipe:**

| | | |
|---|---|---|
| Diamond Block | Emerald Block | Diamond Block |
| Netherite Ingot | Nether Star | Netherite Ingot |
| Gold Block | Emerald Block | Gold Block |

**How to use:**
- Right-click to eat it and gain a life
- Can't use while dead
- Won't go over max lives (default 5)
- Shows success/failure message

**Fully customizable:**
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

Change letters in `row1/row2/row3` and define your own materials in `ingredients`. All Minecraft material names work.

### Head Wearing Effects
- Wear a dead player's head = get Speed II and Night Vision
- Handy for running to revival structures
- Can disable in `config.yml` (`hrm.head-wearing-effects: false`)

## Grace Period

New players get some time to learn before deaths start counting.

**How it works:**
- Configurable grace time (default 24 hours, set to `"0"` to disable)
- Only counts when online (pauses when offline)
- Deaths during grace don't cost lives
- Shows remaining time in messages
- Admins can set custom grace with `/psadmin grace <player> <hours>`

**Formats:**
- `"24h"` = 24 hours
- `"2h30m"` = 2 hours 30 minutes
- `"90m"` = 90 minutes
- `"0"` = disabled

**Example:** Player with 24h grace plays for 3 hours, logs off, comes back = 21h remaining.

## Limbo Visiting

Living players can check out Limbo using `/limbo`.

**How it works:**
- `/limbo` - teleport to Limbo (needs `polarsouls.visit` permission)
- `/leavelimbo` (or `/hub`) - come back to Main
- Living players can go whenever
- Dead players are stuck until revived
- Different welcome messages for visitors vs dead people

## Requirements

- **Minecraft:** 1.21.X (Spigot, Paper, or Purpur)
- **Proxy:** Velocity (BungeeCord/Waterfall might work but not tested)
- **Database:** MySQL 5.7+ or MariaDB 10.2+ (can share with other plugins like CoreProtect)
- **Java:** 21+
- **Servers:** Two backend servers (Main + Limbo)

> **Important:** Do NOT enable `hardcore=true` in `server.properties` on either server. Keep it `false`. The plugin handles hardcore stuff internally - if you turn on actual hardcore mode it'll break things. If you already did, either delete your world or look up how to edit the game files.

> **Cosmetic Hearts:** The plugin can display hardcore-style hearts on clients (`hardcore-hearts: true` in config) without enabling actual hardcore mode.
> **This is inspired by https://github.com/cerus-mc/hardcore-hearts.**

## Proxy / Backend Setup

### Important stuff

**Don't install on proxy:**
- This is NOT a proxy plugin
- Only put it on backend servers (Main and Limbo)
- Don't put it in Velocity/BungeeCord/Waterfall plugins folder

**Put it on both servers:**
- Main server (where you play)
- Limbo server (where dead people go)

### Player Info Forwarding

Your proxy needs to forward player info so UUIDs and IPs stay consistent:

**For Velocity:**
```toml
# in velocity.toml
player-info-forwarding-mode = "modern"
```

**For BungeeCord/Waterfall:**
- Enable IP forwarding in BungeeCord's `config.yml`
- Set `bungeecord: true` in `spigot.yml` on both servers
- Configure Paper forwarding if using Paper

### Network Setup

1. Both servers connect to the same MySQL database
2. Database credentials must match on both configs
3. Server names in config (`main-server-name` and `limbo-server-name`) must match your proxy config
4. Make sure firewalls let servers talk to database

### Testing

After setup, test everything:
1. Join Main with a test account
2. Die enough to lose all lives
3. Check that you got sent to Limbo
4. Run `/psadmin revive <player>` from Main console
5. Check that you got sent back to Main

> **Note:** BungeeCord/Waterfall not tested but might work. Let us know if you try it! [Open an issue](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/issues)

## Installation

### Step 1: Download
Download the latest release (`PolarSouls-1.3.6.jar`) from the [Releases page](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/releases).

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

The `config.yml` includes detailed comments explaining each setting. If you need help, feel free to reach out via [GitHub Issues](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/issues).

## Config

Highly customizable. Check the generated `config.yml` for everything.

### Important sections

#### Server role
```yaml
# super important - set this right!
is-limbo-server: false    # false on Main, true on Limbo

# must match your proxy config
main-server-name: "main"
limbo-server-name: "limbo"
```

#### Security
```yaml
# Block Limbo OPs from using admin commands (recommended: true)
limbo-op-security-check: true
```

Prevents OP users on the Limbo server from abusing `/revive` and `/psadmin` commands. OPs need the `polarsouls.bypass-limbo-op-security` permission to use admin commands on Limbo. See [Permissions](#permissions) for details.

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
/pstatus YourUsername
# Output: YourUsername - Lives: 2 - Status: Alive

# Revive a dead player
/revive YourUsername
# or
/psadmin revive YourUsername

# Give a player extra lives
/psadmin lives YourUsername 5

# Set a 48-hour grace period
/psadmin grace NewPlayer 48

# Force-kill a player for testing
/psadmin kill TestPlayer

# View detailed player information
/psadmin info YourUsername
# Shows: UUID, username, lives, death status, join time, grace period, last seen

# Reset a player's data
/psadmin reset YourUsername
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
| `polarsouls.bypass-limbo-op-security` | Allow OPs to use admin commands on Limbo server | false |

### Limbo OP Security

**Important:** By default, OP users on the Limbo server are blocked from using `/revive` and `/psadmin` commands to prevent abuse. This stops someone who only has OP on the Limbo server from messing with player data.

**How it works:**
- **Main server:** Admin commands work normally (OP or explicit permissions)
- **Limbo server:** OP users are blocked unless they're whitelisted or have bypass permission
- **Console:** Always works on both servers

**To allow an OP to use admin commands on Limbo (choose one):**

**Option 1: Whitelist (Easiest - no permissions plugin needed)**
```yaml
# In config.yml
limbo-trusted-admins:
  - "069a79f4-44e9-4726-a5be-fca90e38aaf5"  # Your UUID (recommended)
  - "YourUsername"                           # Or username
```
Then run `/psadmin reload` from the **console** or an already whitelisted/bypass-enabled admin, or restart the server, to apply changes.

**Option 2: Using LuckPerms**
```bash
/lp user <player> permission set polarsouls.bypass-limbo-op-security true
```

**Option 3: Remove OP and use explicit permissions**
```bash
/deop <player>
/lp user <player> permission set polarsouls.admin true
```

This security check can be disabled with `limbo-op-security-check: false` in config (not recommended).

## Troubleshooting

### Players aren't being transferred to Limbo
- Check that `is-limbo-server` is set correctly on both servers (false on Main, true on Limbo)
- Verify both servers use identical database credentials
- Confirm `main-server-name` and `limbo-server-name` match your proxy config
- Check Velocity/proxy player forwarding is enabled
- Look for errors in console related to database connection or BungeeCord messaging

### Revivals aren't working
- Ensure HRM is enabled: `hrm.enabled: true` and `hrm.structure-revive: true`
- Check revival structure is built correctly (see structure guide above)
- Verify `detect-hrm-revive: true` in Main server config
- Try manual revival with `/revive <player>` to test database connectivity
- Check both servers can access the shared MySQL database

### Players lose lives during grace period
- Check grace period is configured: `grace-period: "24h"` (not `"0"`)
- Grace period counts only online time - verify with `/pstatus <player>`
- If player joined before enabling grace, use `/psadmin grace <player> <hours>` to set manually

### Admin commands not working on Limbo server
- This is normal security behavior! OPs are blocked on Limbo by default
- Grant bypass permission: `/lp user <player> permission set polarsouls.bypass-limbo-op-security true`
- Or disable the security check: `limbo-op-security-check: false` in config (not recommended)
- Console commands always work
- See [Permissions](#permissions) section for full details

### Version mismatch warnings
- Both servers MUST run the same PolarSouls version
- Download the same `.jar` file for both Main and Limbo
- Check console logs for version numbers
- Update both servers simultaneously

### Database connection errors
- Verify MySQL/MariaDB is running and accessible
- Test database credentials with MySQL client
- Check firewall rules allow backend servers to reach database
- For Pterodactyl: Use the database host from the panel, not "localhost"
- Ensure database exists (create it if needed)

### Players reconnecting go straight to Limbo (hybrid mode)
- This is intended behavior! In hybrid mode, disconnecting while dead skips spectator timeout
- If you want dead players to stay as spectators forever, use `death-mode: "spectator"`

### Extra Life items not working
- Confirm `extra-life.enabled: true`
- Check recipe is valid (all material names must be correct)
- Verify player doesn't have max lives already
- Can't use while dead, player must be alive

### Hardcore hearts not showing
- Enable in config: `hardcore-hearts: true`
- Requires client mod support or resource pack (cosmetic only)
- Does not affect gameplay - lives system works regardless

### Getting help
If issues persist:
1. Enable debug mode: `debug: true` in config
2. Restart servers and reproduce the issue
3. Check console logs for errors
4. [Open an issue](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/issues) with:
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
- Report bugs: [GitHub Issues](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/issues)
- Feature requests: [GitHub Issues](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/issues)

## License

This project is licensed under the GNU General Public License v3.0 (GPL-3.0).

See the [LICENSE](LICENSE) file for full details.
