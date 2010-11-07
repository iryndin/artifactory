/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.ISO9075;
import org.artifactory.api.fs.DeployableUnit;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.deployable.DeployableUnitSearchControls;
import org.artifactory.api.search.deployable.DeployableUnitSearchResult;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.repo.RepoPath;
import org.artifactory.search.SearcherBase;
import org.artifactory.util.PathUtils;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.util.Set;

/**
 * Holds the deployable unit search logic
 *
 * @author Noam Y. Tenne
 */
public class DeployableUnitSearcher extends SearcherBase<DeployableUnitSearchControls, DeployableUnitSearchResult> {

    @Override
    public SearchResults<DeployableUnitSearchResult> doSearch(DeployableUnitSearchControls controls)
            throws RepositoryException {

        RepoPath pathToSearch = controls.getPathToSearchWithin();

        StringBuilder queryBuilder = getPathQueryBuilder(controls);

        //Add path
        if (!StringUtils.isEmpty(pathToSearch.getPath()) && PathUtils.hasText(pathToSearch.getPath())) {
            String relativePath = ISO9075.encodePath(pathToSearch.getPath());
            addElementsToSb(queryBuilder, relativePath);
        }

        queryBuilder.append(FORWARD_SLASH).append("element(*, ").append(JcrTypes.NT_ARTIFACTORY_FILE).
                append(") [jcr:contains(@").append(JcrTypes.PROP_ARTIFACTORY_NAME).append(", '*.pom')]");

        QueryResult queryResult = performQuery(controls.isLimitSearchResults(), queryBuilder.toString());
        Set<DeployableUnitSearchResult> results = Sets.newHashSet();
        NodeIterator nodeIterator = queryResult.getNodes();
        try {
            while (nodeIterator.hasNext()) {
                Node node = nodeIterator.nextNode();
                // this is the parent node of the pom files - the version folder
                Node versionNode = node.getParent();
                RepoPath folderRepoPath = JcrPath.get().getRepoPath(versionNode.getPath());
                DeployableUnit du = new DeployableUnit(folderRepoPath);
                results.add(new DeployableUnitSearchResult(du));
            }
        } catch (RepositoryException re) {
            handleNotFoundException(re);
        }
        return new SearchResults<DeployableUnitSearchResult>(Lists.newArrayList(results), nodeIterator.getSize());
    }
}