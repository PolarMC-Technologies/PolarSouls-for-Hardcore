# PolarSouls

Spigot/Paper/Purpur plugin for Minecraft 1.21.1 that adds a lives system to a Velocity proxy network. When players lose all their lives, they get sent to a Limbo server until teammates revive them.

## How It Works

The plugin runs on two backend servers behind a Velocity proxy: **Main** (survival) and **Limbo** (purgatory). Both share a MySQL database.

- Players start with configurable lives (default: 2)
- Each death costs one life; optional grace period for new players
- At 0 lives, dead players enter spectator briefly, then get sent to Limbo
- Teammates revive dead players via the ritual structure, `/revive`, or the Revive Skull
- Revived players get sent back to Main automatically
- Alive players can visit Limbo with `/limbo` and return with `/leavelimbo`

## Death Modes

| Mode | Behavior |
|------|----------|
| **`hybrid`** (default) | Spectator for a time window, then transferred to Limbo. Teammates can revive during the window. Reconnecting while dead skips the spectator window and sends straight to Limbo. |
| **`spectator`** | Dead players stay on Main as spectators permanently until revived. |
| **`limbo`** | Dead players are immediately transferred to Limbo. |

All modes support reviving via the ritual structure, Revive Skull, and `/revive`.

## Built-in HRM Features

### Player Head Drops
On final death, the player's head drops at their death location. They also get a message with their death coords.

### Revival Ritual Structure
3x3x3 structure to revive dead players:

- **Bottom layer (3x3):** 4 Soul Sand corners, 4 Stair edges, 1 beacon ore center
- **Middle layer:** 4 Wither Roses above the soul sand, 1 Fence on the ore
- **Top:** Place the dead player's head on the fence

Lightning strikes and the player revives with Resistance V and Glowing. Structure partially breaks after use.

### Wearing Head Effects
Wearing a player head in the helmet slot gives Nausea I (10s on equip), Slowness I, Health Boost V, and Resistance I while worn.

### Craftable Items

**Revive Skull** — Right-click to open a menu of dead players and receive their head for the ritual.

| | | |
|---|---|---|
| Obsidian | Ghast Tear | Obsidian |
| Totem of Undying | Any Skull | Totem of Undying |
| Obsidian | Ghast Tear | Obsidian |

**Extra Life** — Right-click to gain +1 life. Recipe is fully configurable in `config.yml`.

### Limbo Visiting
Alive players can visit Limbo using `/limbo` to interact with dead players. Use `/leavelimbo` to return. Dead players cannot leave until revived.

## Requirements

- Minecraft 1.21.1 (Spigot or Paper)
- Velocity proxy with BungeeCord forwarding enabled
- MySQL database
- Java 21+
- Two backend servers: Main + Limbo (Limbo optional in `spectator` mode)

> **Note:** You do **not** need `hardcore=true` in `server.properties`. Leave it as `false` on both servers.

## Installation

1. Place `PolarSouls-1.0.0.jar` in the `plugins/` folder of **both** servers
2. Start both servers to generate `config.yml`
3. Configure the database connection (same credentials on both)
4. On the Limbo server, set `is-limbo-server: true`
5. Set the spawn in-game with `/setlimbospawn`
6. Restart both servers

## Configuration

See the generated `config.yml` for all options. Key settings:

```yaml
is-limbo-server: false           # true on Limbo server only

database:
  host: localhost
  port: 3306
  name: polarsouls
  username: root
  password: password

lives:
  default: 2
  max-lives: 5
  on-revive: 1
  grace-period-hours: 24
  revive-cooldown-seconds: 30

main:
  death-mode: "hybrid"           # hybrid | spectator | limbo
  hybrid-timeout-seconds: 300

hrm:
  enabled: true
  drop-heads: true
  structure-revive: true
  head-wearing-effects: true
  revive-skull-recipe: true

extra-life:
  enabled: true

main-server-name: "main"
limbo-server-name: "limbo"
```

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/psadmin <sub> [args]` | Admin commands (lives, grace, kill, revive, reset, info, reload) | `polarsouls.admin` |
| `/revive <player>` | Revive a dead player | `polarsouls.revive` |
| `/pstatus [player]` | Check lives and status | `polarsouls.status` |
| `/psetlives <player> <n>` | Set a player's lives | `polarsouls.admin` |
| `/setlimbospawn` | Set the Limbo spawn point | `polarsouls.admin` |
| `/limbo` | Visit the Limbo server | `polarsouls.visit` |
| `/leavelimbo` | Return from Limbo to Main | `polarsouls.visit` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `polarsouls.admin` | Full admin access | op |
| `polarsouls.revive` | Can revive dead players | op |
| `polarsouls.status` | Can check player status | true |
| `polarsouls.visit` | Can visit Limbo as a living player | true |
| `polarsouls.bypass` | Bypass all death mechanics | false |

## Building

```bash
mvn clean package
```

Output: `target/PolarSouls-1.0.0-shaded.jar`

## License

MIT
