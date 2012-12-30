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

package org.artifactory.repo.interceptor;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.jcr.factory.JcrFsItemFactory;
import org.artifactory.jcr.factory.VfsItemFactory;
import org.artifactory.mime.MavenNaming;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.interceptor.storage.StorageInterceptorAdapter;
import org.artifactory.sapi.fs.VfsItem;

/**
 * Interceptor which handles maven metadata calculation upon creation and removal
 *
 * @author Noam Tenne
 */
public class MavenMetadataCalculationInterceptor extends StorageInterceptorAdapter {

    /**
     * If the newly created item is a pom file, this method will calculate the maven metadata of it's parent folder
     *
     * @param fsItem       Newly created item
     * @param statusHolder StatusHolder
     */
    //@Async(delayUntilAfterCommit = true, transactional = true, shared = true, failIfNotScheduledFromTransaction = true)
    @Override
    public void afterCreate(VfsItem fsItem, MutableStatusHolder statusHolder) {
        if (shouldRecalculateOnCreate(fsItem)) {
            // calculate maven metadata on the grandparent folder (the artifact id node)
            if (MavenNaming.isUniqueSnapshot(fsItem.getPath())) {
                // unique snapshots require instant metadata calculation since it is used to calculate future snapshots
                RepoPath parentFolder = fsItem.getRepoPath().getParent();
                ContextHelper.get().getRepositoryService().calculateMavenMetadata(parentFolder);
            }

            if (isPomFile(fsItem)) {
                // for pom files we need to trigger metadata calculation on the grandparent -
                // potential new version and snapshot.
                // this can be done asynchronously since it's usually not requires instant update
                RepoPath grandparentFolder = getAncestor(fsItem.getRepoPath(), 2);
                ContextHelper.get().getRepositoryService().calculateMavenMetadataAsync(grandparentFolder);
            }
        }
    }

    public static RepoPath getAncestor(RepoPath repoPath, int degree) {
        RepoPath result = repoPath.getParent();   // first ancestor
        for (int i = degree - 1; i > 0 && result != null; i--) {
            result = result.getParent();
        }
        return result;
    }

    private boolean shouldRecalculateOnCreate(VfsItem fsItem) {
        if (!fsItem.isFile()) {
            return false;
        }
        JcrFsItemFactory storingRepo = VfsItemFactory.getStoringRepo(fsItem);
        if (!isLocalNonCachedRepository(storingRepo)) {
            return false;
        }
        // it's a local non-cache repository, check the snapshot behavior
        LocalRepo localRepo = (LocalRepo) storingRepo;

        String path = fsItem.getPath();
        ModuleInfo moduleInfo = localRepo.getItemModuleInfo(path);
        if (moduleInfo.isIntegration() &&
                SnapshotVersionBehavior.DEPLOYER.equals(localRepo.getMavenSnapshotVersionBehavior())) {
            return false;
        }
        return true;
    }

    /**
     * Checks that the given storing repo is a non-cache local repo, since it is the only kind that metadata calculation
     * can be performed on.
     *
     * @param storingRepo Repo to check
     * @return boolean - True if calculation is allowed on this type of repo. False if not
     */
    private boolean isLocalNonCachedRepository(JcrFsItemFactory storingRepo) {
        return storingRepo.isLocal() && (!storingRepo.isCache());
    }

    private boolean isPomFile(VfsItem fsItem) {
        return MavenNaming.isPom(fsItem.getRepoPath().getPath());
    }
}
