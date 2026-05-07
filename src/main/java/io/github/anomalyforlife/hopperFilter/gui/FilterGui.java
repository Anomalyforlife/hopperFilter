package io.github.anomalyforlife.hopperFilter.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.anomalyforlife.hopperFilter.model.FilterMatchOptions;
import io.github.anomalyforlife.hopperFilter.model.HopperKey;
import io.github.anomalyforlife.hopperFilter.service.FilterService;
import io.github.anomalyforlife.hopperFilter.upgrade.UpgradeConfig;
import io.github.anomalyforlife.hopperFilter.upgrade.UpgradeService;
import io.github.anomalyforlife.hopperFilter.util.ItemMatch;
import io.github.anomalyforlife.hopperFilter.util.LanguageManager;
import io.github.anomalyforlife.hopperFilter.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class FilterGui {
    private final FilterService filterService;
    private final Messages messages;
    private final LanguageManager lang;
    // Null when upgrades are disabled or not in special-hopper mode
    private final UpgradeService upgradeService;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final Component title;

    private final String msgAdded;
    private final String msgAddedWarning;
    private final String msgAlready;
    private final String msgFull;
    private final String msgRemoved;

    public FilterGui(FilterService filterService,
                     UpgradeService upgradeService,
                     Messages messages,
                     LanguageManager lang,
                     Component title,
                     String msgAdded,
                     String msgAddedWarning,
                     String msgAlready,
                     String msgFull,
                     String msgRemoved) {
        this.filterService = filterService;
        this.upgradeService = upgradeService;
        this.messages = messages;
        this.lang = lang;
        this.title = title;
        this.msgAdded = msgAdded;
        this.msgAddedWarning = msgAddedWarning;
        this.msgAlready = msgAlready;
        this.msgFull = msgFull;
        this.msgRemoved = msgRemoved;
    }

    public void open(Player player, HopperKey key) throws Exception {
        ItemStack[] items = filterService.getOrLoad(key);
        int size = filterService.size();
        boolean upgradesActive = upgradeService != null && upgradeService.getConfig().isEnabled();

        Inventory inv = Bukkit.createInventory(new FilterGuiHolder(key), size, title);

        if (upgradesActive) {
            int activeSlots = activeSlots(key);
            // Active filter slots
            for (int i = 0; i < activeSlots; i++) {
                inv.setItem(i, addListIndicator(items[i]));
            }
            // Locked slots (between active and upgrade button)
            ItemStack locked = createLockedSlot();
            for (int i = activeSlots; i < size - 1; i++) {
                inv.setItem(i, locked);
            }
            // Upgrade button always at last slot
            inv.setItem(size - 1, createUpgradeButton(key));
        } else {
            for (int i = 0; i < size; i++) {
                inv.setItem(i, addListIndicator(items[i]));
            }
        }

        player.openInventory(inv);
    }

    public void addFromPlayerInventory(Player player, Inventory gui, ItemStack clicked) throws Exception {
        if (!(gui.getHolder() instanceof FilterGuiHolder holder)) {
            return;
        }
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        HopperKey key = holder.key();
        ItemStack[] filter = filterService.getOrLoad(key);
        int maxSlots = activeSlots(key);

        for (int i = 0; i < maxSlots; i++) {
            ItemStack existing = filter[i];
            if (existing != null && !existing.getType().isAir()
                    && ItemMatch.matches(clicked, existing, FilterMatchOptions.defaults())) {
                messages.actionBar(player, msgAlready);
                return;
            }
        }

        int empty = -1;
        for (int i = 0; i < maxSlots; i++) {
            ItemStack it = filter[i];
            if (it == null || it.getType().isAir()) {
                empty = i;
                break;
            }
        }
        if (empty == -1) {
            messages.actionBar(player, msgFull);
            return;
        }

        ItemStack stored = clicked.clone();
        stored.setAmount(1);
        FilterMatchOptions.defaults().applyTo(stored);
        filter[empty] = stored;

        filterService.saveAndCache(key, filter);
        gui.setItem(empty, addListIndicator(stored));

        if (isWarningName(stored)) {
            messages.actionBar(player, msgAddedWarning);
        } else {
            messages.actionBar(player, msgAdded);
        }
    }

    public void removeAt(Player player, Inventory gui, int slot) throws Exception {
        if (!(gui.getHolder() instanceof FilterGuiHolder holder)) {
            return;
        }

        HopperKey key = holder.key();
        int maxSlots = activeSlots(key);

        if (slot < 0 || slot >= maxSlots) {
            return;
        }

        ItemStack[] filter = filterService.getOrLoad(key);
        if (filter[slot] == null || filter[slot].getType().isAir()) {
            return;
        }

        for (int i = slot; i < maxSlots - 1; i++) {
            filter[i] = filter[i + 1];
        }
        filter[maxSlots - 1] = null;

        filterService.saveAndCache(key, filter);

        for (int i = 0; i < maxSlots; i++) {
            gui.setItem(i, addListIndicator(filter[i]));
        }
        messages.actionBar(player, msgRemoved);
    }

    public boolean allows(HopperKey key, ItemStack movingItem) throws Exception {
        ItemStack[] filter = filterService.getOrLoad(key);
        boolean active = false;
        for (ItemStack it : filter) {
            if (it != null && !it.getType().isAir()) {
                active = true;
                break;
            }
        }
        if (!active) {
            return true;
        }

        for (ItemStack filterItem : filter) {
            if (filterItem == null || filterItem.getType().isAir()) {
                continue;
            }
            FilterMatchOptions options = FilterMatchOptions.from(filterItem);
            if (ItemMatch.matches(movingItem, filterItem, options)) {
                return true;
            }
        }
        return false;
    }

    /** Slot index of the upgrade button, or -1 when upgrades inactive. */
    public int upgradeButtonSlot() {
        if (upgradeService == null || !upgradeService.getConfig().isEnabled()) return -1;
        return filterService.size() - 1;
    }

    /** Returns true if this slot is a locked (not-yet-unlocked) filter slot. */
    public boolean isLockedSlot(HopperKey key, int slot) {
        if (upgradeService == null || !upgradeService.getConfig().isEnabled()) return false;
        int active = activeSlots(key);
        return slot >= active && slot < filterService.size() - 1;
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private int activeSlots(HopperKey key) {
        if (upgradeService == null || !upgradeService.getConfig().isEnabled()) {
            return filterService.size();
        }
        int levelSlots = upgradeService.getLevelData(key).filterSlots();
        // Reserve last slot for upgrade button
        return Math.min(levelSlots, filterService.size() - 1);
    }

    private ItemStack createLockedSlot() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize(lang.getUpgradeLockedSlotName()));
            meta.lore(List.of(LEGACY.deserialize(lang.getUpgradeLockedSlotLore())));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createUpgradeButton(HopperKey key) {
        UpgradeConfig cfg = upgradeService.getConfig();
        int level = upgradeService.getLevel(key);
        UpgradeConfig.LevelData data = cfg.getLevelData(level);
        boolean isMax = level >= cfg.getMaxLevel();

        ItemStack item = new ItemStack(isMax ? Material.NETHER_STAR : Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(LEGACY.deserialize(lang.getUpgradeButtonName()));

        List<Component> lore = new ArrayList<>();
        lore.add(LEGACY.deserialize(lang.getUpgradeButtonLevel(level, cfg.getMaxLevel())));
        lore.add(LEGACY.deserialize(lang.getUpgradeButtonSlots(data.filterSlots())));
        lore.add(LEGACY.deserialize(lang.getUpgradeButtonSpeed(data.itemsPerTransfer())));
        lore.add(Component.empty());
        if (isMax) {
            lore.add(LEGACY.deserialize(lang.getUpgradeButtonMaxLevel()));
        } else {
            UpgradeConfig.LevelData nextData = cfg.getLevelData(level + 1);
            lore.add(LEGACY.deserialize(lang.getUpgradeButtonCost(nextData.cost())));
            lore.add(LEGACY.deserialize(lang.getUpgradeButtonClick()));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static boolean isWarningName(ItemStack itemStack) {
        String name = displayName(itemStack);
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("ignore") || lower.contains("nbt") || lower.contains("durability") || lower.contains("enchant");
    }

    private static String displayName(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        var meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }
        Component display = meta.displayName();
        if (display == null) {
            return null;
        }
        return LEGACY.serialize(display);
    }

    private static ItemStack addListIndicator(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return item;
        }
        ItemStack clone = item.clone();
        FilterMatchOptions options = FilterMatchOptions.from(clone);
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) {
            return clone;
        }

        Component currentDisplay = meta.displayName();
        String displayText = currentDisplay != null ? LEGACY.serialize(currentDisplay) : clone.getType().toString();
        String indicator = options.isBlacklisted() ? "§8● " : "§f○ ";
        meta.displayName(LEGACY.deserialize(indicator + displayText));
        clone.setItemMeta(meta);
        return clone;
    }
}
