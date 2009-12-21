/*
 * This file is part of Artifactory.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.security.ldap;

import org.artifactory.security.SecurityServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.ldap.LdapAuthoritiesPopulator;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UserDetailsService;
import org.springframework.security.userdetails.UsernameNotFoundException;

public class DaoLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {
    @Autowired
    private UserDetailsService userDetailsService;

    public GrantedAuthority[] getGrantedAuthorities(DirContextOperations userData,
            String username) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            return userDetails.getAuthorities();
        } catch (UsernameNotFoundException e) {
            return new GrantedAuthority[]{new GrantedAuthorityImpl(SecurityServiceImpl.ROLE_USER)};
        }
    }
}
