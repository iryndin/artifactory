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

package org.artifactory.addon.wicket;

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.addon.AddonFactory;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.repo.Lock;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.build.api.Artifact;
import org.artifactory.build.api.Build;
import org.artifactory.build.api.Dependency;
import org.artifactory.build.api.Module;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.wicket.page.build.actionable.ModuleArtifactActionableItem;
import org.artifactory.webapp.wicket.page.build.actionable.ModuleDependencyActionableItem;

import java.util.List;
import java.util.Set;

/**
 * Addon for continous intergration info handeling
 *
 * @author Noam Y. Tenne
 */
public interface BuildAddon extends AddonFactory {

    /**
     * Returns the builds tab panel
     *
     * @param item Selected repo item
     * @return Builds tab panel
     */
    ITab getBuildsTab(RepoAwareActionableItem item);

    /**
     * Returns the module info tab panel
     *
     * @param buildName   The name of the selected build
     * @param buildNumber The number of the selected build
     * @param module      Selected module
     * @return Module info tab panel
     */
    ITab getModuleInfoTab(String buildName, long buildNumber, Module module);

    /**
     * Returns a customized delete confirmation message that adds alerts in case any selected items are used by builds
     *
     * @param item           Item to be deleted
     * @param defaultMessage Default message to display in any case
     * @return Delete confirmation message
     */
    @Lock(transactional = true)
    String getDeleteItemWarningMessage(ItemInfo item, String defaultMessage);

    /**
     * Returns a customized delete confirmation message that adds alerts in case any selected items are used by builds
     *
     * @param versionPaths   Selected version repo paths
     * @param defaultMessage The message to display in any case
     * @return Delete confirmation message
     */
    @Lock(transactional = true)
    String getDeleteVersionsWarningMessage(List<RepoPath> versionPaths, String defaultMessage);

    /**
     * Returns a list of build-artifact file info objects
     *
     * @param build Build to extract artifacts from
     * @return Artifact file info list
     */
    Set<FileInfo> getArtifactFileInfo(Build build);

    /**
     * Returns a list of build-dependency file info objects
     *
     * @param build Build to extract artifacts from
     * @return Dependency file info list
     */
    Set<FileInfo> getDependencyFileInfo(Build build);

    /**
     * Returns a list of build-artifact actionable items
     *
     * @param buildName   The name of the searched build
     * @param buildNumber The number of the searched build
     * @param artifacts   Artifacts to create actionable items from
     * @return Artifact actionable item list
     */
    List<ModuleArtifactActionableItem> getModuleArtifactActionableItems(String buildName, long buildNumber,
            List<Artifact> artifacts);

    /**
     * Returns a list of build-dependency actionable items
     *
     * @param buildName    The name of the searched build
     * @param buildNumber  The number of the searched build
     * @param dependencies Dependencies to create actionable items from
     * @return Dependency actionable item list
     */
    List<ModuleDependencyActionableItem> getModuleDependencyActionableItem(String buildName, long buildNumber,
            List<Dependency> dependencies);

    /**
     * Returns the build save search results panel
     *
     * @param requestingAddon The addon that requests the panel
     * @param build           Build to use for results
     * @return Build save search results panel
     */
    Panel getBuildSearchResultsPanel(Addon requestingAddon, Build build);
}