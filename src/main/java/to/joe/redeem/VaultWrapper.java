package to.joe.redeem;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultWrapper {

    private Economy economy = null;
    RedeemMe plugin;

    VaultWrapper(RedeemMe plugin) {
        this.plugin = plugin;
    }

    boolean setup() {
        try {
            Class.forName("net.milkbowl.vault.economy.Economy");
        } catch (ClassNotFoundException e) {
            return false;
        }
        RegisteredServiceProvider<Economy> economyProvider = this.plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return (economy != null);
    }

    Economy getEconomy() {
        return economy;
    }
}