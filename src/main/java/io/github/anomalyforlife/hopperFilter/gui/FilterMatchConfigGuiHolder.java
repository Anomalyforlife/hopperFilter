package io.github.anomalyforlife.hopperFilter.gui;

import java.util.Objects;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import io.github.anomalyforlife.hopperFilter.model.HopperKey;

public final class FilterMatchConfigGuiHolder implements InventoryHolder {
    private final HopperKey key;
    private final int slot;

    public FilterMatchConfigGuiHolder(HopperKey key, int slot) {
        this.key = Objects.requireNonNull(key, "key");
        this.slot = slot;
    }

    public HopperKey key() {
        return key;
    }

    public int slot() {
        return slot;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
