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
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.tree.fs.ZipEntriesTree;
import org.artifactory.api.tree.fs.ZipTreeNode;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.actionable.ActionableItem;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A zip file actionable item which is a file actionable item with hierarchy behavior that allows browsing
 * the internals of the zip.
 *
 * @author Yossi Shaul
 */
public class ZipFileActionableItem extends FileActionableItem implements HierarchicActionableItem {
    private static final Logger log = LoggerFactory.getLogger(ZipFileActionableItem.class);

    public ZipFileActionableItem(FileInfo fileInfo) {
        super(fileInfo);
    }

    public List<ActionableItem> getChildren(AuthorizationService authService) {
        ZipEntriesTree tree;
        try {
            tree = getRepoService().zipEntriesToTree(getFileInfo().getRepoPath());
        } catch (IOException e) {
            log.error("Failed to retrieve zip entries: " + e.getMessage());
            return Collections.emptyList();
        }

        ZipTreeNode root = tree.getRoot();
        if (!root.hasChildren()) {
            return Collections.emptyList();
        }

        List<ActionableItem> items = Lists.newArrayList();
        Set<ZipTreeNode> children = root.getChildren();
        for (ZipTreeNode childTreeNode : children) {
            if (childTreeNode.isDirectory()) {
                items.add(new ArchivedFolderActionableItem(getRepoPath(), childTreeNode));
            } else {
                items.add(new ArchivedFileActionableItem(getRepoPath(), childTreeNode));
            }
        }
        return items;
    }

    public boolean hasChildren(AuthorizationService authService) {
        // always assume the zip has children (we'll make sure it does later)
        return true;
    }

    public boolean isCompactAllowed() {
        return false;
    }

    public void setCompactAllowed(boolean compactAllowed) {

    }
}
