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

package org.artifactory.search.gavc;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.gavc.GavcSearchControls;
import org.artifactory.api.search.gavc.GavcSearchResult;
import org.artifactory.fs.FileInfo;
import org.artifactory.jcr.factory.VfsItemFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.PathFactory;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.sapi.data.VfsNode;
import org.artifactory.sapi.data.VfsNodeType;
import org.artifactory.sapi.search.VfsComparatorType;
import org.artifactory.sapi.search.VfsQueryResult;
import org.artifactory.sapi.search.VfsRepoQuery;
import org.artifactory.search.SearcherBase;
import org.artifactory.util.PathUtils;

import java.util.List;

import static org.artifactory.storage.StorageConstants.PROP_ARTIFACTORY_NAME;

/**
 * Holds the GAVC search logic
 *
 * @author Noam Tenne
 */
public class GavcSearcher extends SearcherBase<GavcSearchControls, GavcSearchResult> {

    @Override
    public ItemSearchResults<GavcSearchResult> doSearch(GavcSearchControls controls) {
        //Validate and escape all input values
        String groupInput = escapeGroupPath(controls.getGroupId());
        boolean groupContainsWildCard = inputContainsWildCard(groupInput);

        VfsRepoQuery query = createRepoQuery(controls);
        query.setNodeTypeFilter(VfsNodeType.FILE);

        //Build search path from inputted group
        if (groupContainsWildCard) {
            query.addAllSubPathFilter();
        }
        query.addRelativePathFilter(groupInput);
        if (groupContainsWildCard) {
            query.addAllSubPathFilter();
        }
        query.addPathFilters(controls.getArtifactId(), controls.getVersion());

        String classifier = controls.getClassifier();
        if (!StringUtils.isBlank(classifier)) {
            query.addCriterion(PROP_ARTIFACTORY_NAME, VfsComparatorType.CONTAINS, "*-" + classifier + "*");
        }

        boolean limit = controls.isLimitSearchResults();
        VfsQueryResult queryResult = query.execute(limit);
        List<GavcSearchResult> results = Lists.newArrayList();
        Iterable<VfsNode> nodes = queryResult.getNodes();
        PathFactory pathFactory = PathFactoryHolder.get();
        for (VfsNode node : nodes) {
            if (limit && results.size() >= getMaxResults()) {
                break;
            }
            RepoPath repoPath = pathFactory.getRepoPath(node.absolutePath());

            String repoKey = repoPath.getRepoKey();
            LocalRepo localRepo = getRepoService().localOrCachedRepositoryByKey(repoKey);
            if (!isResultAcceptable(repoPath, localRepo)) {
                continue;
            }

            FileInfo fileInfo = VfsItemFactory.createFileInfoProxy(repoPath);

            ModuleInfo moduleInfo = localRepo.getItemModuleInfo(repoPath.getPath());

            if (moduleInfo.isValid()) {
                results.add(new GavcSearchResult(fileInfo, moduleInfo));
            }
        }

        return new ItemSearchResults<GavcSearchResult>(results, queryResult.getCount());
    }

    /**
     * Swaps all backward-slashes ('\') to forward ones ('/'). Removes leading and trailing slashes (if any), Replaces
     * all periods ('.') to forward slashes.
     *
     * @param groupInput The inputted group path
     * @return String - Group path after escape
     */
    private String escapeGroupPath(String groupInput) {
        if (StringUtils.isBlank(groupInput)) {
            groupInput = "";
        }
        groupInput = groupInput.replace('\\', '/');
        groupInput = PathUtils.trimSlashes(groupInput).toString();
        groupInput = groupInput.replace('.', '/');
        return groupInput;
    }
}