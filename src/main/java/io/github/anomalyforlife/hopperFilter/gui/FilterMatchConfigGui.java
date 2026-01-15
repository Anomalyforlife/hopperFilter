package io.github.anomalyforlife.hopperFilter.gui;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

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
import io.github.anomalyforlife.hopperFilter.util.LanguageManager;
import io.github.anomalyforlife.hopperFilter.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class FilterMatchConfigGui {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    
    private final FilterService filterService;
    private final Messages messages;
    private final LanguageManager languageManager;

    public FilterMatchConfigGui(FilterService filterService, Messages messages, LanguageManager languageManager) {
        this.filterService = Objects.requireNonNull(filterService, "filterService");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.languageManager = Objects.requireNonNull(languageManager, "languageManager");
    }

    public void open(Player player, HopperKey key, int slot) throws Exception {
        ItemStack[] filter = filterService.getOrLoad(key);
        if (slot < 0 || slot >= filter.length) {
            return;
        }
        ItemStack item = filter[slot];
        if (item == null || item.getType().isAir()) {
            messages.send(player, languageManager.getErrorEmptySlot());
            return;
        }
        Inventory inventory = Bukkit.createInventory(new FilterMatchConfigGuiHolder(key, slot), 9, LEGACY.deserialize(languageManager.getConfigGuiTitle()));
        populateInventory(inventory, item);
        player.openInventory(inventory);
    }

    public void handleClick(Player player, Inventory inventory, FilterMatchConfigGuiHolder holder, int clickedSlot) throws Exception {
        OptionEntry option = optionForSlot(clickedSlot);
        if (option == null) {
            return;
        }
        ItemStack[] filter = filterService.getOrLoad(holder.key());
        int targetSlot = holder.slot();
        if (targetSlot < 0 || targetSlot >= filter.length) {
            return;
        }
        ItemStack item = filter[targetSlot];
        if (item == null || item.getType().isAir()) {
            messages.send(player, languageManager.getErrorItemNoLongerPresent());
            return;
        }
        FilterMatchOptions current = FilterMatchOptions.from(item);
        FilterMatchOptions updated = option.toggle(current);
        updated.applyTo(item);
        filter[targetSlot] = item;
        filterService.saveAndCache(holder.key(), filter);
        inventory.setItem(option.slot(), option.createItem(updated, languageManager));
        inventory.setItem(4, item.clone());
        messages.actionBar(player, option.message(updated, languageManager));
    }

    private void populateInventory(Inventory inventory, ItemStack entry) {
        FilterMatchOptions options = FilterMatchOptions.from(entry);
        ItemStack filler = createFillerPane();
        inventory.setItem(0, filler.clone());
        inventory.setItem(1, createOptionEntry(1, languageManager.getMatchMaterial(), options).createItem(options, languageManager));
        inventory.setItem(2, createOptionEntry(2, languageManager.getMatchDurability(), options).createItem(options, languageManager));
        inventory.setItem(3, createOptionEntry(3, languageManager.getMatchName(), options).createItem(options, languageManager));
        inventory.setItem(4, entry.clone());
        inventory.setItem(5, createOptionEntry(5, languageManager.getMatchNBT(), options).createItem(options, languageManager));
        inventory.setItem(6, createOptionEntry(6, languageManager.getMatchTag(), options).createItem(options, languageManager));
        inventory.setItem(7, filler.clone());
        inventory.setItem(8, filler.clone());
    }

    private OptionEntry optionForSlot(int slot) {
        return switch (slot) {
            case 1 -> createOptionEntry(1, languageManager.getMatchMaterial(), null);
            case 2 -> createOptionEntry(2, languageManager.getMatchDurability(), null);
            case 3 -> createOptionEntry(3, languageManager.getMatchName(), null);
            case 5 -> createOptionEntry(5, languageManager.getMatchNBT(), null);
            case 6 -> createOptionEntry(6, languageManager.getMatchTag(), null);
            default -> null;
        };
    }

    private OptionEntry createOptionEntry(int slot, LanguageManager.OptionTexts texts, FilterMatchOptions options) {
        Material material = switch (slot) {
            case 1 -> Material.DIAMOND;
            case 2 -> Material.IRON_PICKAXE;
            case 3 -> Material.NAME_TAG;
            case 5 -> Material.ENCHANTED_BOOK;
            case 6 -> Material.CHEST;
            default -> Material.GRAY_DYE;
        };

        Function<FilterMatchOptions, Boolean> getter = switch (slot) {
            case 1 -> FilterMatchOptions::matchType;
            case 2 -> FilterMatchOptions::matchDurability;
            case 3 -> FilterMatchOptions::matchName;
            case 5 -> FilterMatchOptions::matchNBT;
            case 6 -> FilterMatchOptions::matchTag;
            default -> null;
        };

        Function<FilterMatchOptions, FilterMatchOptions> toggler = switch (slot) {
            case 1 -> opts -> opts.withMatchType(!opts.matchType());
            case 2 -> opts -> opts.withMatchDurability(!opts.matchDurability());
            case 3 -> opts -> opts.withMatchName(!opts.matchName());
            case 5 -> opts -> opts.withMatchNBT(!opts.matchNBT());
            case 6 -> opts -> opts.withMatchTag(!opts.matchTag());
            default -> null;
        };

        return new OptionEntry(slot, texts, material, getter, toggler);
    }

    private static ItemStack createFillerPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize("§8"));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static final class OptionEntry {
        private final int slot;
        private final LanguageManager.OptionTexts texts;
        private final Material material;
        private final Function<FilterMatchOptions, Boolean> valueGetter;
        private final Function<FilterMatchOptions, FilterMatchOptions> toggler;

        private OptionEntry(int slot, 
                           LanguageManager.OptionTexts texts,
                           Material material,
                           Function<FilterMatchOptions, Boolean> valueGetter,
                           Function<FilterMatchOptions, FilterMatchOptions> toggler) {
            this.slot = slot;
            this.texts = texts;
            this.material = material;
            this.valueGetter = valueGetter;
            this.toggler = toggler;
        }

        private int slot() {
            return slot;
        }

        private FilterMatchOptions toggle(FilterMatchOptions options) {
            return toggler.apply(options);
        }

        private ItemStack createItem(FilterMatchOptions options, LanguageManager languageManager) {
            boolean enabled = Boolean.TRUE.equals(valueGetter.apply(options));
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null && texts != null) {
                String state = enabled ? texts.getOnState() : texts.getOffState();
                String extra = "";
                if (slot == 6 && enabled) {
                    String tagName = options.matchTagName();
                    if (tagName != null && !tagName.isBlank()) {
                        extra = " §8(" + tagName + ")";
                    }
                }
                meta.displayName(LEGACY.deserialize("§6" + texts.getName() + extra + " §7- " + state));
                List<Component> lore = List.of(
                        LEGACY.deserialize("§7" + texts.getDescription()),
                        LEGACY.deserialize("§7" + languageManager.getClickToChange())
                );
                meta.lore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(meta);
            }
            return item;
        }

        private String message(FilterMatchOptions options, LanguageManager languageManager) {
            boolean enabled = Boolean.TRUE.equals(valueGetter.apply(options));
            if (texts != null) {
                return "§a" + texts.getName() + " " + (enabled ? "attivato" : "disattivato");
            }
            return "§aOpzione " + (enabled ? "attivata" : "disattivata");
        }
    }
}
