package io.github.anomalyforlife.hopperFilter.listeners;

import java.util.List;
import java.util.logging.Logger;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
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

import io.github.anomalyforlife.hopperFilter.gui.FilterGui;
import io.github.anomalyforlife.hopperFilter.gui.FilterGuiHolder;
import io.github.anomalyforlife.hopperFilter.gui.FilterMatchConfigGui;
import io.github.anomalyforlife.hopperFilter.gui.FilterMatchConfigGuiHolder;
import io.github.anomalyforlife.hopperFilter.gui.FilterTagSelectGui;
import io.github.anomalyforlife.hopperFilter.gui.FilterTagSelectGuiHolder;
import io.github.anomalyforlife.hopperFilter.model.FilterMatchOptions;
import io.github.anomalyforlife.hopperFilter.model.HopperKey;
import io.github.anomalyforlife.hopperFilter.service.FilterService;
import io.github.anomalyforlife.hopperFilter.util.ItemMatch;
import io.github.anomalyforlife.hopperFilter.util.Messages;

public final class HopperFilterListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(HopperFilterListener.class.getName());

    // FIX #4: campi volatile per permettere l'aggiornamento atomico al reload
    private volatile FilterService filterService;
    private volatile FilterGui gui;
    private volatile FilterMatchConfigGui configGui;
    private volatile FilterTagSelectGui tagSelectGui;
    private volatile Messages messages;
    private volatile int tntBlockedRadius;
    private volatile String msgCleared;
    private volatile String msgMustSneakToBreak;
    private volatile String msgMustHaveBreakPerm;
    private volatile String msgTooClose;

    public HopperFilterListener(FilterService filterService,
                               FilterGui gui,
                               FilterMatchConfigGui configGui,
                               FilterTagSelectGui tagSelectGui,
                               Messages messages,
                               int tntBlockedRadius,
                               String msgCleared,
                               String msgMustSneakToBreak,
                               String msgMustHaveBreakPerm,
                               String msgTooClose) {
        this.filterService = filterService;
        this.gui = gui;
        this.configGui = configGui;
        this.tagSelectGui = tagSelectGui;
        this.messages = messages;
        this.tntBlockedRadius = tntBlockedRadius;
        this.msgCleared = msgCleared;
        this.msgMustSneakToBreak = msgMustSneakToBreak;
        this.msgMustHaveBreakPerm = msgMustHaveBreakPerm;
        this.msgTooClose = msgTooClose;
    }

    /**
     * FIX #4: aggiorna i riferimenti interni dopo un /hf reload,
     * senza dover ri-registrare il listener con Bukkit.
     */
    public synchronized void update(FilterService filterService,
                                    FilterGui gui,
                                    FilterMatchConfigGui configGui,
                                    FilterTagSelectGui tagSelectGui,
                                    Messages messages,
                                    int tntBlockedRadius,
                                    String msgCleared,
                                    String msgMustSneakToBreak,
                                    String msgMustHaveBreakPerm,
                                    String msgTooClose) {
        this.filterService = filterService;
        this.gui = gui;
        this.configGui = configGui;
        this.tagSelectGui = tagSelectGui;
        this.messages = messages;
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
            if (ItemMatch.matches(movingItem, entry.filterItem(), entry.options())) {
                return true;
            }
        }
        return false;
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        Inventory destination = event.getDestination();
        if (!(destination.getHolder() instanceof Hopper hopperHolder)) {
            return;
        }

        try {
            HopperKey key = HopperKey.fromLocation(hopperHolder.getLocation());

            // Special-hopper mode: ignore non-special hoppers entirely.
            if (!filterService.isFilteredHopper(key)) {
                return;
            }

            ItemStack[] filter = filterService.getOrLoadView(key);
            List<CompiledEntry> compiled = compileFilter(filter);
            if (!isActive(compiled)) {
                return;
            }

            ItemStack attempted = event.getItem();
            if (allows(compiled, attempted)) {
                return;
            }

            Inventory source = event.getSource();
            int sourceSlot = findFirstAllowedSlot(source, destination, compiled);
            if (sourceSlot == -1) {
                event.setCancelled(true);
                return;
            }

            // BUG FIX #10: Re-verify the slot is still valid (race condition guard)
            ItemStack sourceItem = source.getItem(sourceSlot);
            if (sourceItem == null || sourceItem.getType().isAir()) {
                event.setCancelled(true);
                return;
            }

            ItemStack one = sourceItem.clone();
            one.setAmount(1);
            if (!canFit(destination, one)) {
                event.setCancelled(true);
                return;
            }

            // BUG FIX #10: Clone before modifying to avoid race conditions
            ItemStack clonedSourceItem = sourceItem.clone();
            
            event.setCancelled(true);

            int newAmount = clonedSourceItem.getAmount() - 1;
            if (newAmount <= 0) {
                source.setItem(sourceSlot, null);
            } else {
                clonedSourceItem.setAmount(newAmount);
                source.setItem(sourceSlot, clonedSourceItem);
            }

            destination.addItem(one);
        } catch (Exception e) {
            // FIX #2: logga invece di ingoiare silenziosamente
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

            // Special-hopper mode: ignore non-special hoppers entirely.
            if (!filterService.isFilteredHopper(key)) {
                return;
            }

            ItemStack[] filter = filterService.getOrLoadView(key);
            List<CompiledEntry> compiled = compileFilter(filter);
            if (!isActive(compiled)) {
                return;
            }

            ItemStack attempted = event.getItem() == null ? null : event.getItem().getItemStack();
            if (allows(compiled, attempted)) {
                return;
            }

            event.setCancelled(true);
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

            // Modalità hopper speciali: se non è un filtered hopper, lascia aprire la GUI vanilla.
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

        // Spec: if (!requireSpecialHopper) return;
        if (!filterService.isSpecialHopperRequired()) {
            return;
        }

        ItemStack placedItem = event.getItemInHand();
        if (!filterService.isSpecialHopperItem(placedItem)) {
            return;
        }

        try {
            HopperKey key = HopperKey.fromLocation(event.getBlockPlaced().getLocation());
            filterService.registerFilteredHopper(key);
        } catch (Exception e) {
            // If we can't persist it, cancel placement to avoid an untracked special hopper.
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

        // FIX #8: slot negativo (es. drag fuori dall'inventario) — ignora subito
        if (event.getSlot() < 0) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof FilterGuiHolder holder) {
            event.setCancelled(true);
            try {
                if (event.getClick() == ClickType.LEFT && event.getClickedInventory() != null && event.getClickedInventory().equals(top)) {
                    configGui.open(player, holder.key(), event.getSlot());
                    return;
                }

                if (event.getClick() == ClickType.LEFT && event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
                    ItemStack clicked = event.getCurrentItem();
                    gui.addFromPlayerInventory(player, top, clicked);
                    return;
                }

                if (event.getClick().isRightClick() && event.getClickedInventory() != null && event.getClickedInventory().equals(top)) {
                    int slot = event.getSlot();
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

            // Special-hopper mode: ignore non-special hoppers entirely.
            if (specialMode && !isSpecialFilteredHopper) {
                return;
            }

            boolean hasAny = filterService.hasAny(key);

            // Keep the existing behavior: require sneak+perm only if the hopper has an active filter.
            if (hasAny) {
                // FIX #1: "hopperfilter.break" — coerente con plugin.yml (era "hopperfilter.admin.break")
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

            // If this is a special filtered hopper, preserve its identity on drop.
            if (isSpecialFilteredHopper) {
                event.setDropItems(false);
                
                // BUG FIX #9: Drop the hopper contents manually before dropping the special hopper item
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
                    block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), filterService.createSpecialHopperItem(1));
                }
            }

            if (hasAny) {
                filterService.clearAndCache(key);
                messages.actionBar(player, msgCleared);
            }

            // Special-hopper mode: remove from the location table only when breaking is allowed.
            if (specialMode) {
                filterService.unregisterFilteredHopper(key);
            }
        } catch (Exception e) {
            messages.send(player, "§cDB error: " + e.getMessage());
            event.setCancelled(true);
            LOGGER.log(java.util.logging.Level.WARNING, "[HopperFilter] Error in BlockBreakEvent", e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        // FIX #3: rimuovi solo gli hopper filtrati dalla blockList invece di
        //         cancellare l'intera esplosione. I blocchi circostanti vengono
        //         distrutti normalmente.
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
                    // FIX #2: logga l'errore; proteggi il blocco per sicurezza
                    LOGGER.log(java.util.logging.Level.WARNING, "[HopperFilter] Error checking hopper during explosion", e);
                    return true;
                }
            });
        } catch (Exception e) {
            // FIX #2: logga invece di ingoiare silenziosamente
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
            // FIX #2: logga invece di ingoiare silenziosamente
            LOGGER.log(java.util.logging.Level.WARNING, "[HopperFilter] Error in BlockPlaceEvent (TNT check)", e);
        }
    }
}