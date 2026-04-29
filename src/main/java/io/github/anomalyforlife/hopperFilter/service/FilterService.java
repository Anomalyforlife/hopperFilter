package io.github.anomalyforlife.hopperFilter.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.inventory.ItemStack;

import io.github.anomalyforlife.hopperFilter.FilteredHopperItem;
import io.github.anomalyforlife.hopperFilter.model.HopperKey;
import io.github.anomalyforlife.hopperFilter.storage.HopperFilterStorage;

public final class FilterService {
    private final HopperFilterStorage storage;
    private final int size;

    private final String specialHopperName;
    private final List<String> specialHopperLore;

    private final boolean specialHopperRequired;
    private final ConcurrentHashMap<HopperKey, Boolean> filteredHopperCache;

    private final Map<HopperKey, ItemStack[]> cache = new ConcurrentHashMap<>();

    public FilterService(HopperFilterStorage storage,
                         int size,
                         boolean specialHopperRequired,
                         String specialHopperName,
                         List<String> specialHopperLore) throws Exception {
        this.storage = Objects.requireNonNull(storage, "storage");
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        this.size = size;

        this.specialHopperName = specialHopperName;
        this.specialHopperLore = specialHopperLore == null ? java.util.List.of() : java.util.List.copyOf(specialHopperLore);

        this.specialHopperRequired = specialHopperRequired;
        if (specialHopperRequired) {
            storage.initFilteredHopperLocations();
            Set<HopperKey> keys = storage.loadFilteredHopperLocations();
            this.filteredHopperCache = new ConcurrentHashMap<>(Math.max(16, keys.size() * 2));
            for (HopperKey key : keys) {
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

    public void registerFilteredHopper(HopperKey key) throws Exception {
        if (!specialHopperRequired) {
            return;
        }
        Objects.requireNonNull(key, "key");
        storage.addFilteredHopperLocation(key);
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
