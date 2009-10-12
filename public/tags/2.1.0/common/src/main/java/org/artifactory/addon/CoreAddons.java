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

package org.artifactory.addon;

import org.artifactory.version.ArtifactoryConfigVersion;

/**
 * Core services addons interface.
 *
 * @author Yossi Shaul
 */
public interface CoreAddons extends AddonFactory {
    /**
     * @return True if creation of new admin accounts is allowed.
     */
    boolean isNewAdminAccountAllowed();

    /**
     * Validates the given server ID
     *
     * @param serverId Server ID to validate
     * @return True if the server ID is valid. False if not
     */
    boolean isServerIdValid(String serverId);

    /**
     * Indicates if a logback configuration convertion should be performed
     *
     * @param versionBeforeConversion Original rtifactory version (before conversion)
     * @return True if logback configuration conversion should be performed, false if not
     */
    boolean isLogbackConversionRequired(ArtifactoryConfigVersion versionBeforeConversion);
}