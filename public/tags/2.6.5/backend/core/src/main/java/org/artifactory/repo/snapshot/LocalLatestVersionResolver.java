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

import com.google.common.collect.Lists;
import com.google.common.collect.TreeMultimap;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.module.ModuleInfoBuilder;
import org.artifactory.api.module.ModuleInfoUtils;
import org.artifactory.api.repo.BaseBrowsableItem;
import org.artifactory.api.repo.BrowsableItemCriteria;
import org.artifactory.api.repo.RepositoryBrowsingService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.jcr.fs.JcrTreeNode;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.versioning.MavenVersionComparator;
import org.artifactory.md.Properties;
import org.artifactory.mime.MavenNaming;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.request.InternalRequestContext;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

/**
 * Resolves the latest unique snapshot version given a non-unique Maven snapshot artifact request
 * or a request with [RELEASE]/[INTEGRATION] place holders for latest release or latest integration version respectively
 * from local and cache repositories (from remotes only works for maven repos by analyzing the remote maven-metadata).
 *
 * @author Shay Yaakov
 */
public class LocalLatestVersionResolver extends LatestVersionResolver {
    private static final Logger log = LoggerFactory.getLogger(LocalLatestVersionResolver.class);

    private final MavenVersionComparator mavenVersionComparator = new MavenVersionComparator();

    @Override
    protected InternalRequestContext getRequestContext(InternalRequestContext requestContext, Repo repo,
            ModuleInfo originalModuleInfo) {
        if (!(repo.isLocal())) {
            return requestContext;
        }

        String path = requestContext.getResourcePath();
        if (repo.getDescriptor().isMavenRepoLayout() && MavenNaming.isNonUniqueSnapshot(path)) {
            requestContext = getMavenLatestSnapshotRequestContext(requestContext, repo, originalModuleInfo);
        } else {
            boolean searchForReleaseVersion = StringUtils.contains(path, "[RELEASE]");
            boolean searchForIntegrationVersion = StringUtils.contains(path, "[INTEGRATION]");
            if (searchForReleaseVersion || searchForIntegrationVersion) {
                requestContext = getLatestVersionRequestContext(requestContext, (StoringRepo) repo,
                        originalModuleInfo, searchForReleaseVersion);
            }
        }

        return requestContext;
    }

    private InternalRequestContext getLatestVersionRequestContext(InternalRequestContext requestContext,
            StoringRepo repo, ModuleInfo originalModuleInfo, boolean searchForReleaseVersion) {
        RepoLayout repoLayout = repo.getDescriptor().getRepoLayout();
        ModuleInfo baseRevisionModule = getBaseRevisionModuleInfo(originalModuleInfo);

        VersionsRetriever retriever =
                searchForReleaseVersion ? new ReleaseVersionsRetriever() : new SnapshotVersionsRetriever();
        String baseArtifactPath = ModuleInfoUtils.constructArtifactPath(baseRevisionModule, repoLayout, false);
        JcrTreeNode artifactSearchNode = retriever.getTreeNode(repo, repoLayout, baseArtifactPath, true);
        TreeMultimap<Calendar, VfsItem> versionsItems = null;
        if (artifactSearchNode != null) {
            versionsItems = retriever.collectVersionsItems(repo, artifactSearchNode);
        }

        if (repoLayout.isDistinctiveDescriptorPathPattern()) {
            String baseDescriptorPath = ModuleInfoUtils.constructDescriptorPath(baseRevisionModule, repoLayout, false);
            if (!baseDescriptorPath.equals(baseArtifactPath)) {
                JcrTreeNode descriptorNode = retriever.getTreeNode(repo, repoLayout, baseDescriptorPath, true);
                if (descriptorNode != null) {
                    versionsItems = retriever.collectVersionsItems(repo, descriptorNode);
                }
            }
        }

        if (versionsItems != null) {
            if (searchForReleaseVersion && !ConstantValues.requestSearchLatestReleaseByDateCreated.getBoolean()) {
                return getRequestContentForReleaseByVersion(versionsItems.values(), repo, requestContext,
                        originalModuleInfo);
            } else {
                return getRequestContextFromMap(versionsItems, repo, requestContext, originalModuleInfo,
                        searchForReleaseVersion);
            }
        } else {
            return requestContext;
        }
    }

    private InternalRequestContext getRequestContentForReleaseByVersion(Collection<VfsItem> vfsItems,
            StoringRepo repo, InternalRequestContext requestContext, ModuleInfo originalModuleInfo) {
        List<VfsItem> itemsList = Lists.newArrayList(vfsItems);
        String originalPath = requestContext.getResourcePath();
        RepositoryService repositoryService = getRepositoryService();
        ModuleInfo latestModuleInfo = null;
        String latestArtifactPath = null;
        for (VfsItem item : itemsList) {
            if (item.isFile()) {
                ModuleInfo itemModuleInfo = repositoryService.getItemModuleInfo(item.getRepoPath());
                if (itemModuleInfo.isValid()) {
                    if (areModuleInfosTheSame(originalModuleInfo, itemModuleInfo) && isPropertiesMatch(item,
                            requestContext.getProperties())) {
                        ModuleInfo resultModuleInfo = getLatestModuleInfo(itemModuleInfo, latestModuleInfo);
                        if (!resultModuleInfo.equals(latestModuleInfo)) {
                            latestModuleInfo = resultModuleInfo;
                            latestArtifactPath = item.getRepoPath().getPath();
                        }
                    }
                }
            }
        }

        String metadataName = null;
        if (NamingUtils.isMetadata(originalPath)) {
            metadataName = NamingUtils.getMetadataName(originalPath);
        }
        if (StringUtils.isNotBlank(metadataName)) {
            latestArtifactPath = NamingUtils.getMetadataPath(latestArtifactPath, metadataName);
        }
        return translateRepoRequestContext(requestContext, repo, latestArtifactPath);
    }

    private InternalRequestContext getRequestContextFromMap(TreeMultimap<Calendar, VfsItem> versionsItems,
            StoringRepo repo, InternalRequestContext requestContext, ModuleInfo originalModuleInfo,
            boolean searchForReleaseVersion) {
        String originalPath = requestContext.getResourcePath();
        SortedSet<Calendar> keySet = versionsItems.keySet();
        Calendar[] calendarKeys = keySet.toArray(new Calendar[keySet.size()]);
        RepositoryService repositoryService = ContextHelper.get().getRepositoryService();
        for (int calendarKeyIdx = calendarKeys.length - 1; calendarKeyIdx >= 0; calendarKeyIdx--) {
            SortedSet<VfsItem> vfsItems = versionsItems.get(calendarKeys[calendarKeyIdx]);
            VfsItem[] vfsItemsArray = vfsItems.toArray(new VfsItem[vfsItems.size()]);
            for (int vfsItemIdx = vfsItemsArray.length - 1; vfsItemIdx >= 0; vfsItemIdx--) {
                VfsItem item = vfsItemsArray[vfsItemIdx];
                if (item.isFile()) {
                    ModuleInfo itemModuleInfo = repositoryService.getItemModuleInfo(item.getRepoPath());
                    boolean isIntegration = itemModuleInfo.isIntegration();
                    boolean matchReleaseSearch = searchForReleaseVersion && !isIntegration;
                    boolean matchIntegrationSearch = !searchForReleaseVersion && isIntegration;
                    if (itemModuleInfo.isValid() && (matchReleaseSearch || matchIntegrationSearch)) {
                        if (areModuleInfosTheSame(originalModuleInfo, itemModuleInfo) && isPropertiesMatch(item,
                                requestContext.getProperties())) {
                            String metadataName = null;
                            if (NamingUtils.isMetadata(originalPath)) {
                                metadataName = NamingUtils.getMetadataName(originalPath);
                            }
                            String artifactTranslatedPath = item.getRepoPath().getPath();
                            if (StringUtils.isNotBlank(metadataName)) {
                                artifactTranslatedPath = NamingUtils.getMetadataPath(artifactTranslatedPath,
                                        metadataName);
                            }
                            return translateRepoRequestContext(requestContext, repo, artifactTranslatedPath);
                        }
                    }
                }
            }
        }

        return requestContext;
    }

    private boolean isPropertiesMatch(VfsItem<?, ?> fsItem, Properties requestProps) {
        if (requestProps == null || requestProps.isEmpty()) {
            return true;
        }
        Properties nodeProps = fsItem.getMetadata(Properties.class);
        if (nodeProps == null) {
            return true;
        }
        Properties.MatchResult result = nodeProps.matchQuery(requestProps);
        return !Properties.MatchResult.CONFLICT.equals(result);
    }

    private ModuleInfo getBaseRevisionModuleInfo(ModuleInfo deployedModuleInfo) {
        return new ModuleInfoBuilder().organization(deployedModuleInfo.getOrganization()).
                module(deployedModuleInfo.getModule()).baseRevision(deployedModuleInfo.getBaseRevision()).build();
    }

    private InternalRequestContext getMavenLatestSnapshotRequestContext(InternalRequestContext requestContext,
            Repo repo, ModuleInfo originalModuleInfo) {
        LocalRepoDescriptor repoDescriptor = (LocalRepoDescriptor) repo.getDescriptor();
        if (repoDescriptor.getSnapshotVersionBehavior().equals(SnapshotVersionBehavior.NONUNIQUE)) {
            return requestContext;
        }

        String path = requestContext.getResourcePath();
        String parentPath = PathUtils.getParent(path);
        RepoPath parentRepoPath = RepoPathFactory.create(repo.getKey(), parentPath);
        boolean isDeployerBehavior = SnapshotVersionBehavior.DEPLOYER.equals(
                repoDescriptor.getSnapshotVersionBehavior());
        String artifactPath = getLatestArtifactPath(parentRepoPath, originalModuleInfo, isDeployerBehavior,
                requestContext.getProperties());
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
     * @param properties         the original request properties (can be null)
     * @return a path to the latest unique artifact (null if not found)
     */
    private String getLatestArtifactPath(RepoPath parentRepoPath, ModuleInfo originalModuleInfo,
            boolean isDeployerBehavior, Properties requestProperties) {
        RepositoryService repositoryService = getRepositoryService();
        ModuleInfo latestModuleInfo = null;
        long latestLastModified = 0;
        String latestArtifactPath = null;
        BrowsableItemCriteria criteria = new BrowsableItemCriteria.Builder(parentRepoPath)
                .requestProperties(requestProperties)
                .includeChecksums(false)
                .includeRemoteResources(false)
                .build();
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
        boolean releaseCondition = StringUtils.equals(originalModuleInfo.getOrganization(),
                moduleInfo.getOrganization())
                && StringUtils.equals(originalModuleInfo.getModule(), moduleInfo.getModule())
                && StringUtils.equals(originalModuleInfo.getClassifier(), moduleInfo.getClassifier())
                && StringUtils.equals(originalExtWithoutMetadata, moduleInfo.getExt());

        boolean integrationCondition = releaseCondition
                && StringUtils.equals(originalModuleInfo.getBaseRevision(), moduleInfo.getBaseRevision());

        return originalModuleInfo.isIntegration() ? integrationCondition : releaseCondition;
    }
}
