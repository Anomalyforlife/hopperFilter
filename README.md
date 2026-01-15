# HopperFilter

Paper/Spigot plugin that adds a **hopper filter** (allowlist): when a hopper is “filtered”, it can transfer only the items that match the rules configured in the GUI.

- **Minecraft/Paper API**: `1.21.x` (in `plugin.yml` `api-version: 1.21`)
- **Java**: `21` (see `build.gradle`)

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

### 1) Open the main GUI

1. Make sure your main hand is empty.
2. Hold **SHIFT**.
3. **Right-click** a hopper.

Required permission:
- `hopperfilter.opengui`

Note: to interact with the GUI (add/remove/configure items) you also need `hopperfilter.use`.

### 2) Add / remove items from the filter

In the main GUI:
- **Add**: left-click an item in the player inventory (bottom section).
- **Remove**: right-click a filter slot (top section).

The filter works as an **allowlist**: if the filter contains at least one item, only items matching at least one filter entry are allowed through.

### 3) Configure match options (per slot)

- **Middle-click (mouse wheel)** a filter slot (main GUI) to open the “Filter Item Options” GUI.
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
- `/hopperfilter <reload|info|clear>`
- Alias: `/hf`

Subcommands:
- `reload`: reloads config (and lang).
- `info`: look at a hopper (within 6 blocks) and tells you whether it’s filtered.
- `clear`: look at a hopper (within 6 blocks) and clears its filter.

## Permissions

Defined in `plugin.yml`:

| Permission | What it does | Default |
|---|---|---|
| `hopperfilter.opengui` | Allows opening the filter GUI | false |
| `hopperfilter.use` | Allows using the GUI (add/remove/config) | false |
| `hopperfilter.break` | Allows breaking filtered hoppers (must sneak) | false |
| `hopperfilter.admin.reload` | Allows `/hopperfilter reload` | op |
| `hopperfilter.admin.info` | Allows `/hopperfilter info` | op |
| `hopperfilter.admin.clear` | Allows `/hopperfilter clear` | op |

### Important note (break permission)

In the current code, breaking filtered hoppers is checked against `hopperfilter.admin.break` (see the listener). In `plugin.yml`, the declared permission is `hopperfilter.break`.

If you want to break filtered hoppers **without changing code**, also grant:
- `hopperfilter.admin.break`

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

### lang.yml

Contains all plugin texts (GUI titles, tooltips, messages, commands). You can customize colors and strings.

## Installation

1. Build or download the jar.
2. Put the jar into the server `plugins/` folder.
3. Start the server (or run `/hopperfilter reload`).
4. Configure permissions and (optionally) the database.

## Build & Dev (for developers)

Requirements:
- JDK 21
- Gradle installed (this repo does not include `gradlew.bat`/`gradlew`)

Useful tasks:
- Build jar (shadowJar without classifier):
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

---

Website/author: `italiarevenge.com` / Anomalyforlife
