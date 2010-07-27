/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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


import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.ldap.SearchPattern;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
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

    private LdapContextSource contextSource;

    private MessageFormat userDnPattern;
    private FilterBasedLdapUserSearch userSearch;

    public ArtifactoryBindAuthenticator(LdapContextSource contextSource, LdapSetting ldapSetting) {
        super(contextSource);
        init(contextSource, ldapSetting);
    }

    public void init(LdapContextSource contextSource, LdapSetting ldapSetting) {
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
            this.userSearch = new FilterBasedLdapUserSearch(searchBase, search.getSearchFilter(), contextSource);
            this.userSearch.setSearchSubtree(search.isSearchSubTree());
        }
    }

    @Override
    protected LdapContextSource getContextSource() {
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
        // may have LDAPs that have no password for the users, see RTFACT-3103, RTFACT-3378
        if (!StringUtils.hasText(password)) {
            throw new BadCredentialsException("Empty password used.");
        }

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
                catch (IncorrectResultSizeDataAccessException irsae) {
                    log.error("User: {} found {} times in LDAP server", username, irsae.getActualSize());
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
                new BindWithSpecificDnContextSource(getContextSource(), userDn, password));

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

    private static class BindWithSpecificDnContextSource implements ContextSource {
        private LdapContextSource ctxFactory;
        private DistinguishedName userDn;
        private String password;

        public BindWithSpecificDnContextSource(LdapContextSource ctxFactory, String userDn, String password) {
            this.ctxFactory = ctxFactory;
            this.userDn = new DistinguishedName(userDn);
            this.userDn.prepend(ctxFactory.getBaseLdapPath());
            this.password = password;
        }

        public DirContext getReadOnlyContext() throws DataAccessException {
            return ctxFactory.getContext(userDn.toString(), password);
        }

        public DirContext getReadWriteContext() throws DataAccessException {
            return getReadOnlyContext();
        }

        public DirContext getContext(String s, String s1) throws NamingException {
            return ctxFactory.getContext(s, s1);
        }
    }
}
