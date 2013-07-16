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

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.properties;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.PropertiesWebAddon;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.fs.ItemInfo;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.model.FolderActionableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Properties display tab panel. Holds properties info.
 *
 * @author Noam Tenne
 */
public class PropertiesTabPanel extends Panel {
    private static final Logger log = LoggerFactory.getLogger(PropertiesTabPanel.class);

    @SpringBean
    private AddonsManager addonsManager;

    @SpringBean
    private RepositoryService repoService;

    public PropertiesTabPanel(String id, RepoAwareActionableItem item) {
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
    }
}
