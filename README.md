# Clean Chit Chat

A lightweight server-side NeoForge mod that cleans Minecraft player chat formatting while preserving decorated names/ranks such as FTB Ranks.

## Features

- Server-side only
- Removes the vanilla outer `< >` chat brackets by default
- Preserves the player's existing decorated display name and rank formatting
- Toggle bracket handling at runtime
- `/chitchat enable`
- `/chitchat disable`
- `/chitchat bracket on`
- `/chitchat bracket off`
- `/chitchat reload`
- No LuckPerms required
- No client installation required

## Compatibility

- Minecraft: **1.21.1**
- NeoForge: **21.1.250+**

## License

Copyright (C) 2026 Bendemen Studios

Licensed under the **GNU General Public License v3.0 only (GPL-3.0-only)**. See [LICENSE](LICENSE).

## Author

**Bendemen Studios**

## Building

Use the NeoForge 1.21.1 development environment and run:

```text
./gradlew build
```

The resulting JAR is placed in `build/libs/`.

## Server Commands

All `/chitchat` commands require operator/permission access appropriate to the server command system.

- `/chitchat enable` — enable Clean Chit Chat
- `/chitchat disable` — disable Clean Chit Chat
- `/chitchat bracket on` — keep the normal `< >` brackets
- `/chitchat bracket off` — remove the normal `< >` brackets
- `/chitchat reload` — reload Clean Chit Chat settings without restarting the server
