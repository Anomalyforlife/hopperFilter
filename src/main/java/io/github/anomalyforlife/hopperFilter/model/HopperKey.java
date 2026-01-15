package io.github.anomalyforlife.hopperFilter.model;

import java.util.Objects;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

public final class HopperKey {
    private final UUID worldUuid;
    private final int x;
    private final int y;
    private final int z;

    public HopperKey(UUID worldUuid, int x, int y, int z) {
        this.worldUuid = Objects.requireNonNull(worldUuid, "worldUuid");
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static HopperKey fromLocation(Location location) {
        World world = Objects.requireNonNull(location.getWorld(), "world");
        return new HopperKey(world.getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public UUID worldUuid() {
        return worldUuid;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HopperKey hopperKey)) return false;
        return x == hopperKey.x && y == hopperKey.y && z == hopperKey.z && worldUuid.equals(hopperKey.worldUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldUuid, x, y, z);
    }

    @Override
    public String toString() {
        return worldUuid + ":" + x + "," + y + "," + z;
    }
}
