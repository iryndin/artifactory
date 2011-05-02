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

package org.artifactory.search.xml;

import com.google.common.collect.Lists;
import org.apache.jackrabbit.JcrConstants;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.xml.XmlSearchResult;
import org.artifactory.api.search.xml.metadata.MetadataSearchControls;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.jcr.fs.FileInfoProxy;
import org.artifactory.repo.RepoPath;

import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.util.List;

/**
 * Holds the xml files search logic
 *
 * @author Noam Tenne
 */
public class XmlFileSearcher extends XmlSearcherBase<XmlSearchResult> {

    @Override
    protected void appendMetadataPath(StringBuilder queryBuilder, String metadataName) {
        queryBuilder.append("/element(*, ").append(JcrTypes.NT_ARTIFACTORY_FILE).append(") ");
        addFuncExp(queryBuilder, JcrTypes.PROP_ARTIFACTORY_NAME, metadataName);
        queryBuilder.append("/").append(JcrTypes.NODE_ARTIFACTORY_XML).append(FORWARD_SLASH);
    }

    @Override
    protected SearchResults<XmlSearchResult> filterAndReturnResults(
            MetadataSearchControls controls, QueryResult queryResult) throws RepositoryException {

        List<XmlSearchResult> results = Lists.newArrayList();
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

                if (!isResultAcceptable(repoPath)) {
                    continue;
                }

                FileInfoProxy fileInfo = new FileInfoProxy(repoPath);
                results.add(new XmlSearchResult(fileInfo, metadataNameFromPath));
            } catch (RepositoryException re) {
                handleNotFoundException(re);
            }
        }
        return new SearchResults<XmlSearchResult>(results, rows.getSize());
    }
}