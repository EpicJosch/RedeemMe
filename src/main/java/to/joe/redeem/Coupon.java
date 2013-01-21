package to.joe.redeem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class Coupon {

    static RedeemMe plugin;

    private int id;
    private String name = null;
    private String description = null;
    private String creator = null;
    private String code = null;
    private String player = null;
    private Double money = null;
    private Long redeemed = null;
    private Long embargo = null;
    private Long expiry = null;
    private String server = null;

    private List<ItemStack> items = new ArrayList<ItemStack>();
    private LinkedHashMap<String, Boolean> commands = new LinkedHashMap<String, Boolean>(); //command, console

    public static LinkedHashMap<Integer, String> getAvailablePackagesByName(String name) throws SQLException { //id, server
        LinkedHashMap<Integer, String> packages = new LinkedHashMap<Integer, String>();
        PreparedStatement ps = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM coupons WHERE player = ? AND (expiry > ? OR expiry IS NULL) AND (? > embargo OR embargo IS NULL) AND redeemed IS NULL");
        ps.setString(1, name);
        ps.setLong(2, System.currentTimeMillis() / 1000L);
        ps.setLong(3, System.currentTimeMillis() / 1000L);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            packages.put(rs.getInt("id"), rs.getString("server"));
        }
        return packages;
    }

    public static int idFromCode(String code) throws SQLException, NonexistentCouponException {
        PreparedStatement ps = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM coupons WHERE code = ?");
        ps.setString(1, code.replaceAll("-", ""));
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("id");
        } else {
            throw new NonexistentCouponException();
        }
    }

    public Coupon(int id) throws InvalidConfigurationException, SQLException, NonexistentCouponException { //From id
        this.id = id;
        PreparedStatement ps = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM coupons WHERE id = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            throw new NonexistentCouponException();
        }
        name = rs.getString("name");
        description = rs.getString("description");
        creator = rs.getString("creator");
        code = rs.getString("code");
        player = rs.getString("player");
        if (rs.getDouble("money") != 0) {
            money = rs.getDouble("money");
        }
        if (rs.getLong("redeemed") != 0) {
            redeemed = rs.getLong("redeemed");
        }
        if (rs.getLong("embargo") != 0) {
            embargo = rs.getLong("embargo");
        }
        if (rs.getLong("expiry") != 0) {
            expiry = rs.getLong("expiry");
        }
        server = rs.getString("server");

        PreparedStatement ps2 = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM couponitems WHERE id = ?");
        ps2.setInt(1, rs.getInt("id"));
        ResultSet rs2 = ps2.executeQuery();
        while (rs2.next()) {
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(rs2.getString("item"));
            items.add(config.getItemStack("item"));
        }

        PreparedStatement ps3 = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM couponcommands WHERE id = ?");
        ps3.setInt(1, rs.getInt("id"));
        ResultSet rs3 = ps3.executeQuery();
        while (rs3.next()) {
            commands.put(rs3.getString("command"), rs3.getBoolean("console"));
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCreator() {
        return creator;
    }

    public String getCode() {
        return code;
    }

    public String getPlayer() {
        return player;
    }

    public Double getMoney() {
        return money;
    }

    public Long getRedeemed() {
        return redeemed;
    }

    public Long getEmbargo() {
        return embargo;
    }

    public Long getExpiry() {
        return expiry;
    }

    public String getServer() {
        return server;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public LinkedHashMap<String, Boolean> getCommands() {
        return commands;
    }

    public boolean isEmpty() {
        return getMoney() == null && getItems().isEmpty() && getCommands().isEmpty();
    }

    public void setRedeemed(String player) throws SQLException {
        PreparedStatement ps = plugin.getMySQL().getFreshPreparedStatementHotFromTheOven("UPDATE coupons SET player = ?, redeemed = ? WHERE id = ?");
        ps.setString(1, player);
        ps.setLong(2, System.currentTimeMillis() / 1000L);
        ps.setInt(3, id);
        ps.execute();
    }
}