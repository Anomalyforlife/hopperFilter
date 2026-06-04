# HopperFilter

Paper plugin that adds a **hopper filter** (allowlist/blacklist): when a hopper is "filtered", it can transfer only the items that match the rules configured in the GUI.

- **Minecraft/Paper**: `1.21.4+` (tested on `1.21.11`)
- **Java**: `21`

---

## Modes

HopperFilter supports two operational modes (configurable in `config.yml`):

### Mode A — Special hoppers (recommended)

When `filtered-hopper.require-special-hopper: true`:

- Only **special Filtered Hoppers** (given via `/hf give`) can be configured and will apply item filtering.
- Normal hoppers behave like vanilla (no GUI, no filtering).
- Special hopper locations are persisted in the DB table `filtered_hopper_locations` and cached in memory.
- The **upgrade system**, **Hopper Converter item**, and **conversion commands** are only available in this mode.

### Mode B — Global mode

When `filtered-hopper.require-special-hopper: false`:

- **All** hoppers are filterable.
- The special hopper location table is **not used**.
- `/hf give`, `/hf giveconverter`, `/hf converthopper`, `/hf convertradius`, and the Hopper Converter item are disabled.
- The upgrade system is disabled.

---

## Features

- GUI to configure each hopper's filter (allowlist or blacklist).
- **Per-item** matching rules (stored on the filter item via PDC):
  - Match Material
  - Match Durability
  - Match Name
  - Match NBT
  - Match Tag (dynamic Bukkit tag selection)
- **Smart duplicate detection**: when adding an item to the filter, the plugin checks whether that item would already pass the existing filter entries (using their real match options), not just whether an identical ItemStack is present.
- **Upgrade system** (special hopper mode): levels unlock more filter slots and faster transfer speeds; optionally charges Vault economy.
- **Hopper Converter item**: a special item given to players that lets them convert any vanilla hopper into a Filtered Hopper by right-clicking it. Consumed on use (not consumed in Creative).
- **Convert commands**: admin commands to convert hoppers by looking at them or by radius.
- "Hopper jam" fix: if the first item in a source container is blocked, the plugin pulls the **first allowed item** instead of getting stuck.
- Persistent storage (SQLite or MySQL) with HikariCP connection pooling.
- Optional Vault economy integration for upgrade costs.
- Resource pack support: custom model data for Filtered Hopper and Hopper Converter items (IDs configurable, matching `HopperFilter-ResourcePack.zip`).

---

## How to use (in-game)

### Obtaining a Filtered Hopper (special mode)

```
/hf give <player> [amount]
```

Place the received hopper, then configure it with SHIFT + right-click.

### Opening the filter GUI

1. Empty main hand.
2. Hold **SHIFT**.
3. **Right-click** a Filtered Hopper.

Required permissions: `hopperfilter.opengui` (open) + `hopperfilter.use` (interact).

### Adding / removing items

- **Add**: left-click an item in your inventory (bottom half of the GUI).
- **Remove**: right-click a filter slot (top half).

The filter works as an **allowlist** by default: if at least one item is present, only matching items pass. Individual entries can be switched to **blacklist** mode in the per-item options.

### Per-item match options

Left-click a filter slot to open the item options screen:

| Option | Description |
|---|---|
| Match Material | Item type must match |
| Match Durability | Damage value must match |
| Match Name | Custom display name must match |
| Match NBT | Item meta (enchants, model data, etc.) must match |
| Match Tag | Match any item in a chosen Bukkit tag (e.g. all planks) |
| Blacklist | Block this item instead of allowing it |

### Upgrades (special mode only)

Left-click the **upgrade button** (bottom-right of the GUI) to level up the hopper. Each level unlocks more filter slots and/or faster transfer speed. If Vault is enabled, the displayed cost is charged.

Required permission: `hopperfilter.upgrade`

### Hopper Converter item

The Hopper Converter is a special item that converts a vanilla hopper into a Filtered Hopper by right-clicking it. It is consumed on use (one item per conversion). In Creative mode it is not consumed.

- Give it with `/hf giveconverter <player> [amount]`.
- The item **cannot be placed** as a block.
- Required permission to use: `hopperfilter.convert`.

---

## Commands

Main command: `/hopperfilter` — alias: `/hf`

| Subcommand | Description | Permission |
|---|---|---|
| `reload` | Reload config and lang | `hopperfilter.admin.reload` |
| `info` | Look at a hopper (≤6 blocks) — show if it is filtered | `hopperfilter.admin.info` |
| `clear` | Look at a hopper (≤6 blocks) — clear its filter | `hopperfilter.admin.clear` |
| `give <player> [amount]` | Give a Filtered Hopper item (max 64). Disabled in global mode. | `hopperfilter.admin.give` |
| `giveconverter <player> [amount]` | Give a Hopper Converter item (max 64). Disabled in global mode. | `hopperfilter.admin.give` |
| `converthopper` | Look at a hopper (≤6 blocks) and convert it to a Filtered Hopper | `hopperfilter.admin.convert` |
| `convertradius <radius>` | Convert all vanilla hoppers within `<radius>` blocks (max 500) | `hopperfilter.admin.convert` |
| `maxupgrade <player>` | Upgrade all Filtered Hoppers owned by a player to max level | `hopperfilter.giveupgrades.max` |
| `upgraderadius <radius>` | Upgrade all Filtered Hoppers within `<radius>` blocks to max level (max 500) | `hopperfilter.giveupgrades.max` |

All subcommands support **tab completion**.

> **Note on `maxupgrade`**: hoppers placed before owner-tracking was added have no owner UUID and will not be affected. Use `convertradius` / `upgraderadius` for those.

---

## Permissions

| Permission | What it does | Default |
|---|---|---|
| `hopperfilter.opengui` | Open the filter GUI | op |
| `hopperfilter.use` | Interact with the GUI (add/remove/configure items) | op |
| `hopperfilter.break` | Break actively-filtered hoppers (must sneak) | op |
| `hopperfilter.upgrade` | Upgrade a hopper via the GUI | op |
| `hopperfilter.convert` | Use the **Hopper Converter item** (right-click) | op |
| `hopperfilter.admin.reload` | `/hf reload` | op |
| `hopperfilter.admin.info` | `/hf info` | op |
| `hopperfilter.admin.clear` | `/hf clear` | op |
| `hopperfilter.admin.give` | `/hf give` and `/hf giveconverter` | op |
| `hopperfilter.admin.convert` | `/hf converthopper` and `/hf convertradius` | op |
| `hopperfilter.giveupgrades.max` | `/hf maxupgrade` and `/hf upgraderadius` | op |

> Breaking a filtered hopper requires sneaking. In special mode, breaking drops a special hopper item (preserves name/lore/PDC/level) so it can be reused.

---

## Configuration

Files are created under `plugins/HopperFilter/`:
- `config.yml`
- `lang.yml`

### config.yml

**Storage**

| Key | Description | Default |
|---|---|---|
| `storage.type` | `sqlite` or `mysql` | `sqlite` |
| `storage.sqlite.file` | DB filename | `filters.db` |
| `storage.mysql.*` | MySQL connection settings | — |
| `storage.pool.maximumPoolSize` | Max pool size (SQLite forced to 1) | `10` |
| `storage.pool.minimumIdle` | Min idle connections | `2` |
| `storage.pool.connectionTimeoutMillis` | Connection timeout (ms) | `10000` |

**Filter**

| Key | Description | Default |
|---|---|---|
| `filter.size` | Filter GUI size (multiple of 9, max 54) | `54` |
| `tnt.blockedRadius` | TNT placement blocked within this radius of a Filtered Hopper | `5` |

**Filtered Hopper item**

| Key | Description | Default |
|---|---|---|
| `filtered-hopper.enabled` | Enable the feature | `true` |
| `filtered-hopper.require-special-hopper` | `true` = special hoppers only; `false` = all hoppers | `true` |
| `filtered-hopper.accept-name-lore-fallback` | Also recognise filtered hoppers by name+lore (for shop plugins that can't set PDC) | `false` |
| `filtered-hopper.name` | Display name of the Filtered Hopper item | `§6Filtered Hopper` |
| `filtered-hopper.lore` | Lore lines | `§7This hopper can filter items.` |
| `filtered-hopper.custom-model-data` | Custom model data ID for resource pack (0 = disabled) | `0` |
| `filtered-hopper.give-message-sender` | Message to the command sender on give | `§aGiven {amount}x …` |
| `filtered-hopper.give-message-receiver` | Message to the receiver on give | `§aYou received {amount}x …` |

**Hopper Converter item**

| Key | Description | Default |
|---|---|---|
| `hopper-converter.enabled` | Enable the converter item (special mode only) | `true` |
| `hopper-converter.accept-name-lore-fallback` | Also recognise converter items by name+lore (for shop plugins) | `false` |
| `hopper-converter.item` | Material of the converter item (any Bukkit material name) | `HOPPER` |
| `hopper-converter.name` | Display name | `§5Hopper Converter` |
| `hopper-converter.lore` | Lore lines | — |
| `hopper-converter.custom-model-data` | Custom model data ID for resource pack (0 = disabled) | `0` |
| `hopper-converter.give-message-sender` | Message to the command sender on giveconverter | `§aGiven {amount}x …` |
| `hopper-converter.give-message-receiver` | Message to the receiver on giveconverter | `§aYou received {amount}x …` |

**Upgrades** (special mode only)

| Key | Description | Default |
|---|---|---|
| `upgrades.enabled` | Enable the upgrade system | `true` |
| `upgrades.vault-required` | Charge Vault economy for upgrades | `true` |
| `upgrades.max-level` | Maximum upgrade level | `10` |
| `upgrades.levels.<n>.cost` | Cost to reach level `n` | — |
| `upgrades.levels.<n>.filter-slots` | Active filter slots at level `n` | — |
| `upgrades.levels.<n>.items-per-transfer` | Items moved per hopper tick at level `n` (vanilla = 1) | — |

Default level table:

| Level | Cost | Filter slots | Items/tick |
|:---:|---:|:---:|:---:|
| 1 | 0 | 3 | 1 |
| 2 | 1 000 | 5 | 2 |
| 3 | 2 000 | 7 | 3 |
| 4 | 3 000 | 10 | 4 |
| 5 | 4 000 | 12 | 5 |
| 6 | 5 000 | 18 | 6 |
| 7 | 6 000 | 25 | 7 |
| 8 | 7 000 | 32 | 8 |
| 9 | 8 000 | 43 | 9 |
| 10 | 10 000 | 52 | 10 |

### lang.yml

All GUI titles, tooltips, messages, and command replies. Fully customizable.

---

## Resource pack

The plugin ships with `HopperFilter-ResourcePack.zip`. It overrides the hopper item model to show distinct textures based on custom model data:

| ID | Item | Texture |
|:---:|---|---|
| `1` | Filtered Hopper | Gold tint |
| `2` | Hopper Converter | Purple tint |

Set `filtered-hopper.custom-model-data: 1` and `hopper-converter.custom-model-data: 2` in `config.yml` to enable the resource pack textures.

> The visual differentiation applies to the **item in inventory/hand only**. The placed block always looks like a vanilla hopper.

---

## Storage

Two DB tables:

- `hopper_filter_items` — filter contents (items + match options) per hopper location.
- `filtered_hopper_locations` — special hopper locations, upgrade level, owner UUID, and per-hopper settings. Only used when `require-special-hopper: true`.

---

## Installation

1. Place the jar in `plugins/`.
2. Start the server (config and lang files are generated automatically).
3. Configure permissions and, optionally, database settings and upgrade costs.
4. *(Optional)* Load `HopperFilter-ResourcePack.zip` on the server and set the custom model data IDs in `config.yml`.

**Optional dependency**: [Vault](https://www.spigotmc.org/resources/vault.34315/) + an economy plugin (e.g. EssentialsX) to enable upgrade costs.

JAR variants:

| Jar | Embedded deps |
|---|---|
| `HopperFilter-x.y.z.jar` | HikariCP only (Paper downloads the rest) |
| `*-thin.jar` | None (Paper downloads everything) |
| `*-all.jar` | All — no internet required at startup |

---

## Build & Dev

Requirements: JDK 21, Gradle.

| Task | Description |
|---|---|
| `gradle shadowJar` | Default jar (embeds HikariCP) |
| `gradle shadowAllJar` | Fully shaded jar |
| `gradle build` | All variants |
| `gradle runServer` | Start a local Paper 1.21.11 server with the plugin |

Output: `build/libs/`.

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| `DB error …` | Check `storage.type` and connection parameters. |
| Filter doesn't persist | Ensure the server can write to `plugins/HopperFilter/`. |
| Can't open the GUI | Need `hopperfilter.opengui`, empty main hand, and sneak. |
| Can open but can't interact | Need `hopperfilter.use`. |
| Upgrade button missing | Requires `require-special-hopper: true` + `upgrades.enabled: true`. |
| Economy not charging | Verify Vault + economy plugin are installed and `upgrades.vault-required: true`. |
| `maxupgrade` reports 0 hoppers | Hoppers placed before owner-tracking have no UUID. Use `/hf upgraderadius`. |
| Converter item has no effect | Check `hopper-converter.enabled: true` and that the player has `hopperfilter.convert`. |
| Converter item can be placed | Should never happen — placement is always cancelled by the plugin regardless of material. |
| Custom textures not showing | Verify the resource pack is loaded and the `custom-model-data` IDs in config match the pack. |

---

Website/author: `italiarevenge.com` / Anomalyforlife

---

## Special Thanks

<a href="https://github.com/bmlzootown" target="_blank"><img src="https://github.com/bmlzootown.png" width="24" height="24" style="vertical-align:middle; border-radius:50%;"/> <strong>bmlzootown</strong></a> <a href="https://github.com/Anomalyforlife/hopperFilter/issues/1" target="_blank">(Issue&nbsp;#1)</a>
<br>
<a href="https://github.com/DJRikyu" target="_blank"><img src="https://github.com/DJRikyu.png" width="24" height="24" style="vertical-align:middle; border-radius:50%;"/> <strong>DJRikyu</strong></a> Collaboration
