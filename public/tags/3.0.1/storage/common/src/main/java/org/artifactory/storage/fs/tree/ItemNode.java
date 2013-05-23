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

import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;

import java.util.List;

/**
 * A detached item info node.
 *
 * @author Yossi Shaul
 */
public abstract class ItemNode<T extends ItemInfo> {

    protected final T itemInfo;

    public ItemNode(T itemInfo) {
        this.itemInfo = itemInfo;
    }

    /**
     * @return The item info represented by this node
     */
    public T getItemInfo() {
        return itemInfo;
    }

    /**
     * @return The repository path of the item held by this node
     */
    public RepoPath getRepoPath() {
        return itemInfo.getRepoPath();
    }

    /**
     * @return The file/folder name of this item node
     * @see org.artifactory.repo.RepoPath#getName()
     */
    public String getName() {
        return itemInfo.getName();
    }

    /**
     * @return True if this node represents a folder
     */
    public boolean isFolder() {
        return itemInfo.isFolder();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getRepoPath() + "]";
    }

    public abstract List<ItemNode<? extends ItemInfo>> getChildren();

    public abstract boolean hasChildren();
}
