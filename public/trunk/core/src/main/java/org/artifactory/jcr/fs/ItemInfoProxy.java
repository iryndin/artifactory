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

package org.artifactory.jcr.fs;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.fs.ItemInfoImpl;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

/**
 * An item info that can lazily load its data from jcr. Non thread-safe!
 *
 * @author Yoav Landman
 */
public abstract class ItemInfoProxy<T extends ItemInfo> extends ItemInfoImpl {
    private static final Logger log = LoggerFactory.getLogger(ItemInfoProxy.class);

    private T materialized;

    public static ItemInfoProxy create(ItemInfo info) {
        if (info.isFolder()) {
            return new FolderInfoProxy(info.getRepoPath());
        } else {
            return new FileInfoProxy(info.getRepoPath());
        }
    }

    public ItemInfoProxy(RepoPath repoPath) {
        super(repoPath);
    }

    @Override
    public long getCreated() {
        return getMaterialized().getCreated();
    }

    @Override
    public long getLastModified() {
        return getMaterialized().getLastModified();
    }

    @Override
    public String getModifiedBy() {
        return getMaterialized().getModifiedBy();
    }

    @Override
    public String getCreatedBy() {
        return getMaterialized().getCreatedBy();
    }

    @Override
    public long getLastUpdated() {
        return getMaterialized().getLastUpdated();
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
            RepositoryService repositoryService = ContextHelper.get().getRepositoryService();
            materialized = (T) repositoryService.getItemInfo(getRepoPath());
        }
        return materialized;
    }
}
