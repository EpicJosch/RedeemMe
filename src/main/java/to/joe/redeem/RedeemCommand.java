package to.joe.redeem;

import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import to.joe.redeem.exception.NonexistentCouponException;

public class RedeemCommand implements CommandExecutor {

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
            if (Redeem.listAvailable(player)) {
                sender.sendMessage(ChatColor.GREEN + "Type /redeem details <id> to see what the package contains");
            } else {
                sender.sendMessage(ChatColor.RED + "You have nothing to redeem");
            }
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("details")) {
            try {
                Package pack = new Package(Integer.parseInt(args[1]));
                Redeem.details(pack, player, "package", false);
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
                if (Redeem.details(pack, player, "coupon", true)) {
                    pack.setRedeemed(player.getName());
                    plugin.getLogger().info(player.getName() + " has redeemed coupon with id " + id);
                    sender.sendMessage(ChatColor.GREEN + "Coupon successfully redeemed!");
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
                if (Redeem.details(pack, player, "package", true)) {
                    pack.setRedeemed(player.getName());
                    plugin.getLogger().info(player.getName() + " has redeemed coupon with id " + id);
                    sender.sendMessage(ChatColor.GREEN + "Package successfully redeemed!");
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

        sender.sendMessage(ChatColor.RED + "/redeem list -> shows a list of packages you may redeem");
        sender.sendMessage(ChatColor.RED + "/redeem details <id> -> shows a list of items in each package");
        sender.sendMessage(ChatColor.RED + "/redeem coupon <code> -> redeems a coupon");
        sender.sendMessage(ChatColor.RED + "/redeem <id> -> redeems a package");
        return true;
    }
}