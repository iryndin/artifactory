/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
