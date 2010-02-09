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

import java.util.List;

/**
 * Provides addon factory by type.
 *
 * @author Yossi Shaul
 */
public interface AddonsManager {

    <T extends AddonFactory> T addonByType(Class<T> type);

    List<String> getInstalledAddonNames();

    List<String> getEnabledAddonNames();

    AddonInfo getAddonInfoByName(String addonName);

    boolean isServerIdValid();

    boolean isServerIdValid(String serverId);

    /**
     * Returns the request property of the given addon name
     *
     * @param addonName Name of addon to query
     * @param addonKey  Key of requested property
     * @return Property value if addon name and property key were found. Null if not
     */
    String getAddonProperty(String addonName, String addonKey);

    /**
     * To be called upon the updating of the server ID so we can notify admins on the addon status
     *
     * @param currentServerId Currently configured server ID
     * @param updatedServerId Newly configured server ID
     */
    void onServerIdUpdated(String currentServerId, String updatedServerId);

    /**
     * Indicates if the server ID has changed since last server restart
     *
     * @return True if server ID was modified and the new value is valid. False if no modifications ocurred, or if the
     *         New value is not a valid ID
     */
    boolean isServerIdChanged();

    /**
     * Indicates whether the given addon is activated
     *
     * @param addonName Name of addon to inquire for
     * @return True if the given addon is activated
     */
    <T extends AddonFactory> boolean isAddonActivated(String addonName);
}
