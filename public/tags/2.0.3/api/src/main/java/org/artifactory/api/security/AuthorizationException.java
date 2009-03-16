package org.artifactory.api.security;

/**
 * Throw this exception when user tries to perform action she is not authorize to.
 *
 * @author Yossi Shaul
 */
public class AuthorizationException extends RuntimeException {
    public AuthorizationException(String message) {
        super(message);
    }
}
