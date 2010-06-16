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

import java.io.Serializable;

/**
 * User: freds Date: Jul 27, 2008 Time: 8:31:30 PM
 */
public class DirectoryItem implements Serializable, Comparable<DirectoryItem> {
    private static final long serialVersionUID = 1L;

    public static final String UP = "..";

    private final String name;
    private final ItemInfo itemInfo;

    public DirectoryItem(ItemInfo itemInfo) {
        this.itemInfo = itemInfo;
        this.name = itemInfo.getName();
    }

    public DirectoryItem(String name, ItemInfo itemInfo) {
        this.itemInfo = itemInfo;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ItemInfo getItemInfo() {
        return itemInfo;
    }

    public String getPath() {
        return getItemInfo().getRelPath();
    }

    public boolean isDirectory() {
        return getItemInfo().isFolder();
    }

    public boolean isFolder() {
        return getItemInfo().isFolder();
    }

    public int compareTo(DirectoryItem o) {
        if (name.equals(o.name)) {
            return 0;
        }
        if (name.equals(UP)) {
            return -1;
        }
        if (o.name.equals(UP)) {
            return 1;
        }
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DirectoryItem item = (DirectoryItem) o;

        return !(name != null ? !name.equals(item.name) : item.name != null);
    }

    @Override
    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        return result;
    }
}
