package io.github.anomalyforlife.hopperFilter;

import java.io.File;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.plugin.java.JavaPlugin;

import com.zaxxer.hikari.HikariConfig;

import io.github.anomalyforlife.hopperFilter.commands.HopperFilterCommand;
import io.github.anomalyforlife.hopperFilter.gui.FilterGui;
import io.github.anomalyforlife.hopperFilter.gui.FilterMatchConfigGui;
import io.github.anomalyforlife.hopperFilter.gui.FilterTagSelectGui;
import io.github.anomalyforlife.hopperFilter.listeners.HopperFilterListener;
import io.github.anomalyforlife.hopperFilter.model.FilterMatchOptions;
import io.github.anomalyforlife.hopperFilter.service.FilterService;
import io.github.anomalyforlife.hopperFilter.storage.HikariConnectionProvider;
import io.github.anomalyforlife.hopperFilter.storage.HopperFilterStorage;
import io.github.anomalyforlife.hopperFilter.storage.JdbcHopperFilterStorage;
import io.github.anomalyforlife.hopperFilter.util.LanguageManager;
import io.github.anomalyforlife.hopperFilter.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class HopperFilter extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private HikariConnectionProvider connectionProvider;
    private HopperFilterStorage storage;
    private FilterService filterService;
    private FilterGui gui;
    private FilterMatchConfigGui configGui;
    private FilterTagSelectGui tagSelectGui;
    private Messages messages;
    private LanguageManager languageManager;

    private HopperFilterCommand commandExecutor;

    // FIX #4: il listener viene creato una volta sola e i suoi campi aggiornati al reload
    private HopperFilterListener listener;
    private boolean listenerRegistered = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getLogger().info("Enabling HopperFilter v" + getPluginMeta().getVersion());

        try {
            setupAndConnect();
        } catch (Exception e) {
            getLogger().severe("Failed to start HopperFilter: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // FIX #4: registra il listener una volta sola; al reload si aggiorna
        //         tramite updateListener() senza ri-registrarlo
        if (!listenerRegistered) {
            getServer().getPluginManager().registerEvents(listener, this);
            listenerRegistered = true;
        }

        registerCommand();
    }

    private void registerCommand() {
        try {
            final CommandMap commandMap = getServer().getCommandMap();

            if (commandExecutor == null) {
                commandExecutor = new HopperFilterCommand(
                        this::reloadAll,
                        filterService,
                        messages,
                        languageManager,
                        getConfig().getString("filtered-hopper.name", "§6Filtered Hopper"),
                        getConfig().getStringList("filtered-hopper.lore"),
                        getConfig().getString("filtered-hopper.give-message-sender", "§aGiven {amount}x Filtered Hopper to {player}."),
                        getConfig().getString("filtered-hopper.give-message-receiver", "§aYou received {amount}x Filtered Hopper.")
                );
            } else {
                commandExecutor.update(
                        filterService,
                        messages,
                        languageManager,
                        getConfig().getString("filtered-hopper.name", "§6Filtered Hopper"),
                        getConfig().getStringList("filtered-hopper.lore"),
                        getConfig().getString("filtered-hopper.give-message-sender", "§aGiven {amount}x Filtered Hopper to {player}."),
                        getConfig().getString("filtered-hopper.give-message-receiver", "§aYou received {amount}x Filtered Hopper.")
                );
            }

            BukkitCommand hopperFilterCmd = new BukkitCommand("hopperfilter") {
                {
                    setDescription("HopperFilter admin commands");
                    setUsage("/hopperfilter <reload|info|clear|give>");
                    setAliases(java.util.List.of("hf"));
                }

                @Override
                public boolean execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
                    return commandExecutor.onCommand(sender, this, label, args);
                }
            };

            commandMap.register("hopperfilter", this.getName().toLowerCase(), hopperFilterCmd);
        } catch (Exception e) {
            getLogger().severe("Failed to register command: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupAndConnect() throws Exception {
        FilterMatchOptions.init(this);
        this.languageManager = new LanguageManager(this);
        this.messages = new Messages(languageManager.getPrefix());

        // FIX #5: chiudi il vecchio provider prima di crearne uno nuovo
        if (connectionProvider != null) {
            connectionProvider.close();
            connectionProvider = null;
        }

        this.connectionProvider = createConnectionProvider();
        this.storage = new JdbcHopperFilterStorage(connectionProvider);
        this.storage.init();

        int requestedSize = getConfig().getInt("filter.size", 54);
        int size = clampInventorySize(requestedSize);
        if (size != requestedSize) {
            getLogger().warning("Config filter.size=" + requestedSize + " is invalid; using " + size + " instead.");
        }

        boolean filteredHopperEnabled = getConfig().getBoolean("filtered-hopper.enabled", true);
        boolean requireSpecialHopper = getConfig().getBoolean("filtered-hopper.require-special-hopper", true);
        boolean specialMode = filteredHopperEnabled && requireSpecialHopper;

        this.filterService = new FilterService(
            storage,
            size,
            specialMode,
            getConfig().getString("filtered-hopper.name", "§6Filtered Hopper"),
            getConfig().getStringList("filtered-hopper.lore")
        );

        Component title = LEGACY.deserialize(languageManager.getMainGuiTitle());
        this.gui = new FilterGui(
                filterService,
                messages,
                title,
                languageManager.getMsgAdded(),
                languageManager.getMsgAddedWarning(),
                languageManager.getMsgAlready(),
                languageManager.getMsgFull(),
                languageManager.getMsgRemoved()
        );
        this.configGui = new FilterMatchConfigGui(filterService, messages, languageManager);
        this.tagSelectGui = new FilterTagSelectGui(filterService, configGui, messages, languageManager);

        // FIX #4: al primo avvio crea il listener; al reload aggiorna i riferimenti interni
        if (listener == null) {
            listener = new HopperFilterListener(
                    filterService,
                    gui,
                    configGui,
                    tagSelectGui,
                    messages,
                    getConfig().getInt("tnt.blockedRadius", 5),
                    languageManager.getMsgCleared(),
                    languageManager.getMsgMustSneakToBreak(),
                    languageManager.getMsgMustHaveBreakPerm(),
                    languageManager.getMsgTooCloseToFilteredHopper()
            );
        } else {
            listener.update(
                    filterService,
                    gui,
                    configGui,
                    tagSelectGui,
                    messages,
                    getConfig().getInt("tnt.blockedRadius", 5),
                    languageManager.getMsgCleared(),
                    languageManager.getMsgMustSneakToBreak(),
                    languageManager.getMsgMustHaveBreakPerm(),
                    languageManager.getMsgTooCloseToFilteredHopper()
            );
        }

        // Keep command behavior in sync with reloads.
        if (commandExecutor != null) {
            commandExecutor.update(
                    filterService,
                    messages,
                    languageManager,
                    getConfig().getString("filtered-hopper.name", "§6Filtered Hopper"),
                    getConfig().getStringList("filtered-hopper.lore"),
                    getConfig().getString("filtered-hopper.give-message-sender", "§aGiven {amount}x Filtered Hopper to {player}."),
                    getConfig().getString("filtered-hopper.give-message-receiver", "§aYou received {amount}x Filtered Hopper.")
            );
        }
    }

    // FIX #5: usa sempre HikariCP, sia per SQLite che per MySQL
    private HikariConnectionProvider createConnectionProvider() {
        String type = getConfig().getString("storage.type", "sqlite");
        type = type == null ? "sqlite" : type.toLowerCase(Locale.ROOT);

        HikariConfig config = new HikariConfig();

        if ("mysql".equals(type)) {
            String host = getConfig().getString("storage.mysql.host", "127.0.0.1");
            int port = getConfig().getInt("storage.mysql.port", 3306);
            String database = getConfig().getString("storage.mysql.database", "hopperfilter");
            String user = getConfig().getString("storage.mysql.username", "root");
            String pass = getConfig().getString("storage.mysql.password", "change-me");
            boolean useSsl = getConfig().getBoolean("storage.mysql.useSSL", false);
            String params = getConfig().getString("storage.mysql.parameters", "useUnicode=true&characterEncoding=utf8&serverTimezone=UTC");

            String query = "useSSL=" + useSsl;
            if (params != null && !params.isBlank()) {
                query += "&" + params;
            }

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?" + query);
            config.setUsername(user);
            config.setPassword(pass);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File dbFile = new File(getDataFolder(), getConfig().getString("storage.sqlite.file", "filters.db"));
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
            // SQLite non supporta connessioni concorrenti: forza pool size a 1
            config.setMaximumPoolSize(1);
        }

        int maxPool = getConfig().getInt("storage.pool.maximumPoolSize", 10);
        int minIdle = getConfig().getInt("storage.pool.minimumIdle", 2);
        long connTimeout = getConfig().getLong("storage.pool.connectionTimeoutMillis", 10000L);

        // Per SQLite il pool size è già stato forzato a 1 sopra; per MySQL usa la config
        if (!"sqlite".equals(type)) {
            config.setMaximumPoolSize(maxPool);
            config.setMinimumIdle(minIdle);
        }
        config.setConnectionTimeout(connTimeout);
        config.setPoolName("HopperFilter-Pool");

        return new HikariConnectionProvider(config);
    }

    private static int clampInventorySize(int requested) {
        int size = requested;
        if (size < 9) {
            size = 9;
        }
        if (size > 54) {
            size = 54;
        }
        int remainder = size % 9;
        if (remainder != 0) {
            size = size - remainder;
            if (size < 9) {
                size = 9;
            }
        }
        return size;
    }

    private void reloadAll() {
        reloadConfig();
        Bukkit.getScheduler().runTask(this, () -> {
            try {
                setupAndConnect();
                getLogger().info("HopperFilter reloaded successfully.");
            } catch (Exception e) {
                getLogger().severe("Reload failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.close();
            storage = null;
        }
        // FIX #5: chiudi il pool HikariCP alla disabilitazione
        if (connectionProvider != null) {
            connectionProvider.close();
            connectionProvider = null;
        }
    }
}