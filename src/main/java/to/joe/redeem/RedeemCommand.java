package to.joe.redeem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
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
                PreparedStatement ps = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM coupons WHERE player = ? AND (expiry > ? OR expiry IS NULL) AND (? > embargo OR embargo IS NULL) AND (server = ? OR server IS NULL) AND redeemed IS NULL");
                ps.setString(1, player.getName());
                ps.setLong(2, System.currentTimeMillis() / 1000L);
                ps.setLong(3, System.currentTimeMillis() / 1000L);
                ps.setString(4, plugin.getServer().getServerId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    sender.sendMessage(ChatColor.GREEN + "You have the following packages to redeem");
                    StringBuilder sb = new StringBuilder(ChatColor.YELLOW + "");
                    do {
                        if (rs.getString("name") == null) {
                            sb.append(rs.getInt("id")).append("\n");
                        } else {
                            sb.append(rs.getInt("id")).append(ChatColor.AQUA).append(" | ").append(ChatColor.GOLD).append(rs.getString("name")).append("\n");
                        }
                    } while (rs.next());
                    sender.sendMessage(sb.substring(0, sb.length()));
                    sender.sendMessage(ChatColor.GREEN + "Type /redeem details <id> to see what items that ID contains");
                } else {
                    sender.sendMessage(ChatColor.RED + "You have nothing to redeem");
                }
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Error getting list of things to redeem", e);
            }
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("details")) {
            try {
                PreparedStatement ps = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM coupons WHERE player LIKE ? AND (expiry > ? OR expiry IS NULL) AND (? > embargo OR embargo IS NULL) AND (server = ? OR server IS NULL) AND id = ? and redeemed IS NULL");
                ps.setString(1, player.getName());
                ps.setLong(2, System.currentTimeMillis() / 1000L);
                ps.setLong(3, System.currentTimeMillis() / 1000L);
                ps.setString(4, plugin.getServer().getServerId());
                ps.setInt(5, Integer.parseInt(args[1]));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    PreparedStatement ps2 = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM couponitems WHERE id = ?");
                    ps2.setInt(1, Integer.parseInt(args[1]));
                    ResultSet rs2 = ps2.executeQuery();
                    PreparedStatement ps3 = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM couponcommands WHERE id = ?");
                    ps3.setInt(1, Integer.parseInt(args[1]));
                    ResultSet rs3 = ps3.executeQuery();
                    boolean hasItems = rs2.next();
                    boolean hasCommands = rs3.next();
                    if (hasItems || hasCommands || rs.getDouble("money") != 0) {
                        if (rs.getString("name") != null) {
                            sender.sendMessage(ChatColor.GREEN + "Name: " + ChatColor.YELLOW + rs.getString("name"));
                        }
                        if (rs.getString("description") != null) {
                            sender.sendMessage(ChatColor.GREEN + "Description: " + ChatColor.YELLOW + rs.getString("description"));
                        }
                        if (rs.getString("creator") != null) {
                            sender.sendMessage(ChatColor.GREEN + "Given by: " + ChatColor.YELLOW + rs.getString("creator"));
                        }
                        sender.sendMessage(ChatColor.GREEN + "ID " + Integer.parseInt(args[1]) + " contains the following item(s)");
                        if (rs.getDouble("money") != 0) {
                            sender.sendMessage(ChatColor.GREEN + "" + rs.getDouble("money") + " " + ChatColor.GOLD + RedeemMe.economy.currencyNamePlural());
                        }
                        if (hasItems) {
                            do {
                                YamlConfiguration config = new YamlConfiguration();
                                try {
                                    config.loadFromString(rs2.getString("item"));
                                } catch (InvalidConfigurationException e) {
                                    sender.sendMessage(ChatColor.RED + "Data for id " + args[1] + " is corrupted!");
                                    return true;
                                }
                                ItemStack item = config.getItemStack("item");
                                if (item.getItemMeta().hasDisplayName()) {
                                    sender.sendMessage(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getItemMeta().getDisplayName());
                                } else {
                                    sender.sendMessage(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getType().toString());
                                }
                            } while (rs2.next());
                        }
                        if (hasCommands) {
                            do {
                                String commandLine = rs3.getString("command").replaceAll("<player>", player.getName());
                                sender.sendMessage(ChatColor.GREEN + "Command: " + ChatColor.GOLD + commandLine);
                            } while (rs3.next());
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "ID " + Integer.parseInt(args[1]) + " has no items to redeem");
                    }
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Please enter a valid ID number");
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Error getting details", e);
            }
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("coupon")) {
            try {
                PreparedStatement ps = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM coupons WHERE player IS NULL AND code LIKE ? AND (expiry > ? OR expiry IS NULL) AND (? > embargo OR embargo IS NULL) AND (server = ? OR server IS NULL) AND redeemed IS NULL");
                ps.setString(1, args[1].replaceAll("-", ""));
                ps.setLong(2, System.currentTimeMillis() / 1000L);
                ps.setLong(3, System.currentTimeMillis() / 1000L);
                ps.setString(4, plugin.getServer().getServerId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    PreparedStatement ps2 = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM couponitems WHERE id = ?");
                    ps2.setInt(1, rs.getInt("id"));
                    ResultSet rs2 = ps2.executeQuery();
                    PreparedStatement ps3 = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM couponcommands WHERE id = ?");
                    ps3.setInt(1, rs.getInt("id"));
                    ResultSet rs3 = ps3.executeQuery();
                    boolean hasItems = rs2.next();
                    boolean hasCommands = rs3.next();
                    if (hasItems || hasCommands || rs.getDouble("money") != 0) {
                        sender.sendMessage(ChatColor.GREEN + "Redeeming coupon " + ChatColor.GOLD + args[1]);
                        if (rs.getDouble("money") != 0) {
                            sender.sendMessage(ChatColor.GREEN + "" + rs.getDouble("money") + " " + ChatColor.GOLD + RedeemMe.economy.currencyNamePlural());
                            RedeemMe.economy.depositPlayer(player.getName(), rs.getDouble("money"));
                        }
                        if (hasItems) {
                            do {
                                YamlConfiguration config = new YamlConfiguration();
                                try {
                                    config.loadFromString(rs2.getString("item"));
                                } catch (InvalidConfigurationException e) {
                                    sender.sendMessage(ChatColor.RED + "Data for id " + args[1] + " is corrupted!");
                                    return true;
                                }
                                ItemStack item = config.getItemStack("item");
                                if (StrangeWeapon.isStrangeWeapon(item)) {
                                    item = new StrangeWeapon(item).clone();
                                }
                                if (item.getItemMeta().hasDisplayName()) {
                                    sender.sendMessage(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getItemMeta().getDisplayName());
                                } else {
                                    sender.sendMessage(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getType().toString());
                                }
                                player.getInventory().addItem(item);
                            } while (rs2.next());
                        }
                        if (hasCommands) {
                            do {
                                CommandSender actor = sender;
                                if (rs3.getBoolean("console")) {
                                    actor = player.getServer().getConsoleSender();
                                }
                                String commandLine = rs3.getString("command").replaceAll("<player>", player.getName());
                                sender.sendMessage(ChatColor.GREEN + "Command: " + ChatColor.GOLD + commandLine);
                                plugin.getServer().dispatchCommand(actor, commandLine);
                            } while (rs3.next());
                        }
                        PreparedStatement ps4 = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("UPDATE coupons SET player = ?, redeemed = ? WHERE id = ?");
                        ps4.setString(1, player.getName());
                        ps4.setLong(2, System.currentTimeMillis() / 1000L);
                        ps4.setInt(3, rs.getInt("id"));
                        ps4.execute();
                        this.plugin.getLogger().info(player.getName() + " has redeemed coupon with id " + rs.getInt("id"));
                        sender.sendMessage(ChatColor.GREEN + "Coupon successfully redeemed!");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "That coupon code is not valid. It may have been redeemed already or may not exist.");
                }
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Error redeeming coupon", e);
            }
            return true;
        }

        if (args.length == 1) {
            try {
                PreparedStatement ps = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM coupons WHERE player LIKE ? AND (expiry > ? OR expiry IS NULL) AND (? > embargo OR embargo IS NULL) AND (server = ? OR server IS NULL) AND id = ? and redeemed IS NULL");
                ps.setString(1, player.getName());
                ps.setLong(2, System.currentTimeMillis() / 1000L);
                ps.setLong(3, System.currentTimeMillis() / 1000L);
                ps.setString(4, plugin.getServer().getServerId());
                ps.setInt(5, Integer.parseInt(args[0]));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    PreparedStatement ps2 = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM couponitems WHERE id = ?");
                    ps2.setInt(1, rs.getInt("id"));
                    ResultSet rs2 = ps2.executeQuery();
                    PreparedStatement ps3 = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM couponcommands WHERE id = ?");
                    ps3.setInt(1, rs.getInt("id"));
                    ResultSet rs3 = ps3.executeQuery();
                    boolean hasItems = rs2.next();
                    boolean hasCommands = rs3.next();
                    if (hasItems || hasCommands || rs.getDouble("money") != 0) {
                        sender.sendMessage(ChatColor.GREEN + "Redeeming ID " + ChatColor.GOLD + rs.getInt("id"));
                        if (rs.getDouble("money") != 0) {
                            sender.sendMessage(ChatColor.GREEN + "" + rs.getDouble("money") + " " + ChatColor.GOLD + RedeemMe.economy.currencyNamePlural());
                            RedeemMe.economy.depositPlayer(player.getName(), rs.getDouble("money"));
                        }
                        if (hasItems) {
                            do {
                                YamlConfiguration config = new YamlConfiguration();
                                try {
                                    config.loadFromString(rs2.getString("item"));
                                } catch (InvalidConfigurationException e) {
                                    sender.sendMessage(ChatColor.RED + "Data for id " + args[1] + " is corrupted!");
                                    return true;
                                }
                                ItemStack item = config.getItemStack("item");
                                if (StrangeWeapon.isStrangeWeapon(item)) {
                                    item = new StrangeWeapon(item).clone();
                                }
                                if (item.getItemMeta().hasDisplayName()) {
                                    sender.sendMessage(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getItemMeta().getDisplayName());
                                } else {
                                    sender.sendMessage(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getType().toString());
                                }
                                player.getInventory().addItem(item);
                            } while (rs2.next());
                        }
                        if (hasCommands) {
                            do {
                                CommandSender actor = sender;
                                if (rs3.getBoolean("console")) {
                                    actor = player.getServer().getConsoleSender();
                                }
                                String commandLine = rs3.getString("command").replaceAll("<player>", player.getName());
                                sender.sendMessage(ChatColor.GREEN + "Command: " + ChatColor.GOLD + commandLine);
                                plugin.getServer().dispatchCommand(actor, commandLine);
                            } while (rs3.next());
                        }
                        PreparedStatement ps4 = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("UPDATE coupons SET redeemed = ? WHERE id = ?");
                        ps4.setLong(1, System.currentTimeMillis() / 1000L);
                        ps4.setInt(2, rs.getInt("id"));
                        ps4.execute();
                        this.plugin.getLogger().info(player.getName() + " has redeemed items with id " + rs.getInt("id"));
                        sender.sendMessage(ChatColor.GREEN + "Items successfully redeemed!");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "That ID number is not valid. It may have been redeemed already or may not exist.");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Please enter a valid ID number");
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Error redeeming items", e);
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