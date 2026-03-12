---
layout: default
title: PolarSouls Documentation
description: Hardcore lives system plugin for Minecraft 1.21.X
---

# PolarSouls Documentation

Hardcore lives system plugin for Minecraft 1.21.X with Limbo exile, revival mechanics, and cross-server persistence.

[Modrinth](https://modrinth.com/project/Pb03qu6T){: .btn }
[GitHub](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore){: .btn }
[Releases](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/releases){: .btn }

## Overview

PolarSouls is designed for Velocity proxy networks and provides a high-stakes hardcore loop:

- Configurable lives with max cap and extra-life mechanics
- Three death modes: `hybrid`, `spectator`, `limbo`
- Ritual and command-based revival flows
- Automatic transfer between Main and Limbo servers
- MySQL/MariaDB persistence across backend servers

## Requirements

- Minecraft: 1.21.X (Spigot, Paper, or Purpur)
- Proxy: Velocity
- Database: MySQL 5.7+ or MariaDB 10.2+
- Java: 21+
- Architecture: Main server + Limbo server

> Do not enable `hardcore=true` in `server.properties`. Leave it `false` and let PolarSouls manage hardcore behavior.

## Documentation

- [Quick Start](quick-start)
- [Installation](installation)
- [Configuration](configuration)
- [Commands](commands)
- [Revival System](revival-system)
- [Troubleshooting](troubleshooting)
- [FAQ](faq)

## Quick Start Summary

1. Install the plugin on both backend servers.
2. Configure identical DB credentials on both servers.
3. Set `is-limbo-server` correctly on each server.
4. Ensure names match your `velocity.toml` server names.
5. Set Limbo spawn with `/setlimbospawn`.
6. Restart and verify with `/pstatus` and a revive test.

## Support

- [Issue Tracker](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/issues)
- [Project Repository](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore)
