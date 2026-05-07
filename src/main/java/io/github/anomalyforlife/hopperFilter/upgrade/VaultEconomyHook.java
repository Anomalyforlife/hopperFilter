package io.github.anomalyforlife.hopperFilter.upgrade;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * Accesses Vault's Economy via Vault's own classloader so Paper's plugin
 * classloader isolation cannot prevent class resolution at runtime.
 */
public final class VaultEconomyHook {
    private static final Logger LOGGER = Logger.getLogger(VaultEconomyHook.class.getName());

    private final Object economy;
    private final Method hasMethod;
    private final Method withdrawMethod;
    private final Method depositMethod;

    private VaultEconomyHook(Object economy, Method has, Method withdraw, Method deposit) {
        this.economy = economy;
        this.hasMethod = has;
        this.withdrawMethod = withdraw;
        this.depositMethod = deposit;
    }

    public static VaultEconomyHook load(Server server) throws Exception {
        var vaultPlugin = server.getPluginManager().getPlugin("Vault");
        if (vaultPlugin == null) return null;

        ClassLoader vaultLoader = vaultPlugin.getClass().getClassLoader();
        Class<?> economyClass = vaultLoader.loadClass("net.milkbowl.vault.economy.Economy");

        Object services = server.getServicesManager();
        Method getRegistration = services.getClass().getMethod("getRegistration", Class.class);
        Object rsp = getRegistration.invoke(services, economyClass);
        if (rsp == null) return null;

        Object instance = rsp.getClass().getMethod("getProvider").invoke(rsp);
        if (instance == null) return null;

        return new VaultEconomyHook(
                instance,
                economyClass.getMethod("has", OfflinePlayer.class, double.class),
                economyClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class),
                economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class)
        );
    }

    public boolean has(Player player, double amount) {
        try {
            return Boolean.TRUE.equals(hasMethod.invoke(economy, player, amount));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Vault Economy.has() failed", e);
            return false;
        }
    }

    public void withdraw(Player player, double amount) {
        try {
            withdrawMethod.invoke(economy, player, amount);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Vault Economy.withdrawPlayer() failed", e);
        }
    }

    public void deposit(Player player, double amount) {
        try {
            depositMethod.invoke(economy, player, amount);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Vault Economy.depositPlayer() failed", e);
        }
    }
}
