package to.joe.redeem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Objects;
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

import to.joe.redeem.exception.NonexistentCouponException;

public class PrintCouponCommand implements CommandExecutor {

    private final RedeemMe plugin;

    public PrintCouponCommand(RedeemMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players may use this command");
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /printcoupon <code> [quantity]");
            return true;
        }
        int quantity = 1;
        if (args.length > 1) {
            try {
                quantity = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                //Ignore
            }
        }
        Player player;
        if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            return true;
        }
        try {
            ItemStack couponItem = new ItemStack(Material.PAPER, quantity);
            ItemMeta meta = couponItem.getItemMeta();
            ArrayList<String> lore = new ArrayList<>();
            int id = Package.idFromCode(args[0].replaceAll("-", ""));
            Package pack = new Package(id);
            if (pack.getRedeemed() != null || pack.hasAlreadyDropped(player.getName())) {
                sender.sendMessage(ChatColor.RED + "This coupon has been redeemed already");
                return true;
            }
            if (pack.getServer() != null && !pack.getServer().equals(player.getServer().getName())) {
                sender.sendMessage(ChatColor.RED + "This coupon is not valid on this server");
                sender.sendMessage(ChatColor.RED + "It must be redeemed on " + pack.getServer());
                return true;
            }
            assert meta != null;
            if (pack.getName() == null) {
                meta.setDisplayName(ChatColor.BLUE + "Coupon");
            } else {
                meta.setDisplayName(ChatColor.BLUE + "Coupon: " + ChatColor.GOLD + pack.getName());
            }
            if (pack.getDescription() != null) {
                lore.add(ChatColor.BLUE + "Description: " + ChatColor.GOLD + pack.getDescription());
            }
            if (pack.getCreator() != null) {
                lore.add(ChatColor.BLUE + "Given by: " + ChatColor.GOLD + pack.getCreator());
            }
            if (pack.isNotEmpty()) {
                lore.add(ChatColor.GREEN + "This coupon contains the following item(s)");
                if (pack.getMoney() != null) {
                    lore.add(ChatColor.BLUE + "" + pack.getMoney() + " " + ChatColor.GOLD + RedeemMe.vault.getEconomy().currencyNamePlural());
                }
                if (!pack.getItems().isEmpty()) {
                    for (ItemStack item : pack.getItems()) {
                        if (Objects.requireNonNull(item.getItemMeta()).hasDisplayName()) {
                            lore.add(ChatColor.BLUE + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getItemMeta().getDisplayName());
                        } else {
                            lore.add(ChatColor.BLUE + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getType());
                        }
                    }
                }
                if (!pack.getCommands().isEmpty()) {
                    for (Entry<String, Boolean> com : pack.getCommands().entrySet()) {
                        lore.add(ChatColor.BLUE + "Command: " + ChatColor.GOLD + com.getKey());
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