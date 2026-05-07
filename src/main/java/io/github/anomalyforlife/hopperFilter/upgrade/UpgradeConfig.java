package io.github.anomalyforlife.hopperFilter.upgrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

public final class UpgradeConfig {

    private final boolean enabled;
    private final boolean vaultRequired;
    private final int maxLevel;
    private final List<LevelData> levels; // index 0 = level 1

    public UpgradeConfig(boolean enabled, boolean vaultRequired, int maxLevel, List<LevelData> levels) {
        this.enabled = enabled;
        this.vaultRequired = vaultRequired;
        this.maxLevel = maxLevel;
        this.levels = Collections.unmodifiableList(new ArrayList<>(levels));
    }

    public boolean isEnabled() { return enabled; }
    public boolean isVaultRequired() { return vaultRequired; }
    public int getMaxLevel() { return maxLevel; }

    /** Returns data for the given level (1-based). Clamps to valid range. */
    public LevelData getLevelData(int level) {
        int idx = Math.max(0, Math.min(level - 1, levels.size() - 1));
        return levels.get(idx);
    }

    public static UpgradeConfig fromSection(ConfigurationSection section) {
        if (section == null) return defaults();
        boolean enabled = section.getBoolean("enabled", true);
        boolean vaultRequired = section.getBoolean("vault-required", true);
        int maxLevel = Math.max(1, section.getInt("max-level", 10));
        ConfigurationSection levelsSection = section.getConfigurationSection("levels");
        List<LevelData> levels = new ArrayList<>();
        if (levelsSection != null) {
            for (int lvl = 1; lvl <= maxLevel; lvl++) {
                ConfigurationSection s = levelsSection.getConfigurationSection(String.valueOf(lvl));
                if (s != null) {
                    levels.add(new LevelData(
                        s.getInt("cost", 0),
                        Math.max(1, s.getInt("filter-slots", 3)),
                        Math.max(1, s.getInt("transfer-speed-ticks", 16))
                    ));
                } else {
                    levels.add(new LevelData(0, 3, 16));
                }
            }
        }
        if (levels.isEmpty()) return defaults();
        return new UpgradeConfig(enabled, vaultRequired, maxLevel, levels);
    }

    public static UpgradeConfig defaults() {
        List<LevelData> lvls = new ArrayList<>();
        lvls.add(new LevelData(    0,  3, 16));
        lvls.add(new LevelData( 1000,  6, 15));
        lvls.add(new LevelData( 2000,  9, 14));
        lvls.add(new LevelData( 3000, 12, 13));
        lvls.add(new LevelData( 4000, 15, 12));
        lvls.add(new LevelData( 5000, 18, 11));
        lvls.add(new LevelData( 6000, 21, 10));
        lvls.add(new LevelData( 7000, 24,  9));
        lvls.add(new LevelData( 8000, 27,  9));
        lvls.add(new LevelData(10000, 30,  8));
        return new UpgradeConfig(true, true, 10, lvls);
    }

    public record LevelData(int cost, int filterSlots, int transferSpeedTicks) {}
}
