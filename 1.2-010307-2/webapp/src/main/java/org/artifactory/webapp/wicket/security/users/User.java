package org.artifactory.webapp.wicket.security.users;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.userdetails.UserDetails;
import org.apache.log4j.Logger;
import org.artifactory.security.SecurityHelper;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class User implements Serializable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(User.class);

    private String username;
    private String password;
    private String retypedPassword;
    private boolean admin;
    private static final GrantedAuthority[] USER_GAS =
            new GrantedAuthority[]{new GrantedAuthorityImpl("USER")};
    private static final GrantedAuthority[] ADMIN_GAS =
            new GrantedAuthority[]{new GrantedAuthorityImpl("ADMIN"),
                    new GrantedAuthorityImpl("USER")};

    public User() {
    }

    public User(UserDetails details) {
        this.username = details.getUsername();
        this.password = details.getPassword();
        admin = SecurityHelper.isAdmin(details);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRetypedPassword() {
        return retypedPassword;
    }

    public void setRetypedPassword(String retypedPassword) {
        this.retypedPassword = retypedPassword;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public UserDetails getUserDetails() {
        return new org.acegisecurity.userdetails.User(
                username, password, true, true, true, true,
                (admin ? ADMIN_GAS : USER_GAS));
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return username.equals(user.username);
    }

    public int hashCode() {
        return username.hashCode();
    }

    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", retypedPassword='" + retypedPassword + '\'' +
                ", admin=" + admin +
                '}';
    }
}
