package to.joe.redeem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class RedeemAPI {

    /**
     * Creates a new coupon or package, depending on the parameters specified
     * 
     * @param name
     *            The name for this coupon. Can be null.
     * @param description
     *            The description for this coupon. Can be null.
     * @param creator
     *            The creator for this coupon. Can be null.
     * @param code
     *            The coupon code for this coupon. This should be 15 alphanumeric characters. Set to null if you are creating a package instead.
     * @param player
     *            The player this package will be owned by. Set to null if you are creating a coupon or "*" if you are creating a coupon that can be redeemed by more than one player.
     * @param money
     *            The amount of currency the player will get this coupon is redeemed. Can be null.
     * @param embargo
     *            The Unix time that this coupon is not good before. Can be null.
     * @param expiry
     *            The Unix time that this coupon is not good after. Can be null
     * @param server
     *            The server this coupon will be valid on. Set to null for all servers. Compared against {@link Server#getServerId()}
     * @return The id of the newly inserted coupon. Use this id along with {@link RedeemAPI#addItem(int, ItemStack)} or {@link RedeemAPI#addCommand(int, String, boolean)} to add items or commands to the coupon.
     * @throws SQLException
     */
    public static int newCoupon(String name, String description, String creator, String code, String player, Double money, Long embargo, Long expiry, String server) throws SQLException {
        PreparedStatement ps = RedeemMe.getMySQL().getFreshPreparedStatementWithGeneratedKeys("INSERT INTO coupons (name, description, created, creator, code, player, money, embargo, expiry, server) VALUES (?,?,?,?,?,?,?,?,?,?)");
        if (name == null) {
            ps.setNull(1, Types.VARCHAR);
        } else {
            ps.setString(1, name);
        }
        if (description == null) {
            ps.setNull(2, Types.VARCHAR);
        } else {
            ps.setString(2, description);
        }
        ps.setInt(3, (int) System.currentTimeMillis() / 1000);
        if (creator == null) {
            ps.setNull(4, Types.VARCHAR);
        } else {
            ps.setString(4, creator);
        }
        if (code == null) {
            ps.setNull(5, Types.VARCHAR);
        } else {
            ps.setString(5, code);
        }
        if (player == null) {
            ps.setNull(6, Types.VARCHAR);
        } else {
            ps.setString(6, player);
        }
        if (money == null) {
            ps.setNull(7, Types.DOUBLE);
        } else {
            ps.setDouble(7, money);
        }
        if (embargo == null) {
            ps.setNull(8, Types.INTEGER);
        } else {
            ps.setLong(8, embargo);
        }
        if (expiry == null) {
            ps.setNull(9, Types.INTEGER);
        } else {
            ps.setLong(9, expiry);
        }
        if (server == null) {
            ps.setNull(10, Types.VARCHAR);
        } else {
            ps.setString(10, server);
        }
        System.out.println(ps.toString());
        ps.execute();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next())
            return rs.getInt(1);
        else
            return -1;
    }

    /**
     * Adds an {@link ItemStack} to an already existing coupon
     * 
     * @param id
     *            The id of an already existing coupon
     * @param item
     *            The {@link ItemStack} to add
     * @throws SQLException
     */
    public static void addItem(int id, ItemStack item) throws SQLException {
        PreparedStatement ps = RedeemMe.getMySQL().getFreshPreparedStatementHotFromTheOven("INSERT INTO couponitems (id, item) VALUES (?,?)");
        ps.setInt(1, id);
        YamlConfiguration config = new YamlConfiguration();
        config.set("item", item);
        ps.setString(2, config.saveToString());
        ps.execute();
    }

    /**
     * Adds a command to an already existing coupon
     * 
     * @param id
     *            The id of an already existing coupon
     * @param command
     *            The command to add
     * @param console
     *            If this command should be executed as the console
     * @throws SQLException
     */
    public static void addCommand(int id, String command, boolean console) throws SQLException {
        PreparedStatement ps = RedeemMe.getMySQL().getFreshPreparedStatementHotFromTheOven("INSERT INTO couponcommands (id, command, console) VALUES (?,?,?)");
        ps.setInt(1, id);
        ps.setString(2, command);
        ps.setBoolean(3, console);
        ps.execute();
    }
}
