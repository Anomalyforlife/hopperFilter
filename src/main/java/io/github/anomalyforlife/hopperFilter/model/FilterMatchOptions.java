package io.github.anomalyforlife.hopperFilter.model;

import java.util.Objects;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class FilterMatchOptions {
    private static final byte ENABLED = 1;
    private static final byte DISABLED = 0;

    private static NamespacedKey KEY_TYPE;
    private static NamespacedKey KEY_DURABILITY;
    private static NamespacedKey KEY_NBT;
    private static NamespacedKey KEY_NAME;
    private static NamespacedKey KEY_TAG;
    private static NamespacedKey KEY_TAG_NAME;

    private final boolean matchType;
    private final boolean matchDurability;
    private final boolean matchNBT;
    private final boolean matchName;
    private final boolean matchTag;
    private final String matchTagName;

    private FilterMatchOptions(boolean matchType, boolean matchDurability, boolean matchNBT, boolean matchName, boolean matchTag, String matchTagName) {
        this.matchType = matchType;
        this.matchDurability = matchDurability;
        this.matchNBT = matchNBT;
        this.matchName = matchName;
        this.matchTag = matchTag;
        this.matchTagName = matchTagName;
    }

    public static void init(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        KEY_TYPE = new NamespacedKey(plugin, "match_type");
        KEY_DURABILITY = new NamespacedKey(plugin, "match_durability");
        KEY_NBT = new NamespacedKey(plugin, "match_nbt");
        KEY_NAME = new NamespacedKey(plugin, "match_name");
        KEY_TAG = new NamespacedKey(plugin, "match_tag");
        KEY_TAG_NAME = new NamespacedKey(plugin, "match_tag_name");
    }

    private static void ensureInitialized() {
        if (KEY_TYPE == null || KEY_DURABILITY == null || KEY_NBT == null || KEY_NAME == null || KEY_TAG == null || KEY_TAG_NAME == null) {
            throw new IllegalStateException("FilterMatchOptions not initialized");
        }
    }

    public static FilterMatchOptions defaults() {
        return new FilterMatchOptions(true, true, false, false, false, null);
    }

    public static FilterMatchOptions from(ItemStack item) {
        if (item == null) {
            return defaults();
        }
        ensureInitialized();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return defaults();
        }
        var container = meta.getPersistentDataContainer();
        byte type = deserialize(container.get(KEY_TYPE, PersistentDataType.BYTE), ENABLED);
        byte durability = deserialize(container.get(KEY_DURABILITY, PersistentDataType.BYTE), ENABLED);
        byte nbt = deserialize(container.get(KEY_NBT, PersistentDataType.BYTE), DISABLED);
        byte name = deserialize(container.get(KEY_NAME, PersistentDataType.BYTE), DISABLED);
        byte tag = deserialize(container.get(KEY_TAG, PersistentDataType.BYTE), DISABLED);

        String tagName = container.get(KEY_TAG_NAME, PersistentDataType.STRING);
        if (tagName != null && tagName.isBlank()) {
            tagName = null;
        }

        boolean tagEnabled = tag == ENABLED && tagName != null;
        return new FilterMatchOptions(type == ENABLED, durability == ENABLED, nbt == ENABLED, name == ENABLED, tagEnabled, tagName);
    }

    public void applyTo(ItemStack item) {
        if (item == null) {
            return;
        }
        ensureInitialized();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        var container = meta.getPersistentDataContainer();
        container.set(KEY_TYPE, PersistentDataType.BYTE, (byte) (matchType ? ENABLED : DISABLED));
        container.set(KEY_DURABILITY, PersistentDataType.BYTE, (byte) (matchDurability ? ENABLED : DISABLED));
        container.set(KEY_NBT, PersistentDataType.BYTE, (byte) (matchNBT ? ENABLED : DISABLED));
        container.set(KEY_NAME, PersistentDataType.BYTE, (byte) (matchName ? ENABLED : DISABLED));

        boolean tagEnabled = matchTag && matchTagName != null && !matchTagName.isBlank();
        container.set(KEY_TAG, PersistentDataType.BYTE, (byte) (tagEnabled ? ENABLED : DISABLED));
        if (tagEnabled) {
            container.set(KEY_TAG_NAME, PersistentDataType.STRING, matchTagName);
        } else {
            container.remove(KEY_TAG_NAME);
        }
        item.setItemMeta(meta);
    }

    public boolean matchType() {
        return matchType;
    }

    public boolean matchDurability() {
        return matchDurability;
    }

    public boolean matchNBT() {
        return matchNBT;
    }

    public boolean matchName() {
        return matchName;
    }

    public boolean matchTag() {
        return matchTag;
    }

    public String matchTagName() {
        return matchTagName;
    }

    public FilterMatchOptions withMatchType(boolean value) {
        return new FilterMatchOptions(value, matchDurability, matchNBT, matchName, matchTag, matchTagName);
    }

    public FilterMatchOptions withMatchDurability(boolean value) {
        return new FilterMatchOptions(matchType, value, matchNBT, matchName, matchTag, matchTagName);
    }

    public FilterMatchOptions withMatchNBT(boolean value) {
        return new FilterMatchOptions(matchType, matchDurability, value, matchName, matchTag, matchTagName);
    }

    public FilterMatchOptions withMatchName(boolean value) {
        return new FilterMatchOptions(matchType, matchDurability, matchNBT, value, matchTag, matchTagName);
    }

    public FilterMatchOptions withMatchTag(boolean value) {
        return new FilterMatchOptions(matchType, matchDurability, matchNBT, matchName, value, value ? matchTagName : null);
    }

    public FilterMatchOptions withMatchTagName(String tagName) {
        if (tagName != null && tagName.isBlank()) {
            tagName = null;
        }
        return new FilterMatchOptions(matchType, matchDurability, matchNBT, matchName, tagName != null, tagName);
    }

    private static byte deserialize(Byte value, byte defaultValue) {
        return value == null ? defaultValue : value;
    }
}
