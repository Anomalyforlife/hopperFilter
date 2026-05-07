package io.github.anomalyforlife.hopperFilter.upgrade;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.entity.Player;

import io.github.anomalyforlife.hopperFilter.model.HopperKey;
import io.github.anomalyforlife.hopperFilter.storage.HopperFilterStorage;

public final class UpgradeService {

    private static final Logger LOGGER = Logger.getLogger(UpgradeService.class.getName());

    private final HopperFilterStorage storage;
    private volatile UpgradeConfig config;
    private final VaultEconomyHook economy; // null when Vault absent or upgrades disabled
    private final ConcurrentHashMap<HopperKey, Integer> levelCache = new ConcurrentHashMap<>();

    public UpgradeService(HopperFilterStorage storage, UpgradeConfig config, VaultEconomyHook economy) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.config = Objects.requireNonNull(config, "config");
        this.economy = economy;
    }

    /** Replaces the full level cache on reload. */
    public void loadLevels(Map<HopperKey, Integer> levels) {
        levelCache.clear();
        if (levels != null) {
            levels.forEach((k, v) -> { if (k != null) levelCache.put(k, v == null ? 1 : v); });
        }
    }

    public void updateConfig(UpgradeConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public UpgradeConfig getConfig() { return config; }

    public int getLevel(HopperKey key) {
        return levelCache.getOrDefault(key, 1);
    }

    public UpgradeConfig.LevelData getLevelData(HopperKey key) {
        return config.getLevelData(getLevel(key));
    }

    /** Called when a new special hopper is placed. */
    public void registerHopper(HopperKey key) {
        levelCache.putIfAbsent(key, 1);
    }

    /** Called when a special hopper is broken. */
    public void unregisterHopper(HopperKey key) {
        levelCache.remove(key);
    }

    public UpgradeResult tryUpgrade(Player player, HopperKey key) {
        if (!config.isEnabled()) return UpgradeResult.NOT_ENABLED;
        int current = getLevel(key);
        if (current >= config.getMaxLevel()) return UpgradeResult.MAX_LEVEL;

        int next = current + 1;
        double cost = config.getLevelData(next).cost();

        if (cost > 0 && config.isVaultRequired()) {
            if (economy == null) return UpgradeResult.VAULT_NOT_AVAILABLE;
            if (!economy.has(player, cost)) return UpgradeResult.NOT_ENOUGH_MONEY;
            economy.withdraw(player, cost);
        }

        try {
            storage.saveHopperLevel(key, next);
            levelCache.put(key, next);
            return UpgradeResult.SUCCESS;
        } catch (Exception e) {
            if (cost > 0 && config.isVaultRequired() && economy != null) {
                economy.deposit(player, cost);
            }
            LOGGER.warning("[HopperFilter] Failed to save hopper upgrade: " + e.getMessage());
            return UpgradeResult.DB_ERROR;
        }
    }

    public enum UpgradeResult {
        SUCCESS, MAX_LEVEL, NOT_ENOUGH_MONEY, VAULT_NOT_AVAILABLE, NOT_ENABLED, DB_ERROR
    }
}
