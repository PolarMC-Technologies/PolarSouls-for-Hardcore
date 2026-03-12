# PolarSouls

<img src="https://cdn.modrinth.com/data/Pb03qu6T/images/70ce5f45786d4716bb6d47d242ee3238a2b4ec4a.jpeg" alt="PolarSouls Banner">

**Version 1.3.6** | Minecraft 1.21.X | Spigot/Paper/Purpur

A hardcore lives system plugin for Velocity proxy networks. When you die enough times, you get exiled to a Limbo server until your teammates revive you.

> **[Read the Full Documentation →](https://polarmc-technologies.github.io/PolarSouls-for-Hardcore/)** - Complete installation guides, configuration reference, troubleshooting, and more!

---

## Features

- **Lives System** - start with 2 lives (configurable), max 5
- **Three Death Modes** - instant Limbo, permanent spectator, or hybrid timeout
- **Multiple Revival Methods** - ritual structures, Revive Skull, or commands
- **Grace Period** - newbies get protected time (only counts online time)
- **Extra Life Items** - craftable items for more lives (fully customizable)
- **Cross-Server** - MySQL syncs Main and Limbo
- **Auto Transfer** - dead go to Limbo, revived come back
- **Limbo Visiting** - living players can visit dead teammates

---

## How It Works

Two servers behind a **Velocity proxy** (might work on BungeeCord/Waterfall but not tested): **Main** (play) and **Limbo** (dead zone). Both talk to same MySQL database.

**Flow:**
1. Start with 2 lives (configurable)
2. Die = -1 life
3. Grace period protects newbies (only ticks online)
4. At 0 lives = depends on death mode
5. Revival methods: ritual structures, Revive Skull, or `/revive`
6. Revived = auto-teleport back to Main with lives
7. Living players can `/limbo` and `/leavelimbo`

---

## Death Modes

| Mode | What happens | Good for |
|------|----------|----------|
| **hybrid** (default) | Dead players chill in spectator on Main for 5 min. Team has that long or they get sent to Limbo. Log out = skip straight to Limbo. | Tension and urgency |
| **spectator** | Dead players stay on Main as spectators forever until revived. | Dead people watching team 24/7 |
| **limbo** | Dead = straight to Limbo. | Hardcore strict separation |

All work with ritual structures, Revive Skull, and `/revive`.

---

## Built-in Revival System
*(ripped from [Hardcore Revive Mod](https://modrinth.com/plugin/hardcore-revive-mod))*

### Player Head Drops
When you're fully dead, your head drops where you died with coords in chat.

### Revival Ritual
Build a 3x3x3 beacon-ish thing:

**Structure:**
- **Bottom:** 4 Soul Sand corners, 4 Stairs at edges, 1 Ore in middle
- **Middle:** 4 Wither Roses on Soul Sand, 1 Fence on ore
- **Top:** Dead player's head on fence = revival

Plugin auto-detects and revives them!

### Craftable Items

**Revive Skull** - right-click for a menu of dead players, get their head for rituals.

**Recipe:**
```
Obsidian | Ghast Tear | Obsidian
Totem    | Any Skull  | Totem
Obsidian | Ghast Tear | Obsidian
```

**Extra Life** - right-click for +1 life (max cap applies). Recipe is fully customizable!

**Default:**
```
Diamond Block  | Emerald Block | Diamond Block
Netherite Ingot| Nether Star   | Netherite Ingot
Gold Block     | Emerald Block | Gold Block
```

### Head Effects
Wear a dead player's head = Speed II and Night Vision.

---

## Requirements

- **Minecraft:** 1.21.X (Spigot, Paper, or Purpur)
- **Proxy:** Velocity (BungeeCord/Waterfall untested)
- **Database:** MySQL 5.7+ or MariaDB 10.2+
- **Java:** 21+
- **Servers:** Two backend (Main + Limbo)

> **Important:** Do NOT enable `hardcore=true` in `server.properties`. Keep it `false` - the plugin handles that.

---

## Installation

### Quick Start (8 Steps)

1. **Download** `PolarSouls-1.3.6.jar` from [Releases](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/releases)

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
   
   See the **Configuration** section below for key settings. If you need help, reach out via [GitHub Issues](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/issues).

8. **Restart & test** - Start both servers and test the full death → Limbo → revival flow!

### Proxy Configuration

**For Velocity:** Set `player-info-forwarding-mode = "modern"` in `velocity.toml`

**For BungeeCord/Waterfall:** Enable IP forwarding in `config.yml` and set `bungeecord: true` in `spigot.yml`

> **Note:** Install PolarSouls ONLY on backend servers, NOT on the proxy!

---

## Config

Check `config.yml` for everything. Key stuff:

```yaml
# server role (important!)
is-limbo-server: false    # false on Main, true on Limbo

# lives
lives:
  default: 2              # starting lives
  max-lives: 5            # cap
  on-revive: 1            # lives back on revival
  grace-period: "24h"     # newbie protection
  revive-cooldown-seconds: 30

# death mode
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
| `polarsouls.bypass-limbo-op-security` | Allow OPs to use admin commands on Limbo | false |

### Limbo OP Security

**Important:** OP users on the Limbo server are blocked from using `/revive` and `/psadmin` commands by default to prevent abuse.

**To allow an OP on Limbo (choose one):**
1. **Whitelist** (easiest - no permissions plugin needed):
   ```yaml
   limbo-trusted-admins:
     - "your-uuid-here"  # or username
   ```
2. **Permission**: `/lp user <player> permission set polarsouls.bypass-limbo-op-security true`
3. **Disable check**: `limbo-op-security-check: false` (not recommended)

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

**Need more help?**
- [Full Documentation Wiki](https://polarmc-technologies.github.io/PolarSouls-for-Hardcore/)
- [Troubleshooting Guide](https://polarmc-technologies.github.io/PolarSouls-for-Hardcore/troubleshooting.html)
- [FAQ](https://polarmc-technologies.github.io/PolarSouls-for-Hardcore/faq.html)
- [Open an issue](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/issues) with your version, Minecraft version, proxy type, and console errors

---

## Update Checking

PolarSouls includes automatic update checking via Modrinth:
- Checks for new versions on startup
- Displays notifications in console
- Can be disabled: `check-for-updates: false`
- Manual download required

---

## License

**GPL-3.0** - See [LICENSE](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/blob/main/LICENSE)

---

## Credits

- Revival mechanics inspired by [Hardcore Revive Mod](https://modrinth.com/plugin/hardcore-revive-mod)
- Author: SSoggyTacoMan
- GitHub: [polarmc-technologies/PolarSouls-for-Hardcore](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore)

---

**Enjoying PolarSouls?** Heart it on Modrinth and star the [GitHub repo](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore)!
