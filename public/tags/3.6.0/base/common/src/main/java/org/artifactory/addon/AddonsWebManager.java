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

/**
 * An interface that holds all the web related operations that are available only as part of Artifactory's Add-ons.
 *
 * @author Shay Yaakov
 */
public interface AddonsWebManager {

    String getFooterMessage(boolean admin);

    String getLicenseFooterMessage();

    String getLicenseRequiredMessage(String licensePageUrl);

    void onNoInstalledLicense(boolean userVisitedLicensePage, NoInstalledLicenseAction action);

    boolean isAdminPageAccessible();
}
