package io.github.anomalyforlife.hopperFilter.commands;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.github.anomalyforlife.hopperFilter.model.HopperKey;
import io.github.anomalyforlife.hopperFilter.service.FilterService;
import io.github.anomalyforlife.hopperFilter.util.LanguageManager;
import io.github.anomalyforlife.hopperFilter.util.Messages;

public final class HopperFilterCommand implements CommandExecutor {
    private final Runnable reloadAction;
    private final FilterService filterService;
    private final Messages messages;
    private final LanguageManager languageManager;

    public HopperFilterCommand(Runnable reloadAction, FilterService filterService, Messages messages, LanguageManager languageManager) {
        this.reloadAction = reloadAction;
        this.filterService = filterService;
        this.messages = messages;
        this.languageManager = languageManager;
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
            default -> {
                messages.send(sender, languageManager.getCmdUnknownSubcommand());
                return true;
            }
        }
    }
}
