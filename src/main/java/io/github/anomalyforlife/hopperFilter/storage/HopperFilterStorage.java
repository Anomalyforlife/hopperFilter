package io.github.anomalyforlife.hopperFilter.storage;

import org.bukkit.inventory.ItemStack;

import io.github.anomalyforlife.hopperFilter.model.HopperKey;

public interface HopperFilterStorage extends AutoCloseable {
    void init() throws Exception;

    ItemStack[] loadFilter(HopperKey key, int size) throws Exception;

    void saveFilter(HopperKey key, ItemStack[] items) throws Exception;

    void deleteFilter(HopperKey key) throws Exception;

    @Override
    void close();
}
