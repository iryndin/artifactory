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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.artifactory.descriptor.security.ldap.AuthenticationPattern;
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
import java.util.ArrayList;
import java.util.List;

/**
 * @author freds
 * @date Sep 12, 2008
 */
public class ArtifactoryBindAuthenticator extends BindAuthenticator {
    private static final Logger LOGGER =
            LogManager.getLogger(ArtifactoryBindAuthenticator.class);

    private ContextSource contextSource;
    private List<AuthenticationPatternWrapper> authenticationPatterns;

    public ArtifactoryBindAuthenticator(SpringSecurityContextSource contextSource,
            List<AuthenticationPattern> patterns) {
        super(contextSource);
        init(contextSource, patterns);
    }

    public void init(SpringSecurityContextSource contextSource,
            List<AuthenticationPattern> patterns) {
        Assert.notNull(contextSource, "contextSource must not be null.");
        this.contextSource = contextSource;
        Assert.notEmpty(patterns, "Authentication patterns in LDAP cannot be empty");
        authenticationPatterns = new ArrayList<AuthenticationPatternWrapper>(patterns.size());
        for (AuthenticationPattern pattern : patterns) {
            authenticationPatterns.add(new AuthenticationPatternWrapper(pattern, contextSource));
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

    private class AuthenticationPatternWrapper {
        final MessageFormat userDnPattern;
        final FilterBasedLdapUserSearch userSearch;

        private AuthenticationPatternWrapper(AuthenticationPattern authPattern,
                SpringSecurityContextSource contextSource) {
            boolean hasDnPattern = StringUtils.hasText(authPattern.getUserDnPattern());
            boolean hasSearch = StringUtils.hasText(authPattern.getSearchFilter());
            Assert.isTrue(hasDnPattern || hasSearch,
                    "An Authentication pattern should provide a userDnPattern or a searchFilter (or both)");
            if (hasDnPattern) {
                this.userDnPattern = new MessageFormat(authPattern.getUserDnPattern());
            } else {
                this.userDnPattern = null;
            }
            if (hasSearch) {
                String searchBase = authPattern.getSearchBase();
                if (searchBase == null) {
                    searchBase = "";
                }
                this.userSearch = new FilterBasedLdapUserSearch(searchBase,
                        authPattern.getSearchFilter(), contextSource);
                this.userSearch.setSearchSubtree(authPattern.isSearchSubTree());
            } else {
                this.userSearch = null;
            }
        }

        public DirContextOperations authenticate(Authentication authentication) {
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
                        LOGGER.debug("Searching for user " + username + " failed for " + userSearch,
                                e);
                    }
                }
            }

            return user;
        }
    }

    @Override
    public DirContextOperations authenticate(Authentication authentication) {
        Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication,
                "Can only process UsernamePasswordAuthenticationToken objects");

        for (AuthenticationPatternWrapper pattern : authenticationPatterns) {
            DirContextOperations user = pattern.authenticate(authentication);
            if (user != null) {
                return user;
            }
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
