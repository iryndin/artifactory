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

package org.artifactory.repo.interceptor;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.maven.MavenMetadataCalculator;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;

/**
 * Interceptor which handles maven metadata calculation upon creation and removal
 *
 * @author Noam Tenne
 */
public class MavenMetadataCalculationInterceptor implements RepoInterceptor {

    /**
     * If the newly created item is a pom file, this method will calculate the maven metadata of it's parent folder
     *
     * @param fsItem       Newly created item
     * @param statusHolder StatusHolder
     */
    public void onCreate(JcrFsItem fsItem, StatusHolder statusHolder) {
        if (shouldRecalculateOnCreate(fsItem)) {
            InternalArtifactoryContext context = InternalContextHelper.get();
            RepositoryService repositoryService = context.getRepositoryService();

            JcrFolder parentFolder = fsItem.getParentFolder();
            repositoryService.calculateMavenMetadata(parentFolder.getRepoPath());
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
        if (MavenNaming.isSnapshot(fsItem.getPath()) &&
                SnapshotVersionBehavior.DEPLOYER.equals(localRepo.getSnapshotVersionBehavior())) {
            return false;
        }

        return true;
    }

    /**
     * Adds the deleted item to the maven metadata calculator, in case it affects the metadata and requires calculation
     *
     * @param fsItem       Deleted item
     * @param statusHolder StatusHolder
     */
    public void onDelete(JcrFsItem fsItem, StatusHolder statusHolder) {
        StoringRepo storingRepo = fsItem.getRepo();
        //Only calculate if a pom was deleted from a local non-cached repository. Also avoids possible deadlocks if
        //calculating md for deletion of multiple files under the same folder (since we only expect to have 1 pom child)
        if (isPomFile(fsItem) && isLocalNonCahcedRepository(storingRepo)) {
            InternalArtifactoryContext context = InternalContextHelper.get();
            JcrService jcrService = context.getJcrService();
            JcrSession managedSession = jcrService.getManagedSession();

            MavenMetadataCalculator mavenMetadataCalculator =
                    managedSession.getOrCreateResource(MavenMetadataCalculator.class);
            mavenMetadataCalculator.addDeletedItem(fsItem);
        }
    }

    public void onMove(JcrFsItem sourceItem, JcrFsItem targetItem, StatusHolder statusHolder) {
    }

    public void onCopy(JcrFsItem sourceItem, JcrFsItem targetItem, StatusHolder statusHolder) {
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