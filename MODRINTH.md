# PolarSouls

<img src="https://cdn.modrinth.com/data/Pb03qu6T/images/70ce5f45786d4716bb6d47d242ee3238a2b4ec4a.jpeg" alt="PolarSouls Banner">

**Version 1.3.6** | Minecraft 1.21.X | Spigot/Paper/Purpur

A comprehensive hardcore lives system plugin designed for Velocity proxy networks. When players lose all their lives, they're exiled to a Limbo server until teammates revive them through various methods.

---

## Features

- **Lives System** - Configurable starting lives (default: 2) with maximum cap (default: 5)
- **Three Death Modes** - Choose between immediate Limbo exile, permanent spectator, or hybrid timeout
- **Multiple Revival Methods** - Ritual structures, Revive Skull item, or admin commands
- **Grace Period Protection** - New players get protected time to learn (counts only online time)
- **Extra Life Items** - Craftable items to gain additional lives (fully customizable recipe)
- **Cross-Server Architecture** - MySQL-backed persistence across Main and Limbo servers
- **Automatic Transfer** - Dead players sent to Limbo, revived players return to Main automatically
- **Limbo Visiting** - Alive players can visit Limbo to interact with dead teammates

---

## How It Works

The plugin runs on two backend servers behind a **Velocity proxy** (might work on BungeeCord/Waterfall, not tested yet): **Main** (survival) and **Limbo** (purgatory). Both share a MySQL database.

**Basic Flow:**
1. Players start with configurable lives (default: 2)
2. Each death costs one life
3. **Grace period** protects new players from losing lives (counts only online time)
4. At 0 lives, behavior depends on **death mode** (see below)
5. Teammates can revive using ritual structures, Revive Skull, or `/revive` command
6. Revived players automatically return to Main with restored lives
7. Alive players can visit Limbo with `/limbo` and return with `/leavelimbo`

---

## Death Modes

| Mode | Behavior | Best For |
|------|----------|----------|
| **hybrid** (default) | Dead players enter spectator mode on Main for 5 minutes. Teammates must revive them during this window, or they're auto-transferred to Limbo. Disconnecting skips spectator and goes straight to Limbo. | Servers wanting tension and urgency |
| **spectator** | Dead players stay on Main in spectator mode indefinitely until revived. Never auto-transfer to Limbo. | Servers preferring dead players to spectate constantly |
| **limbo** | Dead players immediately transferred to Limbo upon losing all lives. | Hardcore servers enforcing strict separation |

All modes support reviving via ritual structures, Revive Skull, and `/revive` command.

---

## Built-in Revival System
*(Inspired by [Hardcore Revive Mod](https://modrinth.com/plugin/hardcore-revive-mod))*

### Player Head Drops
On final death, the player's head drops at their death location with a death coordinates message.

### Revival Ritual Structure
Build a 3x3x3 beacon-style structure to revive dead players:

**Structure:**
- **Bottom layer:** 4 Soul Sand corners, 4 Stair blocks at edges, 1 Ore block center
- **Middle layer:** 4 Wither Roses on Soul Sand, 1 Fence on ore block
- **Top:** Place dead player's head on the fence to trigger revival

The plugin auto-detects when the structure is completed and revives the player!

### Craftable Items

**Revive Skull** - Right-click to open a menu of dead players and receive their head for the ritual.

**Crafting Recipe:**
```
Obsidian | Ghast Tear | Obsidian
Totem    | Any Skull  | Totem
Obsidian | Ghast Tear | Obsidian
```

**Extra Life** - Right-click to gain +1 life (max cap applies). Recipe is fully customizable!

**Default Recipe:**
```
Diamond Block  | Emerald Block | Diamond Block
Netherite Ingot| Nether Star   | Netherite Ingot
Gold Block     | Emerald Block | Gold Block
```

### Head Wearing Effects
Wearing a dead player's head grants **Speed II** and **Night Vision** - useful for carrying heads to revival structures!

---

## 📋 Requirements

- **Minecraft:** 1.21.X (Spigot, Paper, or Purpur)
- **Proxy:** Velocity (BungeeCord/Waterfall untested)
- **Database:** MySQL 5.7+ or MariaDB 10.2+
- **Java:** 21 or higher
- **Servers:** Two backend servers (Main + Limbo)

> ⚠️ **Important:** Do **NOT** enable `hardcore=true` in `server.properties`. Leave it as `false` - the plugin manages hardcore mechanics internally.

---

## Installation

### Quick Start (8 Steps)

1. **Download** `PolarSouls-1.3.6.jar` from [Releases](https://github.com/SSoggyTacoBlud/PolarSouls-for-Hardcore/releases)

2. **Install** the JAR in `plugins/` folder of **both** servers (Main and Limbo)

3. **Generate config** - Start both servers to create `config.yml`, then stop them

4. **Configure database** - Edit `config.yml` on **both servers** with **identical** credentials:
   ```yaml
   database:
     host: "localhost"
     port: 3306
     name: "polarsouls"
     username: "root"
     password: "your_password"
   ```

5. **Set server roles:**
   - Main server: `is-limbo-server: false`
   - Limbo server: `is-limbo-server: true`
   - Both: Set `main-server-name` and `limbo-server-name` to match your Velocity config

6. **Set Limbo spawn** - Join Limbo server, stand at spawn point, run `/setlimbospawn`

7. **Configure remaining options** - The `config.yml` has detailed comments explaining each setting. Review and customize:
   - Lives system (default lives, max cap, grace period)
   - Death mode (hybrid/spectator/limbo)
   - HRM features (revival structures, heads, recipes)
   - Messages and colors
   
   See the **Configuration** section below for key settings. If you need help, reach out via [GitHub Issues](https://github.com/SSoggyTacoBlud/PolarSouls-for-Hardcore/issues).

8. **Restart & test** - Start both servers and test the full death → Limbo → revival flow!

### Proxy Configuration

**For Velocity:** Set `player-info-forwarding-mode = "modern"` in `velocity.toml`

**For BungeeCord/Waterfall:** Enable IP forwarding in `config.yml` and set `bungeecord: true` in `spigot.yml`

> 📝 **Note:** Install PolarSouls ONLY on backend servers, NOT on the proxy!

---

## Configuration

See the generated `config.yml` for all options with detailed comments. Key settings:

```yaml
# Server role (CRITICAL!)
is-limbo-server: false    # false on Main, true on Limbo

# Lives settings
lives:
  default: 2              # Starting lives
  max-lives: 5            # Maximum cap
  on-revive: 1            # Lives restored on revival
  grace-period: "24h"     # New player protection
  revive-cooldown-seconds: 30

# Death mode
main:
  death-mode: "hybrid"    # hybrid | spectator | limbo
  hybrid-timeout-seconds: 300

# HRM features
hrm:
  enabled: true
  drop-heads: true
  structure-revive: true
  head-wearing-effects: true
  revive-skull-recipe: true

# Extra Life recipe (fully customizable!)
extra-life:
  enabled: true
  recipe:
    row1: "DED"    # D=Diamond, E=Emerald
    row2: "INI"    # I=Netherite, N=Nether Star
    row3: "GEG"    # G=Gold
    ingredients:
      G: "GOLD_BLOCK"
      E: "EMERALD_BLOCK"
      N: "NETHER_STAR"
      D: "DIAMOND_BLOCK"
      I: "NETHERITE_INGOT"
```

All messages are customizable with Minecraft color codes!

---

## Commands

### Player Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/pstatus [player]` | Check lives and status | `polarsouls.status` |
| `/revive <player>` | Revive a dead player | `polarsouls.revive` |
| `/limbo` | Visit Limbo server | `polarsouls.visit` |
| `/leavelimbo` | Return from Limbo | `polarsouls.visit` |

### Admin Commands
| Command | Description |
|---------|-------------|
| `/psadmin lives <player> <amount>` | Set player's lives |
| `/psadmin revive <player>` | Revive a player |
| `/psadmin kill <player>` | Force-kill a player |
| `/psadmin grace <player> <hours>` | Set grace period |
| `/psadmin reset <player>` | Reset player data |
| `/psadmin info <player>` | View player details |
| `/psadmin reload` | Reload config |
| `/setlimbospawn` | Set Limbo spawn point |

**Aliases:** `/psadmin` = `/psa`, `/leavelimbo` = `/hub`, `/limbo` = `/visitlimbo`

---

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `polarsouls.admin` | Full admin access | op |
| `polarsouls.revive` | Can revive dead players | op |
| `polarsouls.status` | Can check player status | true |
| `polarsouls.visit` | Can visit Limbo as living player | true |
| `polarsouls.bypass` | Bypass all death mechanics | false |

---

## Troubleshooting

**Players not transferring to Limbo?**
- Check `is-limbo-server` is set correctly on both servers
- Verify both servers use identical database credentials
- Confirm server names match proxy config

**Revivals not working?**
- Ensure `hrm.enabled: true` and `hrm.structure-revive: true`
- Verify revival structure is built correctly
- Check `detect-hrm-revive: true` in Main config

**Version mismatch warnings?**
- Both servers MUST run the same PolarSouls version
- Update both simultaneously

**Need more help?** [Open an issue on GitHub](https://github.com/SSoggyTacoBlud/PolarSouls-for-Hardcore/issues) with:
- PolarSouls version
- Minecraft version
- Proxy type
- Console errors

---

## Update Checking

PolarSouls includes automatic update checking via Modrinth:
- Checks for new versions on startup
- Displays notifications in console
- Can be disabled: `check-for-updates: false`
- Manual download required

---

## License

**GPL-3.0** - See [LICENSE](https://github.com/SSoggyTacoBlud/PolarSouls-for-Hardcore/blob/main/LICENSE)

---

## Credits

- Revival mechanics inspired by [Hardcore Revive Mod](https://modrinth.com/plugin/hardcore-revive-mod)
- Author: SSoggyTacoMan
- GitHub: [SSoggyTacoBlud/PolarSouls-for-Hardcore](https://github.com/SSoggyTacoBlud/PolarSouls-for-Hardcore)

---

**Enjoying PolarSouls?** Heart it on Modrinth and star the [GitHub repo](https://github.com/SSoggyTacoBlud/PolarSouls-for-Hardcore)!
