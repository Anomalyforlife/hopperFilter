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
    public static final NamespacedKey LEVEL_KEY = new NamespacedKey("hopperfilter", "upgrade_level");

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

    /** Returns the upgrade level stored in the item's PDC, or 1 if absent. */
    public static int getLevel(ItemStack stack) {
        if (stack == null) return 1;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return 1;
        Integer level = meta.getPersistentDataContainer().get(LEVEL_KEY, PersistentDataType.INTEGER);
        return (level == null || level < 1) ? 1 : level;
    }

    /** Writes the upgrade level into the item's PDC in-place. */
    public static void setLevel(ItemStack stack, int level) {
        if (stack == null) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(LEVEL_KEY, PersistentDataType.INTEGER, level);
        stack.setItemMeta(meta);
    }

    /**
     * Updates the lore to show the upgrade level.
     * If level == 1, removes the level line entirely (item looks as if freshly given).
     * The level line is identified by the LEVEL_KEY PDC entry — we strip any previous
     * level line (detected by the template prefix) and append the new one when level > 1.
     *
     * @param stack       the hopper item to modify in-place
     * @param level       the upgrade level
     * @param loreLine    the formatted lore string (e.g. "§7Upgrade Level: §e3")
     * @param baseLore    the base lore lines from config (used to detect/strip the level line)
     */
    public static void applyLevelLore(ItemStack stack, int level, String loreLine, List<String> baseLore) {
        if (stack == null) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        List<Component> current = meta.lore();
        // Rebuild: keep only base lore lines (drop any previous level line at the end)
        int baseSize = baseLore == null ? 0 : (int) baseLore.stream().filter(l -> l != null).count();
        List<Component> updated = new ArrayList<>();
        if (current != null) {
            // Keep at most baseSize lines (strips any previously appended level line)
            int keep = Math.min(current.size(), baseSize);
            for (int i = 0; i < keep; i++) {
                updated.add(current.get(i));
            }
        }
        if (level > 1 && loreLine != null && !loreLine.isBlank()) {
            updated.add(LEGACY.deserialize(loreLine));
        }
        meta.lore(updated);
        stack.setItemMeta(meta);
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
