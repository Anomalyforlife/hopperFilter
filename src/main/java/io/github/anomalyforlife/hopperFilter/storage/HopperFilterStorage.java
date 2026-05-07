package io.github.anomalyforlife.hopperFilter.storage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

import io.github.anomalyforlife.hopperFilter.model.HopperKey;

public interface HopperFilterStorage extends AutoCloseable {
    void init() throws Exception;

    void initFilteredHopperLocations() throws Exception;

    Map<HopperKey, Integer> loadFilteredHopperLocations() throws Exception;

    void addFilteredHopperLocation(HopperKey key, UUID ownerUuid) throws Exception;

    void removeFilteredHopperLocation(HopperKey key) throws Exception;

    void saveHopperLevel(HopperKey key, int level) throws Exception;

    List<HopperKey> loadHopperKeysByOwner(UUID ownerUuid) throws Exception;

    void setAllLevelsByOwner(UUID ownerUuid, int level) throws Exception;

    ItemStack[] loadFilter(HopperKey key, int size) throws Exception;

    void saveFilter(HopperKey key, ItemStack[] items) throws Exception;

    void deleteFilter(HopperKey key) throws Exception;

    @Override
    void close();
}
