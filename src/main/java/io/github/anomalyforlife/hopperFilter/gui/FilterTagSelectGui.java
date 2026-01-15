package io.github.anomalyforlife.hopperFilter.gui;

import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.anomalyforlife.hopperFilter.model.FilterMatchOptions;
import io.github.anomalyforlife.hopperFilter.model.HopperKey;
import io.github.anomalyforlife.hopperFilter.service.FilterService;
import io.github.anomalyforlife.hopperFilter.util.ItemMatch;
import io.github.anomalyforlife.hopperFilter.util.LanguageManager;
import io.github.anomalyforlife.hopperFilter.util.Messages;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class FilterTagSelectGui {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private static final int SLOT_BACK = 45;
    private static final int SLOT_PREV = 47;
    private static final int SLOT_PAGE = 49;
    private static final int SLOT_NEXT = 51;
    private static final int SLOT_DISABLE = 53;

    private final FilterService filterService;
    private final FilterMatchConfigGui configGui;
    private final Messages messages;
    private final LanguageManager languageManager;

    public FilterTagSelectGui(FilterService filterService,
                             FilterMatchConfigGui configGui,
                             Messages messages,
                             LanguageManager languageManager) {
        this.filterService = Objects.requireNonNull(filterService, "filterService");
        this.configGui = Objects.requireNonNull(configGui, "configGui");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.languageManager = Objects.requireNonNull(languageManager, "languageManager");
    }

    public void open(Player player, HopperKey key, int filterSlot) throws Exception {
        open(player, key, filterSlot, 0);
    }

    private void open(Player player, HopperKey key, int filterSlot, int page) throws Exception {
        ItemStack[] filter = filterService.getOrLoad(key);
        if (filterSlot < 0 || filterSlot >= filter.length) {
            return;
        }
        ItemStack item = filter[filterSlot];
        if (item == null || item.getType().isAir()) {
            messages.send(player, languageManager.getErrorEmptySlot());
            return;
        }

        int size = 54;
        int safePage = Math.max(0, page);
        FilterTagSelectGuiHolder holder = new FilterTagSelectGuiHolder(key, filterSlot, safePage, size);
        Inventory inv = Bukkit.createInventory(holder, size, LEGACY.deserialize(languageManager.getTagSelectTitle()));

        FilterMatchOptions options = FilterMatchOptions.from(item);
        String selected = options.matchTagName();

        List<String> tags = ItemMatch.materialTagNames(item.getType());
        int pageSize = 45;
        int maxPage = tags.isEmpty() ? 0 : (tags.size() - 1) / pageSize;
        if (safePage > maxPage) {
            safePage = maxPage;
            holder = new FilterTagSelectGuiHolder(key, filterSlot, safePage, size);
            inv = Bukkit.createInventory(holder, size, LEGACY.deserialize(languageManager.getTagSelectTitle()));
        }

        int from = safePage * pageSize;
        int to = Math.min(from + pageSize, tags.size());
        int index = 0;
        for (int i = from; i < to; i++) {
            String tagName = tags.get(i);
            int slot = index;
            holder.setTagAt(slot, tagName);
            inv.setItem(slot, tagItem(item.getType(), tagName, Objects.equals(selected, tagName)));
            index++;
        }

        inv.setItem(SLOT_BACK, navItem(Material.ARROW, languageManager.getTagSelectBack()));
        if (safePage > 0) {
            inv.setItem(SLOT_PREV, navItem(Material.ARROW, languageManager.getTagSelectPrev()));
        }
        if (safePage < maxPage) {
            inv.setItem(SLOT_NEXT, navItem(Material.ARROW, languageManager.getTagSelectNext()));
        }
        inv.setItem(SLOT_PAGE, navItem(Material.PAPER, languageManager.getTagSelectPage(safePage + 1, maxPage + 1)));
        inv.setItem(SLOT_DISABLE, navItem(Material.BARRIER, languageManager.getTagSelectDisable()));

        player.openInventory(inv);
    }

    public void handleClick(Player player, Inventory inventory, FilterTagSelectGuiHolder holder, int clickedSlot) throws Exception {
        if (clickedSlot == SLOT_BACK) {
            configGui.open(player, holder.key(), holder.filterSlot());
            return;
        }

        if (clickedSlot == SLOT_PREV && holder.page() > 0) {
            open(player, holder.key(), holder.filterSlot(), holder.page() - 1);
            return;
        }

        if (clickedSlot == SLOT_NEXT) {
            open(player, holder.key(), holder.filterSlot(), holder.page() + 1);
            return;
        }

        ItemStack[] filter = filterService.getOrLoad(holder.key());
        int filterSlot = holder.filterSlot();
        if (filterSlot < 0 || filterSlot >= filter.length) {
            return;
        }
        ItemStack item = filter[filterSlot];
        if (item == null || item.getType().isAir()) {
            messages.send(player, languageManager.getErrorItemNoLongerPresent());
            return;
        }

        FilterMatchOptions current = FilterMatchOptions.from(item);

        if (clickedSlot == SLOT_DISABLE) {
            FilterMatchOptions updated = current.withMatchTag(false);
            updated.applyTo(item);
            filter[filterSlot] = item;
            filterService.saveAndCache(holder.key(), filter);
            configGui.open(player, holder.key(), filterSlot);
            return;
        }

        String tagName = holder.tagAt(clickedSlot);
        if (tagName == null) {
            return;
        }

        FilterMatchOptions updated = current.withMatchTagName(tagName);
        updated.applyTo(item);
        filter[filterSlot] = item;
        filterService.saveAndCache(holder.key(), filter);

        configGui.open(player, holder.key(), filterSlot);
    }

    private static ItemStack navItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize(name));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack tagItem(Material base, String tagName, boolean selected) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize((selected ? "§a" : "§e") + tagName));
            meta.lore(List.of(
                    LEGACY.deserialize(languageManager.getTagSelectItemLine(base.name())),
                    LEGACY.deserialize(selected ? languageManager.getTagSelectSelectedLine() : languageManager.getTagSelectClickToSelectLine())
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
}
