package to.joe.redeem;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import to.joe.strangeweapons.meta.StrangeWeapon;

public class RedeemCommand implements CommandExecutor { //Gold, yellow, aqua

    private RedeemMe plugin;

    public RedeemCommand(RedeemMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players may use this command");
        }
        Player player = (Player) sender;

        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            try {
                LinkedHashMap<Integer, String> packages = Package.getAvailablePackagesByPlayerName(player.getName());
                Iterator<Entry<Integer, String>> iterator = packages.entrySet().iterator();
                if (iterator.hasNext()) {
                    sender.sendMessage(ChatColor.GREEN + "You have the following packages to redeem");
                    StringBuilder thisServer = new StringBuilder(ChatColor.YELLOW + "This server: ");
                    boolean packThisServer = false;
                    StringBuilder otherServers = new StringBuilder(ChatColor.RED + "Other servers: ");
                    boolean packOtherServers = false;
                    do {
                        Entry<Integer, String> pack = iterator.next();
                        if (pack.getValue() == null || pack.getValue().equals(plugin.getServer().getServerId())) {
                            thisServer.append(pack.getKey()).append(", ");
                            packThisServer = true;
                        } else {
                            otherServers.append(pack.getKey()).append(", ");
                            packOtherServers = true;
                        }
                    } while (iterator.hasNext());
                    if (packThisServer) {
                        sender.sendMessage(thisServer.substring(0, thisServer.length() - 2));
                    }
                    if (packOtherServers) {
                        sender.sendMessage(otherServers.substring(0, otherServers.length() - 2));
                    }
                    sender.sendMessage(ChatColor.GREEN + "Type /redeem details <id> to see what items that ID contains");
                } else {
                    sender.sendMessage(ChatColor.RED + "You have nothing to redeem");
                }
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Error getting list of things to redeem!", e);
                sender.sendMessage(ChatColor.RED + "Something went wrong!");
            }
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("details")) {
            try {
                Package pack = new Package(Integer.parseInt(args[1]));
                if (!pack.getPlayer().equals(player.getName())) {
                    sender.sendMessage(ChatColor.RED + "This package is not owned by you");
                    return true;
                }
                if (pack.getRedeemed() != null) {
                    sender.sendMessage(ChatColor.RED + "This package has been redeemed already");
                    return true;
                }
                if (pack.getEmbargo() != null && pack.getEmbargo() > System.currentTimeMillis() / 1000) {
                    sender.sendMessage(ChatColor.RED + "This package is not yet valid");
                    return true;
                }
                if (pack.getExpiry() != null && pack.getExpiry() < System.currentTimeMillis() / 1000) {
                    sender.sendMessage(ChatColor.RED + "This package has expired");
                    return true;
                }
                if (pack.getName() != null) {
                    sender.sendMessage(ChatColor.GREEN + "Name: " + ChatColor.YELLOW + pack.getName());
                }
                if (pack.getDescription() != null) {
                    sender.sendMessage(ChatColor.GREEN + "Description: " + ChatColor.YELLOW + pack.getDescription());
                }
                if (pack.getCreator() != null) {
                    sender.sendMessage(ChatColor.GREEN + "Given by: " + ChatColor.YELLOW + pack.getCreator());
                }
                if (!pack.isEmpty()) {
                    sender.sendMessage(ChatColor.GREEN + "ID " + Integer.parseInt(args[1]) + " contains the following item(s)");
                    if (pack.getMoney() != null) {
                        sender.sendMessage(ChatColor.GREEN + "" + pack.getMoney() + " " + ChatColor.GOLD + RedeemMe.economy.currencyNamePlural());
                    }
                    if (!pack.getItems().isEmpty()) {
                        for (ItemStack item : pack.getItems()) {
                            if (item.getItemMeta().hasDisplayName()) {
                                sender.sendMessage(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getItemMeta().getDisplayName());
                            } else {
                                sender.sendMessage(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getType().toString());
                            }
                        }
                    }
                    if (!pack.getCommands().isEmpty()) {
                        for (String commandLine : pack.getCommands().keySet()) {
                            sender.sendMessage(ChatColor.GREEN + "Command: " + ChatColor.GOLD + commandLine.replaceAll("<player>", player.getName()));
                        }
                    }
                    if (pack.getServer() != null && !pack.getServer().equals(player.getServer().getServerId())) {
                        sender.sendMessage(ChatColor.RED + "This package is not valid on this server");
                        sender.sendMessage(ChatColor.RED + "It must be redeemed on " + pack.getServer());
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "ID " + Integer.parseInt(args[1]) + " has nothing to redeem");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "That's not a number!");
                return true;
            } catch (InvalidConfigurationException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Data for id " + args[1] + " is corrupted!", e);
                sender.sendMessage(ChatColor.RED + "Something went wrong!");
                return true;
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Error getting list of things to redeem!", e);
                sender.sendMessage(ChatColor.RED + "Something went wrong!");
                return true;
            } catch (NonexistentCouponException e) {
                sender.sendMessage(ChatColor.RED + "That package does not exist!");
                return true;
            }
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("coupon")) {
            try {
                int id = Package.idFromCode(args[1].replaceAll("-", ""));
                Package pack = new Package(id);
                if (pack.getRedeemed() != null) {
                    sender.sendMessage(ChatColor.RED + "This coupon has been redeemed already");
                    return true;
                }
                if (pack.getEmbargo() != null && pack.getEmbargo() > System.currentTimeMillis() / 1000) {
                    sender.sendMessage(ChatColor.RED + "This coupon is not yet valid");
                    return true;
                }
                if (pack.getExpiry() != null && pack.getExpiry() < System.currentTimeMillis() / 1000) {
                    sender.sendMessage(ChatColor.RED + "This coupon has expired");
                    return true;
                }
                if (pack.getServer() != null && !pack.getServer().equals(player.getServer().getServerId())) {
                    sender.sendMessage(ChatColor.RED + "This coupon is not valid on this server");
                    sender.sendMessage(ChatColor.RED + "It must be redeemed on " + pack.getServer());
                    return true;
                }
                if (pack.getName() != null) {
                    sender.sendMessage(ChatColor.GREEN + "Name: " + ChatColor.YELLOW + pack.getName());
                }
                if (pack.getDescription() != null) {
                    sender.sendMessage(ChatColor.GREEN + "Description: " + ChatColor.YELLOW + pack.getDescription());
                }
                if (pack.getCreator() != null) {
                    sender.sendMessage(ChatColor.GREEN + "Given by: " + ChatColor.YELLOW + pack.getCreator());
                }
                if (!pack.isEmpty()) {
                    sender.sendMessage(ChatColor.GREEN + "Coupon " + args[1].toUpperCase() + " contains the following item(s)");
                    if (pack.getMoney() != null) {
                        sender.sendMessage(ChatColor.GREEN + "" + pack.getMoney() + " " + ChatColor.GOLD + RedeemMe.economy.currencyNamePlural());
                        RedeemMe.economy.depositPlayer(player.getName(), pack.getMoney());
                    }
                    if (!pack.getItems().isEmpty()) {
                        for (ItemStack item : pack.getItems()) {
                            if (item.getItemMeta().hasDisplayName()) {
                                sender.sendMessage(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getItemMeta().getDisplayName());
                            } else {
                                sender.sendMessage(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getType().toString());
                            }
                            if (plugin.strangeWeaponsEnabled() && StrangeWeapon.isStrangeWeapon(item)) {
                                item = new StrangeWeapon(item).clone();
                            }
                            player.getInventory().addItem(item);
                        }
                    }
                    if (!pack.getCommands().isEmpty()) {
                        for (Entry<String, Boolean> com : pack.getCommands().entrySet()) {
                            String commandLine = com.getKey().replaceAll("<player>", player.getName());
                            sender.sendMessage(ChatColor.GREEN + "Command: " + ChatColor.GOLD + commandLine);
                            CommandSender actor = sender;
                            if (com.getValue()) {
                                actor = plugin.getServer().getConsoleSender();
                            }
                            plugin.getServer().dispatchCommand(actor, commandLine);
                        }
                    }
                    pack.setRedeemed(player.getName());
                    plugin.getLogger().info(player.getName() + " has redeemed coupon with id " + id);
                    sender.sendMessage(ChatColor.GREEN + "Coupon successfully redeemed!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Coupon " + args[1] + " has nothing to redeem");
                }
            } catch (InvalidConfigurationException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Data for id " + args[1] + " is corrupted!", e);
                sender.sendMessage(ChatColor.RED + "Something went wrong!");
                return true;
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Error getting list of things to redeem!", e);
                sender.sendMessage(ChatColor.RED + "Something went wrong!");
                return true;
            } catch (NonexistentCouponException e) {
                sender.sendMessage(ChatColor.RED + "That coupon does not exist!");
                return true;
            }
            return true;
        }

        if (args.length == 1) {
            try {
                int id = Integer.parseInt(args[0]);
                Package pack = new Package(id);
                if (!pack.getPlayer().equals(player.getName())) {
                    sender.sendMessage(ChatColor.RED + "This package is not owned by you");
                    return true;
                }
                if (pack.getRedeemed() != null) {
                    sender.sendMessage(ChatColor.RED + "This package has been redeemed already");
                    return true;
                }
                if (pack.getEmbargo() != null && pack.getEmbargo() > System.currentTimeMillis() / 1000) {
                    sender.sendMessage(ChatColor.RED + "This package is not yet valid");
                    return true;
                }
                if (pack.getExpiry() != null && pack.getExpiry() < System.currentTimeMillis() / 1000) {
                    sender.sendMessage(ChatColor.RED + "This package has expired");
                    return true;
                }
                if (pack.getServer() != null && !pack.getServer().equals(player.getServer().getServerId())) {
                    sender.sendMessage(ChatColor.RED + "This package is not valid on this server");
                    sender.sendMessage(ChatColor.RED + "It must be redeemed on " + pack.getServer());
                    return true;
                }
                if (pack.getName() != null) {
                    sender.sendMessage(ChatColor.GREEN + "Name: " + ChatColor.YELLOW + pack.getName());
                }
                if (pack.getDescription() != null) {
                    sender.sendMessage(ChatColor.GREEN + "Description: " + ChatColor.YELLOW + pack.getDescription());
                }
                if (pack.getCreator() != null) {
                    sender.sendMessage(ChatColor.GREEN + "Given by: " + ChatColor.YELLOW + pack.getCreator());
                }
                if (!pack.isEmpty()) {
                    sender.sendMessage(ChatColor.GREEN + "Package " + id + " contains the following item(s)");
                    if (pack.getMoney() != null) {
                        sender.sendMessage(ChatColor.GREEN + "" + pack.getMoney() + " " + ChatColor.GOLD + RedeemMe.economy.currencyNamePlural());
                        RedeemMe.economy.depositPlayer(player.getName(), pack.getMoney());
                    }
                    if (!pack.getItems().isEmpty()) {
                        for (ItemStack item : pack.getItems()) {
                            if (item.getItemMeta().hasDisplayName()) {
                                sender.sendMessage(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getItemMeta().getDisplayName());
                            } else {
                                sender.sendMessage(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getType().toString());
                            }
                            if (plugin.strangeWeaponsEnabled() && StrangeWeapon.isStrangeWeapon(item)) {
                                item = new StrangeWeapon(item).clone();
                            }
                            player.getInventory().addItem(item);
                        }
                    }
                    if (!pack.getCommands().isEmpty()) {
                        for (Entry<String, Boolean> com : pack.getCommands().entrySet()) {
                            String commandLine = com.getKey().replaceAll("<player>", player.getName());
                            sender.sendMessage(ChatColor.GREEN + "Command: " + ChatColor.GOLD + commandLine);
                            CommandSender actor = sender;
                            if (com.getValue()) {
                                actor = plugin.getServer().getConsoleSender();
                            }
                            plugin.getServer().dispatchCommand(actor, commandLine);
                        }
                    }
                    pack.setRedeemed(player.getName());
                    plugin.getLogger().info(player.getName() + " has redeemed coupon with id " + id);
                    sender.sendMessage(ChatColor.GREEN + "Package successfully redeemed!");
                } else {
                    sender.sendMessage(ChatColor.RED + "ID " + id + " has nothing to redeem");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "That's not a number!");
                return true;
            } catch (InvalidConfigurationException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Data for id " + args[1] + " is corrupted!", e);
                sender.sendMessage(ChatColor.RED + "Something went wrong!");
                return true;
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Error getting list of things to redeem!", e);
                sender.sendMessage(ChatColor.RED + "Something went wrong!");
                return true;
            } catch (NonexistentCouponException e) {
                sender.sendMessage(ChatColor.RED + "That coupon does not exist!");
                return true;
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "/redeem list -> shows a list of packages you may redeem"); //Get list of packages on this server. Get list of packages on other servers. Maybe get list of future packages.
        sender.sendMessage(ChatColor.RED + "/redeem details <number> -> shows a list of items in each package"); //Get package by id
        sender.sendMessage(ChatColor.RED + "/redeem coupon <code> -> redeems a coupon"); //Get package by code
        sender.sendMessage(ChatColor.RED + "/redeem <number> -> redeems a package"); //Get package by id, update package by id
        return true;
    }
}