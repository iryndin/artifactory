package org.artifactory.security.exceptions;

/**
 * Thrown when changing password fails
 *
 * Created by Michael Pasternak on 1/5/16.
 */
public class PasswordChangeException extends RuntimeException {
    public PasswordChangeException(String message) {
        super(message);
    }

    public PasswordChangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
