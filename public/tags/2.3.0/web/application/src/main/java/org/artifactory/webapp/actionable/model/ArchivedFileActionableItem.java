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

package org.artifactory.webapp.actionable.model;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.tree.fs.ZipEntryInfo;
import org.artifactory.api.tree.fs.ZipTreeNode;
import org.artifactory.repo.RepoPath;
import org.artifactory.util.PathUtils;
import org.artifactory.webapp.actionable.RepoAwareActionableItemBase;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.viewable.ViewableTabPanel;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.zipentry.ZipEntryPanel;
import org.artifactory.webapp.wicket.util.ItemCssClass;

import java.util.List;

/**
 * Represents a file or directory inside a zip.
 *
 * @author Yossi Shaul
 */
public class ArchivedFileActionableItem extends RepoAwareActionableItemBase {
    private final ZipTreeNode node;

    public ArchivedFileActionableItem(RepoPath archivePath, ZipTreeNode node) {
        super(archivePath);
        this.node = node;
    }

    public String getDisplayName() {
        return node.getName();
    }

    public String getPath() {
        return node.getPath();
    }

    public ZipEntryInfo getZipEntry() {
        return node.getZipEntry();
    }

    @Override
    public void addTabs(List<ITab> tabs) {
        tabs.add(new AbstractTab(new Model("General")) {
            @Override
            public Panel getPanel(String panelId) {
                return new ZipEntryPanel(panelId, getZipEntry());
            }
        });

        if (NamingUtils.isViewable(node.getName()) || "class".equals(PathUtils.getExtension(node.getName()))) {
            tabs.add(new AbstractTab(new Model("View Source")) {
                @Override
                public Panel getPanel(String panelId) {
                    return new ViewableTabPanel(panelId, ArchivedFileActionableItem.this);
                }
            });
        }
    }

    public void filterActions(AuthorizationService authService) {

    }

    public String getCssClass() {
        return ItemCssClass.getFileCssClass(node.getName()).getCssClass();
    }
}