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

package org.artifactory.addon.license;

import org.artifactory.addon.Addon;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.jfrog.build.api.Build;

import java.util.List;
import java.util.Set;

/**
 * @author Tomer Cohen
 */
public interface LicensesAddon extends Addon {

    /**
     * Perform license calculation on build artifacts. Depending on the {@link Build#buildAgent} this method will
     * extract the license from the descriptor and attach it as a property on the dependencies of the modules.
     *
     * @param build The build to perform the license calculation on.
     */
    void performOnBuildArtifacts(Build build);

    /**
     * Add the special property set for the license on the local repository (if it does not exist).
     *
     * @param descriptor The descriptor that represents the repository for which to add the property set to.
     */
    void addPropertySetToRepository(RealRepoDescriptor descriptor);

    /**
     * Import licenses as part of the full system import.
     */
    void importLicenses(ImportSettings settings);

    /**
     * Export licenses as part of the full system export.
     */
    void exportLicenses(ExportSettings exportSettings);

    List findLicensesInRepos(Set<String> repoKeys, LicenseStatus status);

    void reloadLicensesCache();
}
