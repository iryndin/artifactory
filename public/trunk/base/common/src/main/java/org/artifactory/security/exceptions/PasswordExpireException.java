package org.artifactory.security.exceptions;

/**
 * Thrown when expiring password fails
 *
 * Created by Michael Pasternak on 1/5/16.
 */
public class PasswordExpireException extends RuntimeException {
    public PasswordExpireException(String message) {
        super(message);
    }

    public PasswordExpireException(String message, Throwable cause) {
        super(message, cause);
    }
}
