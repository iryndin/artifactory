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

package org.artifactory.repo.jcr.cache.expirable;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.repo.jcr.JcrCacheRepo;
import org.artifactory.sapi.fs.VfsFile;
import org.artifactory.sapi.fs.VfsFolder;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.sapi.fs.VfsItemVisitor;

import java.util.List;

/**
 * Zap expired cache items. Sets the last updated value of expirable files and folders to a value that will make them
 * expired.
 *
 * @author Yossi Shaul
 */
public class ZapItemVisitor implements VfsItemVisitor {

    private final JcrCacheRepo cacheRepo;
    private long expiredLastUpdated;
    private int updatedItemsCount;

    /**
     * @param cacheRepo The cache repo containing the files to visit
     */
    public ZapItemVisitor(JcrCacheRepo cacheRepo) {
        this.cacheRepo = cacheRepo;
        long retrievalCachePeriodMillis = cacheRepo.getRemoteRepo().getRetrievalCachePeriodSecs() * 1000L;
        expiredLastUpdated = System.currentTimeMillis() - retrievalCachePeriodMillis;
    }

    /**
     * @param file File to visit. Must reside inside the cache repo.
     */
    public void visit(VfsFile file) {
        CacheExpiry cacheExpiry = ContextHelper.get().beanForType(CacheExpiry.class);
        if (cacheExpiry.isExpirable(cacheRepo, file.getPath())) {
            // zap has a meaning only on non unique snapshot files
            file.setLastUpdated(expiredLastUpdated);
            updatedItemsCount++;
        }
    }

    /**
     * @param folder Folder to visit. Must reside inside the cache repo.
     */
    public void visit(VfsFolder folder) {
        // folders are always expirable
        folder.setLastUpdated(expiredLastUpdated);
        // TODO: ugly hack - don't want to expose this method in the JcrFolder -> move to metadata service?
        if (folder instanceof JcrFolder) {
            // maven-metadata will not be visited as any other VfsFile because it's a pure metadata
            ((JcrFolder) folder).updateMavenMetadataLastModifiedIfExists(expiredLastUpdated);
        }
        updatedItemsCount++;

        // zap children
        List<VfsItem> children = folder.getItems(true);
        for (VfsItem child : children) {
            child.accept(this);
        }
    }

    /**
     * @return Number of files and folders affected
     */
    public int getUpdatedItemsCount() {
        return updatedItemsCount;
    }

    public void visit(VfsItem visitable) {
        if (visitable.isFile()) {
            visit((VfsFile) visitable);
        } else {
            visit((VfsFolder) visitable);
        }
    }

}
