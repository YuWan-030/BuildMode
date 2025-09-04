# BuildMode 模组

[English](./README.md) | [简体中文](./README_zh-CN.md)

一个为服务器管理员和建筑玩家设计的 Minecraft Forge 模组，支持建筑区域保护、物品白名单管理和灵活权限集成。

## 主要功能

- **建筑区域**：定义受保护的区域，仅授权玩家可在区域内建筑、破坏或使用特殊物品。
- **物品白名单**：仅白名单物品可在建筑区域内使用，限制强力或创造专属物品在区域外使用。
- **区域增益**：玩家进入建筑区域自动获得生命恢复和伤害吸收效果。
- **怪物控制**：建筑区域内禁止刷怪并自动清理怪物。
- **权限支持**：兼容 LuckPerms（自动降级为 OP 检查）。
- **GUI 与指令**：游戏内 GUI 管理白名单物品，指令管理区域。
- **可配置**：所有区域和白名单数据均存储为 JSON 文件，便于手动编辑。

## 指令

| 指令 | 说明 |
|------|------|
| `/buildmode region add <名称> <维度> <x1> <y1> <z1> <x2> <y2> <z2>` | 添加建筑区域 |
| `/buildmode region add_here <名称> <x2> <y2> <z2>` | 以当前位置为起点添加区域 |
| `/buildmode region remove <名称> <维度>` | 移除区域 |
| `/buildmode region list` | 查看所有区域 |
| `/buildmode help` | 显示帮助 |

## 安装

1. 安装对应版本的 Minecraft Forge。
2. 下载最新版 BuildMode JAR 文件。
3. 将 JAR 文件放入服务器 `mods/` 文件夹。
4. 启动服务器，自动生成配置文件。

## 配置

- 区域：`config/buildmode/regions.json`
- 白名单：`config/buildmode/whitelist.json`
- 怪物移除列表：`config/buildmode/region_mobs.json`

区域配置示例：
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

## 开发

- Fork 仓库并创建功能分支。
- 遵循 Forge 模组开发规范。
- 提交 PR 前请本地测试。

## 许可证

MIT License，详见 LICENSE 文件。

## 致谢

- 基于 Minecraft Forge 开发。
- 集成 LuckPerms 权限支持。
- 由 YuWan-030 维护。

如需反馈问题或建议，请在 GitHub 提交 Issue！

`
