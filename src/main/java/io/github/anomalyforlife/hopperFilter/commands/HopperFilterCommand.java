package io.github.anomalyforlife.hopperFilter.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.anomalyforlife.hopperFilter.FilteredHopperItem;
import io.github.anomalyforlife.hopperFilter.HopperConverterItem;
import io.github.anomalyforlife.hopperFilter.model.HopperKey;
import io.github.anomalyforlife.hopperFilter.service.FilterService;
import io.github.anomalyforlife.hopperFilter.upgrade.UpgradeService;
import io.github.anomalyforlife.hopperFilter.util.LanguageManager;
import io.github.anomalyforlife.hopperFilter.util.Messages;

public final class HopperFilterCommand implements CommandExecutor, TabCompleter {
    private final Runnable reloadAction;
    private volatile FilterService filterService;
    private volatile UpgradeService upgradeService;
    private volatile Messages messages;
    private volatile LanguageManager languageManager;

    private volatile String filteredHopperName;
    private volatile List<String> filteredHopperLore;
    private volatile String giveMessageSender;
    private volatile String giveMessageReceiver;

    private volatile int filteredHopperCustomModelData;
    private volatile boolean converterEnabled;
    private volatile String converterName;
    private volatile List<String> converterLore;
    private volatile Material converterMaterial;
    private volatile int converterCustomModelData;
    private volatile String giveConverterMessageSender;
    private volatile String giveConverterMessageReceiver;

    public HopperFilterCommand(Runnable reloadAction,
                              FilterService filterService,
                              UpgradeService upgradeService,
                              Messages messages,
                              LanguageManager languageManager,
                              String filteredHopperName,
                              List<String> filteredHopperLore,
                              String giveMessageSender,
                              String giveMessageReceiver,
                              int filteredHopperCustomModelData,
                              boolean converterEnabled,
                              String converterName,
                              List<String> converterLore,
                              Material converterMaterial,
                              int converterCustomModelData,
                              String giveConverterMessageSender,
                              String giveConverterMessageReceiver) {
        this.reloadAction = reloadAction;
        update(filterService, upgradeService, messages, languageManager,
               filteredHopperName, filteredHopperLore, giveMessageSender, giveMessageReceiver,
               filteredHopperCustomModelData,
               converterEnabled, converterName, converterLore, converterMaterial, converterCustomModelData,
               giveConverterMessageSender, giveConverterMessageReceiver);
    }

    public void update(FilterService filterService,
                       UpgradeService upgradeService,
                       Messages messages,
                       LanguageManager languageManager,
                       String filteredHopperName,
                       List<String> filteredHopperLore,
                       String giveMessageSender,
                       String giveMessageReceiver,
                       int filteredHopperCustomModelData,
                       boolean converterEnabled,
                       String converterName,
                       List<String> converterLore,
                       Material converterMaterial,
                       int converterCustomModelData,
                       String giveConverterMessageSender,
                       String giveConverterMessageReceiver) {
        this.filterService = filterService;
        this.upgradeService = upgradeService;
        this.messages = messages;
        this.languageManager = languageManager;
        this.filteredHopperName = filteredHopperName;
        this.filteredHopperLore = filteredHopperLore;
        this.giveMessageSender = giveMessageSender;
        this.giveMessageReceiver = giveMessageReceiver;
        this.filteredHopperCustomModelData = filteredHopperCustomModelData;
        this.converterEnabled = converterEnabled;
        this.converterName = converterName;
        this.converterLore = converterLore;
        this.converterMaterial = converterMaterial;
        this.converterCustomModelData = converterCustomModelData;
        this.giveConverterMessageSender = giveConverterMessageSender;
        this.giveConverterMessageReceiver = giveConverterMessageReceiver;
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

                ItemStack stack = FilteredHopperItem.create(amount, filteredHopperName, filteredHopperLore, filteredHopperCustomModelData);
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
            case "giveconverter" -> {
                if (!sender.hasPermission("hopperfilter.admin.give")) {
                    messages.send(sender, languageManager.getCmdNoPermission());
                    return true;
                }
                if (!converterEnabled) {
                    messages.send(sender, "§cThe Hopper Converter is disabled in the config.");
                    return true;
                }
                if (!filterService.isSpecialHopperRequired()) {
                    messages.send(sender, "§cThis command is disabled in global mode.");
                    return true;
                }
                if (args.length < 2) {
                    messages.send(sender, "§cUsage: /" + label + " giveconverter <player> [amount]");
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

                ItemStack stack = HopperConverterItem.create(amount, converterName, converterLore, converterMaterial, converterCustomModelData);
                var leftovers = target.getInventory().addItem(stack);
                if (!leftovers.isEmpty()) {
                    for (ItemStack left : leftovers.values()) {
                        if (left == null || left.getType().isAir()) continue;
                        target.getWorld().dropItemNaturally(target.getLocation(), left);
                    }
                }

                String senderMsg = giveConverterMessageSender;
                if (senderMsg == null || senderMsg.isBlank()) {
                    senderMsg = "§aGiven {amount}x Hopper Converter to {player}.";
                }
                senderMsg = senderMsg
                        .replace("{amount}", String.valueOf(amount))
                        .replace("{player}", target.getName());
                messages.send(sender, senderMsg);

                String recvMsg = giveConverterMessageReceiver;
                if (recvMsg == null || recvMsg.isBlank()) {
                    recvMsg = "§aYou received {amount}x Hopper Converter.";
                }
                recvMsg = recvMsg.replace("{amount}", String.valueOf(amount));
                messages.send(target, recvMsg);
                return true;
            }
            case "maxupgrade" -> {
                if (!sender.hasPermission("hopperfilter.giveupgrades.max")) {
                    messages.send(sender, languageManager.getCmdNoPermission());
                    return true;
                }
                if (upgradeService == null) {
                    messages.send(sender, "§cThe upgrade system is not enabled.");
                    return true;
                }
                if (args.length < 2) {
                    messages.send(sender, "§cUsage: /" + label + " maxupgrade <player>");
                    return true;
                }
                UUID targetUuid;
                String targetName;
                Player online = Bukkit.getPlayerExact(args[1]);
                if (online != null) {
                    targetUuid = online.getUniqueId();
                    targetName = online.getName();
                } else {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
                    if (!op.hasPlayedBefore()) {
                        messages.send(sender, "§cPlayer not found.");
                        return true;
                    }
                    targetUuid = op.getUniqueId();
                    targetName = op.getName() != null ? op.getName() : args[1];
                }
                try {
                    int count = upgradeService.upgradeAllToMax(targetUuid);
                    if (count == 0) {
                        messages.send(sender, "§e" + targetName + " has no tracked filtered hoppers. "
                                + "§7(Hoppers placed before owner-tracking was added have no owner data — use /hf upgraderadius instead.)");
                    } else {
                        messages.send(sender, "§aUpgraded §f" + count + "§a hopper(s) of §f" + targetName + "§a to max level.");
                    }
                } catch (Exception e) {
                    messages.send(sender, languageManager.getCmdDbError(e.getMessage()));
                }
                return true;
            }
            case "upgraderadius" -> {
                if (!sender.hasPermission("hopperfilter.giveupgrades.max")) {
                    messages.send(sender, languageManager.getCmdNoPermission());
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    messages.send(sender, languageManager.getCmdOnlyPlayers());
                    return true;
                }
                if (upgradeService == null) {
                    messages.send(sender, "§cThe upgrade system is not enabled.");
                    return true;
                }
                if (args.length < 2) {
                    messages.send(sender, "§cUsage: /" + label + " upgraderadius <radius>");
                    return true;
                }
                double radius;
                try {
                    radius = Double.parseDouble(args[1]);
                } catch (NumberFormatException e) {
                    messages.send(sender, "§cInvalid radius: must be a number.");
                    return true;
                }
                if (radius <= 0 || radius > 500) {
                    messages.send(sender, "§cRadius must be between 1 and 500.");
                    return true;
                }
                try {
                    int count = upgradeService.upgradeInRadiusToMax(player.getLocation(), radius);
                    if (count == 0) {
                        messages.send(sender, "§eNo filtered hoppers found within §f" + (int) radius + "§e blocks.");
                    } else {
                        messages.send(sender, "§aUpgraded §f" + count + "§a hopper(s) within §f" + (int) radius + "§a blocks to max level.");
                    }
                } catch (Exception e) {
                    messages.send(sender, languageManager.getCmdDbError(e.getMessage()));
                }
                return true;
            }
            case "converthopper" -> {
                if (!sender.hasPermission("hopperfilter.admin.convert")) {
                    messages.send(sender, languageManager.getCmdNoPermission());
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    messages.send(sender, languageManager.getCmdOnlyPlayers());
                    return true;
                }
                if (!filterService.isSpecialHopperRequired()) {
                    messages.send(sender, "§eAll hoppers are already filtered in global mode.");
                    return true;
                }
                Block target = player.getTargetBlockExact(6);
                if (target == null || target.getType() != Material.HOPPER) {
                    messages.send(sender, languageManager.getCmdLookAtHopper());
                    return true;
                }
                HopperKey key = HopperKey.fromLocation(target.getLocation());
                try {
                    if (filterService.isFilteredHopper(key)) {
                        messages.send(sender, "§eThis hopper is already a filtered hopper.");
                        return true;
                    }
                    filterService.registerFilteredHopper(key, player.getUniqueId());
                    if (upgradeService != null) upgradeService.registerHopper(key, 1);
                    messages.send(sender, "§aHopper converted to filtered hopper.");
                } catch (Exception e) {
                    messages.send(sender, languageManager.getCmdDbError(e.getMessage()));
                }
                return true;
            }
            case "convertradius" -> {
                if (!sender.hasPermission("hopperfilter.admin.convert")) {
                    messages.send(sender, languageManager.getCmdNoPermission());
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    messages.send(sender, languageManager.getCmdOnlyPlayers());
                    return true;
                }
                if (!filterService.isSpecialHopperRequired()) {
                    messages.send(sender, "§eAll hoppers are already filtered in global mode.");
                    return true;
                }
                if (args.length < 2) {
                    messages.send(sender, "§cUsage: /" + label + " convertradius <radius>");
                    return true;
                }
                double radius;
                try {
                    radius = Double.parseDouble(args[1]);
                } catch (NumberFormatException e) {
                    messages.send(sender, "§cInvalid radius: must be a number.");
                    return true;
                }
                if (radius <= 0 || radius > 500) {
                    messages.send(sender, "§cRadius must be between 1 and 500.");
                    return true;
                }
                int r = (int) Math.ceil(radius);
                org.bukkit.Location center = player.getLocation();
                int converted = 0;
                int alreadyFiltered = 0;
                try {
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dy = -r; dy <= r; dy++) {
                            for (int dz = -r; dz <= r; dz++) {
                                if (Math.sqrt(dx * dx + dy * dy + dz * dz) > radius) continue;
                                Block b = center.getWorld().getBlockAt(
                                        center.getBlockX() + dx,
                                        center.getBlockY() + dy,
                                        center.getBlockZ() + dz);
                                if (b.getType() != Material.HOPPER) continue;
                                HopperKey k = HopperKey.fromLocation(b.getLocation());
                                if (filterService.isFilteredHopper(k)) { alreadyFiltered++; continue; }
                                filterService.registerFilteredHopper(k, player.getUniqueId());
                                if (upgradeService != null) upgradeService.registerHopper(k, 1);
                                converted++;
                            }
                        }
                    }
                } catch (Exception e) {
                    messages.send(sender, languageManager.getCmdDbError(e.getMessage()));
                    return true;
                }
                if (converted == 0 && alreadyFiltered == 0) {
                    messages.send(sender, "§eNo hoppers found within §f" + (int) radius + "§e blocks.");
                } else if (converted == 0) {
                    messages.send(sender, "§eAll §f" + alreadyFiltered + "§e hopper(s) within §f" + (int) radius + "§e blocks are already filtered.");
                } else {
                    messages.send(sender, "§aConverted §f" + converted + "§a hopper(s) within §f" + (int) radius + "§a blocks."
                            + (alreadyFiltered > 0 ? " §7(" + alreadyFiltered + " already filtered)" : ""));
                }
                return true;
            }
            default -> {
                messages.send(sender, languageManager.getCmdUnknownSubcommand());
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return new ArrayList<>(List.of(
                    "reload", "info", "clear", "give", "giveconverter",
                    "converthopper", "convertradius", "maxupgrade", "upgraderadius"
            )).stream().filter(s -> s.startsWith(partial)).collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("give") || sub.equals("giveconverter") || sub.equals("maxupgrade")) {
                String partial = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
            }
            if (sub.equals("upgraderadius") || sub.equals("convertradius")) {
                return List.of("5", "10", "20", "50", "100");
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("give") || sub.equals("giveconverter")) {
                return List.of("1", "16", "32", "64");
            }
        }
        return List.of();
    }
}
