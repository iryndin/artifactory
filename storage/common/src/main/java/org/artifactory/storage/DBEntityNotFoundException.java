package org.artifactory.storage;

/**
 * Exception for DB entities which are not found
 * @author Shay Bagants
 */
public class DBEntityNotFoundException extends StorageException {
    public DBEntityNotFoundException(String message) {
        super(message);
    }

    public DBEntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public DBEntityNotFoundException(Throwable cause) {
        super(cause);
    }
}
