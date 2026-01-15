package io.github.anomalyforlife.hopperFilter.listeners;

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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.github.anomalyforlife.hopperFilter.gui.FilterGui;
import io.github.anomalyforlife.hopperFilter.gui.FilterGuiHolder;
import io.github.anomalyforlife.hopperFilter.gui.FilterMatchConfigGui;
import io.github.anomalyforlife.hopperFilter.gui.FilterMatchConfigGuiHolder;
import io.github.anomalyforlife.hopperFilter.gui.FilterTagSelectGui;
import io.github.anomalyforlife.hopperFilter.gui.FilterTagSelectGuiHolder;
import io.github.anomalyforlife.hopperFilter.model.HopperKey;
import io.github.anomalyforlife.hopperFilter.service.FilterService;
import io.github.anomalyforlife.hopperFilter.util.Messages;

public final class HopperFilterListener implements Listener {
    private final FilterService filterService;
    private final FilterGui gui;
    private final FilterMatchConfigGui configGui;
    private final FilterTagSelectGui tagSelectGui;
    private final Messages messages;

    private final int tntBlockedRadius;

    private final String msgCleared;
    private final String msgMustSneakToBreak;
    private final String msgMustHaveBreakPerm;
    private final String msgTooClose;

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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        Inventory destination = event.getDestination();
        if (!(destination.getHolder() instanceof Hopper hopperHolder)) {
            return;
        }

        try {
            HopperKey key = HopperKey.fromLocation(hopperHolder.getLocation());
            if (!filterService.hasAny(key)) {
                return;
            }
            ItemStack item = event.getItem();
            if (!gui.allows(key, item)) {
                event.setCancelled(true);
            }
        } catch (Exception e) {
            // Fail open to avoid breaking hoppers due to DB issues.
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

        event.setCancelled(true);
        try {
            gui.open(player, HopperKey.fromLocation(block.getLocation()));
        } catch (Exception e) {
            messages.send(player, "§cDB error: " + e.getMessage());
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

        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof FilterGuiHolder holder) {
            event.setCancelled(true);
            try {
                if (event.getClick() == ClickType.MIDDLE && event.getClickedInventory() != null && event.getClickedInventory().equals(top)) {
                    configGui.open(player, holder.key(), event.getSlot());
                    return;
                }

                // Add item: left click in player's inventory
                if (event.getClick() == ClickType.LEFT && event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
                    ItemStack clicked = event.getCurrentItem();
                    gui.addFromPlayerInventory(player, top, clicked);
                    return;
                }

                // Remove item: right click inside GUI
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
                if (event.getSlot() == 6) {
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
            if (!filterService.hasAny(key)) {
                return;
            }

            if (!player.hasPermission("hopperfilter.admin.break")) {
                messages.actionBar(player, msgMustHaveBreakPerm);
                event.setCancelled(true);
                return;
            }
            if (!player.isSneaking()) {
                messages.actionBar(player, msgMustSneakToBreak);
                event.setCancelled(true);
                return;
            }

            filterService.clearAndCache(key);
            messages.actionBar(player, msgCleared);
        } catch (Exception e) {
            // If DB is down, don't allow breaking to avoid orphaned data confusion.
            messages.send(player, "§cDB error: " + e.getMessage());
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        try {
            for (Block block : event.blockList()) {
                if (block.getType() != Material.HOPPER) {
                    continue;
                }
                HopperKey key = HopperKey.fromLocation(block.getLocation());
                if (filterService.hasAny(key)) {
                    event.setCancelled(true);
                    return;
                }
            }
        } catch (Exception e) {
            // ignore
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
                        if (filterService.hasAny(key)) {
                            event.setCancelled(true);
                            messages.actionBar(player, msgTooClose);
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }
}
