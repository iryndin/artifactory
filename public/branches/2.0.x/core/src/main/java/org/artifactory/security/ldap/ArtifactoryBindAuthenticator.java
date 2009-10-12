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


import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.ldap.SearchPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.security.Authentication;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.ldap.SpringSecurityContextSource;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.providers.ldap.authenticator.BindAuthenticator;
import org.springframework.security.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.naming.directory.DirContext;
import java.text.MessageFormat;

/**
 * @author freds
 * @date Sep 12, 2008
 */
public class ArtifactoryBindAuthenticator extends BindAuthenticator {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryBindAuthenticator.class);

    private ContextSource contextSource;

    private MessageFormat userDnPattern;
    private FilterBasedLdapUserSearch userSearch;

    public ArtifactoryBindAuthenticator(SpringSecurityContextSource contextSource,
            LdapSetting ldapSetting) {
        super(contextSource);
        init(contextSource, ldapSetting);
    }

    public void init(SpringSecurityContextSource contextSource, LdapSetting ldapSetting) {
        Assert.notNull(contextSource, "contextSource must not be null.");
        this.contextSource = contextSource;
        boolean hasDnPattern = StringUtils.hasText(ldapSetting.getUserDnPattern());
        SearchPattern search = ldapSetting.getSearch();
        boolean hasSearch = search != null;
        Assert.isTrue(hasDnPattern || hasSearch,
                "An Authentication pattern should provide a userDnPattern or a searchFilter (or both)");

        if (hasDnPattern) {
            this.userDnPattern = new MessageFormat(ldapSetting.getUserDnPattern());
        }

        if (hasSearch) {
            String searchBase = search.getSearchBase();
            if (searchBase == null) {
                searchBase = "";
            }
            this.userSearch = new FilterBasedLdapUserSearch(searchBase,
                    search.getSearchFilter(), contextSource);
            this.userSearch.setSearchSubtree(search.isSearchSubTree());
        }
    }

    @Override
    protected ContextSource getContextSource() {
        return contextSource;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Nothing to do, check done at constructor time
    }

    @Override
    public DirContextOperations authenticate(Authentication authentication) {
        Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication,
                "Can only process UsernamePasswordAuthenticationToken objects");

        DirContextOperations user = null;

        String username = authentication.getName();
        String password = (String) authentication.getCredentials();

        if (userDnPattern != null) {
            // If DN patterns are configured, try authenticating with them directly
            user = bindWithDn(userDnPattern.format(new Object[]{username}), username, password);
        }

        if (user == null) {
            if (userSearch != null) {
                try {
                    // Otherwise use the configured locator to find the user
                    // and authenticate with the returned DN.
                    DirContextOperations userFromSearch = userSearch.searchForUser(username);
                    user = bindWithDn(userFromSearch.getDn().toString(), username, password);
                } catch (UsernameNotFoundException e) {
                    log.debug("Searching for user {} failed for {}: {}",
                            new Object[]{userSearch, username, e.getMessage()});
                }
            }
        }

        if (user != null) {
            return user;
        }

        throw new BadCredentialsException(
                messages.getMessage("BindAuthenticator.badCredentials", "Bad credentials"));
    }

    private DirContextOperations bindWithDn(String userDn, String username, String password) {
        SpringSecurityLdapTemplate template = new SpringSecurityLdapTemplate(
                new BindWithSpecificDnContextSource(
                        (SpringSecurityContextSource) getContextSource(), userDn, password));

        try {
            return template.retrieveEntry(userDn, getUserAttributes());

        } catch (BadCredentialsException e) {
            // This will be thrown if an invalid user name is used and the method may
            // be called multiple times to try different names, so we trap the exception
            // unless a subclass wishes to implement more specialized behaviour.
            handleBindException(userDn, username, e.getCause());
        }

        return null;
    }

    private class BindWithSpecificDnContextSource implements ContextSource {
        private SpringSecurityContextSource ctxFactory;
        DistinguishedName userDn;
        private String password;

        public BindWithSpecificDnContextSource(SpringSecurityContextSource ctxFactory,
                String userDn, String password) {
            this.ctxFactory = ctxFactory;
            this.userDn = new DistinguishedName(userDn);
            this.userDn.prepend(ctxFactory.getBaseLdapPath());
            this.password = password;
        }

        public DirContext getReadOnlyContext() throws DataAccessException {
            return ctxFactory.getReadWriteContext(userDn.toString(), password);
        }

        public DirContext getReadWriteContext() throws DataAccessException {
            return getReadOnlyContext();
        }
    }
}
