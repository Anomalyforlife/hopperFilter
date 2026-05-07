package io.github.anomalyforlife.hopperFilter.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.Location;
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
        registerHopper(key, 1);
    }

    /** Called when a special hopper is placed with a known starting level. */
    public void registerHopper(HopperKey key, int level) {
        int clamped = Math.max(1, Math.min(level, config.getMaxLevel()));
        levelCache.put(key, clamped);
        try {
            storage.saveHopperLevel(key, clamped);
        } catch (Exception e) {
            LOGGER.warning("[HopperFilter] Failed to persist hopper level on place: " + e.getMessage());
        }
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

    /**
     * Sets all hoppers owned by the given player to the maximum level.
     * Updates both the DB and the in-memory cache.
     *
     * @return number of hoppers upgraded
     */
    public int upgradeAllToMax(UUID ownerUuid) throws Exception {
        int maxLevel = config.getMaxLevel();
        List<HopperKey> keys = storage.loadHopperKeysByOwner(ownerUuid);
        if (keys.isEmpty()) return 0;
        storage.setAllLevelsByOwner(ownerUuid, maxLevel);
        for (HopperKey key : keys) {
            levelCache.put(key, maxLevel);
        }
        return keys.size();
    }

    /**
     * Sets all hoppers within {@code radius} blocks of {@code center} to the maximum level.
     * Operates on the in-memory cache (no DB query needed to find candidates).
     *
     * @return number of hoppers upgraded
     */
    public int upgradeInRadiusToMax(Location center, double radius) throws Exception {
        if (center == null || center.getWorld() == null) return 0;
        int maxLevel = config.getMaxLevel();
        UUID worldId = center.getWorld().getUID();
        double cx = center.getX(), cy = center.getY(), cz = center.getZ();
        double r2 = radius * radius;

        List<HopperKey> targets = new ArrayList<>();
        for (HopperKey key : levelCache.keySet()) {
            if (!key.worldUuid().equals(worldId)) continue;
            double dx = key.x() + 0.5 - cx;
            double dy = key.y() + 0.5 - cy;
            double dz = key.z() + 0.5 - cz;
            if (dx * dx + dy * dy + dz * dz <= r2) {
                targets.add(key);
            }
        }
        if (targets.isEmpty()) return 0;

        for (HopperKey key : targets) {
            storage.saveHopperLevel(key, maxLevel);
            levelCache.put(key, maxLevel);
        }
        return targets.size();
    }

    public enum UpgradeResult {
        SUCCESS, MAX_LEVEL, NOT_ENOUGH_MONEY, VAULT_NOT_AVAILABLE, NOT_ENABLED, DB_ERROR
    }
}
