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
     * Creates a new coupon code and immediately inserts it into the database
     * 
     * @param remaining
     *            The amount of this coupon that may be redeemed
     * @throws SQLException
     */
    public CouponCode(int remaining) throws SQLException {
        this.code = randomString(15);
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

    int getID() {
        return id;
    }
}