/*
 * This file is part of Artifactory.
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

package org.artifactory.api.fs;

import org.artifactory.api.repo.RepoPath;

/**
 * @author yoavl
 */
public abstract class ItemInfoImpl implements ItemInfo {

    private final RepoPath repoPath;
    private final String name;
    private long created;
    protected long lastModified;

    protected ItemInfoImpl(RepoPath repoPath) {
        if (repoPath == null) {
            throw new IllegalArgumentException("RepoPath cannot be null");
        }
        this.repoPath = repoPath;
        this.name = repoPath.getName();
        this.created = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
    }

    protected ItemInfoImpl(ItemInfo info) {
        this(info.getRepoPath());
        this.created = info.getCreated();
        this.lastModified = info.getLastModified();
    }

    protected ItemInfoImpl(ItemInfo info, RepoPath repoPath) {
        this(repoPath);
        this.created = info.getCreated();
        this.lastModified = info.getLastModified();
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public String getName() {
        return name;
    }

    public String getRepoKey() {
        return repoPath.getRepoKey();
    }

    public String getRelPath() {
        return repoPath.getPath();
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public String getModifiedBy() {
        return getInternalXmlInfo().getModifiedBy();
    }

    public void setModifiedBy(String name) {
        getInternalXmlInfo().setModifiedBy(name);
    }

    public String getCreatedBy() {
        return getInternalXmlInfo().getCreatedBy();
    }

    public void setCreatedBy(String name) {
        getInternalXmlInfo().setCreatedBy(name);
    }

    public long getLastUpdated() {
        return getInternalXmlInfo().getLastUpdated();
    }

    public void setLastUpdated(long lastUpdated) {
        getInternalXmlInfo().setLastUpdated(lastUpdated);
    }

    public boolean isIdentical(ItemInfo info) {
        return this.getClass() == info.getClass() &&
                this.lastModified == info.getLastModified() &&
                this.created == info.getCreated() &&
                this.repoPath.equals(info.getRepoPath()) &&
                this.name.equals(info.getName()) &&
                this.getInternalXmlInfo().isIdentical(info.getInternalXmlInfo());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ItemInfoImpl info = (ItemInfoImpl) o;
        return repoPath.equals(info.repoPath);
    }

    @Override
    public int hashCode() {
        return repoPath.hashCode();
    }

    @Override
    public String toString() {
        return "ItemInfo{" +
                "repoPath=" + repoPath +
                ", created=" + created +
                ", lastModified=" + lastModified +
                '}';
    }
}
