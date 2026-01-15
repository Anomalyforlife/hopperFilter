package io.github.anomalyforlife.hopperFilter.util;

import java.util.Objects;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public final class LanguageManager {
    private final String prefix;
    private final String mainGuiTitle;
    private final String configGuiTitle;
    private final String clickToChange;
    
    private final String msgAdded;
    private final String msgAddedWarning;
    private final String msgAlready;
    private final String msgFull;
    private final String msgRemoved;
    private final String msgCleared;
    
    private final String msgMustSneakToBreak;
    private final String msgMustHaveBreakPerm;
    private final String msgTooCloseToFilteredHopper;
    
    private final String errorDbError;
    private final String errorEmptySlot;
    private final String errorItemNoLongerPresent;
    
    private final String cmdReloadSuccess;
    private final String cmdNoPermission;
    private final String cmdUsage;
    private final String cmdOnlyPlayers;
    private final String cmdLookAtHopper;
    private final String cmdHopperFiltered;
    private final String cmdHopperNotFiltered;
    private final String cmdClearedViaCommand;
    private final String cmdUnknownSubcommand;
    
    private final OptionTexts matchMaterial;
    private final OptionTexts matchDurability;
    private final OptionTexts matchName;
    private final OptionTexts matchNBT;

    public LanguageManager(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        
        // Carica lang.yml da risorse e cartella plugin
        plugin.saveResource("lang.yml", false);
        FileConfiguration config = plugin.getConfig();
        
        // Carica prefisso
        this.prefix = config.getString("prefix", "§7[§dHopperFilter§7] §r");
        
        // GUI
        this.mainGuiTitle = config.getString("gui.main-title", "§c§n§lHopper§r §d§n§lFilter§r");
        this.configGuiTitle = config.getString("gui.config-title", "§6Filter Item Options");
        this.clickToChange = config.getString("gui.click-to-change", "§7Clicca per cambiare.");
        
        // Messaggi
        this.msgAdded = config.getString("messages.added", "§a§lItem added to Filter!");
        this.msgAddedWarning = config.getString("messages.added-warning", "§a§lItem added to Filter! §c§lWARNING: §6§lIt will catch every variant of this item!");
        this.msgAlready = config.getString("messages.already-in-filter", "§6§lItem is already in Filter!");
        this.msgFull = config.getString("messages.filter-full", "§c§lFilter is full!");
        this.msgRemoved = config.getString("messages.removed", "§4§lItem removed from Filter!");
        this.msgCleared = config.getString("messages.cleared", "§a§lFilter cleared!");
        
        this.msgMustSneakToBreak = config.getString("messages.must-sneak-to-break", "§c§lYou must shift to break the Hopper Filter!");
        this.msgMustHaveBreakPerm = config.getString("messages.must-have-break-perm", "§c§lYou must have permission to break the Hopper Filter!");
        this.msgTooCloseToFilteredHopper = config.getString("messages.too-close-to-filtered-hopper", "§c§lYou are too close to a Filtered Hopper!");
        
        // Errori
        this.errorDbError = config.getString("errors.db-error", "§cDB error: {error}");
        this.errorEmptySlot = config.getString("errors.empty-slot", "§cLo slot selezionato è vuoto.");
        this.errorItemNoLongerPresent = config.getString("errors.item-no-longer-present", "§cL'oggetto non è più presente.");
        
        // Comandi
        this.cmdReloadSuccess = config.getString("commands.reload-success", "§aReloaded.");
        this.cmdNoPermission = config.getString("commands.no-permission", "§cNo permission.");
        this.cmdUsage = config.getString("commands.usage", "§cUsage: /{command} <reload|info|clear>");
        this.cmdOnlyPlayers = config.getString("commands.only-players", "§cOnly players can use this.");
        this.cmdLookAtHopper = config.getString("commands.look-at-hopper", "§cLook at a hopper (within 6 blocks).");
        this.cmdHopperFiltered = config.getString("commands.hopper-filtered", "§aThis hopper is filtered.");
        this.cmdHopperNotFiltered = config.getString("commands.hopper-not-filtered", "§7This hopper has no filter.");
        this.cmdClearedViaCommand = config.getString("commands.cleared-via-command", "§aCleared filter.");
        this.cmdUnknownSubcommand = config.getString("commands.unknown-subcommand", "§cUnknown subcommand.");
        
        // Opzioni di matching
        this.matchMaterial = new OptionTexts(
            config.getString("gui.options.match-material.name", "Match Material"),
            config.getString("gui.options.match-material.description", "Richiede lo stesso materiale prima di lasciare passare l'oggetto."),
            config.getString("gui.options.match-material.on", "§aON"),
            config.getString("gui.options.match-material.off", "§cOFF")
        );
        this.matchDurability = new OptionTexts(
            config.getString("gui.options.match-durability.name", "Match Durability"),
            config.getString("gui.options.match-durability.description", "Controlla che la durabilita dell'oggetto coincidano."),
            config.getString("gui.options.match-durability.on", "§aON"),
            config.getString("gui.options.match-durability.off", "§cOFF")
        );
        this.matchName = new OptionTexts(
            config.getString("gui.options.match-name.name", "Match Name"),
            config.getString("gui.options.match-name.description", "Verifica il nome personalizzato dell'oggetto."),
            config.getString("gui.options.match-name.on", "§aON"),
            config.getString("gui.options.match-name.off", "§cOFF")
        );
        this.matchNBT = new OptionTexts(
            config.getString("gui.options.match-nbt.name", "Match NBT"),
            config.getString("gui.options.match-nbt.description", "Considera incantesimi, modelli e altri dati NBT."),
            config.getString("gui.options.match-nbt.on", "§aON"),
            config.getString("gui.options.match-nbt.off", "§cOFF")
        );
    }

    public String getPrefix() {
        return prefix;
    }

    public String getMainGuiTitle() {
        return mainGuiTitle;
    }

    public String getConfigGuiTitle() {
        return configGuiTitle;
    }

    public String getClickToChange() {
        return clickToChange;
    }

    public String getMsgAdded() {
        return msgAdded;
    }

    public String getMsgAddedWarning() {
        return msgAddedWarning;
    }

    public String getMsgAlready() {
        return msgAlready;
    }

    public String getMsgFull() {
        return msgFull;
    }

    public String getMsgRemoved() {
        return msgRemoved;
    }

    public String getMsgCleared() {
        return msgCleared;
    }

    public String getMsgMustSneakToBreak() {
        return msgMustSneakToBreak;
    }

    public String getMsgMustHaveBreakPerm() {
        return msgMustHaveBreakPerm;
    }

    public String getMsgTooCloseToFilteredHopper() {
        return msgTooCloseToFilteredHopper;
    }

    public String getErrorDbError() {
        return errorDbError;
    }

    public String getErrorEmptySlot() {
        return errorEmptySlot;
    }

    public String getErrorItemNoLongerPresent() {
        return errorItemNoLongerPresent;
    }

    public String getCmdReloadSuccess() {
        return cmdReloadSuccess;
    }

    public String getCmdNoPermission() {
        return cmdNoPermission;
    }

    public String getCmdUsage() {
        return cmdUsage;
    }

    public String getCmdOnlyPlayers() {
        return cmdOnlyPlayers;
    }

    public String getCmdLookAtHopper() {
        return cmdLookAtHopper;
    }

    public String getCmdHopperFiltered() {
        return cmdHopperFiltered;
    }

    public String getCmdHopperNotFiltered() {
        return cmdHopperNotFiltered;
    }

    public String getCmdClearedViaCommand() {
        return cmdClearedViaCommand;
    }

    public String getCmdUnknownSubcommand() {
        return cmdUnknownSubcommand;
    }

    public String getCmdDbError(String error) {
        return errorDbError.replace("{error}", error);
    }

    public OptionTexts getMatchMaterial() {
        return matchMaterial;
    }

    public OptionTexts getMatchDurability() {
        return matchDurability;
    }

    public OptionTexts getMatchName() {
        return matchName;
    }

    public OptionTexts getMatchNBT() {
        return matchNBT;
    }

    public static final class OptionTexts {
        private final String name;
        private final String description;
        private final String onState;
        private final String offState;

        public OptionTexts(String name, String description, String onState, String offState) {
            this.name = Objects.requireNonNull(name, "name");
            this.description = Objects.requireNonNull(description, "description");
            this.onState = Objects.requireNonNull(onState, "onState");
            this.offState = Objects.requireNonNull(offState, "offState");
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getOnState() {
            return onState;
        }

        public String getOffState() {
            return offState;
        }
    }
}
