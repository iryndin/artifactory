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

package org.artifactory.addon;

/**
 * Core services addons interface.
 *
 * @author Yossi Shaul
 */
public interface CoreAddons extends Addon {
    /**
     * @return True if creation of new admin accounts is allowed.
     */
    boolean isCreateDefaultAdminAccountAllowed();

    /**
     * Check that an a certain user is the AOL administrator, by checking that via the AOL dashboard.
     *
     * @param username The username to check if its deletion is allowed.
     * @param address  The remote address that is performing this check.
     * @return True if the user is a pure Artifactory user, that is NOT allowed access to the AOL dashboard
     */
    boolean isAolDashboardAdmin(String username, String address);
}