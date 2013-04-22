package to.joe.redeem.exception;

public class InvalidCouponCodeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidCouponCodeException(String message) {
        super(message);
    }
}