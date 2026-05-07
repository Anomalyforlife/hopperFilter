# HopperFilter

Paper plugin that adds a **hopper filter** (allowlist): when a hopper is "filtered", it can transfer only the items that match the rules configured in the GUI.

- **Minecraft/Paper API**: `1.21.x`
- **Java**: `21`

## Filtered Hopper modes

HopperFilter supports two operational modes (configurable in `config.yml`):

### Mode A — Special hoppers (recommended)

When `filtered-hopper.require-special-hopper: true`:

- Only **special hoppers** (given via `/hf give`) can be configured and will apply item filtering.
- Normal hoppers behave vanilla (no GUI, no filtering).
- Special hopper locations are persisted in the DB table `filtered_hopper_locations` and cached in memory.
- The **upgrade system** is only available in this mode.

### Mode B — Global mode

When `filtered-hopper.require-special-hopper: false`:

- **All** hoppers are filterable.
- The special hopper location table is **not used** and there is no cache/DB overhead.
- `/hf give` is disabled.
- The upgrade system is disabled.

## Features

- GUI to configure each hopper's filter.
- **Per-item** matching rules (stored directly on the filter item via PDC):
  - Match Material
  - Match Durability
  - Match Name
  - Match NBT
  - Match Tag (dynamic tag selection)
- **Dynamic** tag selection: shows only the Bukkit tags applicable to the selected item.
- **Upgrade system** (special hopper mode): levels unlock more filter slots, faster transfer speeds, and optionally charge Vault economy.
- "Hopper jam" fix: if the first item in the source chest is blocked, the plugin tries to pull the **first allowed item** instead of getting stuck.
- Persistent storage (SQLite or MySQL) with connection pooling (HikariCP).
- Optional Vault economy integration for upgrade costs.

## How to use (in-game)

If you are in **Special hopper mode**, you must place a special hopper first:

1. `/hf give <player> [amount]`
2. Place the received hopper.
3. Configure it as usual (SHIFT + right-click).

### 1) Open the main GUI

1. Make sure your main hand is empty.
2. Hold **SHIFT**.
3. **Right-click** a hopper.

Required permissions: `hopperfilter.opengui` (open) and `hopperfilter.use` (interact).

In special hopper mode, the GUI only opens on special hoppers.

### 2) Add / remove items from the filter

- **Add**: left-click an item in the player inventory (bottom section).
- **Remove**: right-click a filter slot (top section).

The filter works as an **allowlist**: if the filter contains at least one item, only matching items are allowed through.

### 3) Configure match options (per slot)

- **Left click** a filter slot to open the "Filter Item Options" GUI.

Options:
- **Match Material**: requires the same material.
- **Match Durability**: requires the same durability/damage.
- **Match Name**: compares the custom display name.
- **Match NBT**: compares item meta (enchants, models, etc.).
- **Match Tag**: allows matching by a Bukkit tag (e.g. all planks).

### 4) Tag selection

When you enable **Match Tag**, open the tag selector to pick one of the tags applicable to that item's material. Disable with the "Disable Tag Matching" button.

### 5) Upgrades (special hopper mode only)

- **Left-click the upgrade button** in the GUI (bottom-right corner) to upgrade the hopper to the next level.
- Each level unlocks more filter slots and/or a faster transfer speed.
- If Vault is enabled, the cost shown in the button is charged on upgrade.

Required permission: `hopperfilter.upgrade`

## Commands

Main command: `/hopperfilter` — alias: `/hf`

| Subcommand | Description | Permission |
|---|---|---|
| `reload` | Reload config and lang | `hopperfilter.admin.reload` |
| `info` | Look at a hopper (≤6 blocks) to see if it is filtered | `hopperfilter.admin.info` |
| `clear` | Look at a hopper (≤6 blocks) and clear its filter | `hopperfilter.admin.clear` |
| `give <player> [amount]` | Give a special Filtered Hopper (max 64). Disabled in global mode. | `hopperfilter.admin.give` |
| `maxupgrade <player>` | Upgrade all filtered hoppers owned by a player to max level | `hopperfilter.giveupgrades.max` |
| `upgraderadius <radius>` | Upgrade all filtered hoppers within `<radius>` blocks of you to max level (max 500) | `hopperfilter.giveupgrades.max` |

> **Note on `maxupgrade`**: hoppers placed before owner-tracking was added have no owner data and will not be affected. Use `upgraderadius` for those.

## Permissions

| Permission | What it does | Default |
|---|---|---|
| `hopperfilter.opengui` | Open the filter GUI | op |
| `hopperfilter.use` | Interact with the GUI (add/remove/configure items) | op |
| `hopperfilter.break` | Break actively-filtered hoppers (must sneak) | op |
| `hopperfilter.upgrade` | Upgrade a hopper via the GUI | op |
| `hopperfilter.admin.reload` | `/hf reload` | op |
| `hopperfilter.admin.info` | `/hf info` | op |
| `hopperfilter.admin.clear` | `/hf clear` | op |
| `hopperfilter.admin.give` | `/hf give` | op |
| `hopperfilter.giveupgrades.max` | `/hf maxupgrade` and `/hf upgraderadius` | op |

### Note (breaking special filtered hoppers)

- The sneak+permission check applies only when the hopper has an **active filter**.
- In special hopper mode, breaking a special hopper drops a special hopper item (preserves name/lore/PDC), so it can be reused.

## Configuration

Files are created under `plugins/HopperFilter/`:
- `config.yml`
- `lang.yml`

### config.yml

**Storage**

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
| `storage.mysql.parameters` | Extra JDBC parameters | `useUnicode=true&...` |
| `storage.pool.maximumPoolSize` | Max pool size (SQLite is forced to 1) | `10` |
| `storage.pool.minimumIdle` | Min idle connections | `2` |
| `storage.pool.connectionTimeoutMillis` | Connection timeout (ms) | `10000` |

**Filter**

| Key | Description | Default |
|---|---|---|
| `filter.size` | Filter GUI size (multiple of 9, max 54) | `54` |
| `tnt.blockedRadius` | TNT placement blocked radius around filtered hoppers | `5` |

**Special hopper**

| Key | Description | Default |
|---|---|---|
| `filtered-hopper.enabled` | Enable the filtered-hopper feature | `true` |
| `filtered-hopper.require-special-hopper` | `true` = only special hoppers filter; `false` = all hoppers filter | `true` |
| `filtered-hopper.accept-name-lore-fallback` | Also recognize filtered hoppers by matching name+lore (for shop plugins that can't set PDC tags) | `false` |
| `filtered-hopper.name` | Display name of the special hopper item | `§6Filtered Hopper` |
| `filtered-hopper.lore` | Lore lines of the special hopper item | `§7This hopper can filter items.` |
| `filtered-hopper.give-message-sender` | Message shown to the command sender on `/hf give` | `§aGiven {amount}x ...` |
| `filtered-hopper.give-message-receiver` | Message shown to the receiver on `/hf give` | `§aYou received {amount}x ...` |

**Upgrades** (only active when `filtered-hopper.require-special-hopper: true`)

| Key | Description | Default |
|---|---|---|
| `upgrades.enabled` | Enable the upgrade system | `true` |
| `upgrades.vault-required` | Charge Vault economy for upgrades. If `false`, upgrades are free. | `true` |
| `upgrades.max-level` | Maximum upgrade level | `10` |
| `upgrades.levels.<n>.cost` | Currency cost to reach level `n` (level 1 = no cost) | — |
| `upgrades.levels.<n>.filter-slots` | Number of active filter slots at level `n` | — |
| `upgrades.levels.<n>.transfer-speed-ticks` | Ticks between item transfers at level `n` (vanilla = 8; lower = faster) | — |

Default level table (can be fully customized):

| Level | Cost | Filter slots | Speed (ticks) |
|:---:|---:|:---:|:---:|
| 1 | 0 | 3 | 16 (half vanilla) |
| 2 | 1 000 | 5 | 14 |
| 3 | 2 000 | 7 | 11 |
| 4 | 3 000 | 10 | 9 |
| 5 | 4 000 | 12 | 8 (vanilla) |
| 6 | 5 000 | 18 | 8 |
| 7 | 6 000 | 25 | 8 |
| 8 | 7 000 | 32 | 8 |
| 9 | 8 000 | 43 | 6 |
| 10 | 10 000 | 53 | 4 (2× vanilla) |

### lang.yml

Contains all plugin texts (GUI titles, tooltips, messages, command replies). Fully customizable.

## Storage

The plugin uses 2 tables:

- `hopper_filter_items`: stores the filter contents per hopper location.
- `filtered_hopper_locations`: stores special hopper locations, their upgrade level, and owner UUID (only used when `require-special-hopper: true`).

When `require-special-hopper: false`, `filtered_hopper_locations` is not queried or maintained.

## Installation

1. Build or download the jar.
2. Place the jar in the server `plugins/` folder.
3. Start the server (or run `/hf reload`).
4. Configure permissions and, optionally, the database and upgrade costs.

**Optional**: install [Vault](https://www.spigotmc.org/resources/vault.34315/) and an economy plugin (e.g. EssentialsX) to enable upgrade costs.

JAR variants:
- **Default jar** (`HopperFilter-x.y.z.jar`): embeds HikariCP; Paper downloads the remaining runtime libraries declared in `plugin.yml`.
- **`*-thin.jar`**: no embedded deps; Paper downloads everything.
- **`*-all.jar`**: fully shaded; no internet required at startup.

## Build & Dev

Requirements:
- JDK 21
- Gradle (this repo does not include a Gradle wrapper)

Useful tasks:

| Task | Description |
|---|---|
| `gradle shadowJar` | Build the default jar (embeds HikariCP) |
| `gradle shadowAllJar` | Build the fully shaded jar |
| `gradle build` | Build all variants |
| `gradle runServer` | Start a local Paper server with the plugin loaded |

Output goes to `build/libs/`.

## Troubleshooting

- **"DB error …"**: check `storage.type` and connection parameters. For MySQL, verify that the database exists and credentials are correct.
- **Filter doesn't persist**: make sure the server has write access to `plugins/HopperFilter/`.
- **Can't open the GUI**: you need `hopperfilter.opengui`, an empty main hand, and must be sneaking.
- **Can open but can't interact**: you need `hopperfilter.use`.
- **Upgrade button missing**: upgrades require `filtered-hopper.require-special-hopper: true` and `upgrades.enabled: true` in `config.yml`.
- **Economy not charging**: check that Vault and an economy plugin are installed and that `upgrades.vault-required: true`.
- **`maxupgrade` reports 0 hoppers**: hoppers placed before v3.0.0 have no owner data. Use `/hf upgraderadius` to upgrade by proximity instead.
- **Special hopper stops working after breaking**: make sure you are running the correct jar version; special hoppers should drop as special items (with lore/PDC) when broken.

---

Website/author: `italiarevenge.com` / Anomalyforlife

---

## Special Thanks

Special thanks to the users:

<a href="https://github.com/bmlzootown" target="_blank"><img src="https://github.com/bmlzootown.png" width="24" height="24" style="vertical-align:middle; border-radius:50%;"/> <strong>bmlzootown</strong></a> <a href="https://github.com/Anomalyforlife/hopperFilter/issues/1" target="_blank">(Issue&nbsp;#1)</a>
<br>
<a href="https://github.com/DJRikyu" target="_blank"><img src="https://github.com/DJRikyu.png" width="24" height="24" style="vertical-align:middle; border-radius:50%;"/> <strong>DJRikyu</strong></a> Collaboration
