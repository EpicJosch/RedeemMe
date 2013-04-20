package to.joe.redeem;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import to.joe.strangeweapons.meta.StrangeWeapon;

public class Redeem {

    static boolean listAvailable(Player player) {
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
                    if (pack.getValue() == null || pack.getValue().equals(RedeemMe.getInstance().getServer().getServerId())) {
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
                return true;
            }
            return false;
        } catch (SQLException e) {
            RedeemMe.getInstance().getLogger().log(Level.SEVERE, "Error getting list of things to redeem!", e);
            player.sendMessage(ChatColor.RED + "Something went wrong!");
            return false;
        }
    }

    static boolean details(Package pack, Player player, String type, boolean redeemPackage) throws SQLException { //TODO Make sure player has room in their inventory
        if (!pack.getPlayer().equals("*") && !pack.getPlayer().equals(player.getName())) {
            player.sendMessage(ChatColor.RED + "This " + type + " is not owned by you");
            return false;
        }
        if (pack.getRedeemed() != null || pack.hasAlreadyDropped(player.getName())) {
            player.sendMessage(ChatColor.RED + "This " + type + " has been redeemed already");
            return false;
        }
        if (pack.getEmbargo() != null && pack.getEmbargo() > System.currentTimeMillis() / 1000) {
            player.sendMessage(ChatColor.RED + "This " + type + " is not yet valid");
            return false;
        }
        if (pack.getExpiry() != null && pack.getExpiry() < System.currentTimeMillis() / 1000) {
            player.sendMessage(ChatColor.RED + "This " + type + " has expired");
            return false;
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
        if (pack.getServer() != null && !pack.getServer().equals(player.getServer().getServerId())) {
            player.sendMessage(ChatColor.RED + "This " + type + " is not valid on this server");
            player.sendMessage(ChatColor.RED + "It must be redeemed on " + pack.getServer());
            return false;
        }
        if (!pack.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "ID " + pack.getId() + " contains the following item(s)"); //TODO Show coupon code instead of id
            if (pack.getMoney() != null) {
                player.sendMessage(ChatColor.GREEN + "" + pack.getMoney() + " " + ChatColor.GOLD + RedeemMe.economy.currencyNamePlural());
                if (redeemPackage) {
                    RedeemMe.economy.depositPlayer(player.getName(), pack.getMoney());
                }
            }
            if (!pack.getItems().isEmpty()) {
                for (ItemStack item : pack.getItems()) {
                    if (item.getItemMeta().hasDisplayName()) {
                        player.sendMessage(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getItemMeta().getDisplayName());
                    } else {
                        player.sendMessage(ChatColor.GREEN + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getType().toString());
                    }
                    if (redeemPackage) {
                        if (RedeemMe.getInstance().strangeWeaponsEnabled() && StrangeWeapon.isStrangeWeapon(item)) {
                            item = new StrangeWeapon(item).clone();
                        }
                        player.getInventory().addItem(item);
                    }
                }
            }
            if (!pack.getCommands().isEmpty()) {
                for (Entry<String, Boolean> com : pack.getCommands().entrySet()) {
                    String commandLine = com.getKey().replaceAll("<player>", player.getName());
                    player.sendMessage(ChatColor.GREEN + "Command: " + ChatColor.GOLD + commandLine);
                    if (redeemPackage) {
                        CommandSender actor = player;
                        if (com.getValue()) {
                            actor = RedeemMe.getInstance().getServer().getConsoleSender();
                        }
                        RedeemMe.getInstance().getServer().dispatchCommand(actor, commandLine);
                    }
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "ID " + pack.getId() + " has nothing to redeem");
        }
        return true;
    }
}