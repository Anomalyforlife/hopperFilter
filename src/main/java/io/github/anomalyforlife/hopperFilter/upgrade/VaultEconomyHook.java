package io.github.anomalyforlife.hopperFilter.upgrade;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;

/**
 * Isolates all Economy class references so that the JVM only loads this class
 * after Vault is confirmed present — preventing NoClassDefFoundError on servers
 * that run without Vault installed.
 */
public final class VaultEconomyHook {

    private final Economy economy;

    private VaultEconomyHook(Economy economy) {
        this.economy = economy;
    }

    public static VaultEconomyHook load(Server server) {
        RegisteredServiceProvider<Economy> rsp = server.getServicesManager().getRegistration(Economy.class);
        return rsp != null ? new VaultEconomyHook(rsp.getProvider()) : null;
    }

    public boolean has(Player player, double amount) {
        return economy.has(player, amount);
    }

    public void withdraw(Player player, double amount) {
        economy.withdrawPlayer(player, amount);
    }

    public void deposit(Player player, double amount) {
        economy.depositPlayer(player, amount);
    }
}
