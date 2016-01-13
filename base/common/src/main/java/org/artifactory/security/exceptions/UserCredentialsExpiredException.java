package org.artifactory.security.exceptions;

import org.springframework.security.authentication.CredentialsExpiredException;

/**
 * Thrown when user credentials have expired
 *
 * @author Michael Pasternak
 */
public class UserCredentialsExpiredException extends CredentialsExpiredException {
    public UserCredentialsExpiredException(String msg) {
        super(msg);
    }

    public UserCredentialsExpiredException(String msg, Throwable t) {
        super(msg, t);
    }

    /**
     * Produces UserCredentialsExpiredException
     *
     * @param userName
     * @return {@link UserCredentialsExpiredException}
     */
    public static UserCredentialsExpiredException instance(String userName) {
        return new UserCredentialsExpiredException(
                "Your credentials have expired, You must change your password before trying to login again"
        );
    }
}
