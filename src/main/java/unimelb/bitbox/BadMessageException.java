package unimelb.bitbox;

/**
 * Thrown when a data field in a received message is missing or malformed.
 * This is usually the peer's fault.
 *
 * @author TransfictionRailways
 */
public class BadMessageException extends Exception {
    public BadMessageException(String message) {
        super(message);
    }
}
