/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.api.tree.fs;

import org.artifactory.api.tree.TreeNode;
import org.artifactory.util.PathUtils;

import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;

/**
 * A tree node representing ZipEntry.
 *
 * @author Yossi Shaul
 */
public class ZipTreeNode implements TreeNode<ZipEntryInfo>, Serializable, Comparable<ZipTreeNode> {

    private boolean directory;
    private Set<ZipTreeNode> children;
    private String entryPath;
    private ZipEntryInfo zipEntry;

    public ZipTreeNode(String entryPath, boolean directory) {
        this.directory = directory;
        this.entryPath = entryPath;
        zipEntry = new ZipEntryInfo(entryPath);
    }

    public Set<ZipTreeNode> getChildren() {
        return children;
    }

    public ZipEntryInfo getData() {
        return zipEntry;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    public boolean isLeaf() {
        return !hasChildren();
    }

    public boolean isDirectory() {
        return directory;
    }

    public ZipTreeNode getChild(ZipEntryInfo data) {
        if (!data.getName().startsWith(this.entryPath)) {
            return null;
        }

        return getChild(PathUtils.getName(data.getName()));
    }

    public String getPath() {
        return entryPath;
    }

    ZipTreeNode getChild(String relativePath) {
        if (children != null) {
            for (ZipTreeNode child : children) {
                if (child.getName().equals(relativePath)) {
                    return child;
                }
            }
        }
        return null;
    }

    public void addChild(ZipTreeNode child) {
        if (!directory) {
            throw new IllegalStateException("Cannot add children to a leaf node");
        }
        if (children == null) {
            children = new TreeSet<ZipTreeNode>();
        }
        children.add(child);
    }

    public String getName() {
        return PathUtils.getName(getPath());
    }

    public void setZipEntry(ZipEntryInfo zipEntry) {
        this.zipEntry = zipEntry;
    }

    public ZipEntryInfo getZipEntry() {
        return zipEntry;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ZipTreeNode that = (ZipTreeNode) o;

        if (!entryPath.equals(that.entryPath)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return entryPath.hashCode();
    }

    public int compareTo(ZipTreeNode o) {
        if (o.directory && !directory) {
            return 1;
        }
        if (!o.directory && directory) {
            return -1;
        }
        return entryPath.compareTo(o.entryPath);
    }
}
