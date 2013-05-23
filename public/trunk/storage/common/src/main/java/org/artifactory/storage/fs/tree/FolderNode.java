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

import com.google.common.collect.Lists;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.FolderInfo;
import org.artifactory.fs.ItemInfo;
import org.artifactory.storage.fs.service.FileService;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A folder node is a virtual file system node with {@link org.artifactory.fs.FolderInfo} as its data.
 * The node is detached from the database and may not exist anymore.
 *
 * @author Yossi Shaul
 */
public class FolderNode extends ItemNode<FolderInfo> {

    List<ItemNode<? extends ItemInfo>> cachedChildrenNodes;
    private final ItemNodeFilter filter;

    public FolderNode(FolderInfo itemInfo, @Nullable ItemNodeFilter filter) {
        super(itemInfo);
        this.filter = filter;
    }

    @Override
    public List<ItemNode<? extends ItemInfo>> getChildren() {
        if (cachedChildrenNodes == null) {
            List<ItemInfo> children = getFileService().loadChildren(itemInfo.getRepoPath());
            cachedChildrenNodes = Lists.newArrayListWithCapacity(children.size());
            for (ItemInfo child : children) {
                if (filter == null || filter.accepts(child)) {
                    if (child.isFolder()) {
                        cachedChildrenNodes.add(new FolderNode((FolderInfo) child, filter));
                    } else {
                        cachedChildrenNodes.add(new FileNode((FileInfo) child));
                    }
                }
            }
        }
        return cachedChildrenNodes;
    }

    @Override
    public boolean hasChildren() {
        if (cachedChildrenNodes != null) {
            return !cachedChildrenNodes.isEmpty();
        } else {
            return getFileService().hasChildren(itemInfo.getRepoPath());
        }
    }

    private FileService getFileService() {
        return ContextHelper.get().beanForType(FileService.class);
    }
}
