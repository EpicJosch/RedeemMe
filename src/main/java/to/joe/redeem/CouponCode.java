package to.joe.redeem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import to.joe.redeem.exception.CouponCodeAlreadyExistsException;
import to.joe.redeem.exception.InvalidCouponCodeException;

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
     *            The custom coupon code to create. Must be 15 alphanumeric characters. Will be converted to an uppercase string. An {@link InvalidCouponCodeException} will be thrown if an invalid code is passed.
     * @param remaining
     *            The amount of this coupon that may be redeemed
     * @throws SQLException
     *             Thrown on SQL error
     * @throws CouponCodeAlreadyExistsException
     *             Thrown when the coupon code already exists
     */
    public CouponCode(String code, int remaining) throws SQLException, CouponCodeAlreadyExistsException {
        this.code = code.toUpperCase();
        this.remaining = remaining;

        if (!this.code.matches("[A-Za-z0-9]{15}")) {
            throw new InvalidCouponCodeException("The coupon code " + this.code + " is not valid");
        }

        try {
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
        } catch (SQLException e) {
            if (e.getMessage().matches("Duplicate entry .*? for key 'code'")) {
                throw new CouponCodeAlreadyExistsException("The code " + code + " already exists in the database");
            } else {
                throw e;
            }
        }
    }

    /**
     * Creates a new coupon code and immediately inserts it into the database
     * 
     * @param remaining
     *            The amount of this coupon that may be redeemed
     * @throws SQLException
     *             Thrown on SQL error
     * @throws CouponCodeAlreadyExistsException
     *             Thrown when the coupon code already exists
     */
    public CouponCode(int remaining) throws SQLException, CouponCodeAlreadyExistsException {
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