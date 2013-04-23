package to.joe.redeem;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import to.joe.strangeweapons.meta.StrangeWeapon;

public class Redeem {

    static boolean listAvailable(Player player) {
        try {
            LinkedHashMap<Integer, String> packages = Package.getAvailablePackagesByPlayerName(player.getName());
            Iterator<Entry<Integer, String>> iterator = packages.entrySet().iterator();
            if (iterator.hasNext()) {
                player.sendMessage(ChatColor.GREEN + "You have the following package IDs to redeem");
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

    static boolean details(Package pack, Player player, String type, boolean redeemPackage) throws SQLException {
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
            player.sendMessage(ChatColor.BLUE + "Name: " + ChatColor.GOLD + pack.getName());
        }
        if (pack.getDescription() != null) {
            player.sendMessage(ChatColor.BLUE + "Description: " + ChatColor.GOLD + pack.getDescription());
        }
        if (pack.getCreator() != null) {
            player.sendMessage(ChatColor.BLUE + "Given by: " + ChatColor.GOLD + pack.getCreator());
        }
        if (pack.getServer() != null && !pack.getServer().equals(player.getServer().getServerId())) {
            player.sendMessage(ChatColor.RED + "This " + type + " is not valid on this server");
            player.sendMessage(ChatColor.RED + "It must be redeemed on " + pack.getServer());
            return false;
        }
        if (!pack.isEmpty()) {
            Inventory fillInv = RedeemMe.getInstance().getServer().createInventory(null, InventoryType.PLAYER);
            fillInv.setContents(player.getInventory().getContents());
            if (!fillInv.addItem(pack.getItems().toArray(new ItemStack[0])).isEmpty() && redeemPackage) {
                player.sendMessage(ChatColor.RED + "You don't have enough room in your inventory to redeem this package.");
                return false;
            }
            player.sendMessage(ChatColor.GREEN + "This package contains the following item(s)");
            if (pack.getMoney() != null && RedeemMe.economy != null) {
                player.sendMessage(ChatColor.BLUE + "" + pack.getMoney() + " " + ChatColor.GOLD + RedeemMe.economy.currencyNamePlural());
                if (redeemPackage) {
                    RedeemMe.economy.depositPlayer(player.getName(), pack.getMoney());
                }
            }
            for (ItemStack item : pack.getItems()) {
                if (item.getItemMeta().hasDisplayName()) {
                    player.sendMessage(ChatColor.BLUE + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getItemMeta().getDisplayName());
                } else {
                    player.sendMessage(ChatColor.BLUE + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getType().toString());
                }
                if (redeemPackage) {
                    if (RedeemMe.getInstance().strangeWeaponsEnabled() && StrangeWeapon.isStrangeWeapon(item)) {
                        item = new StrangeWeapon(item).clone();
                    }
                    player.getInventory().addItem(item);
                }
            }
            for (Entry<String, Boolean> com : pack.getCommands().entrySet()) {
                String commandLine = com.getKey().replaceAll("<player>", player.getName());
                player.sendMessage(ChatColor.BLUE + "Command: " + ChatColor.GOLD + commandLine);
                if (redeemPackage) {
                    CommandSender actor = player;
                    if (com.getValue()) {
                        actor = RedeemMe.getInstance().getServer().getConsoleSender();
                    }
                    RedeemMe.getInstance().getServer().dispatchCommand(actor, commandLine);
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "ID " + pack.getId() + " has nothing to redeem");
        }
        return true;
    }
}