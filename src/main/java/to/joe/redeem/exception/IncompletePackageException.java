package to.joe.redeem.exception;

public class IncompletePackageException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public IncompletePackageException(String message) {
        super(message);
    }
}