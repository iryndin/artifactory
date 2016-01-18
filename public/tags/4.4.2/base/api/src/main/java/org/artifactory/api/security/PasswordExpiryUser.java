package org.artifactory.api.security;

/**
 * A lean object that holds only necessary info to avoid thousand of user objects on expiry and
 * notification jobs.
 *
 * @author Dan Feldman
 */
public class PasswordExpiryUser {

    private String userName;
    private String email;
    private long passwordCreated;
    private long userId;

    public PasswordExpiryUser(long userId, String userName, String email, long passwordCreated) {
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.passwordCreated = passwordCreated;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmail() {
        return email;
    }

    public long getPasswordCreated() {
        return passwordCreated;
    }

    public long getUserId() {
        return userId;
    }
}
