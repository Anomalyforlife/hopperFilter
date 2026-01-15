package io.github.anomalyforlife.hopperFilter.util;

import java.util.Objects;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class Messages {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final String prefix;

    public Messages(String prefix) {
        this.prefix = Objects.requireNonNull(prefix, "prefix");
    }

    public void send(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        sender.sendMessage(LEGACY.deserialize(prefix + message));
    }

    public void actionBar(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        Component component = LEGACY.deserialize(message);
        player.sendActionBar(component);
    }
}
