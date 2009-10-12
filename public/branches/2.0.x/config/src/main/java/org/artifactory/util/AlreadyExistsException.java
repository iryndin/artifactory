package org.artifactory.util;

/**
 * Thrown when calling a create/add method and the target object already exists.
 *
 * @author Yossi Shaul
 */
public class AlreadyExistsException extends RuntimeException {
    public AlreadyExistsException(String message) {
        super(message);
    }

    public AlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
