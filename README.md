# HyEssentials v1.0.0
**The Ultimate Essentials Suite for Hytale Servers.**

Developed by **ArkFlame Development**, HyEssentials is a comprehensive, high-performance, and modular essentials plugin designed to bring the familiar power of EssentialsX to the Hytale platform. Built from the ground up with **full async support** and a **modular architecture**, it ensures your server remains lag-free while providing every feature your players expect.

---

## 🚀 Key Features

- **⚡ High Performance:** All heavy operations (I/O, Permissions, Economy) run asynchronously to keep the main thread fluid.
- **🧩 Modular Design:** Enable only the features you need. Each system (TPA, Kits, Homes) is its own module.
- **🌍 Multi-Language Support:** Full localization system with automatic client-locale detection.
- **🛡️ Built-in Permissions:** Robust group-based permission system with wildcards and priority handling.
- **💰 Economy Ready:** Interface-based economy system ready for Vault-style integrations.
- **🏠 Advanced Teleportation:** TPA system with warmups, cooldowns, and movement cancellation.
- **📦 Kit System:** Create kits with cooldowns, prices, and permission-based access.

---

## 📁 Project Structure

```text
com.arkflame.hyessentials/
├── HyEssentials.java (Main Entry)
├── tasks/           (Async execution system)
├── managers/        (Feature logic: Teleport, Home, Warp, Chat, etc.)
├── config/          (YML & Language management)
├── economy/         (IEconomy implementation)
├── modules/         (IModule system for feature toggling)
├── commands/        (30+ command implementations)
├── data/            (POJOs for UserData, Kits, Warps)
└── util/            (Color & Time utilities)
```

---

## 🛠️ Installation

1. Download `HyEssentials.jar`.
2. Place the file into your Hytale server's `plugins/` folder.
3. Start the server to generate default configuration files.
4. Customize `config.yml` and `lang/messages_en.yml` located in `plugins/HyEssentials/`.
5. Restart or reload the plugin.

---

## 🎮 Command Reference

### Teleportation
| Command | Description |
| :--- | :--- |
| `/tpa <player>` | Request to teleport to a player |
| `/tpaccept` | Accept a pending request |
| `/home [name]` | Teleport to a saved home |
| `/sethome [name]` | Save your current location as a home |
| `/warp [name]` | Teleport to a server-wide warp |
| `/spawn` | Return to the server spawn point |
| `/back` | Return to your previous 5 locations |

### Gameplay & Admin
| Command | Description |
| :--- | :--- |
| `/gamemode <mode>` | Change game mode (Survival, Creative, etc.) |
| `/fly` | Toggle flight mode |
| `/give <player> <item>` | Give items to players |
| `/repair` | Repair the item in your hand |
| `/heal` / `/feed` | Restore health or hunger |
| `/time <set/add>` | Adjust world time |

### Social & Moderation
| Command | Description |
| :--- | :--- |
| `/msg <player> <msg>`| Send a private message |
| `/reply <msg>` | Reply to the last message received |
| `/mute <player>` | Prevent a player from chatting |
| `/ignore <player>` | Block messages from a specific player |

---

## 🔐 Permissions Overview

- `essentials.*` — Access to all features.
- `essentials.gamemode` — Change gamemode.
- `essentials.tpa` — Use the TPA system.
- `essentials.sethome.<number>` — Limit how many homes a player can set.
- `essentials.kits.<name>` — Access to a specific kit.
- `essentials.chat.color` — Use color codes in chat.

---

## ⚙️ Configuration (Preview)

HyEssentials is highly configurable via `config.yml`.

```yaml
# Teleportation Settings
teleport:
  warmup: 5  # seconds players must stand still
  cooldown: 30  # seconds between teleport uses
  cancel-on-move: true
  
# Chat Formatting
chat:
  format: "{prefix}{player}{suffix}: {message}"

# Module Toggles
modules:
  home: true
  warp: true
  kit: true
  economy: true
```

---

## 👨‍💻 For Developers

HyEssentials is built for extensibility. You can hook into the module system or implement your own economy provider.

**Example: Checking for Permissions**
```java
if (plugin.getPermissionManager().hasPermission(player.getUniqueId(), "essentials.fly")) {
    // Logic here
}
```

---

## 📜 License
Copyright © 2026 **ArkFlame Development**. All rights reserved.
*This software is provided as-is for Hytale server administrators.*
