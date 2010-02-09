package org.artifactory.security;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.userdetails.User;
import org.acegisecurity.userdetails.UserDetailsService;
import org.acegisecurity.userdetails.memory.InMemoryDaoImpl;
import org.acegisecurity.userdetails.memory.UserMap;
import org.apache.log4j.Logger;
import org.artifactory.Startable;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SecurityHelper implements Startable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SecurityHelper.class);

    private String adminCredenial;
    private UserDetailsService userDetailsService;


    public SecurityHelper(String adminCredenial) {
        this.adminCredenial = adminCredenial;
    }

    public UserDetailsService getUserDetailsService() {
        return userDetailsService;
    }

    public void start() {
        userDetailsService = new InMemoryDaoImpl();
        UserMap userMap = new UserMap();
        GrantedAuthorityImpl guestAuthority = new GrantedAuthorityImpl("guest");
        GrantedAuthority[] authorities = new GrantedAuthority[]{
                new GrantedAuthorityImpl("admin"),
                guestAuthority};
        User admin = new User("admin", adminCredenial, true, true, true, true, authorities);
        userMap.addUser(admin);
        User guest = new User("guest", "", true, true, true, true,
                new GrantedAuthority[]{guestAuthority});
        userMap.addUser(guest);
        try {
            InMemoryDaoImpl svc = (InMemoryDaoImpl) userDetailsService;
            svc.setUserMap(userMap);
            svc.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up HTTP authentication filtering.", e);
        }
    }

    public void stop() {
    }
}
