package io.github.anomalyforlife.hopperFilter.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

import org.bukkit.inventory.ItemStack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import io.github.anomalyforlife.hopperFilter.model.HopperKey;

public final class JdbcHopperFilterStorage implements HopperFilterStorage {
    private static final String TABLE_NAME = "hopper_filter_items";
    private static final Type ITEM_MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    private static final String BIN_PREFIX = "B64:";

    private final ConnectionProvider connectionProvider;
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(JdbcHopperFilterStorage.class.getName());
    private final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    public JdbcHopperFilterStorage(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
    }

    @Override
    public void init() throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                            "world_uuid TEXT NOT NULL," +
                            "x INTEGER NOT NULL," +
                            "y INTEGER NOT NULL," +
                            "z INTEGER NOT NULL," +
                            "slot INTEGER NOT NULL," +
                            "item TEXT NOT NULL," +
                            "PRIMARY KEY(world_uuid, x, y, z, slot)" +
                            ")"
            )) {
                ps.executeUpdate();
            }
        }
    }

    @Override
    public ItemStack[] loadFilter(HopperKey key, int size) throws SQLException {
        ItemStack[] items = new ItemStack[size];
        Arrays.fill(items, null);

        try (Connection connection = connectionProvider.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT slot, item FROM " + TABLE_NAME + " WHERE world_uuid=? AND x=? AND y=? AND z=?"
            )) {
                ps.setString(1, key.worldUuid().toString());
                ps.setInt(2, key.x());
                ps.setInt(3, key.y());
                ps.setInt(4, key.z());

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int slot = rs.getInt(1);
                        String json = rs.getString(2);
                        if (slot < 0 || slot >= size || json == null) {
                            continue;
                        }
                        items[slot] = deserializeItem(json);
                    }
                }
            }
        }
        return items;
    }

    @Override
    public void saveFilter(HopperKey key, ItemStack[] items) throws SQLException {
        if (items == null || items.length == 0) {
            throw new IllegalArgumentException("items must not be empty");
        }

        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM " + TABLE_NAME + " WHERE world_uuid=? AND x=? AND y=? AND z=?"
                )) {
                    delete.setString(1, key.worldUuid().toString());
                    delete.setInt(2, key.x());
                    delete.setInt(3, key.y());
                    delete.setInt(4, key.z());
                    delete.executeUpdate();
                }

                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO " + TABLE_NAME + "(world_uuid,x,y,z,slot,item) VALUES (?,?,?,?,?,?)"
                )) {
                    for (int slot = 0; slot < items.length; slot++) {
                        ItemStack item = items[slot];
                        if (item == null || item.getType().isAir()) {
                            continue;
                        }

                        String stored = serializeItem(item);
                        if (stored == null || stored.isBlank()) {
                            LOGGER.warning("Skipping unserializable ItemStack for " + key + " at slot " + slot);
                            continue;
                        }

                        insert.setString(1, key.worldUuid().toString());
                        insert.setInt(2, key.x());
                        insert.setInt(3, key.y());
                        insert.setInt(4, key.z());
                        insert.setInt(5, slot);
                        insert.setString(6, stored);
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public void deleteFilter(HopperKey key) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM " + TABLE_NAME + " WHERE world_uuid=? AND x=? AND y=? AND z=?"
            )) {
                ps.setString(1, key.worldUuid().toString());
                ps.setInt(2, key.x());
                ps.setInt(3, key.y());
                ps.setInt(4, key.z());
                ps.executeUpdate();
            }
        }
    }

    @Override
    public void close() {
        // Nothing to close for plain JDBC connections.
    }

    private String serializeItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return null;
        }

        // Preferred: binary (base64) storage. Much more robust across metas/PDC.
        try {
            byte[] bytes = serializeItemToBytes(itemStack);
            return BIN_PREFIX + Base64.getEncoder().encodeToString(bytes);
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Binary ItemStack serialization failed, falling back to JSON: " + t.getMessage(), t);
        }

        // Fallback: legacy JSON map (backward compatibility)
        try {
            Map<String, Object> serialized = itemStack.serialize();
            serialized.values().removeIf(v -> v != null && v.getClass().getName().contains("Optional"));
            String json = gson.toJson(serialized, ITEM_MAP_TYPE);
            if (json == null || json.isBlank()) {
                return null;
            }
            return json;
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "JSON ItemStack serialization failed: " + t.getMessage(), t);
            return null;
        }
    }

    private static byte[] serializeItemToBytes(ItemStack itemStack) throws IOException {
        // Use ItemStack#serializeAsBytes when present (Paper/Spigot), otherwise fallback to BukkitObjectOutputStream.
        try {
            var method = ItemStack.class.getMethod("serializeAsBytes");
            Object result = method.invoke(itemStack);
            if (result instanceof byte[] bytes && bytes.length > 0) {
                return bytes;
            }
        } catch (ReflectiveOperationException ignored) {
            // fall back below
        } catch (Exception e) {
            // fall back below
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Object oos = null;
        try {
            Class<?> clazz = Class.forName("org.bukkit.util.io.BukkitObjectOutputStream");
            oos = clazz.getConstructor(java.io.OutputStream.class).newInstance(baos);
            clazz.getMethod("writeObject", Object.class).invoke(oos, itemStack);
        } catch (ReflectiveOperationException e) {
            throw new IOException("BukkitObjectOutputStream fallback failed", e);
        } finally {
            if (oos instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception ignored) {
                }
            }
        }
        return baos.toByteArray();
    }

    private ItemStack deserializeItem(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        if (json.startsWith(BIN_PREFIX)) {
            String b64 = json.substring(BIN_PREFIX.length());
            try {
                byte[] bytes = Base64.getDecoder().decode(b64);
                return deserializeItemFromBytes(bytes);
            } catch (Throwable ex) {
                LOGGER.warning("Failed to deserialize binary ItemStack from storage. Data length=" + json.length() + " Error=" + ex.getMessage());
                return null;
            }
        }

        try {
            Map<String, Object> map = gson.fromJson(json, ITEM_MAP_TYPE);
            return ItemStack.deserialize(map);
        } catch (Exception ex) {
            LOGGER.warning("Failed to deserialize ItemStack from storage. Data length=" + json.length() + " Error=" + ex.getMessage());
            return null;
        }
    }

    private static ItemStack deserializeItemFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        // Prefer ItemStack#deserializeBytes when present.
        try {
            var method = ItemStack.class.getMethod("deserializeBytes", byte[].class);
            Object result = method.invoke(null, (Object) bytes);
            if (result instanceof ItemStack stack) {
                return stack;
            }
        } catch (ReflectiveOperationException ignored) {
            // fall back below
        } catch (Exception e) {
            // fall back below
        }

        Object ois = null;
        try {
            Class<?> clazz = Class.forName("org.bukkit.util.io.BukkitObjectInputStream");
            ois = clazz.getConstructor(java.io.InputStream.class).newInstance(new ByteArrayInputStream(bytes));
            Object obj = clazz.getMethod("readObject").invoke(ois);
            if (obj instanceof ItemStack stack) {
                return stack;
            }
            return null;
        } catch (ReflectiveOperationException e) {
            IOException ioe = new IOException("BukkitObjectInputStream fallback failed", e);
            ioe.addSuppressed(new ClassNotFoundException(e.getMessage(), e));
            throw ioe;
        } finally {
            if (ois instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
