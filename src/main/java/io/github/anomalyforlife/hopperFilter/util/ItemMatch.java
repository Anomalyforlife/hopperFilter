package io.github.anomalyforlife.hopperFilter.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.Tag;
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

        boolean sameType = movingItem.getType() == filterItem.getType();
        if (options.matchTag()) {
            String selectedTag = options.matchTagName();
            if (selectedTag == null || selectedTag.isBlank()) {
                if (!sameType) {
                    return false;
                }
            } else {
                if (!sameType && !bothInTag(selectedTag, filterItem.getType(), movingItem.getType())) {
                    return false;
                }
            }
        } else if (options.matchType() && !sameType) {
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

    public static List<String> materialTagNames(Material material) {
        if (material == null) {
            return List.of();
        }

        List<String> names = new ArrayList<>();
        for (Field field : Tag.class.getFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Object value;
            try {
                value = field.get(null);
            } catch (IllegalAccessException e) {
                continue;
            }
            if (!(value instanceof Tag<?> rawTag)) {
                continue;
            }
            try {
                @SuppressWarnings("unchecked")
                Tag<Material> materialTag = (Tag<Material>) rawTag;
                if (materialTag.isTagged(material)) {
                    names.add(field.getName());
                }
            } catch (Throwable ignored) {
            }
        }

        Collections.sort(names);
        return List.copyOf(names);
    }

    private static boolean bothInTag(String tagFieldName, Material a, Material b) {
        Tag<Material> tag = getMaterialTagByFieldName(tagFieldName);
        if (tag == null) {
            return false;
        }
        return tag.isTagged(a) && tag.isTagged(b);
    }

    private static final Map<String, Tag<Material>> MATERIAL_TAG_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> MATERIAL_TAG_MISSING = ConcurrentHashMap.newKeySet();

    @SuppressWarnings("unchecked")
    private static Tag<Material> getMaterialTagByFieldName(String tagFieldName) {
        if (tagFieldName == null || tagFieldName.isBlank()) {
            return null;
        }
        if (MATERIAL_TAG_MISSING.contains(tagFieldName)) {
            return null;
        }

        Tag<Material> cached = MATERIAL_TAG_CACHE.get(tagFieldName);
        if (cached != null) {
            return cached;
        }

        try {
            Field field = Tag.class.getField(tagFieldName);
            Object value = field.get(null);
            if (value instanceof Tag<?>) {
                Tag<Material> tag = (Tag<Material>) value;
                MATERIAL_TAG_CACHE.put(tagFieldName, tag);
                return tag;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        MATERIAL_TAG_MISSING.add(tagFieldName);
        return null;
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
        serialized.remove("PublicBukkitValues");
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
