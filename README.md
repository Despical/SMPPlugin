# SMPlugin

[![CI](https://github.com/Despical/SMPPlugin/actions/workflows/build.yml/badge.svg)](https://github.com/Despical/SMPPlugin/actions/workflows/build.yml)
![Java 25](https://img.shields.io/badge/Java-25-007396.svg)
![Gradle](https://img.shields.io/badge/Gradle-9.7.0-079ec0?logo=gradle&logoColor=white)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62b47a)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

SMPlugin is a Paper utility plugin built for KAYIK SMP. It provides practical administration, teleportation, chat, visibility, and quality-of-life tools in one lightweight plugin.

---

## Features

- Game mode, teleportation, safe-surface, and movement-speed commands.
- Timed teleport requests with movement-sensitive cancellation and per-player request controls.
- Private messaging, replies, operator spy mode, and persistent public/private chat history.
- Read-only and editable inventory or Ender Chest viewing for online players.
- Vanish support that stays hidden from both current and newly joined players.
- Latest-death location tracking with shareable location information.
- Configurable MOTD, join and quit messages, chat formatting, and a bundled server icon.
- A 50% sleeping rule applied to loaded worlds.

---

## Requirements

- Java 25
- Paper 1.21.11 or a compatible server implementation

---

## Building

Clone the repository:

```bash
git clone https://github.com/Despical/SMPPlugin.git
cd SMPPlugin
```

Build the plugin jar:

```bash
./gradlew shadowJar
```

On Windows:

```cmd
gradlew.bat shadowJar
```

The packaged jar is created under `build/libs/`.

Run the full verification used by CI:

```bash
./gradlew build
```

On Windows:

```cmd
gradlew.bat build
```

---

## Configuration

All configuration is bundled in `src/main/resources/config.yml` and copied to the plugin data folder on first startup. It controls server messages, MOTD rendering, teleport requests, chat formatting, inventory viewing, speed, and death-location behavior.

Persistent chat history and death locations are saved beneath `plugins/SMPlugin/`.

---

## License

This project is licensed under the [GPL-3.0 License](https://www.gnu.org/licenses/gpl-3.0.html). See [LICENSE](LICENSE) for the full text.
