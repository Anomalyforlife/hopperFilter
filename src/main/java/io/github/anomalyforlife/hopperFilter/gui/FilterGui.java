package io.github.anomalyforlife.hopperFilter.gui;

import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.github.anomalyforlife.hopperFilter.model.FilterMatchOptions;
import io.github.anomalyforlife.hopperFilter.model.HopperKey;
import io.github.anomalyforlife.hopperFilter.service.FilterService;
import io.github.anomalyforlife.hopperFilter.util.ItemMatch;
import io.github.anomalyforlife.hopperFilter.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class FilterGui {
    private final FilterService filterService;
    private final Messages messages;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final Component title;

    private final String msgAdded;
    private final String msgAddedWarning;
    private final String msgAlready;
    private final String msgFull;
    private final String msgRemoved;

    public FilterGui(FilterService filterService,
                     Messages messages,
                     Component title,
                     String msgAdded,
                     String msgAddedWarning,
                     String msgAlready,
                     String msgFull,
                     String msgRemoved) {
        this.filterService = filterService;
        this.messages = messages;
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
        Inventory inv = Bukkit.createInventory(new FilterGuiHolder(key), size, title);
        for (int i = 0; i < size; i++) {
            inv.setItem(i, items[i]);
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

        for (ItemStack existing : filter) {
            if (existing != null && !existing.getType().isAir() && clicked.isSimilar(existing)) {
                messages.actionBar(player, msgAlready);
                return;
            }
        }

        int empty = -1;
        for (int i = 0; i < filterService.size(); i++) {
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
        gui.setItem(empty, stored);

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
        if (slot < 0 || slot >= filterService.size()) {
            return;
        }

        HopperKey key = holder.key();
        ItemStack[] filter = filterService.getOrLoad(key);
        if (filter[slot] == null || filter[slot].getType().isAir()) {
            return;
        }

        for (int i = slot; i < filterService.size() - 1; i++) {
            filter[i] = filter[i + 1];
        }
        filter[filterService.size() - 1] = null;

        filterService.saveAndCache(key, filter);
        for (int i = 0; i < filterService.size(); i++) {
            gui.setItem(i, filter[i]);
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
}
