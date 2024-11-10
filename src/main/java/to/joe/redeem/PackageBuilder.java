package to.joe.redeem;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import to.joe.redeem.exception.CouponCodeAlreadyExistsException;
import to.joe.redeem.exception.IncompletePackageException;

public class PackageBuilder {

    private String name = null;
    private String description = null;
    private String creator = null;
    private CouponCode code = null;
    private String player = null;
    private Double money = null;
    private final List<ItemStack> items = new ArrayList<>();
    private final LinkedHashMap<String, Boolean> commands = new LinkedHashMap<>();
    private Long embargo = null;
    private Long expiry = null;
    private String server = null;
    private boolean alreadyBuilt = false;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Sets the name of this package.
     *
     * @param name The name of this package
     */
    public void withName(String name) {
        this.name = name;
    }

    /**
     * Sets the description of this package.
     *
     * @param description The description of this package
     */
    public void withDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the creator of this package.
     *
     * @param creator The creator of this package
     */
    public void withCreator(String creator) {
        this.creator = creator;
    }

    /**
     * Sets the {@link CouponCode} associated with this package.
     *
     * @param code The {@link CouponCode} for this package
     */
    public void withCode(CouponCode code) {
        if (this.player != null) {
            throw new IncompletePackageException("Player or code already set");
        }
        this.code = code;
        this.player = "*";
    }

    /**
     * Sets the player who is to redeem this package.
     *
     * @param player The player who will redeem this package
     */
    public void forPlayer(String player) {
        if (this.player != null) {
            throw new IncompletePackageException("Player or code already set");
        }
        if (!player.matches("[A-Za-z0-9_]{2,16}")) {
            throw new IncompletePackageException("Player name not valid");
        }
        this.player = player;
    }

    /**
     * Sets the money included in this package.
     *
     * @param money The amount of money included
     */
    public void withMoney(double money) {
        this.money = money;
    }

    /**
     * Adds an {@link ItemStack} to this package.
     *
     * @param item The {@link ItemStack} to add
     */
    public void withItemStack(ItemStack item) {
        this.items.add(item);
    }

    /**
     * Adds a command to this package.
     *
     * @param command      Command to run
     * @param runAsConsole If the command should be run as console, otherwise player
     */
    public void withCommand(String command, boolean runAsConsole) {
        this.commands.put(command, runAsConsole);
    }

    /**
     * Sets the embargo of the package. The package will not be available before this time.
     *
     * @param embargo The embargo in unix time
     */
    public void withEmbargo(long embargo) {
        this.embargo = embargo;
    }

    /**
     * Sets the expiry of the package. The package will not be available after this time.
     *
     * @param expiry The expiry in unix time
     */
    public void withExpiry(long expiry) {
        this.expiry = expiry;
    }

    /**
     * Sets the server this package may be redeemed on.
     *
     * @param server The server this package may be redeemed on
     */
    public void onServer(String server) {
        this.server = server;
    }

    /**
     * Saves this package and inserts it into the database.
     * This will throw an {@link IncompletePackageException} if it does not contain either money, an item, or a command.
     * If you attempt to build a package that has already been built, nothing will happen.
     *
     */
    public void build() throws SQLException, CouponCodeAlreadyExistsException {
        if (alreadyBuilt) {
            return;
        }
        if (this.money == null && this.items.isEmpty() && this.commands.isEmpty()) {
            throw new IncompletePackageException("Package is empty");
        }
        if (this.player == null) {
            throw new IncompletePackageException("Player or code not set");
        }
        Integer codeID = null;
        if (this.code != null) {
            this.code.save();
            codeID = this.code.getID();
        }
        new Package(this.name, this.description, this.creator, codeID, this.player, this.money, this.embargo, this.expiry, this.server, this.items, this.commands);
        alreadyBuilt = true;
    }

    /**
     * Returns an array of strings showing the details of this builder. The output is intended to be used with {@link Player#sendMessage(String[])} or similar.
     * 
     * @return An array of strings showing the details of this builder.
     */
    public String[] details() {
        List<String> output = new ArrayList<>();
        if (this.name != null) {
            output.add(ChatColor.BLUE + "Name: " + ChatColor.GOLD + this.name);
        }
        if (this.description != null) {
            output.add(ChatColor.BLUE + "Description: " + ChatColor.GOLD + this.description);
        }
        if (this.creator != null) {
            output.add(ChatColor.BLUE + "Given by: " + ChatColor.GOLD + this.creator);
        }
        if (this.server == null) {
            output.add(ChatColor.BLUE + "Redeemable on " + ChatColor.GOLD + "all " + ChatColor.BLUE + "servers");
        } else {
            output.add(ChatColor.BLUE + "Redeemable on " + ChatColor.GOLD + this.server);
        }
        if (this.embargo != null) {
            output.add(ChatColor.BLUE + "Available after " + ChatColor.GOLD + sdf.format(new Date(this.embargo * 1000)));
        }
        if (this.expiry != null) {
            output.add(ChatColor.BLUE + "Expiring at " + ChatColor.GOLD + sdf.format(new Date(this.expiry * 1000)));
        }
        if (this.code != null) {
            if (code.getRemaining() == -1) {
                output.add(ChatColor.BLUE + "With code " + ChatColor.GOLD + this.code.getCode() + ChatColor.BLUE + " and " + ChatColor.GOLD + "unlimited " + ChatColor.BLUE + "uses");
            } else {
                output.add(ChatColor.BLUE + "With code " + ChatColor.GOLD + this.code.getCode() + ChatColor.BLUE + " and " + ChatColor.GOLD + code.getRemaining() + ChatColor.BLUE + " use(s)");
            }
        } else {
            output.add(ChatColor.BLUE + "For player " + ChatColor.GOLD + this.player);
        }
        if (this.money == null && this.items.isEmpty() && this.commands.isEmpty()) {
            output.add(ChatColor.RED + "Containing no items");
        } else {
            output.add(ChatColor.GREEN + "Containing the following item(s)");
            if (money != null) {
                output.add(ChatColor.BLUE + "" + this.money + " " + ChatColor.GOLD + RedeemMe.vault.getEconomy().currencyNamePlural());
            }
            for (ItemStack item : this.items) {
                if (Objects.requireNonNull(item.getItemMeta()).hasDisplayName()) {
                    output.add(ChatColor.BLUE + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getItemMeta().getDisplayName());
                } else {
                    output.add(ChatColor.BLUE + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getType());
                }
            }
            for (Entry<String, Boolean> com : this.commands.entrySet()) {
                String runner = com.getValue() ? "console" : "the player";
                output.add(ChatColor.BLUE + "Command: " + ChatColor.GOLD + com.getKey() + ChatColor.BLUE + " run as " + ChatColor.GOLD + runner);
            }
        }
        return output.toArray(new String[0]);
    }

    /**
     * @return the code
     */
    public CouponCode getCode() {
        return code;
    }

    /**
     * @return the player
     */
    public String getPlayer() {
        return player;
    }

}