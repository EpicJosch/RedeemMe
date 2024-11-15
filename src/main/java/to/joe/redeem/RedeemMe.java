package to.joe.redeem;

import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import to.joe.redeem.exception.NonexistentCouponException;

public class RedeemMe extends JavaPlugin implements Listener {

    static VaultWrapper vault = null;
    private MySQL sql;
    private static RedeemMe instance;

    MySQL getMySQL() {
        return sql;
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

        vault = new VaultWrapper(this);
        if (vault.setup()) {
            getLogger().info("Economy detected, money enabled");
        } else {
            getLogger().info("Economy not detected, money disabled");
        }

        if (getServer().getName().equals("unnamed")) {
            getLogger().warning("This server does not have an ID set!");
            getLogger().warning("Set \"server-id\" in your server.properties");
        }

        PluginCommand redeemCommand = getCommand("redeem");
        PluginCommand printCouponCommand = getCommand("printcoupon");
        PluginCommand createPackageCommand = getCommand("createpackage");

        if(redeemCommand != null) {
            redeemCommand.setExecutor(new RedeemCommand(this));
        } else {
            this.getLogger().warning("Failed to register the redeem command!");
        }

        if(printCouponCommand != null) {
            printCouponCommand.setExecutor(new PrintCouponCommand(this));
        } else {
            this.getLogger().warning("Failed to register the print coupon command!");
        }

        if(createPackageCommand != null) {
            createPackageCommand.setExecutor(new CreatePackageCommand(this));
        } else {
            this.getLogger().warning("Failed to register the create package command!");
        }

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
        if (Redeem.listAvailable(player)) {
            player.sendMessage(ChatColor.GREEN + "Type /redeem for help");
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getItem() != null && event.getItem().getType().equals(Material.PAPER) && event.getItem().hasItemMeta()) {
            ItemMeta meta = event.getItem().getItemMeta();
            Pattern p = Pattern.compile(ChatColor.RED + "Coupon code (.*)");
            assert meta != null;
            Matcher m = p.matcher(Objects.requireNonNull(meta.getLore()).get(meta.getLore().size() - 1));
            if (m.matches()) {
                Player player = event.getPlayer();
                try {
                    int id = Package.idFromCode(m.group(1).replaceAll("-", ""));
                    Package pack = new Package(id);
                    if (Redeem.details(pack, player, "coupon", true)) {
                        pack.setRedeemed(player.getName());
                        getLogger().info(player.getName() + " has redeemed coupon with id " + id);
                        player.sendMessage(ChatColor.GREEN + "Coupon successfully redeemed!");
                        player.setItemInHand(null);
                    }
                } catch (InvalidConfigurationException e) {
                    getLogger().log(Level.SEVERE, "Data for id " + m.group(1) + " is corrupted!", e);
                    player.sendMessage(ChatColor.RED + "Something went wrong!");
                } catch (SQLException e) {
                    getLogger().log(Level.SEVERE, "Error getting list of things to redeem!", e);
                    player.sendMessage(ChatColor.RED + "Something went wrong!");
                } catch (NonexistentCouponException e) {
                    player.sendMessage(ChatColor.RED + "That coupon does not exist!");
                }
            }
        }
    }
}