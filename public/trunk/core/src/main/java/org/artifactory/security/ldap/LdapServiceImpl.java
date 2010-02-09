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

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.security.LdapService;
import org.artifactory.api.security.LdapUser;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.ldap.SearchPattern;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.AbstractFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.stereotype.Service;

import java.util.List;

import static javax.naming.directory.SearchControls.SUBTREE_SCOPE;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * This class tests an ldap connection given a ldap settings.
 *
 * @author Yossi Shaul
 * @author Tomer Cohen
 */
@Service
public class LdapServiceImpl extends AbstractLdapService implements LdapService {
    private static final Logger log = LoggerFactory.getLogger(LdapServiceImpl.class);

    public StatusHolder testLdapConnection(LdapSetting ldapSetting, String username, String password) {
        StatusHolder status = new StatusHolder();
        try {
            LdapContextSource securityContext =
                    ArtifactoryLdapAuthenticator.createSecurityContext(ldapSetting);
            ArtifactoryBindAuthenticator authenticator = new ArtifactoryBindAuthenticator(
                    securityContext, ldapSetting);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, password);
            authenticator.authenticate(authentication);
            status.setStatus("Successfully connected and authenticated the test user", log);
        } catch (Exception e) {
            handleException(e, status, username);
        }
        return status;
    }


    @SuppressWarnings({"unchecked"})
    public LdapUser getDnFromUserName(LdapSetting ldapSetting, String userName) {
        if (ldapSetting == null) {
            return null;
        }
        SearchPattern searchPattern = ldapSetting.getSearch();
        String searchBase = searchPattern.getSearchBase();
        if (isBlank(searchBase)) {
            searchBase = EMPTY;
        }
        AbstractFilter filter;
        LdapTemplate ldapTemplate = createLdapTemplate(ldapSetting);
        if (!isBlank(searchPattern.getSearchFilter())) {
            return getUserFromLdapSearch(ldapTemplate, userName, ldapSetting);
        } else {
            filter = new EqualsFilter("uid", userName);
            List<LdapUser> user = ldapTemplate.search(
                    searchBase, filter.encode(), SUBTREE_SCOPE, new UserContextMapper());
            if (!user.isEmpty()) {
                return user.get(0);
            }
        }
        return null;
    }

    public DirContextOperations searchUserInLdap(LdapTemplate ldapTemplate, String userName, LdapSetting settings) {
        FilterBasedLdapUserSearch ldapUserSearch = getFilterBasedLdapUserSearch(ldapTemplate, settings);
        DirContextOperations contextOperations = null;
        try {
            log.debug("Searching for user {}", userName);
            contextOperations = ldapUserSearch.searchForUser(userName);
            // Only DirContextAdapter can be used since the LDAP connection need to be released and we still need
            // read access to this LDAP context.
            if (contextOperations != null && !(contextOperations instanceof DirContextAdapter)) {
                throw new ClassCastException(
                        "Cannot use LDAP DirContext class " + contextOperations.getClass().getName() +
                                " it should be " + DirContextAdapter.class.getName());
            }
            log.debug("Found user {}, has DN: {}", userName, contextOperations.getNameInNamespace());
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.debug("Failed to retrieve groups user '{}' via LDAP: {}", userName, e.getMessage());
        } catch (CommunicationException ce) {
            String message =
                    String.format("Failed to retrieve groups for user '%s' via LDAP: communication error", userName);
            log.warn(message);
        } catch (Exception e) {
            String message = "Unexpected exception in LDAP query:";
            log.debug(message, e);
            log.warn(message + "for user {} vid LDAP: {}", userName, e.getMessage());
        }
        return contextOperations;
    }


    private LdapUser getUserFromLdapSearch(LdapTemplate ldapTemplate, String userName, LdapSetting settings) {
        DirContextOperations contextOperations = searchUserInLdap(ldapTemplate, userName, settings);
        if (contextOperations == null) {
            return null;
        }
        LdapUser user = new LdapUser(userName, contextOperations.getNameInNamespace());
        return user;
    }

    private FilterBasedLdapUserSearch getFilterBasedLdapUserSearch(LdapTemplate ldapTemplate, LdapSetting settings) {
        SearchPattern pattern = settings.getSearch();
        if (isBlank(pattern.getSearchBase())) {
            pattern.setSearchBase(EMPTY);
        }
        FilterBasedLdapUserSearch ldapUserSearch =
                new FilterBasedLdapUserSearch(settings.getSearch().getSearchBase(),
                        pattern.getSearchFilter(),
                        (BaseLdapPathContextSource) ldapTemplate.getContextSource());
        ldapUserSearch.setSearchSubtree(settings.getSearch().isSearchSubTree());
        return ldapUserSearch;
    }

}
