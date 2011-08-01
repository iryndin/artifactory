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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.module.ModuleInfoBuilder;
import org.artifactory.api.module.ModuleInfoUtils;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.fs.JcrTreeNode;
import org.artifactory.jcr.fs.JcrTreeNodeFileFilter;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.util.RepoLayoutUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Pattern;

/**
 * @author yoav
 */
public class IntegrationCleanerInterceptor extends StorageInterceptorAdapter {
    private static final Logger log = LoggerFactory.getLogger(IntegrationCleanerInterceptor.class);

    @Inject
    private JcrService jcrService;

    @Inject
    private RepositoryService repositoryService;

    /**
     * Cleanup old snapshots etc.
     *
     * @param fsItem
     * @param statusHolder
     */
    @Override
    public void afterCreate(JcrFsItem fsItem, MutableStatusHolder statusHolder) {
        final StoringRepo repo = fsItem.getRepo();
        if (!(repo instanceof LocalRepo) || fsItem.isDirectory()) {
            return;
        }

        //If the resource has no size specified, this will update the size
        //(this can happen if we established the resource based on a HEAD request that failed to
        //return the content-length).

        int maxUniqueSnapshots = ((LocalRepo) repo).getMaxUniqueSnapshots();
        if (maxUniqueSnapshots > 0) {

            JcrFile file = (JcrFile) fsItem;

            ModuleInfo deployedModuleInfo = repo.getItemModuleInfo(file.getRelativePath());

            if (!deployedModuleInfo.isValid() || !deployedModuleInfo.isIntegration()) {
                return;
            }

            TreeMultimap<Calendar, JcrFsItem> cleanupCandidates = TreeMultimap.create();
            Map<String, Calendar> integrationCreationMap = Maps.newHashMap();
            Multimap<Calendar, ModuleInfo> folderIntegrationMap = HashMultimap.create();

            RepoLayout repoLayout = file.getRepo().getDescriptor().getRepoLayout();

            ModuleInfo baseRevisionModule = getBaseRevisionModuleInfo(deployedModuleInfo);

            String baseArtifactPath = ModuleInfoUtils.constructArtifactPath(baseRevisionModule, repoLayout, false);

            JcrTreeNode artifactSearchNode = getTreeNode(repo, repoLayout, baseArtifactPath);

            if (artifactSearchNode != null) {
                collectItemsToClean(repo, cleanupCandidates, integrationCreationMap, folderIntegrationMap,
                        artifactSearchNode);
            }

            if (repoLayout.isDistinctiveDescriptorPathPattern()) {

                String baseDescriptorPath = ModuleInfoUtils.constructDescriptorPath(baseRevisionModule, repoLayout,
                        false);
                if (!baseDescriptorPath.equals(baseArtifactPath)) {

                    JcrTreeNode descriptorSearchNode = getTreeNode(repo, repoLayout, baseDescriptorPath);
                    if (descriptorSearchNode != null) {
                        collectItemsToClean(repo, cleanupCandidates, integrationCreationMap, folderIntegrationMap,
                                descriptorSearchNode);
                    }
                }
            }

            while (cleanupCandidates.keySet().size() > maxUniqueSnapshots) {
                performCleanup(repo, cleanupCandidates, folderIntegrationMap);
            }
        }
    }

    private ModuleInfo getBaseRevisionModuleInfo(ModuleInfo deployedModuleInfo) {
        return new ModuleInfoBuilder().organization(deployedModuleInfo.getOrganization()).
                module(deployedModuleInfo.getModule()).baseRevision(deployedModuleInfo.getBaseRevision()).build();
    }

    private JcrTreeNode getTreeNode(StoringRepo repo, RepoLayout repoLayout, String itemPath) {
        RepoPath searchBasePath = getBaseRepoPathFromPartialItemPath(repo.getKey(), itemPath);

        String regEx = RepoLayoutUtils.generateRegExpFromPattern(repoLayout, itemPath, Lists.<String>newArrayList());
        Pattern pattern = Pattern.compile(regEx);
        FileFilter fileFilter = new FileFilter(repo, pattern);

        return jcrService.getTreeNode(searchBasePath, new MultiStatusHolder(), fileFilter);
    }

    private RepoPath getBaseRepoPathFromPartialItemPath(String repoKey, String itemPath) {
        StringBuilder searchBasePathBuilder = new StringBuilder();
        String[] pathTokens = itemPath.split("/");
        for (String pathToken : pathTokens) {
            if (!pathToken.contains("[") && !pathToken.contains("(")) {
                searchBasePathBuilder.append(pathToken).append("/");
            } else {
                break;
            }
        }
        return new RepoPathImpl(repoKey, searchBasePathBuilder.toString());
    }

    /**
     * Collects cleanup candidates from a layout that aggregates different snapshots under one folder
     */
    private void collectItemsToClean(StoringRepo repo, Multimap<Calendar, JcrFsItem> cleanupCandidates,
            Map<String, Calendar> integrationCreationMap, Multimap<Calendar, ModuleInfo> folderIntegrationMap,
            JcrTreeNode node) {

        if (node.isFolder()) {
            Set<JcrTreeNode> children = node.getChildren();
            for (JcrTreeNode child : children) {
                collectItemsToClean(repo, cleanupCandidates, integrationCreationMap, folderIntegrationMap, child);
            }
        } else {
            RepoPath itemRepoPath = node.getRepoPath();
            JcrFsItem fsItem = repo.getJcrFsItem(itemRepoPath);
            ModuleInfo itemModuleInfo = repo.getItemModuleInfo(itemRepoPath.getPath());

            ModuleInfo folderIntegrationModuleInfo = getFolderIntegrationModuleInfo(itemModuleInfo);
            Calendar itemCreated = node.getCreated();

            String uniqueRevision = itemModuleInfo.getFileIntegrationRevision();

            //If we already keep a creation date for this child's unique revision
            if (integrationCreationMap.containsKey(uniqueRevision)) {

                //If the current child's creation date precedes the existing one
                Calendar existingIntegrationCreation = integrationCreationMap.get(uniqueRevision);
                if (itemCreated.before(existingIntegrationCreation)) {

                    //Update the reference of all the children with the same unique integration
                    integrationCreationMap.put(uniqueRevision, itemCreated);
                    Collection<JcrFsItem> itemsToRelocate = cleanupCandidates.removeAll(existingIntegrationCreation);
                    cleanupCandidates.putAll(itemCreated, itemsToRelocate);
                    cleanupCandidates.put(itemCreated, fsItem);

                    Collection<ModuleInfo> folderIntegrationsToRelocate =
                            folderIntegrationMap.removeAll(existingIntegrationCreation);
                    folderIntegrationMap.putAll(itemCreated, folderIntegrationsToRelocate);
                    if (folderIntegrationModuleInfo.isValid()) {
                        folderIntegrationMap.put(itemCreated, folderIntegrationModuleInfo);
                    }
                } else {

                    //Child's creation date isn't newer, just add it
                    cleanupCandidates.put(existingIntegrationCreation, fsItem);
                    if (folderIntegrationModuleInfo.isValid()) {
                        folderIntegrationMap.put(existingIntegrationCreation, folderIntegrationModuleInfo);
                    }
                }
            } else {

                //No reference exists yet, create one
                integrationCreationMap.put(uniqueRevision, itemCreated);
                cleanupCandidates.put(itemCreated, fsItem);
                if (folderIntegrationModuleInfo.isValid()) {
                    folderIntegrationMap.put(itemCreated, folderIntegrationModuleInfo);
                }
            }
        }
    }

    private ModuleInfo getFolderIntegrationModuleInfo(ModuleInfo itemModuleInfo) {
        String folderIntegrationRevision = itemModuleInfo.getFolderIntegrationRevision();
        if (StringUtils.isBlank(folderIntegrationRevision) ||
                !folderIntegrationRevision.equals(itemModuleInfo.getFileIntegrationRevision())) {
            return new ModuleInfo();
        }

        return new ModuleInfoBuilder().organization(itemModuleInfo.getOrganization()).
                module(itemModuleInfo.getModule()).baseRevision(itemModuleInfo.getBaseRevision()).
                folderIntegrationRevision(folderIntegrationRevision).build();
    }

    private void performCleanup(StoringRepo repo, TreeMultimap<Calendar, JcrFsItem> cleanupCandidates,
            Multimap<Calendar, ModuleInfo> folderIntegrationMap) {
        Calendar first = cleanupCandidates.keySet().first();

        SortedSet<JcrFsItem> itemsToRemove = cleanupCandidates.removeAll(first);
        for (JcrFsItem itemToRemove : itemsToRemove) {
            itemToRemove.bruteForceDelete();
            log.info("Removed old unique snapshot '{}'.", itemToRemove.getRelativePath());
        }

        Collection<ModuleInfo> integrationFolderModuleInfos = folderIntegrationMap.get(first);
        for (ModuleInfo integrationFolderModuleInfo : integrationFolderModuleInfos) {
            prepareIntegrationFolderForCleanup(integrationFolderModuleInfo, repo);
        }
    }

    private void prepareIntegrationFolderForCleanup(ModuleInfo integrationFolderModuleInfo, StoringRepo repo) {
        RepoLayout repoLayout = repo.getDescriptor().getRepoLayout();

        String baseArtifactPath = ModuleInfoUtils.constructArtifactPath(integrationFolderModuleInfo, repoLayout, false);

        cleanIntegrationFolderIfNeeded(repo, baseArtifactPath);

        if (repoLayout.isDistinctiveDescriptorPathPattern()) {

            String baseDescriptorPath = ModuleInfoUtils.constructDescriptorPath(integrationFolderModuleInfo, repoLayout,
                    false);
            if (!baseArtifactPath.equals(baseDescriptorPath) &&
                    baseDescriptorPath.contains(integrationFolderModuleInfo.getFolderIntegrationRevision())) {

                cleanIntegrationFolderIfNeeded(repo, baseArtifactPath);
            }
        }
    }

    private void cleanIntegrationFolderIfNeeded(StoringRepo repo, String basePath) {
        JcrTreeNode folderTreeNode = getTreeNode(repo, repo.getDescriptor().getRepoLayout(), basePath);
        if ((folderTreeNode != null) && folderTreeNode.isFolder()) {
            if (isFolderTreeNodeEmptyOfFiles(folderTreeNode)) {
                repo.getJcrFsItem(folderTreeNode.getRepoPath()).bruteForceDelete();
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

    private static class FileFilter implements JcrTreeNodeFileFilter {

        private final StoringRepo repo;
        private final Pattern pattern;

        public FileFilter(StoringRepo repo, Pattern pattern) {
            this.repo = repo;
            this.pattern = pattern;
        }

        public boolean acceptsFile(RepoPath repoPath) {
            String path = repoPath.getPath();
            if (!pattern.matcher(path).matches()) {
                return false;
            }
            ModuleInfo itemModuleInfo = repo.getItemModuleInfo(path);
            RepoLayout repoLayout = repo.getDescriptor().getRepoLayout();

            //Make sure this file's module info is valid and is actually an integration version
            return itemModuleInfo.isValid() && itemModuleInfo.isIntegration() &&

                    //Checks to make sure it is not a non-unique integration mixed with unique integrations
                    (repoLayout.getFolderIntegrationRevisionRegExp().
                            equals(repoLayout.getFileIntegrationRevisionRegExp()) ||
                            !itemModuleInfo.getFolderIntegrationRevision().
                                    equals(itemModuleInfo.getFileIntegrationRevision()));
        }
    }
}