---
layout: default
title: Installation Guide
---

# Installation Guide

This comprehensive guide covers everything you need to know to properly install and configure PolarSouls on your Velocity proxy network.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Network Architecture](#network-architecture)
3. [Database Setup](#database-setup)
4. [Plugin Installation](#plugin-installation)
5. [Proxy Configuration](#proxy-configuration)
6. [Server Configuration](#server-configuration)
7. [Testing Your Setup](#testing-your-setup)
8. [Common Mistakes to Avoid](#common-mistakes-to-avoid)

## Prerequisites

### Server Requirements

- **Minecraft Version:** 1.21.X
- **Server Software:** Spigot, Paper, or Purpur
- **Proxy Software:** Velocity (BungeeCord/Waterfall untested but may work)
- **Java Version:** 21 or higher
- **Database:** MySQL 5.7+ or MariaDB 10.2+

### Server Setup

You need **two backend servers**:
- **Main Server** - Your primary survival/gameplay server
- **Limbo Server** - Purgatory server for dead players

> **Note:** If using `spectator` death mode, the Limbo server is optional.

### Important: Do NOT Use Hardcore Mode

**CRITICAL:** Do **NOT** enable `hardcore=true` in `server.properties` on either server.

Leave it as `false`. The plugin manages hardcore mechanics internally - enabling Minecraft's built-in hardcore mode will break the plugin.

If you already have it enabled:
- Delete your world and regenerate, OR
- Use an NBT editor to change the hardcore flag in `level.dat`

## Network Architecture

### How PolarSouls Works

```
                    ┌─────────────────┐
                    │  Velocity Proxy │
                    └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
              │                             │
        ┌─────▼─────┐                 ┌─────▼─────┐
        │   Main    │◄───────────────►│   Limbo   │
        │  Server   │   MySQL/MariaDB │  Server   │
        └───────────┘                 └───────────┘
```

- Both servers connect to the same MySQL database
- Players automatically transfer between servers based on death state
- Limbo server checks database periodically for revivals
- Main server handles deaths and sends players to Limbo

## Database Setup

### Creating the Database

PolarSouls will automatically create the necessary table, but you need to create the database first.

**Using MySQL command line:**

```sql
CREATE DATABASE polarsouls CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'polarsouls_user'@'%' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON polarsouls.* TO 'polarsouls_user'@'%';
FLUSH PRIVILEGES;
```

**For shared hosting (Pterodactyl, etc.):**
1. Go to your hosting panel
2. Navigate to the Databases section
3. Create a new database
4. Note down the host, port, database name, username, and password

### Database Configuration

Both servers must use **identical** database credentials:

```yaml
database:
  host: "localhost"              # Database host (use panel host for Pterodactyl)
  port: 3306                     # Database port
  name: "polarsouls"             # Database name
  username: "polarsouls_user"    # Database username
  password: "your_secure_password" # Database password
  pool-size: 5                   # Connection pool size (5 is recommended)
  table-name: "hardcore_players" # Table name (default is fine)
```

> **Tip:** The database can be shared with other plugins like CoreProtect. PolarSouls uses its own table.

### Connection Pool Settings

The `pool-size` setting controls how many database connections are maintained:
- **Small servers (1-20 players):** 5 connections
- **Medium servers (20-50 players):** 10 connections
- **Large servers (50+ players):** 15-20 connections

## Plugin Installation

### Download

Get the latest version:
- [GitHub Releases](https://github.com/SSoggyTacoBlud/PolarSouls-for-Hardcore/releases)
- [Modrinth](https://modrinth.com/plugin/polarsouls)

Download `PolarSouls-1.3.6.jar` (or latest version).

### Installation Steps

1. **Stop both servers** if they're running

2. **Place the JAR file** in the `plugins/` folder of both servers:
   ```
   Main Server:  /plugins/PolarSouls-1.3.6.jar
   Limbo Server: /plugins/PolarSouls-1.3.6.jar
   ```

3. **Start both servers** to generate config files

4. **Stop both servers** to edit configuration

5. **Edit `config.yml`** on both servers (see next section)

### Critical Setup Rules

**DO NOT install on proxy:**
- PolarSouls is NOT a proxy plugin
- Install ONLY on backend servers (Main and Limbo)
- Do NOT place in Velocity/BungeeCord plugins folder

**DO install on both backend servers:**
- Must be installed on Main server
- Must be installed on Limbo server
- Both servers must use the SAME version

## Proxy Configuration

### For Velocity

Edit `velocity.toml`:

```toml
# Enable modern forwarding
player-info-forwarding-mode = "modern"

[servers]
  main = "localhost:25566"   # Your Main server
  limbo = "localhost:25567"  # Your Limbo server

try = [
  "main"  # Players join Main by default
]
```

### For BungeeCord/Waterfall

> **Note:** BungeeCord/Waterfall support is untested. Please [report](https://github.com/SSoggyTacoBlud/PolarSouls-for-Hardcore/issues) if you test it!

**In BungeeCord `config.yml`:**
```yaml
ip_forward: true
```

**On both backend servers in `spigot.yml`:**
```yaml
settings:
  bungeecord: true
```

**If using Paper, also configure `paper.yml`:**
```yaml
settings:
  velocity-support:
    enabled: false  # Disable for BungeeCord
```

## Server Configuration

### Main Server Configuration

Edit `/plugins/PolarSouls/config.yml` on the **Main server**:

```yaml
# SERVER IDENTIFICATION
is-limbo-server: false           # This is the Main server

# SERVER NAMES (must match velocity.toml)
main-server-name: "main"         # Name of Main server in proxy config
limbo-server-name: "limbo"       # Name of Limbo server in proxy config

# DATABASE (must be identical on both servers)
database:
  host: "localhost"
  port: 3306
  name: "polarsouls"
  username: "polarsouls_user"
  password: "your_secure_password"
  pool-size: 5
  table-name: "hardcore_players"

# LIVES SYSTEM
lives:
  default: 2                     # Starting lives for new players
  max-lives: 5                   # Maximum lives cap
  on-revive: 1                   # Lives restored on revival
  grace-period: "24h"            # New player protection
  revive-cooldown-seconds: 30    # Post-revival protection

# DEATH HANDLING
main:
  death-mode: "hybrid"           # hybrid | spectator | limbo
  hybrid-timeout-seconds: 300    # 5 minutes for hybrid mode
  spectator-on-death: false
  detect-hrm-revive: true        # Enable ritual structure detection
  send-to-limbo-delay-ticks: 20  # 1 second delay before transfer
```

### Limbo Server Configuration

Edit `/plugins/PolarSouls/config.yml` on the **Limbo server**:

```yaml
# SERVER IDENTIFICATION
is-limbo-server: true            # This is the Limbo server

# SERVER NAMES (must match velocity.toml and Main server config)
main-server-name: "main"
limbo-server-name: "limbo"

# DATABASE (must be identical to Main server)
database:
  host: "localhost"
  port: 3306
  name: "polarsouls"
  username: "polarsouls_user"
  password: "your_secure_password"
  pool-size: 5
  table-name: "hardcore_players"

# LIMBO SETTINGS
limbo:
  check-interval-seconds: 3      # How often to check for revivals
  spawn:
    world: "world"               # Set with /setlimbospawn command
    x: 0.5
    y: 65.0
    z: 0.5
    yaw: 0.0
    pitch: 0.0
```

### Setting Limbo Spawn

1. Start the Limbo server
2. Join the server in-game
3. Stand at the desired spawn location
4. Run `/setlimbospawn`
5. Confirm the coordinates were saved

The spawn location is saved to config and will be used for all dead players.

## Testing Your Setup

### Pre-Flight Checklist

Before testing, verify:
- [ ] Both servers are running PolarSouls (same version)
- [ ] Both servers have identical database credentials
- [ ] `is-limbo-server` is set correctly (false on Main, true on Limbo)
- [ ] Server names match proxy configuration
- [ ] Limbo spawn has been set
- [ ] Console shows successful database connection on both servers

### Test Procedure

1. **Join Main Server**
   - Connect through the proxy
   - You should spawn on the Main server

2. **Check Initial Status**
   ```
   /pstatus
   ```
   Should show: "Lives: 2 - Status: Alive"

3. **Test Death Mechanic**
   - Kill yourself (fall, lava, etc.)
   - Check lives with `/pstatus`
   - Should show: "Lives: 1 - Status: Alive"

4. **Test Limbo Transfer**
   - Kill yourself again to lose all lives
   - Depending on death mode:
     - **hybrid:** Enter spectator for 5 minutes, then transfer to Limbo
     - **spectator:** Enter spectator on Main indefinitely
     - **limbo:** Immediately transfer to Limbo
   - Verify you're on the Limbo server

5. **Test Revival**
   - From Main server console, run:
     ```
     /psadmin revive <your_username>
     ```
   - Should automatically return to Main server
   - Check lives: Should have 1 life restored

6. **Test Grace Period**
   - Create a new player account
   - Check status: `/pstatus`
   - Should show grace period remaining
   - Die several times
   - Lives should not decrease during grace period

### Verification

If all tests pass, your installation is complete! 

Check console logs for:
- "Database connection established successfully"
- "PolarSouls version X.X.X enabled"
- No error messages or warnings

## Common Mistakes to Avoid

### Mistake 1: Installing on Proxy
**Wrong:** Installing PolarSouls on Velocity/BungeeCord
**Right:** Install only on backend servers (Main and Limbo)

### Mistake 2: Different Database Credentials
**Wrong:** Using different database credentials on each server
**Right:** Both servers must use identical database settings

### Mistake 3: Server Names Don't Match
**Wrong:** Config says "main" but velocity.toml says "survival"
**Right:** Server names must exactly match proxy configuration

### Mistake 4: Wrong is-limbo-server Setting
**Wrong:** Both servers have `is-limbo-server: false`
**Right:** Main = false, Limbo = true

### Mistake 5: Enabling Hardcore Mode
**Wrong:** Setting `hardcore=true` in `server.properties`
**Right:** Keep `hardcore=false` - plugin manages hardcore mechanics

### Mistake 6: Different Plugin Versions
**Wrong:** Main server has v1.3.5, Limbo has v1.3.6
**Right:** Both servers must use the exact same version

### Mistake 7: Not Setting Limbo Spawn
**Wrong:** Forgetting to run `/setlimbospawn`
**Right:** Set spawn point before testing

### Mistake 8: Firewall Blocking Database
**Wrong:** Servers can't reach database due to firewall
**Right:** Ensure firewall allows backend → database connections

## Next Steps

Now that PolarSouls is installed:

1. **Customize Settings** - Review the [Configuration Reference](configuration)
2. **Learn Commands** - Check out the [Commands Guide](commands)
3. **Understand Revival System** - Read the [Revival System Guide](revival-system)
4. **Prepare for Issues** - Familiarize yourself with [Troubleshooting](troubleshooting)

## Need Help?

- [Configuration Reference](configuration)
- [Troubleshooting Guide](troubleshooting)
- [FAQ](faq)
- [Report Issues](https://github.com/SSoggyTacoBlud/PolarSouls-for-Hardcore/issues)

---

[← Quick Start](quick-start) | [Back to Home](index) | [Configuration →](configuration)
