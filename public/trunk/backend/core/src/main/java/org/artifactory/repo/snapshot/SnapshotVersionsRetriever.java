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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.module.ModuleInfoBuilder;
import org.artifactory.api.module.regex.NamedPattern;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.jcr.fs.JcrTreeNode;
import org.artifactory.jcr.fs.JcrTreeNodeFileFilter;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.fs.VfsItem;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Collects integration versions items under a given root node.
 *
 * @author Shay Yaakov
 */
public class SnapshotVersionsRetriever extends VersionsRetriever {
    private static final Logger log = LoggerFactory.getLogger(SnapshotVersionsRetriever.class);

    private Map<String, Calendar> integrationCreationMap = Maps.newHashMap();
    private Multimap<Calendar, ModuleInfo> folderIntegrationMap = HashMultimap.create();

    @Override
    public TreeMultimap<Calendar, VfsItem> collectVersionsItems(StoringRepo repo, JcrTreeNode node) {
        internalCollectVersionsItems(repo, node);
        return versionsItems;
    }

    private void internalCollectVersionsItems(StoringRepo repo, JcrTreeNode node) {
        if (node.isFolder()) {
            Set<JcrTreeNode> children = node.getChildren();
            for (JcrTreeNode child : children) {
                internalCollectVersionsItems(repo, child);
            }
        } else {
            RepoPath itemRepoPath = node.getRepoPath();
            VfsItem fsItem;
            try {
                fsItem = repo.getJcrFsItem(itemRepoPath);
            } catch (RepositoryRuntimeException e) {
                log.warn("Could not get file at '{}' ({}). Skipping.", itemRepoPath, e.getMessage());
                if (log.isDebugEnabled()) {
                    log.error("Error while getting file.", e);
                }
                return;
            }
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
                    Collection<VfsItem> itemsToRelocate = versionsItems.removeAll(existingIntegrationCreation);
                    versionsItems.putAll(itemCreated, itemsToRelocate);
                    versionsItems.put(itemCreated, fsItem);

                    Collection<ModuleInfo> folderIntegrationsToRelocate =
                            folderIntegrationMap.removeAll(existingIntegrationCreation);
                    folderIntegrationMap.putAll(itemCreated, folderIntegrationsToRelocate);
                    if (folderIntegrationModuleInfo.isValid()) {
                        folderIntegrationMap.put(itemCreated, folderIntegrationModuleInfo);
                    }
                } else {

                    //Child's creation date isn't newer, just add it
                    versionsItems.put(existingIntegrationCreation, fsItem);
                    if (folderIntegrationModuleInfo.isValid()) {
                        folderIntegrationMap.put(existingIntegrationCreation, folderIntegrationModuleInfo);
                    }
                }
            } else {

                //No reference exists yet, create one
                integrationCreationMap.put(uniqueRevision, itemCreated);
                versionsItems.put(itemCreated, fsItem);
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

    public Multimap<Calendar, ModuleInfo> getFolderIntegrationMap() {
        return folderIntegrationMap;
    }

    @Override
    public JcrTreeNodeFileFilter getFileFilter(StoringRepo repo, NamedPattern pattern) {
        return new IntegrationFileFilter(repo, pattern);
    }

    private static class IntegrationFileFilter implements JcrTreeNodeFileFilter {
        private final StoringRepo repo;

        private final NamedPattern pattern;

        public IntegrationFileFilter(StoringRepo repo, NamedPattern pattern) {
            this.repo = repo;
            this.pattern = pattern;
        }

        @Override
        public boolean acceptsFile(RepoPath repoPath) {
            String path = repoPath.getPath();
            if (!pattern.matcher(path).matches()) {
                return false;
            }
            ModuleInfo itemModuleInfo = repo.getItemModuleInfo(path);
            RepoLayout repoLayout = repo.getDescriptor().getRepoLayout();

            boolean integrationCondition = itemModuleInfo.isIntegration() &&
                    //Checks to make sure it is not a non-unique integration mixed with unique integrations
                    (StringUtils.equals(repoLayout.getFolderIntegrationRevisionRegExp(),
                            repoLayout.getFileIntegrationRevisionRegExp()) ||
                            !StringUtils.equals(itemModuleInfo.getFolderIntegrationRevision(),
                                    itemModuleInfo.getFileIntegrationRevision()));

            //Make sure this file's module info is valid and is actually an integration version
            return itemModuleInfo.isValid() && integrationCondition;
        }
    }
}
