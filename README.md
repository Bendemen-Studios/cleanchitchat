# Clean Chit Chat

**Clean Chit Chat** is a lightweight, server-side NeoForge mod for Minecraft 1.21.1.

It removes Minecraft's default angle-bracket chat wrapper while preserving the player's existing decorated display name, including FTB Ranks formatting.

## Example

Vanilla / default chat:

```text
<[Beheer] JustPetrov> Hallo
```

Clean Chit Chat:

```text
[Beheer] JustPetrov | Hallo
```

## Features

- Server-side only
- No client installation required
- Removes the vanilla `< >` chat brackets by default
- Preserves the decorated player display name
- Designed to work with FTB Ranks rank/name formatting
- Can be enabled/disabled at runtime
- Bracket handling can be toggled at runtime
- Configuration can be reloaded without restarting the server
- No LuckPerms dependency

## Commands

All commands use the `/chitchat` root command and require permission level 2 (operator/admin access in the normal server command system).

```text
/chitchat enable
/chitchat disable
/chitchat reload
/chitchat bracket on
/chitchat bracket off
```

### Command behavior

- `/chitchat enable` — enables Clean Chit Chat processing.
- `/chitchat disable` — disables Clean Chit Chat processing.
- `/chitchat bracket off` — removes the vanilla angle brackets.
- `/chitchat bracket on` — keeps the vanilla angle brackets.
- `/chitchat reload` — reloads the Clean Chit Chat configuration without a server restart.

## Configuration

The mod creates:

```text
config/cleanchitchat.properties
```

Default configuration:

```properties
enabled=true
remove_angle_brackets=true
```

Changes to this file can be applied with:

```text
/chitchat reload
```

## Compatibility

- **Minecraft:** 1.21.1
- **NeoForge:** 21.1.250+
- **Environment:** Dedicated server

## Building

Use a Java 21 NeoForge 1.21.1 development environment.

```text
gradle build
```

The compiled mod is generated in:

```text
build/libs/
```

## License

Copyright (C) 2026 Bendemen Studios.

Clean Chit Chat is licensed under the **GNU General Public License v3.0 only (GPL-3.0-only)**. See [`LICENSE`](LICENSE).

## Author

**Bendemen Studios**
