package org.artifactory.security;

import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UserDetailsService;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface ExtendedUserDetailsService extends UserDetailsService {
    @SuppressWarnings({"UnnecessaryLocalVariable", "unchecked"})
    List<UserDetails> getAllUsers();

    boolean createUser(UserDetails details);

    void updateUser(UserDetails details);

    void deleteUser(String username);
}
