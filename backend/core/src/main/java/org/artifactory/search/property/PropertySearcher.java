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

package org.artifactory.search.property;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.property.PropertySearchControls;
import org.artifactory.api.search.property.PropertySearchResult;
import org.artifactory.fs.ItemInfo;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.PathFactory;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.sapi.data.VfsNode;
import org.artifactory.sapi.data.VfsNodeType;
import org.artifactory.sapi.search.VfsComparatorType;
import org.artifactory.sapi.search.VfsQueryResult;
import org.artifactory.sapi.search.VfsRepoQuery;
import org.artifactory.search.SearcherBase;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.artifactory.storage.StorageConstants.NODE_ARTIFACTORY_METADATA;
import static org.artifactory.storage.StorageConstants.NODE_ARTIFACTORY_PROPERTIES;

/**
 * @author Noam Tenne
 */
public class PropertySearcher extends SearcherBase<PropertySearchControls, PropertySearchResult> {

    // /repositories/libs-releases-local/artifactory:metadata/properties/artifactory:properties[set1.prop1 = 'val1']

    private final static String[] PROPERTY_NODE_PATH = {
            NODE_ARTIFACTORY_METADATA, Properties.ROOT, NODE_ARTIFACTORY_PROPERTIES};
    private final static String FULL_PROPERTY_PATH;

    static {
        StringBuilder builder = new StringBuilder();
        for (String s : PROPERTY_NODE_PATH) {
            builder.append("/").append(s);
        }
        FULL_PROPERTY_PATH = builder.toString();
    }

    @Override
    public ItemSearchResults<PropertySearchResult> doSearch(PropertySearchControls controls) {
        LinkedHashSet<PropertySearchResult> globalResults = Sets.newLinkedHashSet();

        ////Get all open property keys and search through them
        Set<String> openPropertyKeys = controls.getPropertyKeysByOpenness(PropertySearchControls.OPEN);
        long totalResultCount = executeOpenPropSearch(controls, openPropertyKeys, globalResults);

        //Get all closed property keys and search through them
        Set<String> closedPropertyKeys = controls.getPropertyKeysByOpenness(PropertySearchControls.CLOSED);
        totalResultCount += executeClosedPropSearch(controls, closedPropertyKeys, globalResults);

        //Return global results list
        return new ItemSearchResults<PropertySearchResult>(new ArrayList<PropertySearchResult>(globalResults),
                totalResultCount);
    }

    /**
     * Searches and aggregates results of open properties
     *
     * @param openPropertyKeys Keys to search through
     */
    private long executeOpenPropSearch(PropertySearchControls controls,
            Set<String> openPropertyKeys,
            Set<PropertySearchResult> globalResults) {
        long resultCount = 0;
        for (String key : openPropertyKeys) {
            Set<String> values = controls.get(key);
            for (String value : values) {
                //*[jcr:contains(@myapp:title, 'JSR 170')]
                VfsRepoQuery repoQuery = createRepoQuery(controls);
                repoQuery.addAllSubPathFilter();
                repoQuery.setNodeTypeFilter(VfsNodeType.UNSTRUCTURED);
                repoQuery.addSmartEqualCriterion(key, value);
                VfsQueryResult queryResult = repoQuery.execute(controls.isLimitSearchResults());
                resultCount += processResults(controls, queryResult, globalResults);
            }
        }
        return resultCount;
    }

    /**
     * Searches and aggregates results of closed properties
     *
     * @param closedPropertyKeys Keys to search through
     */
    @SuppressWarnings({"WhileLoopReplaceableByForEach"})
    private long executeClosedPropSearch(PropertySearchControls controls,
            Set<String> closedPropertyKeys,
            Set<PropertySearchResult> globalResults) {
        if (closedPropertyKeys.isEmpty()) {
            return 0;
        }
        VfsRepoQuery repoQuery = createRepoQuery(controls);
        repoQuery.setNodeTypeFilter(VfsNodeType.UNSTRUCTURED);

        // TODO: Should support any boolean
        Iterator<String> keyIterator = closedPropertyKeys.iterator();
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            Iterator<String> valueIterator = controls.get(key).iterator();

            while (valueIterator.hasNext()) {
                String value = valueIterator.next();
                repoQuery.addCriterion(key, VfsComparatorType.EQUAL, value);
            }
        }
        VfsQueryResult queryResult = repoQuery.execute(controls.isLimitSearchResults());
        return processResults(controls, queryResult, globalResults);
    }

    /**
     * Processes, filters and aggregates query results into the global results list The filtering creates an AND like
     * action. The first batch of results for the session automatically gets put in the global results list. Any batch
     * of results after that is compared with the global list. If the new batch of results contains a result that does
     * not exist in the global list, we discard it (Means the result does not fall under both searches, thus failing the
     * AND requirement
     *
     * @param queryResult Result object
     * @throws RepositoryException
     */
    private long processResults(PropertySearchControls controls,
            VfsQueryResult queryResult,
            Set<PropertySearchResult> globalResults) {
        /**
         * If the global results is empty (either first query made, or there were no results from queries executed up
         * until now
         */
        boolean noGlobalResults = globalResults.isEmpty();
        boolean limit = controls.isLimitSearchResults();

        PathFactory pathFactory = PathFactoryHolder.get();
        long resultCount = 0L;
        List<PropertySearchResult> currentSearchResults = Lists.newArrayList();
        for (VfsNode vfsNode : queryResult.getNodes()) {
            if (limit && globalResults.size() >= getMaxResults()) {
                break;
            }
            String path = vfsNode.absolutePath();

            //Make sure the result is actually a property
            if (path.contains(FULL_PROPERTY_PATH)) {
                String artifactPath = path.substring(0, path.lastIndexOf(FULL_PROPERTY_PATH));
                VfsNode node = getVfsDataService().findByPath(artifactPath);
                if (node == null) {
                    // Was deleted in the mean time
                    continue;
                }
                RepoPath repoPath = pathFactory.getRepoPath(node.absolutePath());
                if (!isResultAcceptable(repoPath)) {
                    continue;
                }

                ItemInfo itemInfo = getProxyItemInfo(node);
                PropertySearchResult searchResult = new PropertySearchResult(itemInfo);

                //Make sure that we don't get any double results
                if (!currentSearchResults.contains(searchResult)) {
                    resultCount++;
                    currentSearchResults.add(searchResult);
                }
            }
        }

        /**
         * If the global results list is empty, simply add all our results to set it as a comparison standard for the
         * next set of results
         */
        if (noGlobalResults) {
            globalResults.addAll(currentSearchResults);
        } else {
            //Create a copy of the global results so we can iterate and remove at the same time
            ArrayList<PropertySearchResult> globalCopy = new ArrayList<PropertySearchResult>(globalResults);
            for (PropertySearchResult globalResult : globalCopy) {
                //If the received results do not exist in the global results, discard them
                if (!currentSearchResults.contains(globalResult)) {
                    globalResults.remove(globalResult);
                    resultCount--;
                }
            }
        }
        return resultCount;
    }
}