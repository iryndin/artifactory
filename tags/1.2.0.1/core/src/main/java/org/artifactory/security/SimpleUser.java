package org.artifactory.security;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.userdetails.User;
import org.acegisecurity.userdetails.UserDetails;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SimpleUser extends User {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SimpleUser.class);

    public SimpleUser(String username) {
        super(username, "", true, true, true, true, new GrantedAuthority[]{});
    }

    public SimpleUser(UserDetails recipient) {
        super(recipient.getUsername(),
                recipient.getPassword(),
                recipient.isEnabled(),
                recipient.isAccountNonExpired(),
                recipient.isCredentialsNonExpired(),
                recipient.isAccountNonLocked(),
                recipient.getAuthorities());
    }

    public String toString() {
        return getUsername();
    }
}
