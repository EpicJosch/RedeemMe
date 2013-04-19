package to.joe.redeem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import to.joe.redeem.exception.NonexistentCouponException;

/**
 * Represents a package that can be assigned to a player directly or via a coupon code.
 * 
 * @author Eviltechie
 */
public class Package {

    private int id; //ID stored in the database
    private String name = null; //Package name -optional-
    private String description = null; //Package description -optional-
    private String creator = null; //Package creator -optional-
    private Integer code = null; //If this package was created via a coupon code, this will be set.
    private String player = null; //Player who redeemed the package.
    private Double money = null; //Any money to be given to the player as part of the package.
    private List<ItemStack> items = new ArrayList<ItemStack>(); //Any itemstacks attached to this package.
    private LinkedHashMap<String, Boolean> commands = new LinkedHashMap<String, Boolean>(); //And commands attached to this package. <String commandToRun, Boolean runAsConsole>
    private Long redeemed = null; //Time this package was redeemed.
    private Long embargo = null; //Time this package becomes enabled.
    private Long expiry = null; //Time this package expires.
    private String server = null; //Server this package is valid on. Null if unrestricted.

    /**
     * Gets a map of package ids and servers of all packages that a player may redeem.
     * 
     * @param name
     *            The name of the player to retrieve packages for
     * @return A map of package ids and servers
     * @throws SQLException
     */
    static LinkedHashMap<Integer, String> getAvailablePackagesByPlayerName(String name) throws SQLException { //id, server
        LinkedHashMap<Integer, String> packages = new LinkedHashMap<Integer, String>();
        PreparedStatement ps = RedeemMe.getInstance().getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM packages WHERE player = ? AND (expiry > ? OR expiry IS NULL) AND (? > embargo OR embargo IS NULL) AND redeemed IS NULL");
        ps.setString(1, name);
        ps.setLong(2, System.currentTimeMillis() / 1000L);
        ps.setLong(3, System.currentTimeMillis() / 1000L);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            packages.put(rs.getInt("id"), rs.getString("server"));
        }
        return packages;
    }

    /**
     * Turns a coupon code into a package id
     * 
     * @param code
     *            The coupon code to lookup
     * @return
     * @throws SQLException
     * @throws NonexistentCouponException
     */
    static int idFromCode(String code) throws SQLException, NonexistentCouponException {
        PreparedStatement ps = RedeemMe.getInstance().getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT id FROM packages WHERE code = (SELECT codeid FROM couponcodes WHERE code = ? AND (remaining > 0 OR remaining = -1))");
        ps.setString(1, code.replaceAll("-", ""));
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("id");
        } else {
            throw new NonexistentCouponException();
        }
    }

    /**
     * Loads an already existing package from an id
     * 
     * @param id
     *            The package id of an already existing package
     * @throws SQLException
     * @throws NonexistentCouponException
     * @throws InvalidConfigurationException
     */
    Package(int id) throws SQLException, NonexistentCouponException, InvalidConfigurationException {
        this.id = id;
        PreparedStatement ps = RedeemMe.getInstance().getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM packages WHERE id = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            throw new NonexistentCouponException();
        }
        this.name = rs.getString("name");
        this.description = rs.getString("description");
        this.creator = rs.getString("creator");
        this.code = rs.getInt("code");
        if (rs.wasNull()) {
            this.code = null;
        }
        this.player = rs.getString("player");
        this.money = rs.getDouble("money");
        if (rs.wasNull()) {
            this.money = null;
        }
        this.redeemed = rs.getLong("redeemed");
        if (rs.wasNull()) {
            this.redeemed = null;
        }
        this.embargo = rs.getLong("embargo");
        if (rs.wasNull()) {
            this.embargo = null;
        }
        this.expiry = rs.getLong("expiry");
        if (rs.wasNull()) {
            this.expiry = null;
        }
        this.server = rs.getString("server");

        PreparedStatement ps2 = RedeemMe.getInstance().getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM packageitems WHERE id = ?");
        ps2.setInt(1, rs.getInt("id"));
        ResultSet rs2 = ps2.executeQuery();
        while (rs2.next()) {
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(rs2.getString("item"));
            items.add(config.getItemStack("item"));
        }

        PreparedStatement ps3 = RedeemMe.getInstance().getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM packagecommands WHERE id = ?");
        ps3.setInt(1, rs.getInt("id"));
        ResultSet rs3 = ps3.executeQuery();
        while (rs3.next()) {
            commands.put(rs3.getString("command"), rs3.getBoolean("console"));
        }
    }

    /**
     * Creates a new package for api use.
     * 
     * @param commands2
     * @param items2
     * 
     * @throws SQLException
     */
    Package(String name, String description, String creator, Integer code, String player, Double money, Long embargo, Long expiry, String server, List<ItemStack> items, LinkedHashMap<String, Boolean> commands) throws SQLException {
        PreparedStatement ps = RedeemMe.getInstance().getMySQL().getFreshPreparedStatementWithGeneratedKeys("INSERT INTO packages (name, description, created, creator, code, player, money, embargo, expiry, server) VALUES (?,?,?,?,?,?,?,?,?,?)");
        this.name = name;
        if (name == null) {
            ps.setNull(1, Types.VARCHAR);
        } else {
            ps.setString(1, name);
        }

        this.description = description;
        if (description == null) {
            ps.setNull(2, Types.VARCHAR);
        } else {
            ps.setString(2, description);
        }

        ps.setInt(3, (int) System.currentTimeMillis() / 1000);

        this.creator = creator;
        if (creator == null) {
            ps.setNull(4, Types.VARCHAR);
        } else {
            ps.setString(4, creator);
        }

        this.code = code;
        if (code == null) {
            ps.setNull(5, Types.INTEGER);
        } else {
            ps.setInt(5, code);
        }

        this.player = player;
        if (player == null) {
            ps.setNull(6, Types.VARCHAR);
        } else {
            ps.setString(6, player);
        }

        this.money = money;
        if (money == null) {
            ps.setNull(7, Types.DOUBLE);
        } else {
            ps.setDouble(7, money);
        }

        this.embargo = embargo;
        if (embargo == null) {
            ps.setNull(8, Types.INTEGER);
        } else {
            ps.setLong(8, embargo);
        }

        this.expiry = expiry;
        if (expiry == null) {
            ps.setNull(9, Types.INTEGER);
        } else {
            ps.setLong(9, expiry);
        }

        this.server = server;
        if (server == null) {
            ps.setNull(10, Types.VARCHAR);
        } else {
            ps.setString(10, server);
        }

        ps.execute();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            this.id = rs.getInt(1);
        } else {
            throw new SQLException("Error creating new package!");
        }

        this.items = items;
        for (ItemStack item : items) {
            ps = RedeemMe.getInstance().getMySQL().getFreshPreparedStatementHotFromTheOven("INSERT INTO packageitems (id, item) VALUES (?,?)");
            ps.setInt(1, this.id);
            YamlConfiguration config = new YamlConfiguration();
            config.set("item", item);
            ps.setString(2, config.saveToString());
            ps.execute();
        }

        this.commands = commands;
        for (Entry<String, Boolean> command : commands.entrySet()) {
            ps = RedeemMe.getInstance().getMySQL().getFreshPreparedStatementHotFromTheOven("INSERT INTO packagecommands (id, command, console) VALUES (?,?,?)");
            ps.setInt(1, id);
            ps.setString(2, command.getKey());
            ps.setBoolean(3, command.getValue());
            ps.execute();
        }
    }

    int getId() {
        return id;
    }

    String getName() {
        return name;
    }

    String getDescription() {
        return description;
    }

    String getCreator() {
        return creator;
    }

    Integer getCode() {
        return code;
    }

    String getPlayer() {
        return player;
    }

    Double getMoney() {
        return money;
    }

    List<ItemStack> getItems() {
        return Collections.unmodifiableList(items);
    }

    Map<String, Boolean> getCommands() {
        return Collections.unmodifiableMap(commands);
    }

    Long getRedeemed() {
        return redeemed;
    }

    Long getEmbargo() {
        return embargo;
    }

    Long getExpiry() {
        return expiry;
    }

    String getServer() {
        return server;
    }

    boolean isEmpty() {
        return getMoney() == null && getItems().isEmpty() && getCommands().isEmpty();
    }

    void setRedeemed(String player) throws SQLException {
        int newid = this.id;
        if (this.player.equals("*")) {
            newid = new Package(this.name, this.description, this.creator, this.code, player, this.money, this.embargo, this.expiry, this.server, this.items, this.commands).getId();
            PreparedStatement ps = RedeemMe.getInstance().getMySQL().getFreshPreparedStatementHotFromTheOven("UPDATE couponcodes SET remaining = remaining - 1 WHERE codeid = ? AND remaining != -1");
            ps.setInt(1, this.code);
            ps.execute();
        }
        PreparedStatement ps = RedeemMe.getInstance().getMySQL().getFreshPreparedStatementHotFromTheOven("UPDATE packages SET redeemed = ? WHERE id = ?");
        ps.setLong(1, System.currentTimeMillis() / 1000L);
        ps.setInt(2, newid);
        ps.execute();
    }

    /**
     * Checks if the specified package has already been given through a coupon
     * 
     * @param pack
     *            The package to check
     * @param player
     *            The player to check
     * @return True if the package has already been given
     * @throws SQLException
     */
    boolean hasAlreadyDropped(String player) throws SQLException {
        if (this.code == null) {
            return false;
        }
        PreparedStatement ps = RedeemMe.getInstance().getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM packages WHERE code = ? AND player = ?");
        ps.setInt(1, this.code);
        ps.setString(2, player);
        return ps.executeQuery().next();
    }
}