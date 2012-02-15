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

package org.artifactory.search.deployable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.module.ModuleInfoBuilder;
import org.artifactory.api.module.VersionUnit;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.deployable.VersionUnitSearchControls;
import org.artifactory.api.search.deployable.VersionUnitSearchResult;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.PathFactory;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.sapi.data.VfsNode;
import org.artifactory.sapi.data.VfsNodeType;
import org.artifactory.sapi.search.VfsQueryResult;
import org.artifactory.sapi.search.VfsRepoQuery;
import org.artifactory.search.SearcherBase;

import java.util.Set;

/**
 * Holds the version unit search logic
 *
 * @author Noam Y. Tenne
 */
public class VersionUnitSearcher extends SearcherBase<VersionUnitSearchControls, VersionUnitSearchResult> {

    @Override
    public ItemSearchResults<VersionUnitSearchResult> doSearch(VersionUnitSearchControls controls) {
        RepoPath pathToSearch = controls.getPathToSearchWithin();

        VfsRepoQuery repoQuery = createRepoQuery(controls);
        repoQuery.setSingleRepoKey(pathToSearch.getRepoKey());
        repoQuery.addRelativePathFilter(pathToSearch.getPath());
        repoQuery.addAllSubPathFilter();
        repoQuery.setNodeTypeFilter(VfsNodeType.FILE);
        boolean limit = controls.isLimitSearchResults();
        VfsQueryResult queryResult = repoQuery.execute(limit);
        Multimap<ModuleInfo, RepoPath> moduleInfoToRepoPaths = HashMultimap.create();
        Repo repo = getRepoService().repositoryByKey(pathToSearch.getRepoKey());
        PathFactory pathFactory = PathFactoryHolder.get();
        for (VfsNode node : queryResult.getNodes()) {
            RepoPath fileRepoPath = pathFactory.getRepoPath(node.absolutePath());
            ModuleInfo moduleInfo = repo.getItemModuleInfo(fileRepoPath.getPath());
            if (moduleInfo.isValid()) {
                ModuleInfo stripped = stripModuleInfoFromUnnecessaryData(moduleInfo);
                moduleInfoToRepoPaths.put(stripped, fileRepoPath);
            }
        }
        Set<VersionUnitSearchResult> results = getVersionUnitResults(moduleInfoToRepoPaths);
        return new ItemSearchResults<VersionUnitSearchResult>(Lists.newArrayList(results), queryResult.getCount());
    }

    private Set<VersionUnitSearchResult> getVersionUnitResults(Multimap<ModuleInfo, RepoPath> moduleInfoToRepoPaths) {
        Set<VersionUnitSearchResult> searchResults = Sets.newHashSet();

        for (ModuleInfo moduleInfo : moduleInfoToRepoPaths.keySet()) {

            searchResults.add(new VersionUnitSearchResult(
                    new VersionUnit(moduleInfo, Sets.<RepoPath>newHashSet(moduleInfoToRepoPaths.get(moduleInfo)))));
        }

        return searchResults;
    }

    private ModuleInfo stripModuleInfoFromUnnecessaryData(ModuleInfo moduleInfo) {
        ModuleInfoBuilder moduleInfoBuilder = new ModuleInfoBuilder().organization(moduleInfo.getOrganization()).
                module(moduleInfo.getModule()).baseRevision(moduleInfo.getBaseRevision());
        if (moduleInfo.isIntegration()) {
            String pathRevision = moduleInfo.getFolderIntegrationRevision();
            String artifactRevision = moduleInfo.getFileIntegrationRevision();

            boolean hasPathRevision = StringUtils.isNotBlank(pathRevision);
            boolean hasArtifactRevision = StringUtils.isNotBlank(artifactRevision);

            if (hasPathRevision && !hasArtifactRevision) {
                moduleInfoBuilder.folderIntegrationRevision(pathRevision);
                moduleInfoBuilder.fileIntegrationRevision(pathRevision);
            } else if (!hasPathRevision && hasArtifactRevision) {
                moduleInfoBuilder.fileIntegrationRevision(artifactRevision);
                moduleInfoBuilder.folderIntegrationRevision(artifactRevision);
            } else {
                moduleInfoBuilder.folderIntegrationRevision(pathRevision);
                moduleInfoBuilder.fileIntegrationRevision(artifactRevision);
            }
        }
        return moduleInfoBuilder.build();
    }
}
