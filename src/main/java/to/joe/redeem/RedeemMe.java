package to.joe.redeem;

import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import to.joe.strangeweapons.meta.StrangeWeapon;

public class RedeemMe extends JavaPlugin implements Listener {

    /*
     * TODO Coupon codes that can be used by everybody once
     * TODO Coupon items
     * TODO php script
     * TODO API
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
        
        getServer().getPluginManager().registerEvents(this, this);

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
    
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getItem() != null && event.getItem().getType().equals(Material.PAPER) && event.getItem().hasItemMeta()) {
            ItemMeta meta = event.getItem().getItemMeta();
            Pattern p = Pattern.compile(ChatColor.RED + "Coupon code (.*)");
            Matcher m = p.matcher(meta.getLore().get(meta.getLore().size()-1));
            if (m.matches()) {
                Player player = event.getPlayer();
                try {
                    int id = Coupon.idFromCode(m.group(1).replaceAll("-", ""));
                    Coupon coupon = new Coupon(id);
                    if (coupon.getRedeemed() != null) {
                        player.sendMessage(ChatColor.RED + "This coupon has been redeemed already");
                        player.setItemInHand(null);
                        return;
                    }
                    if (coupon.getEmbargo() != null && coupon.getEmbargo() > System.currentTimeMillis() / 1000) {
                        player.sendMessage(ChatColor.RED + "This coupon is not yet valid");
                        return;
                    }
                    if (coupon.getExpiry() != null && coupon.getExpiry() < System.currentTimeMillis() / 1000) {
                        player.sendMessage(ChatColor.RED + "This coupon has expired");
                        player.setItemInHand(null);
                        return;
                    }
                    if (coupon.getServer() != null && !coupon.getServer().equals(player.getServer().getServerId())) {
                        player.sendMessage(ChatColor.RED + "This coupon is not valid on this server");
                        player.sendMessage(ChatColor.RED + "It must be redeemed on " + coupon.getServer());
                        player.setItemInHand(null);
                        return;
                    }
                    if (coupon.getName() != null) {
                        player.sendMessage(ChatColor.GREEN + "Name: " + ChatColor.YELLOW + coupon.getName());
                    }
                    if (coupon.getDescription() != null) {
                        player.sendMessage(ChatColor.GREEN + "Description: " + ChatColor.YELLOW + coupon.getDescription());
                    }
                    if (coupon.getCreator() != null) {
                        player.sendMessage(ChatColor.GREEN + "Given by: " + ChatColor.YELLOW + coupon.getCreator());
                    }
                    if (!coupon.isEmpty()) {
                        player.sendMessage(ChatColor.GREEN + "Coupon " + m.group(1) + " contains the following item(s)");
                        if (coupon.getMoney() != null) {
                            player.sendMessage(ChatColor.GREEN + "" + coupon.getMoney() + " " + ChatColor.GOLD + RedeemMe.economy.currencyNamePlural());
                            RedeemMe.economy.depositPlayer(player.getName(), coupon.getMoney());
                        }
                        if (!coupon.getItems().isEmpty()) {
                            for (ItemStack item : coupon.getItems()) {
                                if (item.getItemMeta().hasDisplayName()) {
                                    player.sendMessage(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getItemMeta().getDisplayName());
                                } else {
                                    player.sendMessage(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getType().toString());
                                }
                                if (StrangeWeapon.isStrangeWeapon(item)) {
                                    item = new StrangeWeapon(item).clone();
                                }
                                player.getInventory().addItem(item);
                            }
                        }
                        if (!coupon.getCommands().isEmpty()) {
                            for (Entry<String, Boolean> com : coupon.getCommands().entrySet()) {
                                String commandLine = com.getKey().replaceAll("<player>", player.getName());
                                player.sendMessage(ChatColor.GREEN + "Command: " + ChatColor.GOLD + commandLine);
                                CommandSender actor = player;
                                if (com.getValue()) {
                                    actor = getServer().getConsoleSender();
                                }
                                getServer().dispatchCommand(actor, commandLine);
                            }
                        }
                        coupon.setRedeemed(player.getName());
                        getLogger().info(player.getName() + " has redeemed coupon with id " + id);
                        player.sendMessage(ChatColor.GREEN + "Coupon successfully redeemed!");
                        player.setItemInHand(null);
                    } else {
                        player.sendMessage(ChatColor.RED + "Coupon " + m.group(1) + " has nothing to redeem");
                    }
                } catch (InvalidConfigurationException e) {
                    getLogger().log(Level.SEVERE, "Data for id " + m.group(1) + " is corrupted!", e);
                    player.sendMessage(ChatColor.RED + "Something went wrong!");
                    return;
                } catch (SQLException e) {
                    getLogger().log(Level.SEVERE, "Error getting list of things to redeem!", e);
                    player.sendMessage(ChatColor.RED + "Something went wrong!");
                    return;
                } catch (NonexistentCouponException e) {
                    player.sendMessage(ChatColor.RED + "That coupon does not exist!");
                    return;
                }
            }
        }
    }
}
