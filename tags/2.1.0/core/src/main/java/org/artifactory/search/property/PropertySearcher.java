/*
 * This file is part of Artifactory.
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

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.property.PropertySearchControls;
import org.artifactory.api.search.property.PropertySearchResult;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.search.SearcherBase;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Noam Tenne
 */
public class PropertySearcher extends SearcherBase<PropertySearchControls, PropertySearchResult> {

    // /repositories/libs-releases-local/artifactory:metadata/properties/artifactory:properties[set1.prop1 = 'val1']

    PropertySearchControls controls;

    //Global result list
    private final LinkedHashSet<PropertySearchResult> globalResults = Sets.newLinkedHashSet();

    //The path of the generic property node
    private String PROPERTY_NODE_PATH = "/jcr:root" + JcrPath.get().getRepoJcrRootPath() + "//" +
            JcrService.NODE_ARTIFACTORY_METADATA + "/properties/" + JcrService.NODE_ARTIFACTORY_PROPERTIES;

    //The xpath result ordering command
    private String QUERY_ORDERING = "order by jcr:score decending";

    @Override
    public SearchResults<PropertySearchResult> doSearch(PropertySearchControls controls) throws RepositoryException {
        this.controls = controls;

        //Get all open property keys and search through them
        Set<String> openPropertyKeys = controls.getPropertyKeysByOpenness(PropertySearchControls.OPEN);
        executeOpenPropSearch(openPropertyKeys);

        //Get all closed property keys and search through them
        Set<String> closedPropertyKeys = controls.getPropertyKeysByOpenness(PropertySearchControls.CLOSED);
        executeClosedPropSearch(closedPropertyKeys);

        //Return global results lisr
        return new SearchResults<PropertySearchResult>(new ArrayList<PropertySearchResult>(globalResults),
                globalResults.size());
    }

    /**
     * Searches and aggregates results of open properties
     *
     * @param openPropertyKeys Keys to search through
     * @throws RepositoryException
     */
    private void executeOpenPropSearch(Set<String> openPropertyKeys) throws RepositoryException {
        for (String key : openPropertyKeys) {
            Set<String> values = controls.get(key);
            for (String value : values) {

                StringBuilder queryBuilder = new StringBuilder();
                queryBuilder.append(PROPERTY_NODE_PATH);
                queryBuilder.append("[jcr:like(").append(key).append(",'%");

                //If no value is specified, search for all artifacts with the current key
                if (StringUtils.isNotBlank(value)) {
                    queryBuilder.append(value).append("%");
                }
                queryBuilder.append("')] ").append(QUERY_ORDERING);

                QueryResult queryResult = getJcrService().executeXpathQuery(queryBuilder.toString());
                processResults(queryResult);
            }
        }
    }

    /**
     * Searches and aggregates results of closed properties
     *
     * @param closedPropertyKeys Keys to search through
     * @throws RepositoryException
     */
    @SuppressWarnings({"WhileLoopReplaceableByForEach"})
    private void executeClosedPropSearch(Set<String> closedPropertyKeys) throws RepositoryException {
        if (closedPropertyKeys.isEmpty()) {
            return;
        }

        Iterator<String> keyIterator = closedPropertyKeys.iterator();
        StringBuilder propertiesBuilder = new StringBuilder();

        while (keyIterator.hasNext()) {
            //Add key
            String key = keyIterator.next();
            Iterator<String> valueIterator = controls.get(key).iterator();

            while (valueIterator.hasNext()) {
                propertiesBuilder.append(key);
                String value = valueIterator.next();

                //Allow searches on properties with no specific value
                if (StringUtils.isNotBlank(value)) {
                    propertiesBuilder.append(" = '").append(escapeToJcrContainsString(value)).append("' ");
                }

                addOperand(valueIterator, propertiesBuilder);
            }

            addOperand(keyIterator, propertiesBuilder);
        }

        //Remove an operand that might have been added by mistake
        String propertiesQuery = StringUtils.removeEnd(propertiesBuilder.toString(), " and ");
        propertiesQuery = propertiesQuery.trim();

        String queryStr = PROPERTY_NODE_PATH + "[" + propertiesQuery + "] " + QUERY_ORDERING;

        QueryResult queryResult = getJcrService().executeXpathQuery(queryStr);
        processResults(queryResult);
    }

    /**
     * Processes, filters and aggregates query results into the global results list The filtering creates an AND like
     * action. The first batch of results for the session automatically gets put in the global results list. Any batch
     * of results after that is compared with the global list. If the new batch of results contains a result that does
     * not exist in the global list, we discard it (Means the result does not fall under both searches, thus failing the
     * AND requirment
     *
     * @param queryResult Result object
     * @throws RepositoryException
     */
    private void processResults(QueryResult queryResult) throws RepositoryException {

        /**
         * If the global results is empty (either first query made, or there were no results from queries executed up
         * untill now
         */
        boolean noGlobalResults = globalResults.isEmpty();

        List<PropertySearchResult> currentSearchResults = new ArrayList<PropertySearchResult>();
        RowIterator rows = queryResult.getRows();

        //Filter the results and if the search results are limited, stop when reached more than max results + 1
        while (rows.hasNext() && (!controls.isLimitSearchResults() || (globalResults.size() < getMaxResults()))) {
            Row row = rows.nextRow();
            String path = row.getValue(JcrConstants.JCR_PATH).getString();
            String artifactPath = path.substring(0, path.lastIndexOf("/" + JcrService.NODE_ARTIFACTORY_METADATA));
            Node artifactNode = (Node) getJcrService().getManagedSession().getItem(artifactPath);

            ItemInfo itemInfo = getProxyItemInfo(artifactNode);
            boolean canRead = getAuthService().canRead(itemInfo.getRepoPath());
            if (canRead) {
                PropertySearchResult searchResult = new PropertySearchResult(itemInfo);

                //Make sure that we don't get any double results
                if (!currentSearchResults.contains(searchResult)) {
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

                //If the recieved results do not exist in the global results, discard them
                if (!currentSearchResults.contains(globalResult)) {
                    globalResults.remove(globalResult);
                }
            }
        }
    }

    /**
     * Add an operand to the given appender, if the given iterator has another element
     *
     * @param iterator To check if an operand is needed (a next element exists)
     * @param toAppend
     */
    private void addOperand(Iterator<String> iterator, StringBuilder toAppend) {
        if (iterator.hasNext()) {
            toAppend.append("and ");
        }
    }
}