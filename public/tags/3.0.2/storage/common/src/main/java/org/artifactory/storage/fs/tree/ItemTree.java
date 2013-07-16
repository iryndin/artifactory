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

package org.artifactory.storage.fs.tree;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.FolderInfo;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.storage.fs.VfsItemNotFoundException;
import org.artifactory.storage.fs.service.FileService;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A tree representation of the vfs file system. The item tree holds no storage locks and represents the state of the
 * database in the time the tree is created.
 *
 * @author Yossi Shaul
 */
public class ItemTree {

    private final RepoPath rootRepoPath;
    private final ItemNodeFilter filter;

    /**
     * Create an items tree with the repo path as root node.
     *
     * @param root Repo path of the root node.
     */
    public ItemTree(RepoPath root) {
        this(root, null);
    }

    /**
     * Create an items tree with the repo path as root node. The filter is optional and affects only decedents.
     *
     * @param root   Repo path of the root node.
     * @param filter Optional filter for child nodes
     */
    public ItemTree(RepoPath root, @Nullable ItemNodeFilter filter) {
        rootRepoPath = root;
        this.filter = filter;
    }

    /**
     * @return The root node of the tree or null if the root repo path doesn't exist in the storage.
     */
    @Nullable
    public ItemNode<? extends ItemInfo> getRootNode() {
        FileService fileService = ContextHelper.get().beanForType(FileService.class);
        ItemInfo rootItemInfo;
        try {
            rootItemInfo = fileService.loadItem(rootRepoPath);
        } catch (VfsItemNotFoundException e) {
            // nobody promised the root exists
            return null;
        }
        if (rootItemInfo.isFolder()) {
            return new FolderNode((FolderInfo) rootItemInfo, filter);
        } else {
            return new FileNode((FileInfo) rootItemInfo);
        }
    }

    /**
     * Builds the tree and caches all the nodes.
     *
     * @return The root node of the tree
     */
    @Nullable
    public ItemNode buildTree() {
        ItemNode rootNode = getRootNode();
        buildTree(rootNode);
        return rootNode;
    }

    private void buildTree(ItemNode currentNode) {
        if (currentNode == null) {
            return;
        }

        if (currentNode.itemInfo.isFolder()) {
            List<ItemNode<? extends ItemInfo>> children = currentNode.getChildren();
            for (ItemNode child : children) {
                buildTree(child);
            }
        }
    }

}
