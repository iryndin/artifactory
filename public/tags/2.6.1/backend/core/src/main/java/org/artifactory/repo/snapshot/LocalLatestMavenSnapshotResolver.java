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
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.repo.BaseBrowsableItem;
import org.artifactory.api.repo.BrowsableItemCriteria;
import org.artifactory.api.repo.RepositoryBrowsingService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.versioning.MavenVersionComparator;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.request.InternalRequestContext;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Resolves the latest unique snapshot version given a non-unique Maven snapshot artifact request
 * for local and cache repositories.
 *
 * @author Shay Yaakov
 */
public class LocalLatestMavenSnapshotResolver extends LatestMavenSnapshotResolver {
    private static final Logger log = LoggerFactory.getLogger(LocalLatestMavenSnapshotResolver.class);

    private final MavenVersionComparator mavenVersionComparator = new MavenVersionComparator();

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
        boolean isDeployerBehavior = SnapshotVersionBehavior.DEPLOYER.equals(
                repoDescriptor.getSnapshotVersionBehavior());
        String artifactPath = getLatestArtifactPath(parentRepoPath, originalModuleInfo, isDeployerBehavior);
        if (artifactPath != null) {
            String metadataName = null;
            if (NamingUtils.isMetadata(path)) {
                metadataName = NamingUtils.getMetadataName(path);
            }
            if (StringUtils.isNotBlank(metadataName)) {
                artifactPath = NamingUtils.getMetadataPath(artifactPath, metadataName);
            }
            requestContext = translateRepoRequestContext(requestContext, repo, artifactPath);
            return requestContext;
        }

        return requestContext;
    }

    /**
     * Retrieves the path to the latest unique artifact (null if not found)
     *
     * @param parentRepoPath     the parent folder to search within
     * @param originalModuleInfo the user request module info to compare with
     * @param isDeployerBehavior on deployer behaviour compares by last modified, otherwise by version string
     * @return a path to the latest unique artifact (null if not found)
     */
    private String getLatestArtifactPath(RepoPath parentRepoPath, ModuleInfo originalModuleInfo,
            boolean isDeployerBehavior) {
        RepositoryService repositoryService = getRepositoryService();
        ModuleInfo latestModuleInfo = null;
        long latestLastModified = 0;
        String latestArtifactPath = null;
        BrowsableItemCriteria criteria = new BrowsableItemCriteria.Builder(parentRepoPath).
                includeChecksums(false).includeRemoteResources(false).build();
        RepositoryBrowsingService repositoryBrowsingService = ContextHelper.get().beanForType(
                RepositoryBrowsingService.class);

        List<BaseBrowsableItem> children;
        try {
            children = repositoryBrowsingService.getLocalRepoBrowsableChildren(criteria);
        } catch (ItemNotFoundRuntimeException e) {
            // Simply log the message and return null
            log.debug(e.getMessage());
            return null;
        }

        for (BaseBrowsableItem child : children) {
            if (!child.isFolder()) {
                ModuleInfo itemModuleInfo = repositoryService.getItemModuleInfo(child.getRepoPath());
                if (itemModuleInfo.isValid()) {
                    if (areModuleInfosTheSame(originalModuleInfo, itemModuleInfo)) {
                        if (isDeployerBehavior) {
                            long childLastModified = child.getLastModified();
                            if (childLastModified > latestLastModified) {
                                latestLastModified = childLastModified;
                                latestArtifactPath = child.getRepoPath().getPath();
                            }
                        } else {
                            ModuleInfo resultModuleInfo = getLatestModuleInfo(itemModuleInfo, latestModuleInfo);
                            if (!resultModuleInfo.equals(latestModuleInfo)) {
                                latestModuleInfo = resultModuleInfo;
                                latestArtifactPath = child.getRepoPath().getPath();
                            }
                        }
                    }
                }
            }
        }

        return latestArtifactPath;
    }

    /**
     * Compares 2 given module infos and returns the latest one
     */
    private ModuleInfo getLatestModuleInfo(@Nonnull ModuleInfo moduleInfo1, @Nullable ModuleInfo moduleInfo2) {
        if (moduleInfo2 == null) {
            return moduleInfo1;
        }

        String version1 = moduleInfo1.getBaseRevision() + "-" + moduleInfo1.getFileIntegrationRevision();
        String version2 = moduleInfo2.getBaseRevision() + "-" + moduleInfo2.getFileIntegrationRevision();

        return (mavenVersionComparator.compare(version1, version2) >= 0) ? moduleInfo1 : moduleInfo2;
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
