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
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.tree.fs.ZipEntryInfo;
import org.artifactory.api.tree.fs.ZipTreeNode;
import org.artifactory.repo.RepoPath;
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
    private ZipTreeNode node;
    private boolean compactAllowed;
    private String displayName;
    private boolean compacted;

    public ArchivedFolderActionableItem(RepoPath archivePath, ZipTreeNode node, boolean compact) {
        super(archivePath);
        this.node = node;
        this.displayName = node.getName();
        this.compactAllowed = compact;
        if (compact) {
            compact();
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public ZipEntryInfo getZipEntry() {
        return node.getZipEntry();
    }

    @Override
    public void addTabs(List<ITab> tabs) {
        tabs.add(new AbstractTab(Model.of("General")) {
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
                items.add(new ArchivedFolderActionableItem(getRepoPath(), file, compactAllowed));
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
        return compactAllowed;
    }

    public void setCompactAllowed(boolean compactAllowed) {
        this.compactAllowed = compactAllowed;
    }

    private void compact() {
        StringBuilder compactedName = new StringBuilder(displayName);
        ZipTreeNode next = getNextCompactedNode(node);
        while (next != null) {
            compacted = true;
            node = next;
            compactedName.append('/').append(next.getName());
            next = getNextCompactedNode(next);
        }
        displayName = compactedName.toString();
    }

    private ZipTreeNode getNextCompactedNode(ZipTreeNode next) {
        Set<ZipTreeNode> children = next.getChildren();

        // only compact folders with exactly one child
        if (children == null || children.size() != 1) {
            return null;
        }

        // the only child must be a folder
        ZipTreeNode firstChild = children.iterator().next();
        if (!firstChild.isDirectory()) {
            return null;
        }

        return firstChild;
    }

    public String getCssClass() {
        return compacted ? ItemCssClass.folderCompact.getCssClass() : ItemCssClass.folder.getCssClass();
    }
}