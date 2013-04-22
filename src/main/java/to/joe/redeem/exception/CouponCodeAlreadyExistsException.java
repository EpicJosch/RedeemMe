package to.joe.redeem.exception;

public class CouponCodeAlreadyExistsException extends Exception {
    private static final long serialVersionUID = 1L;

    public CouponCodeAlreadyExistsException(String message) {
        super(message);
    }
}