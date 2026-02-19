---
layout: default
title: Quick Start Guide
---

# Quick Start Guide

This guide will help you get PolarSouls up and running in 8 simple steps. For more detailed instructions, see the [Installation Guide](installation).

## Prerequisites

Before you begin, ensure you have:
- Two Minecraft 1.21.X servers (Spigot/Paper/Purpur) behind a Velocity proxy
- MySQL 5.7+ or MariaDB 10.2+ database
- Java 21 or higher

## Installation Steps

### Step 1: Download

Download the latest `PolarSouls-1.3.6.jar` from:
- [GitHub Releases](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/releases)
- [Modrinth](https://modrinth.com/plugin/polarsouls)

### Step 2: Install Plugin

Place `PolarSouls-1.3.6.jar` in the `plugins/` folder of **both** servers:
- Main server: `/plugins/PolarSouls-1.3.6.jar`
- Limbo server: `/plugins/PolarSouls-1.3.6.jar`

> **Important:** Install on backend servers only, NOT on the Velocity proxy!

### Step 3: Generate Config

1. Start both servers to generate default `config.yml` files
2. Stop both servers after generation

You should see a `plugins/PolarSouls/config.yml` file on each server.

### Step 4: Configure Database

Edit `config.yml` on **both servers** with **identical** database credentials:

```yaml
database:
  host: "localhost"        # Your database host
  port: 3306               # Your database port
  name: "polarsouls"       # Your database name
  username: "root"         # Your MySQL username
  password: "your_password" # Your MySQL password
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
is-limbo-server: false        # This is the Main server
main-server-name: "main"      # Must match your Velocity config
limbo-server-name: "limbo"    # Must match your Velocity config
```

**On Limbo server (`config.yml`):**
```yaml
is-limbo-server: true         # This is the Limbo server
main-server-name: "main"      # Must match your Velocity config
limbo-server-name: "limbo"    # Must match your Velocity config
```

> **Tip:** The server names must exactly match the server names in your `velocity.toml` file.

### Step 6: Set Limbo Spawn

1. Start the Limbo server
2. Join the Limbo server in-game
3. Stand where you want dead players to spawn
4. Run `/setlimbospawn`
5. Verify the spawn location is saved in config

### Step 7: Configure Proxy Forwarding

**For Velocity:**

Edit `velocity.toml`:
```toml
player-info-forwarding-mode = "modern"
```

**For BungeeCord/Waterfall:**
- Enable IP forwarding in BungeeCord's `config.yml`
- Set `bungeecord: true` in `spigot.yml` on both backend servers
- Configure Paper forwarding if using Paper

### Step 8: Restart & Test

1. Restart both servers
2. Check console for successful database connection messages
3. Test the complete flow:
   - Join the Main server
   - Use `/pstatus` to check your lives
   - Kill yourself enough times to lose all lives
   - Verify you're transferred to Limbo
   - Use `/psadmin revive <player>` to revive yourself
   - Verify you're returned to Main

## What's Next?

### Customize Your Settings

Review and customize these important settings in `config.yml`:

- **Lives settings** (`default`, `max-lives`, `on-revive`)
- **Death mode** (`limbo`, `spectator`, or `hybrid`)
- **Grace period** duration for new players
- **Extra Life recipe** materials
- **Messages** and colors

See the [Configuration Reference](configuration) for details on all options.

### Learn About Features

- [Revival System](revival-system) - Learn how to revive players using ritual structures and items
- [Commands](commands) - Full list of available commands
- [Death Modes](configuration.md#death-modes) - Understand the three death mode options

### Troubleshooting

Having issues? Check the [Troubleshooting Guide](troubleshooting) for solutions to common problems.

## Quick Reference

### Death Modes

| Mode | Behavior |
|------|----------|
| **hybrid** (default) | Dead players get 5 minutes in spectator mode to be revived, then transferred to Limbo |
| **spectator** | Dead players stay on Main in spectator mode indefinitely until revived |
| **limbo** | Dead players immediately transferred to Limbo upon losing all lives |

### Essential Commands

| Command | Description |
|---------|-------------|
| `/pstatus [player]` | Check lives and status |
| `/revive <player>` | Revive a dead player |
| `/psadmin lives <player> <amount>` | Set player's lives |
| `/psadmin grace <player> <hours>` | Set grace period |
| `/setlimbospawn` | Set Limbo spawn point |

For the complete command list, see [Commands](commands).

## Need Help?

- [Full Installation Guide](installation)
- [Configuration Reference](configuration)
- [Troubleshooting](troubleshooting)
- [FAQ](faq)
- [Report Issues](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/issues)

---

[← Back to Home](index) | [Installation Guide →](installation)
