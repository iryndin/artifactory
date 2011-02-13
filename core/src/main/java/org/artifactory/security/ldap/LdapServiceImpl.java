/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.security.ldap.LdapService;
import org.artifactory.api.security.ldap.LdapUser;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.stereotype.Service;

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

    public MultiStatusHolder testLdapConnection(LdapSetting ldapSetting, String username, String password) {
        MultiStatusHolder status = new MultiStatusHolder();
        try {
            LdapContextSource securityContext =
                    ArtifactoryLdapAuthenticator.createSecurityContext(ldapSetting);
            ArtifactoryBindAuthenticator authenticator = new ArtifactoryBindAuthenticator(
                    securityContext, ldapSetting);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, password);
            authenticator.authenticate(authentication);
            status.setStatus("Successfully connected and authenticated the test user.", log);
            LdapTemplate ldapTemplate = createLdapTemplate(ldapSetting);
            LdapUser ldapUser = getUserFromLdapSearch(ldapTemplate, username, ldapSetting);
            if (ldapUser == null) {
                status.setWarning(
                        "LDAP user search failed, LDAP queries concerning users and groups may not be available.",
                        log);
            }
        } catch (Exception e) {
            SearchPattern pattern = ldapSetting.getSearch();
            if ((pattern != null && StringUtils.isNotBlank(pattern.getSearchFilter())) &&
                    StringUtils.isNotBlank(ldapSetting.getUserDnPattern())) {
                handleException(e, status, username, true);
            } else {
                handleException(e, status, username, false);
            }
        }
        return status;
    }


    @SuppressWarnings({"unchecked"})
    public LdapUser getDnFromUserName(LdapSetting ldapSetting, String userName) {
        if (ldapSetting == null) {
            log.warn("Cannot find user '{}' in LDAP: No LDAP settings defined.", userName);
            return null;
        }
        if (!ldapSetting.isEnabled()) {
            log.warn("Cannot find user '{}' in LDAP: LDAP settings not enabled.", userName);
            return null;
        }
        if (ldapSetting.getSearch() == null || isBlank(ldapSetting.getSearch().getSearchFilter())) {
            log.warn("Cannot find user '{}' in LDAP: No search filter defined.", userName);
            return null;
        }
        LdapTemplate ldapTemplate = createLdapTemplate(ldapSetting);
        return getUserFromLdapSearch(ldapTemplate, userName, ldapSetting);
    }

    public DirContextOperations searchUserInLdap(LdapTemplate ldapTemplate, String userName, LdapSetting settings) {
        if (settings.getSearch() == null) {
            return null;
        }
        DirContextOperations contextOperations = null;
        FilterBasedLdapUserSearch ldapUserSearch = getFilterBasedLdapUserSearch(ldapTemplate, settings);
        try {
            log.debug("Searching for user {}", userName);
            contextOperations = ldapUserSearch.searchForUser(userName);
            // Only DirContextAdapter can be used since the LDAP connection need to be released and we still need
            // read access to this LDAP context.
            if (!(contextOperations instanceof DirContextAdapter)) {
                throw new ClassCastException(
                        "Cannot use LDAP DirContext class " + contextOperations.getClass().getName() +
                                " it should be " + DirContextAdapter.class.getName());
            }
            log.debug("Found user {}, has DN: {}", userName, contextOperations.getNameInNamespace());
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.debug("Failed to retrieve groups user '{}' via LDAP: {}", userName, e.getMessage());
        } catch (CommunicationException ce) {
            String message =
                    String.format("Failed to retrieve groups for user '%s' via LDAP: communication error.", userName);
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
        return new LdapUser(userName, contextOperations.getNameInNamespace());
    }

    private FilterBasedLdapUserSearch getFilterBasedLdapUserSearch(LdapTemplate ldapTemplate, LdapSetting settings) {
        SearchPattern pattern = settings.getSearch();
        if (pattern == null) {

        }
        if (isBlank(pattern.getSearchBase())) {
            log.debug("LDAP settings have no search base defined, using defaults.");
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
