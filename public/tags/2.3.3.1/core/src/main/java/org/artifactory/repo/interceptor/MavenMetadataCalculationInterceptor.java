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

package org.artifactory.repo.interceptor;

import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.jcr.StoringRepo;

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
    @Override
    public void afterCreate(JcrFsItem fsItem, MutableStatusHolder statusHolder) {
        if (shouldRecalculateOnCreate(fsItem)) {
            ArtifactoryContext context = ContextHelper.get();
            RepositoryService repositoryService = context.getRepositoryService();

            // calculate maven metadata on the grandparent folder (the artifact id node)
            JcrFolder artifactIdFolder = fsItem.getAncestor(2);
            if (artifactIdFolder != null) {
                repositoryService.calculateMavenMetadata(artifactIdFolder.getRepoPath());
            }
        }
    }

    private boolean shouldRecalculateOnCreate(JcrFsItem fsItem) {
        if (!isPomFile(fsItem)) {
            return false;
        }
        StoringRepo storingRepo = fsItem.getRepo();
        if (!isLocalNonCahcedRepository(storingRepo)) {
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
    private boolean isLocalNonCahcedRepository(StoringRepo storingRepo) {
        return storingRepo.isLocal() && (!storingRepo.isCache());
    }

    private boolean isPomFile(JcrFsItem fsItem) {
        return fsItem.isFile() && MavenNaming.isPom(fsItem.getRepoPath().getPath());
    }
}
