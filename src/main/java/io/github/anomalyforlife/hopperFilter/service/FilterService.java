package io.github.anomalyforlife.hopperFilter.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.anomalyforlife.hopperFilter.FilteredHopperItem;
import io.github.anomalyforlife.hopperFilter.model.HopperKey;
import io.github.anomalyforlife.hopperFilter.storage.HopperFilterStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class FilterService {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final HopperFilterStorage storage;
    private final int size;

    private final String specialHopperName;
    private final List<String> specialHopperLore;

    private final boolean acceptNameLoreFallback;
    private final String specialHopperNameNormalized;
    private final List<String> specialHopperLoreNormalized;

    private final boolean specialHopperRequired;
    private final ConcurrentHashMap<HopperKey, Boolean> filteredHopperCache;

    private final Map<HopperKey, ItemStack[]> cache = new ConcurrentHashMap<>();

    public FilterService(HopperFilterStorage storage,
                         int size,
                         boolean specialHopperRequired,
                         boolean acceptNameLoreFallback,
                         String specialHopperName,
                         List<String> specialHopperLore) throws Exception {
        this.storage = Objects.requireNonNull(storage, "storage");
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        this.size = size;

        this.specialHopperName = specialHopperName;
        this.specialHopperLore = specialHopperLore == null ? java.util.List.of() : java.util.List.copyOf(specialHopperLore);

        this.acceptNameLoreFallback = acceptNameLoreFallback;
        this.specialHopperNameNormalized = normalizeLegacy(specialHopperName);
        this.specialHopperLoreNormalized = this.specialHopperLore.stream()
            .filter(Objects::nonNull)
            .map(FilterService::normalizeLegacy)
            .toList();

        this.specialHopperRequired = specialHopperRequired;
        if (specialHopperRequired) {
            storage.initFilteredHopperLocations();
            Map<HopperKey, Integer> hopperData = storage.loadFilteredHopperLocations();
            this.filteredHopperCache = new ConcurrentHashMap<>(Math.max(16, hopperData.size() * 2));
            for (HopperKey key : hopperData.keySet()) {
                if (key == null) continue;
                this.filteredHopperCache.put(key, Boolean.TRUE);
            }
        } else {
            this.filteredHopperCache = null;
        }
    }

    public int size() {
        return size;
    }

    public ItemStack createSpecialHopperItem(int amount) {
        return FilteredHopperItem.create(amount, specialHopperName, specialHopperLore);
    }

    /**
     * Returns true if the given item is a special (filtered) hopper item.
     * Primary signal is a PDC marker. Optional fallback can match display name + lore.
     */
    public boolean isSpecialHopperItem(ItemStack stack) {
        if (FilteredHopperItem.isSpecial(stack)) {
            return true;
        }
        if (!acceptNameLoreFallback) {
            return false;
        }
        return matchesNameAndLore(stack);
    }

    private boolean matchesNameAndLore(ItemStack stack) {
        if (stack == null || stack.getType() != Material.HOPPER) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (specialHopperNameNormalized != null && !specialHopperNameNormalized.isBlank()) {
            Component displayName = meta.displayName();
            if (displayName == null) {
                return false;
            }
            String actualName = normalizeLegacy(LEGACY.serialize(displayName));
            if (!Objects.equals(specialHopperNameNormalized, actualName)) {
                return false;
            }
        }

        if (!specialHopperLoreNormalized.isEmpty()) {
            List<Component> lore = meta.lore();
            if (lore == null || lore.size() < specialHopperLoreNormalized.size()) {
                return false;
            }
            for (int i = 0; i < specialHopperLoreNormalized.size(); i++) {
                Component line = lore.get(i);
                if (line == null) {
                    return false;
                }
                String actualLine = normalizeLegacy(LEGACY.serialize(line));
                if (!Objects.equals(specialHopperLoreNormalized.get(i), actualLine)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static String normalizeLegacy(String s) {
        if (s == null) {
            return null;
        }
        return s.replace('&', '§');
    }

    public boolean isSpecialHopperRequired() {
        return specialHopperRequired;
    }

    public boolean isFilteredHopper(HopperKey key) {
        if (!specialHopperRequired) {
            return true;
        }
        if (key == null) {
            return false;
        }
        return filteredHopperCache.containsKey(key);
    }

    public void registerFilteredHopper(HopperKey key, java.util.UUID ownerUuid) throws Exception {
        if (!specialHopperRequired) {
            return;
        }
        Objects.requireNonNull(key, "key");
        storage.addFilteredHopperLocation(key, ownerUuid);
        filteredHopperCache.put(key, Boolean.TRUE);
    }

    public void unregisterFilteredHopper(HopperKey key) throws Exception {
        if (!specialHopperRequired) {
            return;
        }
        Objects.requireNonNull(key, "key");
        storage.removeFilteredHopperLocation(key);
        filteredHopperCache.remove(key);
    }

    public ItemStack[] getOrLoad(HopperKey key) throws Exception {
        ItemStack[] cached = cache.get(key);
        if (cached != null) {
            return cloneFilter(cached);
        }
        ItemStack[] loaded = storage.loadFilter(key, size);
        if (loaded == null || loaded.length != size) {
            loaded = new ItemStack[size];
        }
        cache.put(key, cloneFilter(loaded));
        return cloneFilter(loaded);
    }

    /**
     * Returns the cached filter array for read-only usage (no cloning).
     * Callers MUST NOT mutate the returned array or its ItemStacks.
     */
    public ItemStack[] getOrLoadView(HopperKey key) throws Exception {
        ItemStack[] cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        ItemStack[] loaded = storage.loadFilter(key, size);
        if (loaded == null || loaded.length != size) {
            loaded = new ItemStack[size];
        }

        ItemStack[] stored = cloneFilter(loaded);
        cache.put(key, stored);
        return stored;
    }

    public boolean hasAny(HopperKey key) throws Exception {
        ItemStack[] items = getOrLoad(key);
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                return true;
            }
        }
        return false;
    }

    public void saveAndCache(HopperKey key, ItemStack[] items) throws Exception {
        if (items == null || items.length != size) {
            throw new IllegalArgumentException("items must be length " + size);
        }
        storage.saveFilter(key, items);
        cache.put(key, cloneFilter(items));
    }

    public void clearAndCache(HopperKey key) throws Exception {
        storage.deleteFilter(key);
        cache.remove(key);
    }

    public void invalidate(HopperKey key) {
        cache.remove(key);
    }

    private ItemStack[] cloneFilter(ItemStack[] items) {
        ItemStack[] clone = new ItemStack[size];
        Arrays.fill(clone, null);
        for (int i = 0; i < size; i++) {
            ItemStack it = items[i];
            clone[i] = it == null ? null : it.clone();
        }
        return clone;
    }
}
