package io.github.anomalyforlife.hopperFilter.listeners;

import java.util.List;
import java.util.logging.Logger;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.anomalyforlife.hopperFilter.gui.FilterGui;
import io.github.anomalyforlife.hopperFilter.gui.FilterGuiHolder;
import io.github.anomalyforlife.hopperFilter.gui.FilterMatchConfigGui;
import io.github.anomalyforlife.hopperFilter.gui.FilterMatchConfigGuiHolder;
import io.github.anomalyforlife.hopperFilter.gui.FilterTagSelectGui;
import io.github.anomalyforlife.hopperFilter.gui.FilterTagSelectGuiHolder;
import io.github.anomalyforlife.hopperFilter.model.FilterMatchOptions;
import io.github.anomalyforlife.hopperFilter.model.HopperKey;
import io.github.anomalyforlife.hopperFilter.service.FilterService;
import io.github.anomalyforlife.hopperFilter.upgrade.UpgradeService;
import io.github.anomalyforlife.hopperFilter.util.ItemMatch;
import io.github.anomalyforlife.hopperFilter.util.LanguageManager;
import io.github.anomalyforlife.hopperFilter.util.Messages;

public final class HopperFilterListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(HopperFilterListener.class.getName());

    private volatile FilterService filterService;
    private volatile FilterGui gui;
    private volatile FilterMatchConfigGui configGui;
    private volatile FilterTagSelectGui tagSelectGui;
    private volatile Messages messages;
    private volatile LanguageManager lang;
    private volatile UpgradeService upgradeService; // nullable
    private volatile Plugin plugin;
    private volatile int tntBlockedRadius;
    private volatile String msgCleared;
    private volatile String msgMustSneakToBreak;
    private volatile String msgMustHaveBreakPerm;
    private volatile String msgTooClose;

    public HopperFilterListener(FilterService filterService,
                                UpgradeService upgradeService,
                                Plugin plugin,
                                FilterGui gui,
                                FilterMatchConfigGui configGui,
                                FilterTagSelectGui tagSelectGui,
                                Messages messages,
                                LanguageManager lang,
                                int tntBlockedRadius,
                                String msgCleared,
                                String msgMustSneakToBreak,
                                String msgMustHaveBreakPerm,
                                String msgTooClose) {
        this.filterService = filterService;
        this.upgradeService = upgradeService;
        this.plugin = plugin;
        this.gui = gui;
        this.configGui = configGui;
        this.tagSelectGui = tagSelectGui;
        this.messages = messages;
        this.lang = lang;
        this.tntBlockedRadius = tntBlockedRadius;
        this.msgCleared = msgCleared;
        this.msgMustSneakToBreak = msgMustSneakToBreak;
        this.msgMustHaveBreakPerm = msgMustHaveBreakPerm;
        this.msgTooClose = msgTooClose;
    }

    public synchronized void update(FilterService filterService,
                                    UpgradeService upgradeService,
                                    Plugin plugin,
                                    FilterGui gui,
                                    FilterMatchConfigGui configGui,
                                    FilterTagSelectGui tagSelectGui,
                                    Messages messages,
                                    LanguageManager lang,
                                    int tntBlockedRadius,
                                    String msgCleared,
                                    String msgMustSneakToBreak,
                                    String msgMustHaveBreakPerm,
                                    String msgTooClose) {
        this.filterService = filterService;
        this.upgradeService = upgradeService;
        this.plugin = plugin;
        this.gui = gui;
        this.configGui = configGui;
        this.tagSelectGui = tagSelectGui;
        this.messages = messages;
        this.lang = lang;
        this.tntBlockedRadius = tntBlockedRadius;
        this.msgCleared = msgCleared;
        this.msgMustSneakToBreak = msgMustSneakToBreak;
        this.msgMustHaveBreakPerm = msgMustHaveBreakPerm;
        this.msgTooClose = msgTooClose;
    }

    private record CompiledEntry(ItemStack filterItem, FilterMatchOptions options) {}

    private static List<CompiledEntry> compileFilter(ItemStack[] filter) {
        if (filter == null || filter.length == 0) {
            return java.util.List.of();
        }
        java.util.ArrayList<CompiledEntry> out = new java.util.ArrayList<>(filter.length);
        for (ItemStack it : filter) {
            if (it == null || it.getType().isAir()) continue;
            out.add(new CompiledEntry(it, FilterMatchOptions.from(it)));
        }
        return out;
    }

    private static boolean isActive(List<CompiledEntry> compiled) {
        return compiled != null && !compiled.isEmpty();
    }

    private static boolean allows(List<CompiledEntry> compiled, ItemStack movingItem) {
        if (movingItem == null || movingItem.getType().isAir()) {
            return true;
        }
        if (!isActive(compiled)) {
            return true;
        }

        for (CompiledEntry entry : compiled) {
            if (entry.options().isBlacklisted() && ItemMatch.matches(movingItem, entry.filterItem(), entry.options())) {
                return false;
            }
        }

        for (CompiledEntry entry : compiled) {
            if (!entry.options().isBlacklisted() && ItemMatch.matches(movingItem, entry.filterItem(), entry.options())) {
                return true;
            }
        }

        for (CompiledEntry entry : compiled) {
            if (!entry.options().isBlacklisted()) {
                return false;
            }
        }

        return true;
    }

    private static int findFirstAllowedSlot(Inventory source, Inventory destination, List<CompiledEntry> compiled) {
        if (source == null) return -1;

        ItemStack[] contents = source.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType().isAir()) continue;

            if (!allows(compiled, it)) continue;

            ItemStack one = it.clone();
            one.setAmount(1);
            if (!canFit(destination, one)) continue;

            return i;
        }
        return -1;
    }

    private static boolean canFit(Inventory inventory, ItemStack stack) {
        if (inventory == null) {
            return false;
        }
        if (stack == null || stack.getType().isAir()) {
            return false;
        }

        int max = Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize());
        for (ItemStack existing : inventory.getContents()) {
            if (existing == null || existing.getType().isAir()) {
                return true;
            }
            if (!existing.isSimilar(stack)) {
                continue;
            }
            if (existing.getAmount() < max) {
                return true;
            }
        }
        return false;
    }

    /** Returns items to move per hopper transfer event; default 1 if upgrades are off. */
    private int itemsPerTransfer(HopperKey key) {
        UpgradeService us = upgradeService;
        if (us == null || !us.getConfig().isEnabled()) return 1;
        return us.getLevelData(key).itemsPerTransfer();
    }

    /** Moves one allowed item from source to destination without firing Bukkit events. */
    private void transferOneItem(Inventory source, Inventory destination, List<CompiledEntry> compiled) {
        int slot = findFirstAllowedSlot(source, destination, compiled);
        if (slot == -1) return;
        ItemStack item = source.getItem(slot);
        if (item == null || item.getType().isAir()) return;
        ItemStack one = item.clone();
        one.setAmount(1);
        java.util.Map<Integer, ItemStack> leftover = destination.addItem(one);
        if (!leftover.isEmpty()) return;
        int newAmount = item.getAmount() - 1;
        if (newAmount <= 0) {
            source.setItem(slot, null);
        } else {
            source.getItem(slot).setAmount(newAmount);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        Inventory destination = event.getDestination();

        if (!(destination.getHolder() instanceof Hopper hopperHolder)) {
            // Push direction (hopper → container)
            Inventory source = event.getSource();
            if (!(source.getHolder() instanceof Hopper srcHopper)) return;
            try {
                HopperKey key = HopperKey.fromLocation(srcHopper.getLocation());
                if (!filterService.isFilteredHopper(key)) return;

                int n = itemsPerTransfer(key);
                // Let Minecraft move the 1st item normally; schedule (n-1) extra items for next tick
                for (int i = 1; i < n; i++) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                        transferOneItem(source, destination, List.of()));
                }
            } catch (Exception e) {
                LOGGER.log(java.util.logging.Level.WARNING, "[HopperFilter] Error in InventoryMoveItemEvent", e);
            }
            return;
        }

        try {
            HopperKey key = HopperKey.fromLocation(hopperHolder.getLocation());
            if (!filterService.isFilteredHopper(key)) return;

            Inventory source = event.getSource();
            ItemStack[] filter = filterService.getOrLoadView(key);
            List<CompiledEntry> compiled = compileFilter(filter);

            ItemStack moving = event.getItem();
            if (!allows(compiled, moving)) {
                event.setCancelled(true);
                return;
            }

            // Item is allowed — Minecraft moves it normally (1 item).
            // Schedule (n-1) extra items for upgrade levels.
            int n = itemsPerTransfer(key);
            for (int i = 1; i < n; i++) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                    transferOneItem(source, destination, compiled));
            }
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.WARNING, "[HopperFilter] Error in InventoryMoveItemEvent", e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof Hopper hopperHolder)) {
            return;
        }

        try {
            HopperKey key = HopperKey.fromLocation(hopperHolder.getLocation());
            if (!filterService.isFilteredHopper(key)) return;

            ItemStack[] filter = filterService.getOrLoadView(key);
            List<CompiledEntry> compiled = compileFilter(filter);
            if (!isActive(compiled)) return;

            ItemStack attempted = event.getItem() == null ? null : event.getItem().getItemStack();
            if (!allows(compiled, attempted)) event.setCancelled(true);
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.WARNING, "[HopperFilter] Error in InventoryPickupItemEvent", e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        if (event.getAction().isLeftClick()) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("hopperfilter.opengui")) {
            return;
        }
        if (!player.isSneaking()) {
            return;
        }
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block.getType() != Material.HOPPER) {
            return;
        }

        try {
            HopperKey key = HopperKey.fromLocation(block.getLocation());

            if (filterService.isSpecialHopperRequired() && !filterService.isFilteredHopper(key)) {
                return;
            }

            event.setCancelled(true);
            gui.open(player, key);
        } catch (Exception e) {
            messages.send(player, "§cDB error: " + e.getMessage());
            LOGGER.log(java.util.logging.Level.WARNING, "[HopperFilter] Error opening GUI", e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlaceHopper(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.HOPPER) {
            return;
        }

        if (!filterService.isSpecialHopperRequired()) {
            return;
        }

        ItemStack placedItem = event.getItemInHand();
        if (!filterService.isSpecialHopperItem(placedItem)) {
            return;
        }

        try {
            HopperKey key = HopperKey.fromLocation(event.getBlockPlaced().getLocation());
            filterService.registerFilteredHopper(key, event.getPlayer().getUniqueId());
            if (upgradeService != null) {
                upgradeService.registerHopper(key);
            }
        } catch (Exception e) {
            event.setCancelled(true);
            messages.send(event.getPlayer(), "§cDB error: " + e.getMessage());
            LOGGER.log(java.util.logging.Level.SEVERE, "[HopperFilter] Failed to register filtered hopper location", e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!player.hasPermission("hopperfilter.use")) {
            return;
        }

        if (event.getSlot() < 0) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof FilterGuiHolder holder) {
            event.setCancelled(true);
            try {
                int slot = event.getSlot();
                HopperKey key = holder.key();

                // Upgrade button (last slot, upgrade mode only)
                int upgradeSlot = gui.upgradeButtonSlot();
                if (upgradeSlot >= 0 && slot == upgradeSlot
                        && event.getClickedInventory() != null
                        && event.getClickedInventory().equals(top)) {
                    if (!player.hasPermission("hopperfilter.upgrade")) {
                        messages.actionBar(player, "§c§l✗ §cYou don't have permission to upgrade hoppers.");
                        return;
                    }
                    handleUpgrade(player, key, top);
                    return;
                }

                // Locked slot
                if (gui.isLockedSlot(key, slot)
                        && event.getClickedInventory() != null
                        && event.getClickedInventory().equals(top)) {
                    messages.actionBar(player, lang.getMsgUpgradeLockedSlot());
                    return;
                }

                if (event.getClick() == ClickType.LEFT && event.getClickedInventory() != null && event.getClickedInventory().equals(top)) {
                    configGui.open(player, key, slot);
                    return;
                }

                if (event.getClick() == ClickType.LEFT && event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
                    ItemStack clicked = event.getCurrentItem();
                    gui.addFromPlayerInventory(player, top, clicked);
                    return;
                }

                if (event.getClick().isRightClick() && event.getClickedInventory() != null && event.getClickedInventory().equals(top)) {
                    gui.removeAt(player, top, slot);
                }
            } catch (Exception e) {
                messages.send(player, "§cDB error: " + e.getMessage());
            }
            return;
        }

        if (top.getHolder() instanceof FilterMatchConfigGuiHolder configHolder) {
            event.setCancelled(true);
            try {
                if (event.getSlot() == 8) {
                    tagSelectGui.open(player, configHolder.key(), configHolder.slot());
                    return;
                }
                configGui.handleClick(player, top, configHolder, event.getSlot());
            } catch (Exception e) {
                messages.send(player, "§cDB error: " + e.getMessage());
            }
            return;
        }

        if (top.getHolder() instanceof FilterTagSelectGuiHolder tagHolder) {
            event.setCancelled(true);
            try {
                tagSelectGui.handleClick(player, top, tagHolder, event.getSlot());
            } catch (Exception e) {
                messages.send(player, "§cDB error: " + e.getMessage());
            }
        }
    }

    private void handleUpgrade(Player player, HopperKey key, Inventory currentGui) {
        UpgradeService us = upgradeService;
        if (us == null) return;

        UpgradeService.UpgradeResult result = us.tryUpgrade(player, key);
        switch (result) {
            case SUCCESS -> {
                messages.actionBar(player, lang.getMsgUpgradeSuccess(us.getLevel(key)));
                try {
                    gui.open(player, key); // Refresh GUI to show new level
                } catch (Exception e) {
                    LOGGER.warning("[HopperFilter] Error refreshing GUI after upgrade: " + e.getMessage());
                }
            }
            case MAX_LEVEL -> messages.actionBar(player, lang.getMsgUpgradeMaxLevel());
            case NOT_ENOUGH_MONEY -> {
                int next = us.getLevel(key) + 1;
                double cost = us.getConfig().getLevelData(next).cost();
                messages.actionBar(player, lang.getMsgUpgradeNoMoney(cost));
            }
            case VAULT_NOT_AVAILABLE -> messages.actionBar(player, lang.getMsgUpgradeVaultUnavailable());
            case DB_ERROR -> messages.actionBar(player, lang.getMsgUpgradeError());
            default -> {}
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.HOPPER) {
            return;
        }

        Player player = event.getPlayer();
        try {
            HopperKey key = HopperKey.fromLocation(block.getLocation());

            boolean specialMode = filterService.isSpecialHopperRequired();
            boolean isSpecialFilteredHopper = specialMode && filterService.isFilteredHopper(key);

            if (specialMode && !isSpecialFilteredHopper) {
                return;
            }

            boolean hasAny = filterService.hasAny(key);

            if (hasAny) {
                if (!player.hasPermission("hopperfilter.break")) {
                    messages.actionBar(player, msgMustHaveBreakPerm);
                    event.setCancelled(true);
                    return;
                }
                if (!player.isSneaking()) {
                    messages.actionBar(player, msgMustSneakToBreak);
                    event.setCancelled(true);
                    return;
                }
            }

            if (isSpecialFilteredHopper) {
                event.setDropItems(false);

                try {
                    org.bukkit.block.BlockState state = block.getState();
                    if (state instanceof Hopper hopper) {
                        for (ItemStack item : hopper.getInventory().getContents()) {
                            if (item != null && !item.getType().isAir()) {
                                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), item.clone());
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(java.util.logging.Level.WARNING, "[HopperFilter] Error dropping hopper contents", e);
                }

                if (player.getGameMode() != GameMode.CREATIVE) {
                    ItemStack hopper = new ItemStack(Material.HOPPER, 1);
                    ItemMeta meta = hopper.getItemMeta();
                    if (meta != null) {
                        ItemStack template = filterService.createSpecialHopperItem(1);
                        ItemMeta templateMeta = template.getItemMeta();
                        if (templateMeta != null) {
                            if (templateMeta.hasDisplayName()) {
                                meta.displayName(templateMeta.displayName());
                            }
                            if (templateMeta.hasLore()) {
                                meta.lore(templateMeta.lore());
                            }
                            meta.getPersistentDataContainer().set(
                                io.github.anomalyforlife.hopperFilter.FilteredHopperItem.KEY,
                                org.bukkit.persistence.PersistentDataType.BYTE,
                                (byte) 1
                            );
                        }
                        hopper.setItemMeta(meta);
                    }
                    block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), hopper);
                }
            }

            if (hasAny) {
                filterService.clearAndCache(key);
                messages.actionBar(player, msgCleared);
            }

            if (specialMode) {
                filterService.unregisterFilteredHopper(key);
                if (upgradeService != null) {
                    upgradeService.unregisterHopper(key);
                }
            }

        } catch (Exception e) {
            messages.send(player, "§cDB error: " + e.getMessage());
            event.setCancelled(true);
            LOGGER.log(java.util.logging.Level.WARNING, "[HopperFilter] Error in BlockBreakEvent", e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        try {
            event.blockList().removeIf(block -> {
                if (block.getType() != Material.HOPPER) {
                    return false;
                }
                try {
                    HopperKey key = HopperKey.fromLocation(block.getLocation());

                    if (filterService.isSpecialHopperRequired() && !filterService.isFilteredHopper(key)) {
                        return false;
                    }
                    return filterService.hasAny(key);
                } catch (Exception e) {
                    LOGGER.log(java.util.logging.Level.WARNING, "[HopperFilter] Error checking hopper during explosion", e);
                    return true;
                }
            });
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.WARNING, "[HopperFilter] Error in EntityExplodeEvent", e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlaceTnt(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.TNT) {
            return;
        }

        Player player = event.getPlayer();
        Block origin = event.getBlockPlaced();

        int r = Math.max(0, tntBlockedRadius);
        try {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        Block b = origin.getWorld().getBlockAt(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                        if (b.getType() != Material.HOPPER) {
                            continue;
                        }
                        HopperKey key = HopperKey.fromLocation(b.getLocation());
                        if (filterService.isSpecialHopperRequired() && !filterService.isFilteredHopper(key)) {
                            continue;
                        }
                        if (filterService.hasAny(key)) {
                            event.setCancelled(true);
                            messages.actionBar(player, msgTooClose);
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.WARNING, "[HopperFilter] Error in BlockPlaceEvent (TNT check)", e);
        }
    }
}
