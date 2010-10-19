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

package org.artifactory.repo.interceptor;

import com.google.common.collect.TreeMultimap;
import org.artifactory.api.fs.InternalFileInfo;
import org.artifactory.api.fs.RepoResource;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.resource.FileResource;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;

import java.util.List;
import java.util.SortedSet;

/**
 * @author yoav
 */
public class UniqueSnapshotsCleanerInterceptor extends StorageInterceptorAdapter {
    private static final Logger log = LoggerFactory.getLogger(UniqueSnapshotsCleanerInterceptor.class);

    /**
     * Cleanup old snapshots etc.
     *
     * @param fsItem
     * @param statusHolder
     */
    @Override
    public void afterCreate(JcrFsItem fsItem, MutableStatusHolder statusHolder) {
        StoringRepo repo = fsItem.getRepo();
        if (!(repo instanceof LocalRepo) || fsItem.isDirectory()) {
            return;
        }
        //If the resource has no size specified, this will update the size
        //(this can happen if we established the resource based on a HEAD request that failed to
        //return the content-length).
        RepoResource res = new FileResource((InternalFileInfo) fsItem.getInfo());
        String path = res.getRepoPath().getPath();
        JcrFile file = (JcrFile) fsItem;
        if (!MavenNaming.isUniqueSnapshot(path)) {
            return;
        }
        int maxUniqueSnapshots = ((LocalRepo) repo).getMaxUniqueSnapshots();
        if (maxUniqueSnapshots > 0) {
            //Read the build number and delete old unique snapshot artifacts
            JcrFolder snapshotFolder = file.getParentFolder();
            JcrRepoService jcrRepoService = InternalContextHelper.get().getJcrRepoService();
            List<JcrFsItem> children = jcrRepoService.getChildren(snapshotFolder, true);
            TreeMultimap<Integer, JcrFsItem> itemMap = TreeMultimap.create();
            for (JcrFsItem child : children) {
                String name = child.getName();
                if (MavenNaming.isUniqueSnapshotFileName(name)) {
                    int buildNumber = MavenNaming.getUniqueSnapshotVersionBuildNumber(name);
                    itemMap.put(buildNumber, child);
                }
            }

            while (itemMap.keySet().size() > maxUniqueSnapshots) {
                Integer firstNumber = itemMap.keySet().first();
                SortedSet<JcrFsItem> itemsToRemove = itemMap.removeAll(firstNumber);
                for (JcrFsItem itemToRemove : itemsToRemove) {
                    itemToRemove.bruteForceDelete();
                    log.info("Removed old unique snapshot '{}'.", itemToRemove.getRelativePath());
                }
            }
        }
    }
}