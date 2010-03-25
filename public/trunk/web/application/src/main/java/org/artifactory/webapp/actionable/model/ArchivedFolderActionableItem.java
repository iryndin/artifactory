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

import com.google.common.collect.Lists;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.tree.fs.ZipEntryInfo;
import org.artifactory.api.tree.fs.ZipTreeNode;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.RepoAwareActionableItemBase;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.zipentry.ZipEntryPanel;
import org.artifactory.webapp.wicket.util.ItemCssClass;

import java.util.List;
import java.util.Set;

/**
 * Represents a file or directory inside a zip.
 *
 * @author Yossi Shaul
 */
public class ArchivedFolderActionableItem extends RepoAwareActionableItemBase implements HierarchicActionableItem {
    private final ZipTreeNode node;

    public ArchivedFolderActionableItem(RepoPath archivePath, ZipTreeNode node) {
        super(archivePath);
        this.node = node;
    }

    public String getDisplayName() {
        return node.getName();
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

    }

    public void filterActions(AuthorizationService authService) {

    }

    public List<ActionableItem> getChildren(AuthorizationService authService) {
        List<ActionableItem> items = Lists.newArrayList();
        Set<ZipTreeNode> children = node.getChildren();
        for (ZipTreeNode file : children) {
            if (file.isDirectory()) {
                items.add(new ArchivedFolderActionableItem(getRepoPath(), file));
            } else {
                items.add(new ArchivedFileActionableItem(getRepoPath(), file));
            }
        }
        return items;
    }

    public boolean hasChildren(AuthorizationService authService) {
        return node.hasChildren();
    }

    public boolean isCompactAllowed() {
        return false;
    }

    public void setCompactAllowed(boolean compactAllowed) {

    }

    public String getCssClass() {
        return ItemCssClass.folder.getCssClass();
    }
}