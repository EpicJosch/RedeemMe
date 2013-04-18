package to.joe.redeem;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import to.joe.strangeweapons.meta.StrangeWeapon;

public class RedeemMe extends JavaPlugin implements Listener {

    static Economy economy = null;
    private MySQL sql;
    private boolean strangeWeaponsEnabled = false;
    private static RedeemMe instance;

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return (economy != null);
    }

    MySQL getMySQL() {
        return sql;
    }

    boolean strangeWeaponsEnabled() {
        return strangeWeaponsEnabled;
    }

    static RedeemMe getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        try {
            sql = new MySQL(this, getConfig().getString("database.url"), getConfig().getString("database.username"), getConfig().getString("database.password"));
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error connecting to database!", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (setupEconomy()) {
            getLogger().info("Economy detected, money enabled");
        } else {
            getLogger().info("Economy not detected, money disabled");
        }

        if (getServer().getPluginManager().isPluginEnabled("StrangeWeapons")) {
            getLogger().info("StrangeWeapons detected. Weapons will be cloned.");
            strangeWeaponsEnabled = true;
        }

        if (getServer().getServerId().equals("unnamed")) {
            getLogger().warning("This server does not have an ID set!");
            getLogger().warning("Set \"server-id\" in your server.properties");
        }
        getCommand("redeem").setExecutor(new RedeemCommand(this));
        getCommand("printcoupon").setExecutor(new PrintCouponCommand(this));
        getCommand("rdtest").setExecutor(new RDTestCommand());
        getServer().getPluginManager().registerEvents(this, this);
        instance = this;
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        try {
            LinkedHashMap<Integer, String> packages = Package.getAvailablePackagesByPlayerName(player.getName());
            Iterator<Entry<Integer, String>> iterator = packages.entrySet().iterator();
            if (iterator.hasNext()) {
                player.sendMessage(ChatColor.GREEN + "You have the following packages to redeem");
                StringBuilder thisServer = new StringBuilder(ChatColor.YELLOW + "This server: ");
                boolean packagesThisServer = false;
                StringBuilder otherServers = new StringBuilder(ChatColor.RED + "Other servers: ");
                boolean packagesOtherServers = false;
                do {
                    Entry<Integer, String> pack = iterator.next();
                    if (pack.getValue() == null || pack.getValue().equals(getServer().getServerId())) {
                        thisServer.append(pack.getKey()).append(", ");
                        packagesThisServer = true;
                    } else {
                        otherServers.append(pack.getKey()).append(", ");
                        packagesOtherServers = true;
                    }
                } while (iterator.hasNext());
                if (packagesThisServer) {
                    player.sendMessage(thisServer.substring(0, thisServer.length() - 2));
                }
                if (packagesOtherServers) {
                    player.sendMessage(otherServers.substring(0, otherServers.length() - 2));
                }
                player.sendMessage(ChatColor.GREEN + "Type /redeem for help");
            }
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error getting list of things to redeem!", e);
            player.sendMessage(ChatColor.RED + "Something went wrong!");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getItem() != null && event.getItem().getType().equals(Material.PAPER) && event.getItem().hasItemMeta()) {
            ItemMeta meta = event.getItem().getItemMeta();
            Pattern p = Pattern.compile(ChatColor.RED + "Coupon code (.*)");
            Matcher m = p.matcher(meta.getLore().get(meta.getLore().size() - 1));
            if (m.matches()) {
                Player player = event.getPlayer();
                try {
                    int id = Package.idFromCode(m.group(1).replaceAll("-", ""));
                    Package pack = new Package(id);
                    if (pack.getRedeemed() != null) {
                        player.sendMessage(ChatColor.RED + "This coupon has been redeemed already");
                        player.setItemInHand(null);
                        return;
                    }
                    if (pack.getEmbargo() != null && pack.getEmbargo() > System.currentTimeMillis() / 1000) {
                        player.sendMessage(ChatColor.RED + "This coupon is not yet valid");
                        return;
                    }
                    if (pack.getExpiry() != null && pack.getExpiry() < System.currentTimeMillis() / 1000) {
                        player.sendMessage(ChatColor.RED + "This coupon has expired");
                        player.setItemInHand(null);
                        return;
                    }
                    if (pack.getServer() != null && !pack.getServer().equals(player.getServer().getServerId())) {
                        player.sendMessage(ChatColor.RED + "This coupon is not valid on this server");
                        player.sendMessage(ChatColor.RED + "It must be redeemed on " + pack.getServer());
                        player.setItemInHand(null);
                        return;
                    }
                    if (pack.getName() != null) {
                        player.sendMessage(ChatColor.GREEN + "Name: " + ChatColor.YELLOW + pack.getName());
                    }
                    if (pack.getDescription() != null) {
                        player.sendMessage(ChatColor.GREEN + "Description: " + ChatColor.YELLOW + pack.getDescription());
                    }
                    if (pack.getCreator() != null) {
                        player.sendMessage(ChatColor.GREEN + "Given by: " + ChatColor.YELLOW + pack.getCreator());
                    }
                    if (!pack.isEmpty()) {
                        player.sendMessage(ChatColor.GREEN + "Coupon " + m.group(1) + " contains the following item(s)");
                        if (pack.getMoney() != null) {
                            player.sendMessage(ChatColor.GREEN + "" + pack.getMoney() + " " + ChatColor.GOLD + RedeemMe.economy.currencyNamePlural());
                            RedeemMe.economy.depositPlayer(player.getName(), pack.getMoney());
                        }
                        if (!pack.getItems().isEmpty()) {
                            for (ItemStack item : pack.getItems()) {
                                if (item.getItemMeta().hasDisplayName()) {
                                    player.sendMessage(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getItemMeta().getDisplayName());
                                } else {
                                    player.sendMessage(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getType().toString());
                                }
                                if (strangeWeaponsEnabled() && StrangeWeapon.isStrangeWeapon(item)) {
                                    item = new StrangeWeapon(item).clone();
                                }
                                player.getInventory().addItem(item);
                            }
                        }
                        if (!pack.getCommands().isEmpty()) {
                            for (Entry<String, Boolean> com : pack.getCommands().entrySet()) {
                                String commandLine = com.getKey().replaceAll("<player>", player.getName());
                                player.sendMessage(ChatColor.GREEN + "Command: " + ChatColor.GOLD + commandLine);
                                CommandSender actor = player;
                                if (com.getValue()) {
                                    actor = getServer().getConsoleSender();
                                }
                                getServer().dispatchCommand(actor, commandLine);
                            }
                        }
                        pack.setRedeemed(player.getName());
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