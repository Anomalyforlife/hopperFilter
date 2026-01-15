package io.github.anomalyforlife.hopperFilter.gui;

import java.util.Objects;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import io.github.anomalyforlife.hopperFilter.model.HopperKey;

public final class FilterTagSelectGuiHolder implements InventoryHolder {
    private final HopperKey key;
    private final int filterSlot;
    private final int page;
    private final String[] tagByInventorySlot;

    public FilterTagSelectGuiHolder(HopperKey key, int filterSlot, int page, int inventorySize) {
        this.key = Objects.requireNonNull(key, "key");
        this.filterSlot = filterSlot;
        this.page = Math.max(0, page);
        this.tagByInventorySlot = new String[Math.max(0, inventorySize)];
    }

    public HopperKey key() {
        return key;
    }

    public int filterSlot() {
        return filterSlot;
    }

    public int page() {
        return page;
    }

    public void setTagAt(int inventorySlot, String tagName) {
        if (inventorySlot < 0 || inventorySlot >= tagByInventorySlot.length) {
            return;
        }
        tagByInventorySlot[inventorySlot] = tagName;
    }

    public String tagAt(int inventorySlot) {
        if (inventorySlot < 0 || inventorySlot >= tagByInventorySlot.length) {
            return null;
        }
        return tagByInventorySlot[inventorySlot];
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
