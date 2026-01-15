package io.github.anomalyforlife.hopperFilter;

import java.io.File;
import java.sql.DriverManager;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.anomalyforlife.hopperFilter.commands.HopperFilterCommand;
import io.github.anomalyforlife.hopperFilter.gui.FilterGui;
import io.github.anomalyforlife.hopperFilter.gui.FilterMatchConfigGui;
import io.github.anomalyforlife.hopperFilter.gui.FilterTagSelectGui;
import io.github.anomalyforlife.hopperFilter.listeners.HopperFilterListener;
import io.github.anomalyforlife.hopperFilter.model.FilterMatchOptions;
import io.github.anomalyforlife.hopperFilter.service.FilterService;
import io.github.anomalyforlife.hopperFilter.storage.ConnectionProvider;
import io.github.anomalyforlife.hopperFilter.storage.HopperFilterStorage;
import io.github.anomalyforlife.hopperFilter.storage.JdbcHopperFilterStorage;
import io.github.anomalyforlife.hopperFilter.util.LanguageManager;
import io.github.anomalyforlife.hopperFilter.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class HopperFilter extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private HopperFilterStorage storage;
    private FilterService filterService;
    private FilterGui gui;
    private FilterMatchConfigGui configGui;
    private FilterTagSelectGui tagSelectGui;
    private Messages messages;
    private LanguageManager languageManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            setupAndConnect();
        } catch (Exception e) {
            getLogger().severe("Failed to start HopperFilter: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(
                new HopperFilterListener(
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
                ),
                this
        );

        if (getCommand("hopperfilter") != null) {
            getCommand("hopperfilter").setExecutor(
                    new HopperFilterCommand(this::reloadAll, filterService, messages, languageManager)
            );
        }
    }

    private void setupAndConnect() throws Exception {
        FilterMatchOptions.init(this);
        this.languageManager = new LanguageManager(this);
        this.messages = new Messages(languageManager.getPrefix());

        this.storage = createStorageFromConfig();
        this.storage.init();

        int requestedSize = getConfig().getInt("filter.size", 54);
        int size = clampInventorySize(requestedSize);
        if (size != requestedSize) {
            getLogger().warning("Config filter.size=" + requestedSize + " is invalid; using " + size + " instead.");
        }

        this.filterService = new FilterService(storage, size);

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
    }

    private HopperFilterStorage createStorageFromConfig() {
        String type = getConfig().getString("storage.type", "sqlite");
        type = type == null ? "sqlite" : type.toLowerCase(Locale.ROOT);
        ConnectionProvider provider = createConnectionProvider(type);
        return new JdbcHopperFilterStorage(provider);
    }

    private ConnectionProvider createConnectionProvider(String type) {
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
            String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?" + query;
            return () -> DriverManager.getConnection(jdbcUrl, user, pass);
        }

        File dbFile = new File(getDataFolder(), getConfig().getString("storage.sqlite.file", "filters.db"));
        String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        return () -> DriverManager.getConnection(jdbcUrl);
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
                if (storage != null) {
                    storage.close();
                }
                setupAndConnect();
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
    }
}
