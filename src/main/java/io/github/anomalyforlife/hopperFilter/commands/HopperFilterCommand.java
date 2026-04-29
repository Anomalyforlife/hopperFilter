package io.github.anomalyforlife.hopperFilter.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.anomalyforlife.hopperFilter.FilteredHopperItem;
import io.github.anomalyforlife.hopperFilter.model.HopperKey;
import io.github.anomalyforlife.hopperFilter.service.FilterService;
import io.github.anomalyforlife.hopperFilter.util.LanguageManager;
import io.github.anomalyforlife.hopperFilter.util.Messages;

public final class HopperFilterCommand implements CommandExecutor {
    private final Runnable reloadAction;
    private volatile FilterService filterService;
    private volatile Messages messages;
    private volatile LanguageManager languageManager;

    private volatile String filteredHopperName;
    private volatile List<String> filteredHopperLore;
    private volatile String giveMessageSender;
    private volatile String giveMessageReceiver;

    public HopperFilterCommand(Runnable reloadAction,
                              FilterService filterService,
                              Messages messages,
                              LanguageManager languageManager,
                              String filteredHopperName,
                              List<String> filteredHopperLore,
                              String giveMessageSender,
                              String giveMessageReceiver) {
        this.reloadAction = reloadAction;
        update(filterService, messages, languageManager, filteredHopperName, filteredHopperLore, giveMessageSender, giveMessageReceiver);
    }

    public void update(FilterService filterService,
                       Messages messages,
                       LanguageManager languageManager,
                       String filteredHopperName,
                       List<String> filteredHopperLore,
                       String giveMessageSender,
                       String giveMessageReceiver) {
        this.filterService = filterService;
        this.messages = messages;
        this.languageManager = languageManager;
        this.filteredHopperName = filteredHopperName;
        this.filteredHopperLore = filteredHopperLore;
        this.giveMessageSender = giveMessageSender;
        this.giveMessageReceiver = giveMessageReceiver;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            messages.send(sender, languageManager.getCmdUsage());
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("hopperfilter.admin.reload")) {
                    messages.send(sender, languageManager.getCmdNoPermission());
                    return true;
                }
                reloadAction.run();
                messages.send(sender, languageManager.getCmdReloadSuccess());
                return true;
            }
            case "info" -> {
                if (!sender.hasPermission("hopperfilter.admin.info")) {
                    messages.send(sender, languageManager.getCmdNoPermission());
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    messages.send(sender, languageManager.getCmdOnlyPlayers());
                    return true;
                }
                Block target = player.getTargetBlockExact(6);
                if (target == null || target.getType() != Material.HOPPER) {
                    messages.send(sender, languageManager.getCmdLookAtHopper());
                    return true;
                }
                HopperKey key = HopperKey.fromLocation(target.getLocation());
                try {
                    boolean active = filterService.hasAny(key);
                    messages.send(sender, active ? languageManager.getCmdHopperFiltered() : languageManager.getCmdHopperNotFiltered());
                } catch (Exception e) {
                    messages.send(sender, languageManager.getCmdDbError(e.getMessage()));
                }
                return true;
            }
            case "clear" -> {
                if (!sender.hasPermission("hopperfilter.admin.clear")) {
                    messages.send(sender, languageManager.getCmdNoPermission());
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    messages.send(sender, languageManager.getCmdOnlyPlayers());
                    return true;
                }
                Block target = player.getTargetBlockExact(6);
                if (target == null || target.getType() != Material.HOPPER) {
                    messages.send(sender, languageManager.getCmdLookAtHopper());
                    return true;
                }
                HopperKey key = HopperKey.fromLocation(target.getLocation());
                try {
                    filterService.clearAndCache(key);
                    messages.send(sender, languageManager.getCmdClearedViaCommand());
                } catch (Exception e) {
                    messages.send(sender, languageManager.getCmdDbError(e.getMessage()));
                }
                return true;
            }
            case "give" -> {
                if (!sender.hasPermission("hopperfilter.admin.give")) {
                    messages.send(sender, languageManager.getCmdNoPermission());
                    return true;
                }

                // Spec: if global mode -> disabled or error message
                if (!filterService.isSpecialHopperRequired()) {
                    messages.send(sender, "§cThis command is disabled in global mode.");
                    return true;
                }

                if (args.length < 2) {
                    messages.send(sender, "§cUsage: /" + label + " give <player> [amount]");
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    messages.send(sender, "§cPlayer not found.");
                    return true;
                }

                int amount = 1;
                if (args.length >= 3) {
                    try {
                        amount = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ignored) {
                        amount = 1;
                    }
                }
                if (amount < 1) amount = 1;
                if (amount > 64) amount = 64;

                ItemStack stack = FilteredHopperItem.create(amount, filteredHopperName, filteredHopperLore);
                var leftovers = target.getInventory().addItem(stack);
                if (!leftovers.isEmpty()) {
                    for (ItemStack left : leftovers.values()) {
                        if (left == null || left.getType().isAir()) continue;
                        target.getWorld().dropItemNaturally(target.getLocation(), left);
                    }
                }

                String senderMsg = giveMessageSender;
                if (senderMsg == null || senderMsg.isBlank()) {
                    senderMsg = "§aGiven {amount}x Filtered Hopper to {player}.";
                }
                senderMsg = senderMsg
                        .replace("{amount}", String.valueOf(amount))
                        .replace("{player}", target.getName());
                messages.send(sender, senderMsg);

                String recvMsg = giveMessageReceiver;
                if (recvMsg == null || recvMsg.isBlank()) {
                    recvMsg = "§aYou received {amount}x Filtered Hopper.";
                }
                recvMsg = recvMsg.replace("{amount}", String.valueOf(amount));
                messages.send(target, recvMsg);
                return true;
            }
            default -> {
                messages.send(sender, languageManager.getCmdUnknownSubcommand());
                return true;
            }
        }
    }
}
