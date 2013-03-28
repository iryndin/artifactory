/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.sapi.common.ImportSettings;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Provides addon factory by type.
 *
 * @author Yossi Shaul
 */
public interface AddonsManager {

    void refresh();

    <T extends Addon> T addonByType(Class<T> type);

    List<AddonInfo> getInstalledAddons(@Nullable Set<String> excludedAddonKeys);

    List<String> getEnabledAddonNames();

    boolean isLicenseInstalled();

    boolean isLicenseKeyValid(String licenseKey);

    /**
     * Returns the request property of the given addon name
     *
     * @param addonName Name of addon to query
     * @param addonKey  Key of requested property
     * @return Property value if addon name and property key were found. Null if not
     */
    String getAddonProperty(String addonName, String addonKey);

    /**
     * Indicates whether the given addon is activated
     *
     * @param addonName Name of addon to inquire for
     * @return True if the given addon is activated
     */
    <T extends Addon> boolean isAddonActivated(String addonName);

    /**
     * Installs a new Artifactory license key
     *
     * @param licenseKey The license key
     * @throws IOException If the license is invalid or failed to save the license file
     */
    void installLicense(String licenseKey) throws IOException;

    /**
     * @return The currently installed license key.
     */
    String getLicenseKey();

    /**
     * Returns the hash of the license key (if installed) with an added char for indication of type
     * (<b>t</b>rial \ <b>c</b>ommercial).<br/>
     * <b>NOTE:</b> The returned hash will not be a valid one (inclusion of indication char).
     *
     * @return license hash + type indication
     */
    String getLicenseKeyHash();

    boolean lockdown();

    boolean isInstantiationAuthorized(Class componentClass);

    /**
     * Sends a "forbidden" response to the request if no valid license is installed
     *
     * @param response Response to intercept
     */
    void interceptResponse(ArtifactoryResponse response) throws IOException;

    String[] getLicenseDetails();

    String getProductName();

    void importFrom(ImportSettings settings);
}
