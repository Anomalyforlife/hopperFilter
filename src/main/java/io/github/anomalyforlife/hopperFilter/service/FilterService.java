package io.github.anomalyforlife.hopperFilter.service;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.inventory.ItemStack;

import io.github.anomalyforlife.hopperFilter.model.HopperKey;
import io.github.anomalyforlife.hopperFilter.storage.HopperFilterStorage;

public final class FilterService {
    private final HopperFilterStorage storage;
    private final int size;
    private final Map<HopperKey, ItemStack[]> cache = new ConcurrentHashMap<>();

    public FilterService(HopperFilterStorage storage, int size) {
        this.storage = storage;
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        this.size = size;
    }

    public int size() {
        return size;
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
