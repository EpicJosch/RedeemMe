package to.joe.redeem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.bukkit.inventory.ItemStack;

public class PackageBuilder {

    private String name = null;
    private String description = null;
    private String creator = null;
    private Integer code = null;
    private String player = null;
    private Double money = null;
    private List<ItemStack> items = new ArrayList<ItemStack>();
    private LinkedHashMap<String, Boolean> commands = new LinkedHashMap<String, Boolean>();
    private Long embargo = null;
    private Long expiry = null;
    private String server = null;

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
        this.code = code.getID();
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
     * 
     * @throws SQLException
     */
    public Package build() throws SQLException { //TODO Make sure a code or player was given. TODO Make sure the package is not empty.
        return new Package(this.name, this.description, this.creator, this.code, this.player, this.money, this.embargo, this.expiry, this.server, this.items, this.commands);
    }
}