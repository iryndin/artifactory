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

package org.artifactory.search.metadata.xml;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.util.Text;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.metadata.MetadataSearchControls;
import org.artifactory.api.search.metadata.pom.PomSearchControls;
import org.artifactory.api.search.metadata.pom.PomSearchResult;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.jcr.fs.FileInfoProxy;
import org.artifactory.search.SearcherBase;
import org.artifactory.util.PathUtils;

import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the xml files search logic
 *
 * @author Noam Tenne
 */
public class XmlFileSearcher extends SearcherBase<MetadataSearchControls, PomSearchResult> {

    /**
     * Searches for content within any xml in the repository
     *
     * @param controls The search controls
     * @return SearchResults<PomSearchResult> - Search results
     * @throws RepositoryException
     */
    @Override
    public SearchResults<PomSearchResult> doSearch(MetadataSearchControls controls) throws RepositoryException {
        QueryResult queryResult = doQuery(controls, null);
        List<PomSearchResult> results = new ArrayList<PomSearchResult>();
        RowIterator rows = queryResult.getRows();

        //Filter the results and if the search results are limited, stop when reached more than max results + 1
        while (rows.hasNext() && (!controls.isLimitSearchResults() || (results.size() < getMaxResults()))) {
            try {
                Row row = rows.nextRow();
                String path = row.getValue(JcrConstants.JCR_PATH).getString();
                String metadataNameFromPath = NamingUtils.getMetadataNameFromJcrPath(path);

                //Node path might not be an xml path (if user has searched for "/" in the path with a blank value)
                int xmlNodeIndex = path.lastIndexOf("/" + JcrTypes.NODE_ARTIFACTORY_XML);
                if (xmlNodeIndex > 0) {
                    path = path.substring(0, xmlNodeIndex);
                }
                RepoPath repoPath = JcrPath.get().getRepoPath(path);

                if ((repoPath == null) || (!controls.isSpecificRepoSearch() && !isResultRepoPathValid(repoPath))) {
                    continue;
                }

                FileInfoProxy fileInfo = new FileInfoProxy(repoPath);
                boolean canRead = getAuthService().canRead(fileInfo.getRepoPath());
                if (canRead) {
                    PomSearchResult result = new PomSearchResult(fileInfo, metadataNameFromPath);
                    results.add(result);
                }
            } catch (RepositoryException re) {
                handleNotFoundException(re);
            }
        }
        return new SearchResults<PomSearchResult>(results, rows.getSize());
    }

    /**
     * Searches for POM's within a certain path
     *
     * @param controls The search controls
     * @return QueryResult - Search results
     * @throws RepositoryException
     */
    public QueryResult searchForDeployableUnits(PomSearchControls controls) throws RepositoryException {
        QueryResult queryResult = doQuery(controls, controls.getRepoPath());
        return queryResult;
    }

    /**
     * Performs a query for POM files. If the repo path is valid, it will search for poms in the given path. If the
     * search controls are valid, it will search for content within the poms according to the given params
     *
     * @param controls  The search controls - not mandatory
     * @param pathToPom - Path - not mandatory
     * @return QueryResult - Search results
     * @throws RepositoryException
     */
    private QueryResult doQuery(MetadataSearchControls controls, RepoPath pathToPom) throws RepositoryException {
        StringBuilder queryBuilder = getPathQueryBuilder(controls);
        //Build path to Xml (if valid)
        if ((pathToPom == null)) {
            //Repo path not valid - append to form a double slash
            queryBuilder.append(FORWARD_SLASH);

        } else {
            //Add path
            if (!StringUtils.isEmpty(pathToPom.getPath()) && PathUtils.hasText(pathToPom.getPath())) {
                String relativePath = ISO9075.encodePath(pathToPom.getPath());
                addElementsToSb(queryBuilder, relativePath);
            }

            queryBuilder.append(FORWARD_SLASH);
        }

        //Search for any Xml
        String escapedMetadataName = Text.escapeIllegalXpathSearchChars(controls.getMetadataName());
        queryBuilder.append(". [");
        if (inputContainsWildCard(escapedMetadataName)) {
            queryBuilder.append("jcr:contains(@").append(JcrTypes.PROP_ARTIFACTORY_NAME).append(", '").
                    append(escapedMetadataName).append("')");
        } else {
            queryBuilder.append("@").append(JcrTypes.PROP_ARTIFACTORY_NAME).append(" = '").
                    append(escapedMetadataName).append("'");
        }
        queryBuilder.append("]");

        String xPath = controls.getPath();

        int indexOfAt = -1;

        //Check for a manual attribute
        //If xpath !=null we are in xml search 
        if (xPath != null) {
            queryBuilder.append(FORWARD_SLASH);
            indexOfAt = xPath.indexOf("@");
            addElementsToSb(queryBuilder, JcrTypes.NODE_ARTIFACTORY_XML);
        }

        //Set as default attribute
        String attributeName = "jcr:xmlcharacters";

        //If a manual attribute exists, set it and remove from the XPath
        if (indexOfAt != -1) {
            attributeName = xPath.substring(indexOfAt + 1);
            xPath = xPath.substring(0, indexOfAt);
        }

        //If XPath is valid
        if (StringUtils.isNotBlank(xPath)) {

            xPath = xPath.replace("\\", "/");

            if ((xPath.length() > 1) && (xPath.startsWith("/"))) {
                xPath = xPath.substring(1);
            }
            queryBuilder.append(xPath);
        }

        //If value is valid
        String exp = controls.getValue();
        if (StringUtils.isNotBlank(exp)) {
            if (indexOfAt == -1) {
                queryBuilder.append("/.");
            }

            addExpFunc(queryBuilder, attributeName, exp);
        }

        String query = queryBuilder.toString();

        JcrQuerySpec spec = JcrQuerySpec.xpath(query);
        if (!controls.isLimitSearchResults()) {
            spec.noLimit();
        }
        QueryResult queryResult = getJcrService().executeQuery(spec);
        return queryResult;
    }

    private void addExpFunc(StringBuilder queryBuilder, String attributeName, String exp) {
        String escaped = Text.escapeIllegalXpathSearchChars(exp);
        queryBuilder.append("[");
        if (inputContainsWildCard(escaped)) {
            queryBuilder.append("jcr:contains(@").append(attributeName).append(", '").append(escaped).append("')");
        } else {
            queryBuilder.append("@").append(attributeName).append(" = '").append(escaped).append("'");
        }
        queryBuilder.append("]");
    }
}
