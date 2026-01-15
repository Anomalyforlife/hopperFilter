package io.github.anomalyforlife.hopperFilter.gui;

import java.util.Objects;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import io.github.anomalyforlife.hopperFilter.model.HopperKey;

public final class FilterGuiHolder implements InventoryHolder {
    private final HopperKey key;

    public FilterGuiHolder(HopperKey key) {
        this.key = Objects.requireNonNull(key, "key");
    }

    public HopperKey key() {
        return key;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
