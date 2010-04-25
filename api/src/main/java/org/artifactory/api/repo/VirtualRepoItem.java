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

package org.artifactory.api.repo;

import org.artifactory.api.fs.ItemInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a virtual file/folder inside a virtual repository. Holds the item info of one concrete item (usually the
 * first one) and a list of local repository keys that accessible from the virtual repo and contain this item.
 *
 * @author yoavl
 */
public class VirtualRepoItem {
    private final ItemInfo item;
    private List<String> repoKeys = new ArrayList<String>();

    public VirtualRepoItem(ItemInfo item) {
        this.item = item;
    }

    public String getName() {
        return item.getName();
    }

    public String getPath() {
        return item.getRelPath();
    }

    public boolean isFolder() {
        return item.isFolder();
    }

    public List<String> getRepoKeys() {
        return repoKeys;
    }

    public ItemInfo getItem() {
        return item;
    }

    public void addRepoKey(String repoKey) {
        getRepoKeys().add(repoKey);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VirtualRepoItem item1 = (VirtualRepoItem) o;
        return item.equals(item1.item);

    }

    public int hashCode() {
        return item.hashCode();
    }

    @Override
    public String toString() {
        return "VirtualRepoItem{" +
                "item=" + item +
                ", repoKeys=" + repoKeys +
                '}';
    }
}
