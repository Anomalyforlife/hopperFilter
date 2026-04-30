# HopperFilter

Paper/Spigot plugin that adds a **hopper filter** (allowlist): when a hopper is “filtered”, it can transfer only the items that match the rules configured in the GUI.

- **Minecraft/Paper API**: `1.21.x` (in `plugin.yml` `api-version: 1.21`)
- **Java**: `21` (see `build.gradle`)

## Filtered Hopper modes

HopperFilter supports two operational modes (configurable in `config.yml`):

### Mode A — Special hoppers (recommended)

When `filtered-hopper.require-special-hopper: true`:

- Only **special hoppers** (given via `/hf give`) can be configured and will apply item filtering.
- Normal hoppers behave vanilla (no GUI, no filtering).
- Special hopper locations are persisted in the DB table `filtered_hopper_locations` and cached in memory.

### Mode B — Global mode

When `filtered-hopper.require-special-hopper: false`:

- **All** hoppers are filterable.
- The special hopper location table is **not used** and there is **no cache/DB overhead** for the “special hopper” feature.
- `/hf give` is disabled.

## Features

- GUI to configure each hopper’s filter.
- **Per-item** matching rules (stored directly on the filter item via PDC):
  - Match Material
  - Match Durability
  - Match Name
  - Match NBT
  - Match Tag (dynamic tag selection)
- **Dynamic** tag selection: shows only the Bukkit tags applicable to the selected item.
- “Hopper jam” fix: if the first item in the source chest is blocked, the plugin tries to pull the **first allowed item** instead of getting stuck retrying the same one.
- Persistent storage (SQLite or MySQL) with connection pooling (HikariCP).

## How to use (in-game)

If you are in **Special hopper mode**, you must place a special hopper first:

1. `/hf give <player> [amount]`
2. Place the received hopper.
3. Configure it as usual (SHIFT + right-click).

### 1) Open the main GUI

1. Make sure your main hand is empty.
2. Hold **SHIFT**.
3. **Right-click** a hopper.

Required permission:
- `hopperfilter.opengui`

Note: to interact with the GUI (add/remove/configure items) you also need `hopperfilter.use`.

Special hopper mode note:
- If the clicked hopper is **not** a special hopper, the plugin will not open the GUI (vanilla behavior).

### 2) Add / remove items from the filter

In the main GUI:
- **Add**: left-click an item in the player inventory (bottom section).
- **Remove**: right-click a filter slot (top section).

The filter works as an **allowlist**: if the filter contains at least one item, only items matching at least one filter entry are allowed through.

### 3) Configure match options (per slot)

- **Left Click** a filter slot (main GUI) to open the “Filter Item Options” GUI.
- From there you can enable/disable matching options for that specific filter item.

Options:
- **Match Material**: requires the same material.
- **Match Durability**: requires the same durability/damage.
- **Match Name**: compares the custom display name.
- **Match NBT**: compares item meta (enchants, models, etc). The comparison ignores internal filter data stored in the PDC so it doesn’t break matching.
- **Match Tag**: allows matching by a tag (e.g. all planks).

### 4) Tag selection

When you enable **Match Tag**, you can select a specific tag:
- From the options GUI, use the tag selection button (opens the “Select Tag” GUI).
- Pick one of the suggested tags for that item’s material.
- You can disable tag matching with the “Disable Tag Matching” button.

Tag matching is applied only when a **tag is selected**.

## Commands

Main command:
- `/hopperfilter <reload|info|clear|give>`
- Alias: `/hf`

Subcommands:
- `reload`: reloads config (and lang).
- `info`: look at a hopper (within 6 blocks) and tells you whether it’s filtered.
- `clear`: look at a hopper (within 6 blocks) and clears its filter.
- `give`: gives a **special Filtered Hopper** item.
  - Usage: `/hf give <player> [amount]` (default 1, max 64)
  - Permission: `hopperfilter.admin.give`
  - Disabled in global mode (`filtered-hopper.require-special-hopper: false`)

## Permissions

Defined in `plugin.yml` / `paper-plugin.yml`:

| Permission | What it does | Default |
|---|---|---|
| `hopperfilter.opengui` | Allows opening the filter GUI | op |
| `hopperfilter.use` | Allows using the GUI (add/remove/config) | op |
| `hopperfilter.break` | Allows breaking **actively filtered** hoppers (must sneak) | op |
| `hopperfilter.admin.reload` | Allows `/hopperfilter reload` | op |
| `hopperfilter.admin.info` | Allows `/hopperfilter info` | op |
| `hopperfilter.admin.clear` | Allows `/hopperfilter clear` | op |
| `hopperfilter.admin.give` | Allows `/hopperfilter give` | op |

### Note (breaking special filtered hoppers)

- The sneak+permission check is applied only if the hopper has an **active filter**.
- In special hopper mode, breaking a special hopper drops a special hopper item again (keeps name/lore/PDC), so you can reuse it.

## Configuration

Files are created under `plugins/HopperFilter/`:
- `config.yml`
- `lang.yml`

### config.yml

| Key | Description | Default |
|---|---|---|
| `storage.type` | `sqlite` or `mysql` | `sqlite` |
| `storage.sqlite.file` | DB filename (inside the plugin folder) | `filters.db` |
| `storage.mysql.host` | MySQL host | `127.0.0.1` |
| `storage.mysql.port` | MySQL port | `3306` |
| `storage.mysql.database` | Database name | `hopperfilter` |
| `storage.mysql.username` | Username | `whatever` |
| `storage.mysql.password` | Password | `please-change-me-im-scared` |
| `storage.mysql.useSSL` | SSL | `false` |
| `storage.mysql.parameters` | Extra JDBC parameters | `useUnicode=true&characterEncoding=utf8&serverTimezone=UTC` |
| `storage.pool.maximumPoolSize` | Max pool size (SQLite is forced to 1 internally) | `10` |
| `storage.pool.minimumIdle` | Min idle | `2` |
| `storage.pool.connectionTimeoutMillis` | Connection timeout | `10000` |
| `filter.size` | Filter GUI size (multiple of 9, max 54) | `54` |
| `tnt.blockedRadius` | TNT placement block radius near filtered hoppers | `5` |
| `filtered-hopper.enabled` | Enables the filtered-hopper feature | `true` |
| `filtered-hopper.require-special-hopper` | If `true`, only special hoppers work; if `false`, all hoppers work | `true` |
| `filtered-hopper.name` | Display name of the special hopper item | `§6Filtered Hopper` |
| `filtered-hopper.lore` | Lore lines of the special hopper item | `§7This hopper can filter items.` |
| `filtered-hopper.give-message-sender` | Message shown to the command sender | `§aGiven {amount}x Filtered Hopper to {player}.` |
| `filtered-hopper.give-message-receiver` | Message shown to the receiver | `§aYou received {amount}x Filtered Hopper.` |

## Storage

The plugin uses 2 tables:

- `hopper_filter_items`: stores the filter contents per hopper location.
- `filtered_hopper_locations`: stores **special hopper locations** (only when `filtered-hopper.require-special-hopper: true`).

Global mode guarantee:
- When `filtered-hopper.require-special-hopper: false`, the plugin does **not** query or maintain `filtered_hopper_locations`.

### lang.yml

Contains all plugin texts (GUI titles, tooltips, messages, commands). You can customize colors and strings.

## Installation

1. Build or download the jar.
2. Put the jar into the server `plugins/` folder.
3. Start the server (or run `/hopperfilter reload`).
4. Configure permissions and (optionally) the database.

Tip:
- The default jar embeds **HikariCP** (so it works even if Paper cannot download libraries).
- `*-thin.jar` is the fully thin artifact (Paper will download runtime libraries declared in `plugin.yml` / `paper-plugin.yml`).
- `*-all.jar` is the fully shaded jar (contains all runtime deps) for fully-offline installs.

## Build & Dev (for developers)

Requirements:
- JDK 21
- Gradle installed (this repo does not include `gradlew.bat`/`gradlew`)

Useful tasks:
- Build default jar (embeds HikariCP):
  - `gradle shadowJar`
- Full build:
  - `gradle build`
- Start a local Paper server with the plugin loaded:
  - `gradle runServer`

Output:
- The final jar is generated in `build/libs/`.

## Troubleshooting

- **“DB error …”**: check `storage.type` and the connection parameters. For MySQL, verify host/port/credentials and that the DB exists.
- **The filter doesn’t seem to persist**: make sure the server uses a persistent `plugins/HopperFilter/` folder and that the user has write permissions.
- **Can’t open the GUI**: you need `hopperfilter.opengui`, empty main hand, and sneak.
- **Can open but can’t click**: you need `hopperfilter.use`.
- **Special hopper mode: hopper “stops working” after breaking**: make sure you are using the updated jar; special hoppers should drop as special items (with lore/PDC) when broken.

---

Website/author: `italiarevenge.com` / Anomalyforlife

---

## Special Thanks

Special thanks to the users:

<a href="https://github.com/bmlzootown" target="_blank"><img src="https://github.com/bmlzootown.png" width="24" height="24" style="vertical-align:middle; border-radius:50%;"/> <strong>bmlzootown</strong></a> <a href="https://github.com/Anomalyforlife/hopperFilter/issues/1" target="_blank">(Issue&nbsp;#1)</a>
<br>
<a href="https://github.com/DJRikyu" target="_blank"><img src="https://github.com/DJRikyu.png" width="24" height="24" style="vertical-align:middle; border-radius:50%;"/> <strong>DJRikyu</strong></a> Collaboration