package io.github.anomalyforlife.hopperFilter.storage;

import org.bukkit.inventory.ItemStack;

import io.github.anomalyforlife.hopperFilter.model.HopperKey;

public interface HopperFilterStorage extends AutoCloseable {
    void init() throws Exception;

    /**
     * Initializes the table used to persist locations of "special" filtered hoppers.
     * Only required when special-hopper mode is enabled.
     */
    void initFilteredHopperLocations() throws Exception;

    /**
     * Loads all persisted special filtered hopper locations together with their upgrade level.
     * Only required when special-hopper mode is enabled.
     */
    java.util.Map<HopperKey, Integer> loadFilteredHopperLocations() throws Exception;

    /**
     * Persists a location as a special filtered hopper (default level 1).
     */
    void addFilteredHopperLocation(HopperKey key) throws Exception;

    /**
     * Removes a location from the special filtered hopper table.
     */
    void removeFilteredHopperLocation(HopperKey key) throws Exception;

    /**
     * Updates the upgrade level for an existing special hopper location.
     */
    void saveHopperLevel(HopperKey key, int level) throws Exception;

    ItemStack[] loadFilter(HopperKey key, int size) throws Exception;

    void saveFilter(HopperKey key, ItemStack[] items) throws Exception;

    void deleteFilter(HopperKey key) throws Exception;

    @Override
    void close();
}
