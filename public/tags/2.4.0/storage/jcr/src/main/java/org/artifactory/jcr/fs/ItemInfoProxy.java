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

package org.artifactory.jcr.fs;

import org.artifactory.api.repo.RepositoryService;
import org.artifactory.fs.ItemInfo;
import org.artifactory.fs.MutableItemInfo;
import org.artifactory.jcr.spring.StorageContextHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoPath;
import org.slf4j.Logger;

/**
 * An item info that can lazily load its data from jcr. Non thread-safe!
 *
 * @author Yoav Landman
 */
public abstract class ItemInfoProxy<T extends MutableItemInfo> implements MutableItemInfo {
    private static final Logger log = LoggerFactory.getLogger(ItemInfoProxy.class);

    private final RepoPath repoPath;

    private T materialized;

    public static ItemInfoProxy create(ItemInfo info) {
        if (info.isFolder()) {
            return new FolderInfoProxy(info.getRepoPath());
        } else {
            return new FileInfoProxy(info.getRepoPath());
        }
    }

    public ItemInfoProxy(RepoPath repoPath) {
        this.repoPath = repoPath;
    }

    public RepoPath getRepoPath() {
        // Repo path key needed to materialized => do not materialized
        return this.repoPath;
    }

    public String getName() {
        // Do not materialized
        return this.repoPath.getName();
    }

    public String getRepoKey() {
        // Do not materialized
        return this.repoPath.getRepoKey();
    }

    public String getRelPath() {
        // Do not materialized
        return this.repoPath.getPath();
    }

    public long getCreated() {
        return getMaterialized().getCreated();
    }

    public long getLastModified() {
        return getMaterialized().getLastModified();
    }

    public String getModifiedBy() {
        return getMaterialized().getModifiedBy();
    }

    public String getCreatedBy() {
        return getMaterialized().getCreatedBy();
    }

    public long getLastUpdated() {
        return getMaterialized().getLastUpdated();
    }

    public void setCreated(long created) {
        getMaterialized().setCreated(created);
    }

    public void setLastModified(long lastModified) {
        getMaterialized().setLastModified(lastModified);
    }

    public void setModifiedBy(String name) {
        getMaterialized().setModifiedBy(name);
    }

    public void setCreatedBy(String name) {
        getMaterialized().setCreatedBy(name);
    }

    public void setLastUpdated(long lastUpdated) {
        getMaterialized().setLastUpdated(lastUpdated);
    }

    public boolean isIdentical(ItemInfo info) {
        return getMaterialized().isIdentical(info);
    }

    public boolean merge(MutableItemInfo itemInfo) {
        return getMaterialized().merge(itemInfo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof ItemInfo)) {
            return false;
        }
        ItemInfo info = (ItemInfo) o;
        return repoPath.equals(info.getRepoPath());
    }

    @Override
    public int hashCode() {
        return repoPath.hashCode();
    }

    @Override
    public String toString() {
        return "ItemInfoProxy{" +
                "repoPath=" + repoPath +
                '}';
    }

    public boolean isMaterialized() {
        return materialized != null;
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown", "unchecked"})
    protected T getMaterialized() {
        if (materialized == null) {
            if (log.isTraceEnabled()) {
                log.debug("Materializing ItemInfo: " + getRepoPath() + ".", new Throwable());
            } else {
                log.debug("Materializing ItemInfo: {}.", getRelPath());
            }
            RepositoryService repositoryService = StorageContextHelper.get().getRepositoryService();
            materialized = (T) repositoryService.getItemInfo(getRepoPath());
        }
        return materialized;
    }
}
