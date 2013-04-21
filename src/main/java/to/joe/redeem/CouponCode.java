package to.joe.redeem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

/**
 * This represents a coupon code, a unique 15 character alphanumeric string.
 * 
 * @author Eviltechie
 */
public class CouponCode {

    private int id;
    private String code;
    private int remaining;

    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static Random rnd = new Random();

    static String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }

    /**
     * Creates a new custom coupon code and immediately inserts it into the database
     * 
     * @param code
     *            The custom coupon code to create. Must be 15 alphanumeric characters. TODO Check case sensitivity
     * @param remaining
     *            The amount of this coupon that may be redeemed
     * @throws SQLException
     *             Thrown if the code already exists or on another SQL error.
     */
    public CouponCode(String code, int remaining) throws SQLException {
        this.code = code;
        this.remaining = remaining;

        PreparedStatement ps = RedeemMe.getInstance().getMySQL().getFreshPreparedStatementWithGeneratedKeys("INSERT INTO couponcodes (code, remaining) VALUES (?,?)");
        ps.setString(1, this.code);
        ps.setInt(2, this.remaining);
        ps.execute();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            this.id = rs.getInt(1);
        } else {
            throw new SQLException("Error creating new coupon code!");
        }
    }

    /**
     * Creates a new coupon code and immediately inserts it into the database
     * 
     * @param remaining
     *            The amount of this coupon that may be redeemed
     * @throws SQLException
     *             Thrown if the code already exists or on another SQL error.
     */
    public CouponCode(int remaining) throws SQLException {
        this(randomString(15), remaining);
    }

    int getID() {
        return this.id;
    }

    /**
     * Gets the coupon code that this object represents.
     * 
     * @return The coupon code that this object represents.
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Gets how many times this coupon may be redeemed.
     * 
     * @return How many times this coupon may be redeemed.
     */
    public int getRemaining() {
        return this.remaining;
    }
}