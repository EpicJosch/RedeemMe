package to.joe.redeem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

        try {
            ItemStack couponItem = new ItemStack(Material.PAPER);
            ItemMeta meta = couponItem.getItemMeta();
            ArrayList<String> lore = new ArrayList<String>();
            int id = Coupon.idFromCode(args[0].replaceAll("-", ""));
            Coupon coupon = new Coupon(id);
            if (coupon.getRedeemed() != null) {
                sender.sendMessage(ChatColor.RED + "This coupon has been redeemed already");
                return true;
            }
            if (coupon.getServer() != null && !coupon.getServer().equals(player.getServer().getServerId())) {
                sender.sendMessage(ChatColor.RED + "This coupon is not valid on this server");
                sender.sendMessage(ChatColor.RED + "It must be redeemed on " + coupon.getServer());
                return true;
            }
            if (coupon.getName() == null) {
                meta.setDisplayName(ChatColor.GREEN + "Coupon");
            } else {
                meta.setDisplayName(ChatColor.GREEN + "Coupon: " + ChatColor.YELLOW + coupon.getName());
            }
            if (coupon.getDescription() != null) {
                lore.add(ChatColor.GREEN + "Description: " + ChatColor.YELLOW + coupon.getDescription());
            }
            if (coupon.getCreator() != null) {
                lore.add(ChatColor.GREEN + "Given by: " + ChatColor.YELLOW + coupon.getCreator());
            }
            if (!coupon.isEmpty()) {
                lore.add(ChatColor.GREEN + "This coupon contains the following item(s)");
                if (coupon.getMoney() != null) {
                    lore.add(ChatColor.GREEN + "" + coupon.getMoney() + " " + ChatColor.GOLD + RedeemMe.economy.currencyNamePlural());
                }
                if (!coupon.getItems().isEmpty()) {
                    for (ItemStack item : coupon.getItems()) {
                        if (item.getItemMeta().hasDisplayName()) {
                            lore.add(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getItemMeta().getDisplayName());
                        } else {
                            lore.add(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getType().toString());
                        }
                    }
                }
                if (!coupon.getCommands().isEmpty()) {
                    for (Entry<String, Boolean> com : coupon.getCommands().entrySet()) {
                        lore.add(ChatColor.GREEN + "Command: " + ChatColor.GOLD + com.getKey());
                    }
                }
                lore.add(ChatColor.RED + "Coupon code " + args[0].toUpperCase());
                sender.sendMessage(ChatColor.GREEN + "Printed coupon for " + args[0].toUpperCase());
                meta.setLore(lore);
                couponItem.setItemMeta(meta);
                player.getInventory().addItem(couponItem);
            } else {
                sender.sendMessage(ChatColor.RED + "Coupon " + args[0] + " has nothing to redeem");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "That's not a number!");
            return true;
        } catch (InvalidConfigurationException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Data for id " + args[0] + " is corrupted!", e);
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
}