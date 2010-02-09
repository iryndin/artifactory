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

package org.artifactory.search.metadata;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.metadata.MetadataSearchControls;
import org.artifactory.api.search.metadata.MetadataSearchResult;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.repo.LocalRepo;
import org.artifactory.search.SearcherBase;

import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.util.List;

/**
 * @author Yoav Landman
 */
public class MetadataSearcher extends SearcherBase<MetadataSearchControls, MetadataSearchResult> {
    private static final String DEFAULT_PROPERTY_ATTRIBUTE = "@jcr:xmlcharacters";

    // /repositories/libs-releases-local/g1/g2/a/v/a-v.pom/artifactory:metadata/qa/artifactory:xml/qa/builds/build[2]/result/jcr:xmltext

    @SuppressWarnings({"unchecked"})
    @Override
    public SearchResults<MetadataSearchResult> doSearch(MetadataSearchControls controls) throws RepositoryException {

        String metadataName = controls.getMetadataName();
        String propertyAttribute = DEFAULT_PROPERTY_ATTRIBUTE;
        if (StringUtils.isEmpty(metadataName)) {
            metadataName = "";
        }
        String xpath = controls.getPath();
        xpath = xpath.replace("\\", "/");
        if (xpath.startsWith("/")) {
            xpath = xpath.substring(1);
        }
        // If the xpath contains a "@" denoting an attribute, it should be surrounded in "[ ]. for a proper query.
        int index = xpath.indexOf("@");
        if (index != -1) {
            propertyAttribute = xpath.substring(index);
            xpath = xpath.substring(0, index);
        }
        //We currently include repository level metadata in the results. If decide to show only movable folder, the
        //search query needs to exclude repositories by changing the query to:
        //"/jcr:root/repositories/**//artifactory:metadata/..."
        StringBuilder builder = getPathQueryBuilder(controls);

        builder.append(FORWARD_SLASH).append(JcrTypes.NODE_ARTIFACTORY_METADATA).append(FORWARD_SLASH)
                .append(metadataName).append(FORWARD_SLASH);
        builder.append(JcrTypes.NODE_ARTIFACTORY_XML + "/").append(xpath);

        String search = controls.getValue();
        if (StringUtils.isNotBlank(search)) {
            if (index == -1) {
                builder.append("/. ");
            }
            builder.append("[");
            String escaped = Text.escapeIllegalXpathSearchChars(search);
            if (inputContainsWildCard(escaped)) {
                builder.append("jcr:contains(").append(propertyAttribute).append(", '").append(escaped).append("')");
            } else {
                builder.append(propertyAttribute).append(" = '").append(escaped).append("'");
            }
            builder.append("]");
        }

        String queryStr = builder.toString();

        JcrQuerySpec spec = JcrQuerySpec.xpath(queryStr);
        if (!controls.isLimitSearchResults()) {
            spec.noLimit();
        }
        QueryResult queryResult = getJcrService().executeQuery(spec);
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
                LocalRepo localRepo = getRepoService().localOrCachedRepositoryByKey(repoPath.getRepoKey());
                if ((repoPath == null) || (!controls.isSpecificRepoSearch() && !isRepoPathValid(repoPath, localRepo))) {
                    continue;
                }

                boolean canRead = getAuthService().canRead(repoPath);
                if (canRead) {
                    JcrFsItem jcrFsItem = localRepo.getJcrFsItem(repoPath);
                    Object xmlMetadataObject = null;
                    Class mdObjectClass = controls.getMetadataObjectClass();
                    if (!StringUtils.isEmpty(metadataName) && (mdObjectClass != null)) {
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