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

package org.artifactory.addon.wicket;

import org.apache.wicket.Page;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.artifactory.addon.AddonFactory;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.md.Properties;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.common.wicket.model.sitemap.MenuNode;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.descriptor.repo.RealRepoDescriptor;

import java.util.List;
import java.util.Map;

/**
 * Addon for property creation, addition and searchability
 *
 * @author Noam Tenne
 */
public interface PropertiesAddon extends AddonFactory {

    /**
     * Returns the properties tab panel within a tab
     *
     * @param itemInfo selected item info
     * @return Tab containing the properties search panel
     */
    ITab getPropertiesTabPanel(final ItemInfo itemInfo);

    /**
     * Returns the properties tab panel for a search folder actionable item within a tab
     *
     * @param folderInfo selected item info
     * @param decendents Decendent files of this folder within the search result
     * @return Tab containing the properties search panel
     */
    ITab getSearchPropertiesTabPanel(final FolderInfo folderInfo, List<FileInfo> decendents);

    /**
     * Returns the property search page within a menu node
     *
     * @param nodeTitle Title to give to the node
     * @return Menu node containing the property search page
     */
    MenuNode getPropertySearchMenuNode(String nodeTitle);

    /**
     * Returns the property search panel within a tab
     *
     * @param parent   Parent page of the property search panel
     * @param tabTitle Title to give to the panel tab
     * @return Tab containing the property search panel
     */
    ITab getPropertySearchTabPanel(Page parent, String tabTitle);

    /**
     * Returns the property sets configuration page within a menu node
     *
     * @param nodeTitle Title to give to the node
     * @return Menu node containing the property sets page
     */
    MenuNode getPropertySetsPage(String nodeTitle);

    /**
     * Returns the property sets selection for the repo configuration
     *
     * @param borderId     Border wicket ID
     * @param dragDropId   Drag n' drop wicket ID
     * @param entity       Repo descriptor
     * @param propertySets Availabe property sets
     * @return Property sets selector, if addon enabled. Blank WebMarkupContainer if not
     */
    WebMarkupContainer getPropertySetsBorder(String borderId, String dragDropId, RealRepoDescriptor entity,
            List<PropertySet> propertySets);

    /**
     * Returns map of properties for the given repo paths
     *
     * @param repoPaths     Paths to extract properties for
     * @param mandatoryKeys Any property keys that should be mandatory for resulting properties. If provided, property
     *                      objects will be added to the map only if they contain all the given keys
     * @return Map of repo paths with their corresponding properties
     */
    Map<RepoPath, Properties> getProperties(List<RepoPath> repoPaths, String... mandatoryKeys);
}