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

package org.artifactory.search.metadata;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.metadata.MetadataSearchControls;
import org.artifactory.api.search.metadata.MetadataSearchResult;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.repo.LocalRepo;
import org.artifactory.search.SearcherBase;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yoav Landman
 */
public class MetadataSearcher extends SearcherBase<MetadataSearchControls, MetadataSearchResult> {

    // /repositories/libs-releases-local/g1/g2/a/v/a-v.pom/artifactory:metadata/qa/artifactory:xml/qa/builds/build[2]/result/jcr:xmltext

    @SuppressWarnings({"unchecked"})
    @Override
    public SearchResults<MetadataSearchResult> doSearch(MetadataSearchControls controls) throws RepositoryException {

        String metadataName = controls.getMetadataName();
        if (StringUtils.isEmpty(metadataName)) {
            metadataName = "";
        }
        String xpath = controls.getPath();
        xpath = xpath.replace("\\", "/");
        if (xpath.startsWith("/")) {
            xpath = xpath.substring(1);
        }
        String search = controls.getValue();
        String exp = null;
        if (!StringUtils.isEmpty(search)) {
            if (controls.isExactMatch()) {
                exp = "'" + escapeToJcrContainsString(search) + "'";
            } else {
                exp = "'*" + escapeToJcrContainsString(search) + "*'";
            }
        }
        //We currently include repository level metadata in the results. If decide to show only movable folder, the
        //search query needs to exclude repositories by changing the query to:
        //"/jcr:root/repositories/**//artifactory:metadata/..."
        StringBuilder builder = getPathQueryBuilder(controls);

        builder.append(FORWARD_SLASH).append(JcrService.NODE_ARTIFACTORY_METADATA).append(FORWARD_SLASH)
                .append(metadataName).append(FORWARD_SLASH);
        builder.append(JcrService.NODE_ARTIFACTORY_XML + "/").append(xpath);

        if (!StringUtils.isEmpty(exp)) {
            builder.append("/. [jcr:contains(., ").append(exp).append(")]");
        }
        String queryStr = builder.toString();
        QueryResult queryResult = getJcrService().executeXpathQuery(queryStr);
        List<MetadataSearchResult> results = new ArrayList<MetadataSearchResult>();
        RowIterator rows = queryResult.getRows();

        //Filter the results and if the search results are limited, stop when reached more than max results + 1
        while (rows.hasNext() && (!controls.isLimitSearchResults() || (results.size() < getMaxResults()))) {
            Row row = rows.nextRow();
            String path = row.getValue(JcrConstants.JCR_PATH).getString();
            String metadataNameFromPath = NamingUtils.getMetadataNameFromJcrPath(path);
            String artifactPath = path.substring(0, path.lastIndexOf("/" + JcrService.NODE_ARTIFACTORY_METADATA));
            Node artifactNode = (Node) getJcrService().getManagedSession().getItem(artifactPath);
            RepoPath repoPath = JcrPath.get().getRepoPath(artifactNode.getPath());
            LocalRepo localRepo = getRepoService().localOrCachedRepositoryByKey(repoPath.getRepoKey());
            if (!isRepoPathValid(repoPath, localRepo)) {
                continue;
            }

            boolean canRead = getAuthService().canRead(repoPath);
            if (canRead) {
                JcrFsItem jcrFsItem = localRepo.getJcrFsItem(repoPath);
                Object xmlMetdataObject = null;
                Class mdObjectClass = controls.getMetadataObjectClass();
                if (!StringUtils.isEmpty(metadataName) && (mdObjectClass != null)) {
                    xmlMetdataObject = jcrFsItem.getXmlMetdataObject(mdObjectClass);
                }
                MetadataSearchResult result =
                        new MetadataSearchResult(jcrFsItem.getInfo(), metadataNameFromPath, xmlMetdataObject);
                results.add(result);
            }
            //Release the read locks early
            LockingHelper.releaseReadLock(repoPath);
        }
        return new SearchResults<MetadataSearchResult>(results, rows.getSize());
    }
}