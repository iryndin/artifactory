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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.FolderInfo;
import org.artifactory.fs.ItemInfo;
import org.artifactory.storage.fs.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * A folder node is a virtual file system node with {@link org.artifactory.fs.FolderInfo} as its data.
 * The node is detached from the database and may not exist anymore.
 *
 * @author Yossi Shaul
 */
public class FolderNode extends ItemNode {
    private static final Logger log = LoggerFactory.getLogger(FolderNode.class);

    private List<ItemNode> cachedChildrenNodes;
    private final TreeBrowsingCriteria criteria;

    public FolderNode(FolderInfo itemInfo, TreeBrowsingCriteria criteria) {
        super(itemInfo);
        this.criteria = criteria;
    }

    @Override
    public List<ItemNode> getChildren() {
        if (cachedChildrenNodes != null) {
            return cachedChildrenNodes;
        }
        List<ItemInfo> children = getFileService().loadChildren(itemInfo.getRepoPath());
        List<ItemNode> childrenNodes = Lists.newArrayListWithCapacity(children.size());

        sort(children);

        for (ItemInfo child : children) {
            if (accepts(child)) {
                if (child.isFolder()) {
                    childrenNodes.add(new FolderNode((FolderInfo) child, criteria));
                } else {
                    childrenNodes.add(new FileNode((FileInfo) child));
                }
            }
        }

        if (criteria.isCacheChildren()) {
            cachedChildrenNodes = childrenNodes;
        }

        return childrenNodes;
    }

    @Override
    public List<ItemInfo> getChildrenInfo() {
        List<ItemNode> children = getChildren();
        return Lists.transform(children, new Function<ItemNode, ItemInfo>() {
            @Override
            public ItemInfo apply(ItemNode input) {
                return input.getItemInfo();
            }
        });
    }

    public boolean accepts(ItemInfo child) {
        if (criteria.getFilters() == null) {
            return true;
        }
        for (ItemNodeFilter filter : criteria.getFilters()) {
            if (!filter.accepts(child)) {
                log.debug("Filter {} rejected {}", filter, child);
                return false;
            }
        }
        // all filters accepted the item
        return true;
    }

    private void sort(List<ItemInfo> children) {
        if (criteria.getComparator() == null) {
            return;
        }
        Collections.sort(children, criteria.getComparator());
    }

    @Override
    public boolean hasChildren() {
        if (cachedChildrenNodes != null) {
            return !cachedChildrenNodes.isEmpty();
        } else {
            return getFileService().hasChildren(itemInfo.getRepoPath());
        }
    }

    @Override
    public FolderInfo getItemInfo() {
        return (FolderInfo) super.getItemInfo();
    }

    private FileService getFileService() {
        return ContextHelper.get().beanForType(FileService.class);
    }
}
