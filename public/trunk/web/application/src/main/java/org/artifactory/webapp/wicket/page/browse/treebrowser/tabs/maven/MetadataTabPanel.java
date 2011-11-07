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

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.maven;

import com.google.common.collect.Lists;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.PropertiesWebAddon;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.wicket.behavior.collapsible.CollapsibleBehavior;
import org.artifactory.fs.ItemInfo;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.model.FolderActionableItem;
import org.slf4j.Logger;

import java.util.List;

/**
 * General Metadata display tab panel. Holds both traditional metadata and properties info
 *
 * @author Noam Tenne
 */
public class MetadataTabPanel extends Panel {

    private static final Logger log = LoggerFactory.getLogger(MetadataTabPanel.class);

    @SpringBean
    private AddonsManager addonsManager;

    @SpringBean
    private RepositoryService repoService;

    public MetadataTabPanel(String id, RepoAwareActionableItem item, RepoPath canonicalRepoPath) {
        super(id);
        //Add properties panel
        ItemInfo info;
        if (item instanceof FolderActionableItem) {
            // take the last element if folder compacted compacted
            info = ((FolderActionableItem) item).getFolderInfo();
        } else {
            info = item.getItemInfo();
        }

        PropertiesWebAddon propertiesWebAddon = addonsManager.addonByType(PropertiesWebAddon.class);
        add(propertiesWebAddon.getTreeItemPropertiesPanel("propertiesPanel", info));

        List<String> metadataTypeList = getMetadataNames(canonicalRepoPath);
        if (metadataTypeList.isEmpty()) {
            add(new WebMarkupContainer("metadataPanel"));
        } else {
            MetadataPanel metadataPanel = new MetadataPanel("metadataPanel", canonicalRepoPath, metadataTypeList);
            metadataPanel.add(new CollapsibleBehavior().setPersistenceCookie("metadata"));
            add(metadataPanel);
        }
    }

    /**
     * @return a list of metadata type names that are associated with this item
     */
    private List<String> getMetadataNames(RepoPath canonicalRepoPath) {
        try {
            return repoService.getMetadataNames(canonicalRepoPath);
        } catch (RepositoryRuntimeException rre) {
            String repoPathId = canonicalRepoPath.getId();
            log.error("Error while retrieving metadata names for '{}': {}", repoPathId, rre.getMessage());
            log.debug("Error while retrieving metadata names for '" + repoPathId + "'.", rre);
            return Lists.newArrayList();
        }
    }
}
