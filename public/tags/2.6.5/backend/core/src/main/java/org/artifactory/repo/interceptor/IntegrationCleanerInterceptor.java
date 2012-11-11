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

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.module.ModuleInfoBuilder;
import org.artifactory.api.module.ModuleInfoUtils;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.jcr.factory.VfsItemFactory;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.fs.JcrTreeNode;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.interceptor.storage.StorageInterceptorAdapter;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.repo.snapshot.SnapshotVersionsRetriever;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author yoav
 */
public class IntegrationCleanerInterceptor extends StorageInterceptorAdapter {
    private static final Logger log = LoggerFactory.getLogger(IntegrationCleanerInterceptor.class);

    /**
     * Holds all the snapshot folders currently being cleaned up (1.0-SNAPSHOT, 1.1-SNAPSHOT etc)
     * So there will be only one worker which cleans them up to avoid locking issues
     */
    private ConcurrentMap<String, String> foldersInTransit = new ConcurrentHashMap<String, String>();

    /**
     * Cleanup old snapshots etc.
     *
     * @param fsItem
     * @param statusHolder
     */
    @Override
    public void afterCreate(VfsItem fsItem, MutableStatusHolder statusHolder) {
        final StoringRepo repo = (StoringRepo) VfsItemFactory.getStoringRepo(fsItem);
        if (!(repo instanceof LocalRepo) || fsItem.isDirectory()) {
            return;
        }

        //If the resource has no size specified, this will update the size
        //(this can happen if we established the resource based on a HEAD request that failed to
        //return the content-length).

        int maxUniqueSnapshots = ((LocalRepo) repo).getMaxUniqueSnapshots();
        if (maxUniqueSnapshots > 0) {
            JcrFile file = (JcrFile) fsItem;
            String parentPath = file.getParent();
            String currentCleanup = foldersInTransit.putIfAbsent(parentPath, parentPath);
            if (currentCleanup == null) {
                try {
                    log.debug("Adding parent folder '{}' to in transit map.", parentPath);
                    ModuleInfo deployedModuleInfo = repo.getItemModuleInfo(file.getRelativePath());
                    if (!deployedModuleInfo.isValid() || !deployedModuleInfo.isIntegration()) {
                        return;
                    }

                    conditionalCleanup(repo, maxUniqueSnapshots, deployedModuleInfo);
                } finally {
                    log.debug("Removing '{}' from in transit map.", parentPath);
                    foldersInTransit.remove(parentPath);
                }
            }
        }
    }

    private void conditionalCleanup(StoringRepo repo, int maxUniqueSnapshots, ModuleInfo deployedModuleInfo) {
        RepoLayout repoLayout = repo.getDescriptor().getRepoLayout();
        ModuleInfo baseRevisionModule = getBaseRevisionModuleInfo(deployedModuleInfo);
        String baseArtifactPath = ModuleInfoUtils.constructArtifactPath(baseRevisionModule, repoLayout, false);
        SnapshotVersionsRetriever retriever = new SnapshotVersionsRetriever();
        JcrTreeNode artifactSearchNode = retriever.getTreeNode(repo, repoLayout, baseArtifactPath, false);
        TreeMultimap<Calendar, VfsItem> cleanupCandidates = TreeMultimap.create();
        if (artifactSearchNode != null) {
            cleanupCandidates = retriever.collectVersionsItems(repo, artifactSearchNode);
        }

        if (repoLayout.isDistinctiveDescriptorPathPattern()) {
            String baseDescriptorPath = ModuleInfoUtils.constructDescriptorPath(baseRevisionModule, repoLayout, false);
            if (!baseDescriptorPath.equals(baseArtifactPath)) {
                JcrTreeNode descriptorSearchNode = retriever.getTreeNode(repo, repoLayout, baseDescriptorPath, false);
                if (descriptorSearchNode != null) {
                    cleanupCandidates = retriever.collectVersionsItems(repo, descriptorSearchNode);
                }
            }
        }

        Multimap<Calendar, ModuleInfo> folderIntegrationMap = retriever.getFolderIntegrationMap();
        while (cleanupCandidates.keySet().size() > maxUniqueSnapshots) {
            performCleanup(retriever, repo, cleanupCandidates, folderIntegrationMap);
        }
    }

    private ModuleInfo getBaseRevisionModuleInfo(ModuleInfo deployedModuleInfo) {
        return new ModuleInfoBuilder().organization(deployedModuleInfo.getOrganization()).
                module(deployedModuleInfo.getModule()).baseRevision(deployedModuleInfo.getBaseRevision()).build();
    }

    private void performCleanup(SnapshotVersionsRetriever retriever, StoringRepo repo,
            TreeMultimap<Calendar, VfsItem> cleanupCandidates, Multimap<Calendar, ModuleInfo> folderIntegrationMap) {
        Calendar first = cleanupCandidates.keySet().first();

        SortedSet<VfsItem> itemsToRemove = cleanupCandidates.removeAll(first);
        for (VfsItem itemToRemove : itemsToRemove) {
            bruteForceDeleteAndReplicateEvent(itemToRemove);
            log.info("Removed old unique snapshot '{}'.", itemToRemove.getRelativePath());
        }

        Collection<ModuleInfo> integrationFolderModuleInfos = folderIntegrationMap.get(first);
        for (ModuleInfo integrationFolderModuleInfo : integrationFolderModuleInfos) {
            prepareIntegrationFolderForCleanup(retriever, integrationFolderModuleInfo, repo);
        }
    }

    private void prepareIntegrationFolderForCleanup(SnapshotVersionsRetriever retriever,
            ModuleInfo integrationFolderModuleInfo, StoringRepo repo) {
        RepoLayout repoLayout = repo.getDescriptor().getRepoLayout();

        String baseArtifactPath = ModuleInfoUtils.constructArtifactPath(integrationFolderModuleInfo, repoLayout, false);

        cleanIntegrationFolderIfNeeded(retriever, repo, baseArtifactPath);

        if (repoLayout.isDistinctiveDescriptorPathPattern()) {

            String baseDescriptorPath = ModuleInfoUtils.constructDescriptorPath(integrationFolderModuleInfo, repoLayout,
                    false);
            if (!baseArtifactPath.equals(baseDescriptorPath) &&
                    baseDescriptorPath.contains(integrationFolderModuleInfo.getFolderIntegrationRevision())) {

                cleanIntegrationFolderIfNeeded(retriever, repo, baseArtifactPath);
            }
        }
    }

    private void cleanIntegrationFolderIfNeeded(SnapshotVersionsRetriever retriever, StoringRepo repo,
            String basePath) {
        JcrTreeNode folderTreeNode = retriever.getTreeNode(repo, repo.getDescriptor().getRepoLayout(), basePath, false);
        if ((folderTreeNode != null) && folderTreeNode.isFolder()) {
            if (isFolderTreeNodeEmptyOfFiles(folderTreeNode)) {
                JcrFsItem itemToRemove = repo.getJcrFsItem(folderTreeNode.getRepoPath());
                bruteForceDeleteAndReplicateEvent(itemToRemove);
            }
        }
    }

    private boolean isFolderTreeNodeEmptyOfFiles(JcrTreeNode folderTreeNode) {
        Set<JcrTreeNode> children = folderTreeNode.getChildren();
        for (JcrTreeNode child : children) {
            if (child.isFolder()) {
                isFolderTreeNodeEmptyOfFiles(child);
            } else {
                return false;
            }
        }

        return true;
    }

    private void bruteForceDeleteAndReplicateEvent(VfsItem item) {
        if (item != null) {
            InternalContextHelper.get().getJcrRepoService().bruteForceDeleteAndReplicateEvent(item);
        }
    }
}