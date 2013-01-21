package to.joe.redeem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import to.joe.strangeweapons.meta.StrangeWeapon;

public class PrintCouponCommand implements CommandExecutor {
    
    private RedeemMe plugin;

    public PrintCouponCommand(RedeemMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players may use this command");
        }
        Player player = (Player) sender;
        
        if (args.length > 0) {
            ItemStack coupon = new ItemStack(Material.PAPER);
            ItemMeta couponMeta = coupon.getItemMeta();
            List<String> lore = couponMeta.getLore();
            try {
                PreparedStatement ps = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM coupons WHERE player IS NULL AND code LIKE ? AND (expiry > ? OR expiry IS NULL) AND (? > embargo OR embargo IS NULL) AND (server = ? OR server IS NULL) AND redeemed IS NULL");
                ps.setString(1, args[0].replaceAll("-", ""));
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
                        if (rs.getString("name") != null) {
                            couponMeta.setDisplayName(ChatColor.GREEN + "Coupon: " + ChatColor.YELLOW + rs.getString("name"));
                        } else {
                            couponMeta.setDisplayName(ChatColor.GREEN + "Coupon");
                        }
                        if (rs.getString("description") != null) {
                            lore.add(ChatColor.GREEN + "Description: " + ChatColor.YELLOW + rs.getString("description"));
                        }
                        if (rs.getString("creator") != null) {
                            lore.add(ChatColor.GREEN + "Given by: " + ChatColor.YELLOW + rs.getString("creator"));
                        }
                        sender.sendMessage(ChatColor.GREEN + "Printing coupon " + ChatColor.GOLD + args[1]);
                        if (rs.getDouble("money") != 0) {
                            lore.add(ChatColor.GREEN + "" + rs.getDouble("money") + " " + ChatColor.GOLD + RedeemMe.economy.currencyNamePlural());
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
                                    lore.add(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getItemMeta().getDisplayName());
                                } else {
                                    lore.add(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getType().toString());
                                }
                            } while (rs2.next());
                        }
                        if (hasCommands) {
                            do {
                                String commandLine = rs3.getString("command");
                                lore.add(ChatColor.GREEN + "Command: " + ChatColor.GOLD + commandLine);
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
            coupon.setItemMeta(couponMeta);
            player.getInventory().addItem(coupon);
            return true;
        }
        
        
        
        return true;
    }
    
}
