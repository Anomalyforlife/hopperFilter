package io.github.anomalyforlife.hopperFilter;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class HopperConverterItem {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    public static final NamespacedKey KEY = new NamespacedKey("hopperfilter", "hopper_converter");

    private HopperConverterItem() {}

    public static ItemStack create(int amount, String name, List<String> lore, Material material, int customModelData) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        Material mat = (material != null) ? material : Material.HOPPER;
        ItemStack stack = new ItemStack(mat, Math.min(64, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        if (name != null && !name.isBlank()) {
            meta.displayName(LEGACY.deserialize(name));
        }

        if (lore != null && !lore.isEmpty()) {
            List<Component> loreComponents = new ArrayList<>(lore.size());
            for (String line : lore) {
                if (line == null) continue;
                loreComponents.add(LEGACY.deserialize(line));
            }
            meta.lore(loreComponents);
        }

        if (customModelData > 0) {
            var comp = meta.getCustomModelDataComponent();
            comp.setFloats(java.util.List.of((float) customModelData));
            meta.setCustomModelDataComponent(comp);
        }

        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    /** Detects a converter item by its PDC key alone, regardless of material. */
    public static boolean isConverter(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        Byte marker = meta.getPersistentDataContainer().get(KEY, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }
}
