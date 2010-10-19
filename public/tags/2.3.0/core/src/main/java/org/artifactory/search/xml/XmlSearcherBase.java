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

package org.artifactory.search.xml;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.xml.metadata.MetadataSearchControls;
import org.artifactory.api.search.xml.metadata.MetadataSearchResult;
import org.artifactory.search.SearcherBase;

import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

/**
 * The XML searching base class
 *
 * @author Noam Y. Tenne
 */
public abstract class XmlSearcherBase<T extends MetadataSearchResult> extends SearcherBase<MetadataSearchControls, T> {

    private static final String DEFAULT_PROPERTY_ATTRIBUTE = "jcr:xmlcharacters";

    @Override
    public SearchResults<T> doSearch(MetadataSearchControls controls) throws RepositoryException {
        String metadataName = getFormattedMetadataName(controls.getMetadataName());

        String xPath = getFormattedXpath(controls.getPath());

        String metadataValue = controls.getValue();

        StringBuilder queryBuilder = getPathQueryBuilder(controls);

        appendMetadataPath(queryBuilder, metadataName);

        //Set as default attribute
        String attributeName = DEFAULT_PROPERTY_ATTRIBUTE;

        //If a manual attribute exists, set it and remove from the XPath
        int indexOfAt = xPath.indexOf('@');
        if (indexOfAt != -1) {
            attributeName = xPath.substring(indexOfAt + 1);
            xPath = xPath.substring(0, indexOfAt);
        }
        queryBuilder.append(xPath);

        /**
         * If no manual attribute was defined. then we are looking at a standard XML node, otherwise we are looking
         * for the property directly on the last specified node if the xpath
         */
        if (indexOfAt == -1) {
            queryBuilder.append("/element(*, ").append(JcrConstants.NT_UNSTRUCTURED).append(") ");
        }

        addFuncExp(queryBuilder, attributeName, metadataValue);

        QueryResult queryResult = performQuery(controls.isLimitSearchResults(), queryBuilder.toString());
        return filterAndReturnResults(controls, queryResult);
    }

    /**
     * Appends the path of the metadata to the JCR query, including the metadata name, but not including the xpath
     *
     * @param queryBuilder Query builder to append to
     * @param metadataName Metadata name
     */
    protected abstract void appendMetadataPath(StringBuilder queryBuilder, String metadataName);

    /**
     * Filters and returns the search results
     *
     * @param controls    Search controls
     * @param queryResult Raw results
     * @return Conformed search results
     */
    protected abstract SearchResults<T> filterAndReturnResults(MetadataSearchControls controls, QueryResult queryResult)
            throws RepositoryException;

    /**
     * Appends a function expression to the given query builder
     *
     * @param queryBuilder     Query builder to append to
     * @param attributeName    The of the property\attribute to search
     * @param valueToSearchFor The value of the attribute to search for
     */
    protected void addFuncExp(StringBuilder queryBuilder, String attributeName, String valueToSearchFor) {
        queryBuilder.append("[");
        if (StringUtils.isNotBlank(valueToSearchFor)) {
            if (inputContainsWildCard(valueToSearchFor)) {
                queryBuilder.append("jcr:contains(@").append(attributeName).append(", '").append(valueToSearchFor).
                        append("')");
            } else {
                queryBuilder.append("@").append(attributeName).append(" = '").append(valueToSearchFor).append("'");
            }
        } else {
            queryBuilder.append("@").append(attributeName);
        }
        queryBuilder.append("]");
    }

    /**
     * Formats and returns the metadata name to avoid null values
     *
     * @param metadataName Metadata name to format
     * @return Formatted metadata name
     */
    private String getFormattedMetadataName(String metadataName) {
        if (StringUtils.isBlank(metadataName)) {
            metadataName = "";
        }
        return metadataName;
    }

    /**
     * Formats and returns the xpath for a jcr query
     *
     * @param xpath XPath to format
     * @return Formatted xpath
     */
    private String getFormattedXpath(String xpath) {
        xpath = xpath.replace("\\", "/");
        if (xpath.startsWith("/")) {
            xpath = xpath.substring(1);
        }
        return xpath;
    }
}