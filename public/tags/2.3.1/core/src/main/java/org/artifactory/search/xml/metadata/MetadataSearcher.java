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

package org.artifactory.search.xml.metadata;

import com.google.common.collect.Lists;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.xml.metadata.MetadataSearchControls;
import org.artifactory.api.search.xml.metadata.MetadataSearchResult;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.search.xml.XmlSearcherBase;

import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.util.List;

/**
 * @author Yoav Landman
 */
public class MetadataSearcher extends XmlSearcherBase<MetadataSearchResult> {

    // /repositories/libs-releases-local/g1/g2/a/v/a-v.pom/artifactory:metadata/qa/artifactory:xml/qa/builds/build[2]/result/jcr:xmltext
    // /repositories/libs-releases-local/g1/g2/a/v/a-v.pom/artifactory:metadata/props/artifactory:properties/properties/prop1/val1/jcr:xmltext

    @Override
    protected void appendMetadataPath(StringBuilder queryBuilder, String metadataName) {
        queryBuilder.append(FORWARD_SLASH).append(JcrTypes.NODE_ARTIFACTORY_METADATA).append(FORWARD_SLASH);

        //If the metadata name has a wildcard, then add a condition expression
        if (inputContainsWildCard(metadataName)) {
            queryBuilder.append("element(*, ").append(JcrConstants.NT_UNSTRUCTURED).append(") ");
            addFuncExp(queryBuilder, JcrTypes.PROP_ARTIFACTORY_METADATA_NAME, metadataName);
        } else {
            //If no there are wildcards, then just append it safely to the path (less conditions - faster query) 
            queryBuilder.append(Text.escapeIllegalXpathSearchChars(metadataName));
        }
        queryBuilder.append(FORWARD_SLASH).append("element(*, ").append(JcrConstants.NT_UNSTRUCTURED).append(") [").
                append("fn:name() = '").append(JcrTypes.NODE_ARTIFACTORY_XML).append("' or fn:name() = '").
                append(JcrTypes.NODE_ARTIFACTORY_PROPERTIES).append("']").append(FORWARD_SLASH);
    }

    @Override
    protected SearchResults<MetadataSearchResult> filterAndReturnResults(
            MetadataSearchControls controls, QueryResult queryResult) throws RepositoryException {

        List<MetadataSearchResult> results = Lists.newArrayList();
        RowIterator rows = queryResult.getRows();

        //Filter the results and if the search results are limited, stop when reached more than max results + 1
        while (rows.hasNext() && (!controls.isLimitSearchResults() || (results.size() < getMaxResults()))) {
            try {
                Row row = rows.nextRow();
                String path = row.getValue(JcrConstants.JCR_PATH).getString();

                String metadataNameFromPath = NamingUtils.getMetadataNameFromJcrPath(path);
                String artifactPath = path.substring(0, path.lastIndexOf("/" + JcrTypes.NODE_ARTIFACTORY_METADATA));
                RepoPath repoPath = JcrPath.get().getRepoPath(artifactPath);
                if (repoPath == null) {
                    continue; // probably deleted
                }
                LocalRepo localRepo = getRepoService().localOrCachedRepositoryByKey(repoPath.getRepoKey());
                if (!isRepoPathValid(repoPath, localRepo)) {
                    continue;
                }

                boolean canRead = getAuthService().canRead(repoPath);
                if (canRead) {
                    JcrFsItem jcrFsItem = localRepo.getJcrFsItem(repoPath);
                    Object xmlMetadataObject = null;
                    Class mdObjectClass = controls.getMetadataObjectClass();
                    if (mdObjectClass != null) {
                        xmlMetadataObject = jcrFsItem.getMetadata(mdObjectClass);
                    }
                    MetadataSearchResult result =
                            new MetadataSearchResult(jcrFsItem.getInfo(), metadataNameFromPath, xmlMetadataObject);
                    results.add(result);
                }
                //Release the read locks early
                LockingHelper.releaseReadLock(repoPath);
            } catch (RepositoryException re) {
                handleNotFoundException(re);
            }
        }
        return new SearchResults<MetadataSearchResult>(results, rows.getSize());
    }
}