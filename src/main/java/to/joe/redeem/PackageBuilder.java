package to.joe.redeem;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private List<ItemStack> items = new ArrayList<ItemStack>();
    private LinkedHashMap<String, Boolean> commands = new LinkedHashMap<String, Boolean>();
    private Long embargo = null;
    private Long expiry = null;
    private String server = null;
    private boolean alreadyBuilt = false;

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Sets the name of this package.
     * 
     * @param name
     *            The name of this package
     * @return
     */
    public PackageBuilder withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the description of this package.
     * 
     * @param description
     *            The description of this package
     * @return
     */
    public PackageBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets the creator of this package.
     * 
     * @param creator
     *            The creator of this package
     * @return
     */
    public PackageBuilder withCreator(String creator) {
        this.creator = creator;
        return this;
    }

    /**
     * Sets the {@link CouponCode} associated with this package.
     * 
     * @param code
     *            The {@link CouponCode} for this package
     * @return
     */
    public PackageBuilder withCode(CouponCode code) {
        if (this.player != null) {
            throw new IncompletePackageException("Player or code already set");
        }
        this.code = code;
        this.player = "*";
        return this;
    }

    /**
     * Sets the player who is to redeem this package.
     * 
     * @param player
     *            The player who will redeem this package
     * @return
     */
    public PackageBuilder forPlayer(String player) {
        if (this.player != null) {
            throw new IncompletePackageException("Player or code already set");
        }
        if (!player.matches("[A-Za-z0-9_]{2,16}")) {
            throw new IncompletePackageException("Player name not valid");
        }
        this.player = player;
        return this;
    }

    /**
     * Sets the money included in this package.
     * 
     * @param money
     *            The amount of money included
     * @return
     */
    public PackageBuilder withMoney(double money) {
        this.money = money;
        return this;
    }

    /**
     * Adds an {@link ItemStack} to this package.
     * 
     * @param item
     *            The {@link ItemStack} to add
     * @return
     */
    public PackageBuilder withItemStack(ItemStack item) {
        this.items.add(item);
        return this;
    }

    /**
     * Adds a command to this package.
     * 
     * @param command
     *            Command to run
     * @param runAsConsole
     *            If the command should be run as console, otherwise player
     * @return
     */
    public PackageBuilder withCommand(String command, boolean runAsConsole) {
        this.commands.put(command, runAsConsole);
        return this;
    }

    /**
     * Sets the embargo of the package. The package will not be available before this time.
     * 
     * @param embargo
     *            The embargo in unix time
     * @return
     */
    public PackageBuilder withEmbargo(long embargo) {
        this.embargo = embargo;
        return this;
    }

    /**
     * Sets the expiry of the package. The package will not be available after this time.
     * 
     * @param expiry
     *            The expiry in unix time
     * @return
     */
    public PackageBuilder withExpiry(long expiry) {
        this.expiry = expiry;
        return this;
    }

    /**
     * Sets the server this package may be redeemed on.
     * 
     * @param server
     *            The server this package may be redeemed on
     * @return
     */
    public PackageBuilder onServer(String server) {
        this.server = server;
        return this;
    }

    /**
     * Saves this package and inserts it into the database.
     * This will throw an {@link IncompletePackageException} if it does not contain either money, an item, or a command.
     * If you attempt to build a package that has already been built, nothing will happen.
     * 
     * @throws SQLException
     * @throws CouponCodeAlreadyExistsException
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
        List<String> output = new ArrayList<String>();
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
                if (item.getItemMeta().hasDisplayName()) {
                    output.add(ChatColor.BLUE + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getItemMeta().getDisplayName());
                } else {
                    output.add(ChatColor.BLUE + "" + item.getAmount() + ChatColor.GOLD + "x " + item.getType().toString());
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
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the creator
     */
    public String getCreator() {
        return creator;
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

    /**
     * @return the money
     */
    public Double getMoney() {
        return money;
    }

    /**
     * @return the items
     */
    public List<ItemStack> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * @return the commands
     */
    public Map<String, Boolean> getCommands() {
        return Collections.unmodifiableMap(commands);
    }

    /**
     * @return the embargo
     */
    public Long getEmbargo() {
        return embargo;
    }

    /**
     * @return the expiry
     */
    public Long getExpiry() {
        return expiry;
    }

    /**
     * @return the server
     */
    public String getServer() {
        return server;
    }

}