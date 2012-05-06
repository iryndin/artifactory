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

package org.artifactory.repo.snapshot;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.module.VersionUnit;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.fs.ItemInfo;
import org.artifactory.maven.versioning.MavenVersionComparator;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.request.InternalRequestContext;
import org.artifactory.util.CollectionUtils;
import org.artifactory.util.PathUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Resolves the latest unique snapshot version given a non-unique Maven snapshot artifact request
 * for local and cache repositories.
 *
 * @author Shay Yaakov
 */
public class LocalLatestMavenSnapshotResolver extends LatestMavenSnapshotResolver {

    @Override
    protected InternalRequestContext getRequestContext(InternalRequestContext requestContext, Repo repo,
            ModuleInfo originalModuleInfo) {
        if (!(repo.isLocal())) {
            return requestContext;
        }

        LocalRepoDescriptor repoDescriptor = (LocalRepoDescriptor) repo.getDescriptor();
        if (repoDescriptor.getSnapshotVersionBehavior().equals(SnapshotVersionBehavior.NONUNIQUE)) {
            return requestContext;
        }

        String path = requestContext.getResourcePath();
        String parentPath = PathUtils.getParent(path);
        RepoPath parentRepoPath = RepoPathFactory.create(repo.getKey(), parentPath);
        List<VersionUnit> versionUnitsUnder = getRepositoryService().getVersionUnitsUnder(parentRepoPath);
        if (CollectionUtils.notNullOrEmpty(versionUnitsUnder)) {
            if (SnapshotVersionBehavior.DEPLOYER.equals(repoDescriptor.getSnapshotVersionBehavior())) {
                sortByLastModified(versionUnitsUnder);
            } else {
                sortByVersionString(versionUnitsUnder);
            }

            String metadataName = null;
            if (NamingUtils.isMetadata(path)) {
                metadataName = NamingUtils.getMetadataName(path);
            }
            for (VersionUnit versionUnit : versionUnitsUnder) {
                Set<RepoPath> latestVersionRepoPaths = versionUnit.getRepoPaths();
                for (RepoPath currentVersionUnitRepoPath : latestVersionRepoPaths) {
                    ModuleInfo itemModuleInfo = getRepositoryService().getItemModuleInfo(currentVersionUnitRepoPath);
                    if (areModuleInfosTheSame(originalModuleInfo, itemModuleInfo)) {
                        String artifactPath = currentVersionUnitRepoPath.getPath();
                        if (StringUtils.isNotBlank(metadataName)) {
                            artifactPath = NamingUtils.getMetadataPath(artifactPath, metadataName);
                        }
                        requestContext = translateRepoRequestContext(requestContext, repo, artifactPath);
                        return requestContext;
                    }
                }
            }
        }

        return requestContext;
    }

    private void sortByVersionString(List<VersionUnit> versionUnitsUnder) {
        // sort all the artifacts under the parent directory, latest first
        final MavenVersionComparator mavenVersionComparator = new MavenVersionComparator();
        Collections.sort(versionUnitsUnder, new Comparator<VersionUnit>() {
            @Override
            public int compare(VersionUnit versionUnit1, VersionUnit versionUnit2) {
                ModuleInfo moduleInfo1 = versionUnit1.getModuleInfo();
                ModuleInfo moduleInfo2 = versionUnit2.getModuleInfo();
                String version1 = moduleInfo1.getBaseRevision() + "-" + moduleInfo1.getFileIntegrationRevision();
                String version2 = moduleInfo2.getBaseRevision() + "-" + moduleInfo2.getFileIntegrationRevision();
                return mavenVersionComparator.compare(version2, version1);
            }
        });
    }

    private void sortByLastModified(List<VersionUnit> versionUnitsUnder) {
        // On deployer snapshot behavior we need to sort the results by the last modified field
        Collections.sort(versionUnitsUnder, new Comparator<VersionUnit>() {
            @Override
            public int compare(VersionUnit versionUnit1, VersionUnit versionUnit2) {
                RepoPath repoPath1 = versionUnit1.getRepoPaths().iterator().next();
                RepoPath repoPath2 = versionUnit2.getRepoPaths().iterator().next();
                RepositoryService repositoryService = getRepositoryService();
                ItemInfo itemInfo1 = repositoryService.getItemInfo(repoPath1);
                ItemInfo itemInfo2 = repositoryService.getItemInfo(repoPath2);
                return new Long(itemInfo2.getLastModified()).compareTo(itemInfo1.getLastModified());
            }
        });
    }

    private boolean areModuleInfosTheSame(ModuleInfo originalModuleInfo, ModuleInfo moduleInfo) {
        String originalExtWithoutMetadata = NamingUtils.stripMetadataFromPath(originalModuleInfo.getExt());
        return StringUtils.equals(originalModuleInfo.getOrganization(), moduleInfo.getOrganization()) &&
                StringUtils.equals(originalModuleInfo.getModule(), moduleInfo.getModule()) &&
                StringUtils.equals(originalModuleInfo.getBaseRevision(), moduleInfo.getBaseRevision()) &&
                StringUtils.equals(originalModuleInfo.getClassifier(), moduleInfo.getClassifier()) &&
                StringUtils.equals(originalExtWithoutMetadata, moduleInfo.getExt());
    }
}
