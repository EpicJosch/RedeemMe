package to.joe.redeem;

import java.sql.SQLException;
import java.util.logging.Level;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class RedeemMe extends JavaPlugin {

    /*
     * TODO Packages
     * TODO Coupon codes
     * TODO Coupon codes that can be used by everybody once
     * TODO Packages/coupons that can run commands
     * TODO Coupon items
     * TODO sql schema
     * TODO php script
     * TODO Expiry
     * TODO Embargo
     * TODO Per server
     * TODO Multi server
     * TODO All servers
     * TODO Package/coupon names
     * TODO Package/coupon descriptions
     * TODO Strange Weapons support
     * TODO Money support (via vault)
     * TODO API
     * TODO Show packages from servers even if you can't redeem them on the server you are on
     * TODO Alert users to unclaimed packages
     * TODO Extra alert when packages are close to expiry
     */

    private MySQL sql; //TODO Possibly make this and the getter static so that an api can be easy
    public static Economy economy = null;

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        getCommand("redeem").setExecutor(new RedeemCommand(this));
        getCommand("printcoupon").setExecutor(new PrintCouponCommand(this));

        try {
            sql = new MySQL(this, getConfig().getString("database.url"), getConfig().getString("database.username"), getConfig().getString("database.password"));
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error connecting to database!", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        Coupon.plugin = this;
        
        if (setupEconomy()) {
            getLogger().info("Economy detected, money enabled");
        } else {
            getLogger().info("Economy not detected, money disabled");
        }
        
        if (getServer().getServerId().equals("unnamed")) {
            getLogger().warning("This server does not have an ID set!"); //TODO show where to set this
        }
    }

    public MySQL getMySQL() {
        return sql;
    }
}
