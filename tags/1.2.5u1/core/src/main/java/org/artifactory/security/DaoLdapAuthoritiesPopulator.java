package org.artifactory.security;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.ldap.LdapDataAccessException;
import org.acegisecurity.providers.ldap.LdapAuthoritiesPopulator;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UserDetailsService;
import org.acegisecurity.userdetails.ldap.LdapUserDetails;

public class DaoLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {
    private UserDetailsService userDetailsService;


    public UserDetailsService getUserDetailsService() {
        return userDetailsService;
    }

    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    public GrantedAuthority[] getGrantedAuthorities(LdapUserDetails userDetails) throws LdapDataAccessException {
        UserDetails daoUserDetails = userDetailsService.loadUserByUsername(userDetails.getUsername());
        return daoUserDetails.getAuthorities();
    }
}
