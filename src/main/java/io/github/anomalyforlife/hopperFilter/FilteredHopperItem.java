package io.github.anomalyforlife.hopperFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class FilteredHopperItem {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    // Spec-required key: NamespacedKey("hopperfilter", "filtered_hopper")
    public static final NamespacedKey KEY = new NamespacedKey("hopperfilter", "filtered_hopper");

    private FilteredHopperItem() {
    }

    public static ItemStack create(int amount, String name, List<String> lore) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
        ItemStack stack = new ItemStack(Material.HOPPER, Math.min(64, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

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

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY, PersistentDataType.BYTE, (byte) 1);

        stack.setItemMeta(meta);
        return stack;
    }

    public static boolean isSpecial(ItemStack stack) {
        if (stack == null || stack.getType() != Material.HOPPER) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte marker = meta.getPersistentDataContainer().get(KEY, PersistentDataType.BYTE);
        return Objects.equals(marker, (byte) 1);
    }
}
