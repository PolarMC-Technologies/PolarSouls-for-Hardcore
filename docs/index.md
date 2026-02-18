---
layout: default
title: Home
---

# PolarSouls Documentation

![PolarSouls Banner](https://cdn.modrinth.com/data/Pb03qu6T/images/70ce5f45786d4716bb6d47d242ee3238a2b4ec4a.jpeg)

**Version 1.3.6** | [Modrinth](https://modrinth.com/plugin/polarsouls) | [GitHub](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore)

Welcome to the comprehensive documentation for PolarSouls, a sophisticated hardcore lives system plugin for Minecraft 1.21.X (Spigot/Paper/Purpur) designed for Velocity proxy networks.

## What is PolarSouls?

PolarSouls features a lives-based death mechanic where players are exiled to a Limbo server upon losing all lives, with multiple revival methods to bring them back. The plugin runs on two backend servers behind a proxy: **Main** (survival) and **Limbo** (purgatory), sharing a MySQL database for synchronized player state.

## Key Features

**Lives System** - Configurable starting lives (default: 2) with maximum cap (default: 5)

**Three Death Modes** - Choose between immediate Limbo exile, permanent spectator, or hybrid timeout

**Multiple Revival Methods** - Ritual structures, Revive Skull item, or admin commands

**Grace Period Protection** - New players get protected time to learn (counts only online time)

**Extra Life Items** - Craftable items to gain additional lives (fully customizable recipe)

**Cross-Server Architecture** - MySQL-backed persistence across Main and Limbo servers

**Automatic Transfer** - Dead players sent to Limbo, revived players return to Main automatically

**Limbo Visiting** - Alive players can visit Limbo to interact with dead teammates

## Documentation Structure

This documentation is organized to help you get started quickly and find advanced information when you need it:

### [Quick Start Guide →](quick-start)
Get PolarSouls up and running in 8 simple steps. Perfect for first-time setup.

### [Installation Guide →](installation)
Detailed installation instructions including proxy setup, network configuration, and testing procedures.

### [Configuration Reference →](configuration)
Complete reference for all configuration options with examples and best practices.

### [Commands →](commands)
Full list of player and admin commands with examples and permission details.

### [Revival System →](revival-system)
In-depth guide to the HRM (Hardcore Revive Mode) system including ritual structures, craftable items, and revival mechanics.

### [Troubleshooting →](troubleshooting)
Solutions to common issues and problems you might encounter.

### [FAQ →](faq)
Frequently asked questions about PolarSouls features and functionality.

## Requirements

- **Minecraft:** 1.21.X (Spigot, Paper, or Purpur)
- **Proxy:** Velocity (BungeeCord/Waterfall might work but untested)
- **Database:** MySQL 5.7+ or MariaDB 10.2+
- **Java:** 21 or higher
- **Servers:** Two backend servers (Main + Limbo)

> **Important:** Do **NOT** enable `hardcore=true` in `server.properties`. Leave it as `false` - the plugin manages hardcore mechanics internally.

## Quick Links

- [Download from Modrinth](https://modrinth.com/plugin/polarsouls)
- [Download from GitHub Releases](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/releases)
- [Report Issues](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/issues)
- [GitHub Discussions](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/discussions)

## Getting Started

New to PolarSouls? Start with the [Quick Start Guide](quick-start) to get your server running in minutes!

Already familiar with the basics? Jump to specific sections using the navigation above.

## Support

Need help? Here's how to get support:

1. Check the [Troubleshooting](troubleshooting) page for common issues
2. Review the [FAQ](faq) for frequently asked questions
3. Search [existing issues](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/issues) on GitHub
4. [Open a new issue](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/issues/new) with detailed information

## Contributing

Issues, suggestions, and pull requests are welcome! Visit our [GitHub repository](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore) to contribute.

## License

PolarSouls is licensed under the GNU General Public License v3.0 (GPL-3.0). See the [LICENSE](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore/blob/main/LICENSE) file for details.

---

**Enjoying PolarSouls?** Heart it on [Modrinth](https://modrinth.com/plugin/polarsouls) and star the [GitHub repo](https://github.com/polarmc-technologies/PolarSouls-for-Hardcore)!
