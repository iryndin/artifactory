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

package org.artifactory.addon;

import org.artifactory.api.security.UserInfo;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.springframework.ldap.core.DirContextOperations;

import java.util.List;

/**
 * Used to populate the user with the groups that he belongs to.
 *
 * @author Tomer Cohen
 */
public interface LdapGroupAddon extends Addon {

    /**
     * Populate the group for a certain user by his Distinguished Name (dn)
     *
     * @param dirContextOperations The user context in LDAP
     * @param userInfo             User information about the current user.
     */
    void populateGroups(DirContextOperations dirContextOperations, UserInfo userInfo);

    /**
     * Populate the group for a certain user by his Distinguished Name (dn).
     *
     * @param userDn   The user's distinguished name.
     * @param userInfo User information about the user for which to populate the group for.
     */
    void populateGroups(String userDn, UserInfo userInfo);

    /**
     * Get the enabled {@link LdapSetting} in the system, in the powerpack version of Artifactory this will return more
     * than one enabled LDAP setting configuration.
     *
     * @return The enabled LDAP setting(s)
     */
    List<LdapSetting> getEnabledLdapSettings();
}
