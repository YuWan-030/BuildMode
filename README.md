# BuildMode Plugin

[English](./README.md) | [简体中文](./README_zh-CN.md)

A Minecraft Forge mod for server administrators and builders, providing protected build regions, item whitelist management, and flexible permission support.

## Features

- **Build Regions**: Define protected areas where only authorized players can build, break, or use special items.
- **Item Whitelist**: Only whitelisted items can be used in build regions; restrict powerful or creative-only items outside regions.
- **Region Buffs**: Players inside build regions receive regeneration and absorption effects.
- **Mob Control**: Prevent hostile mob spawning and remove monsters in build regions.
- **Permission Integration**: Supports LuckPerms (auto fallback to OP check).
- **GUI & Commands**: In-game GUI for managing whitelisted items; commands for region management.
- **Configurable**: All regions and whitelist data stored in JSON files for easy editing.

## Commands

| Command | Description |
|---------|-------------|
| `/buildmode region add <name> <dimension> <x1> <y1> <z1> <x2> <y2> <z2>` | Add a build region |
| `/buildmode region add_here <name> <x2> <y2> <z2>` | Add region from current position |
| `/buildmode region remove <name> <dimension>` | Remove a region |
| `/buildmode region list` | List all regions |
| `/buildmode help` | Show help |

## Installation

1. Install Minecraft Forge (see Releases for supported versions).
2. Download the latest BuildMode JAR from GitHub.
3. Place the JAR in your server's `mods/` folder.
4. Start the server. Config files will be auto-generated.

## Configuration

- Regions: `config/buildmode/regions.json`
- Whitelist: `config/buildmode/whitelist.json`
- Mob removal list: `config/buildmode/region_mobs.json`

Example region config:
```json
[
  {
    "name": "spawn",
    "dimension": "minecraft:overworld",
    "x1": 0, "y1": 64, "z1": 0,
    "x2": 100, "y2": 80, "z2": 100
  }
]
```

## Development

- Fork and create feature branches.
- Follow Forge mod best practices.
- Test locally before PR.

## License

MIT License. See LICENSE for details.

## Credits

- Built on Minecraft Forge.
- LuckPerms integration.
- Maintained by YuWan-030.

For issues or suggestions, open a GitHub Issue!

`