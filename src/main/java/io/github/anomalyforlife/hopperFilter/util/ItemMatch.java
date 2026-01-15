package io.github.anomalyforlife.hopperFilter.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.anomalyforlife.hopperFilter.model.FilterMatchOptions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ItemMatch {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private ItemMatch() {
    }

    public static boolean isWildcard(ItemStack filterItem) {
        String name = displayName(filterItem);
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("ignore") || lower.contains("nbt") || lower.contains("durability") || lower.contains("enchant");
    }

    public static boolean matches(ItemStack movingItem, ItemStack filterItem, FilterMatchOptions options) {
        if (movingItem == null || filterItem == null) {
            return false;
        }
        if (isWildcard(filterItem)) {
            return movingItem.getType() == filterItem.getType();
        }
        if (options == null) {
            options = FilterMatchOptions.defaults();
        }
        if (options.matchType() && movingItem.getType() != filterItem.getType()) {
            return false;
        }
        if (options.matchDurability() && !durabilityEquals(movingItem, filterItem)) {
            return false;
        }
        if (options.matchName() && !namesEqual(movingItem, filterItem)) {
            return false;
        }
        if (options.matchNBT() && !nbtEquals(movingItem, filterItem)) {
            return false;
        }
        return true;
    }

    private static boolean durabilityEquals(ItemStack movingItem, ItemStack filterItem) {
        ItemMeta movingMeta = movingItem.getItemMeta();
        ItemMeta filterMeta = filterItem.getItemMeta();
        if (movingMeta instanceof Damageable movingDamage && filterMeta instanceof Damageable filterDamage) {
            return movingDamage.getDamage() == filterDamage.getDamage();
        }
        return true;
    }

    private static boolean namesEqual(ItemStack movingItem, ItemStack filterItem) {
        return Objects.equals(displayName(movingItem), displayName(filterItem));
    }

    private static boolean nbtEquals(ItemStack movingItem, ItemStack filterItem) {
        ItemMeta movingMeta = movingItem.getItemMeta();
        ItemMeta filterMeta = filterItem.getItemMeta();
        if (movingMeta == null && filterMeta == null) {
            return true;
        }
        if (movingMeta == null || filterMeta == null) {
            return false;
        }
        return metaWithoutDisplayAndDamage(movingMeta).equals(metaWithoutDisplayAndDamage(filterMeta));
    }

    private static Map<String, Object> metaWithoutDisplayAndDamage(ItemMeta meta) {
        Map<String, Object> serialized = new HashMap<>(meta.serialize());
        serialized.remove("display");
        serialized.remove("damage");
        return serialized;
    }

    private static String displayName(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        var meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }
        Component display = meta.displayName();
        if (display == null) {
            return null;
        }
        return LEGACY.serialize(display);
    }
}
