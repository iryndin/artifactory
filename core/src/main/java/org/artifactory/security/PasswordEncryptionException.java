package org.artifactory.security;

import org.springframework.security.AuthenticationException;

/**
 * Thrown when Artifactory configured to support encrypted passwords and the
 * authentication manager failed to decrypt.
 *
 * @author Yossi Shaul
 */
public class PasswordEncryptionException extends AuthenticationException {
    public PasswordEncryptionException(String message) {
        super(message);
    }

    public PasswordEncryptionException(String message, Exception cause) {
        super(message, cause);
    }
}
