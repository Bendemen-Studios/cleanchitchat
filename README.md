# Clean Chit Chat

**Latest release: 1.0.1**

**Clean Chit Chat** is a lightweight, server-side NeoForge mod for Minecraft 1.21.1.

It removes Minecraft's default angle-bracket chat wrapper while preserving the player's existing decorated display name, including FTB Ranks formatting.

## Example

With the default settings:

```text
<[Beheer] JustPetrov> Hallo
```

becomes:

```text
[Beheer] JustPetrov | Hallo
```

The separator is formatted as dark gray and the chat message as gray, matching the requested:

```text
&f{name} &r&8| &7{message}
```

The rank prefix/display name remains controlled by the existing FTB Ranks configuration.

## Features

- Server-side only
- No client installation required
- Removes the vanilla `< >` chat brackets by default
- Preserves the decorated player display name
- Designed to work with FTB Ranks rank/name formatting
- Toggleable chat separator
- Toggleable bracket handling
- Runtime enable/disable
- Configuration reload without restarting the server
- No LuckPerms dependency

## Commands

```text
/chitchat enable
/chitchat disable
/chitchat reload
/chitchat bracket on
/chitchat bracket off
/chitchat separation on
/chitchat separation off
```

### Command behavior

- `/chitchat enable` — enables Clean Chit Chat processing.
- `/chitchat disable` — disables Clean Chit Chat processing.
- `/chitchat bracket off` — removes the vanilla angle brackets.
- `/chitchat bracket on` — restores normal angle-bracket chat behavior.
- `/chitchat separation on` — adds the ` | ` separator between the decorated name and chat message.
- `/chitchat separation off` — removes the separator.
- `/chitchat reload` — reloads the configuration without restarting the server.

## Configuration

The mod creates:

```text
config/cleanchitchat.properties
```

Default configuration:

```properties
enabled=true
remove_angle_brackets=true
separation=true
```

Changes can be applied with:

```text
/chitchat reload
```

## FTB Ranks setup

Keep the separator out of `ftbranks.name_format` so TAB/player display names do not contain it.

Example TAB/display-name format:

```text
&8[&r{rank}&r&8] &r&f{name}
```

Clean Chit Chat adds the separator only in chat when `separation=true`:

```text
[Rank] Name | Message
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
