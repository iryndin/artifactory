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

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.xml.metadata.GenericMetadataSearchControls;
import org.artifactory.api.search.xml.metadata.GenericMetadataSearchResult;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.repo.LocalRepo;
import org.artifactory.search.SearcherBase;
import org.joda.time.format.ISODateTimeFormat;

import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author freds
 */
public class StatsInfoSearcher
        extends SearcherBase<GenericMetadataSearchControls<StatsInfo>, GenericMetadataSearchResult<StatsInfo>> {

    @Override
    public SearchResults<GenericMetadataSearchResult<StatsInfo>> doSearch(
            GenericMetadataSearchControls<StatsInfo> controls) throws RepositoryException {
        String metadataName = StatsInfo.ROOT;
        String propertyAttribute;
        String pathName = controls.getPropertyName();
        if ("lastDownloaded".equals(pathName)) {
            propertyAttribute = JcrTypes.PROP_ARTIFACTORY_STATS_LAST_DOWNLOADED;
        } else if ("lastDownloadedBy".equals(pathName)) {
            propertyAttribute = JcrTypes.PROP_ARTIFACTORY_STATS_LAST_DOWNLOADED_BY;
        } else if ("downloadCount".equals(pathName)) {
            propertyAttribute = JcrTypes.PROP_ARTIFACTORY_STATS_DOWNLOAD_COUNT;
        } else {
            propertyAttribute = "";
        }

        //We currently include repository level metadata in the results. If decide to show only movable folder, the
        //search query needs to exclude repositories by changing the query to:
        //"/jcr:root/repositories/**//artifactory:metadata/..."
        StringBuilder builder = getPathQueryBuilder(controls);

        builder.append(FORWARD_SLASH).append(JcrTypes.NODE_ARTIFACTORY_METADATA).append(FORWARD_SLASH)
                .append(metadataName);

        Object search = controls.getValue();
        if (search != null) {
            if (StringUtils.isBlank(propertyAttribute)) {
                throw new IllegalArgumentException("Cannot have a value " + search + " without property!");
            }
            boolean useContains;
            String paramValue;
            String stringValue = search.toString();
            if (search.getClass() == String.class) {
                if (StringUtils.isBlank(stringValue)) {
                    paramValue = null;
                    useContains = false;
                } else {
                    String esc = Text.escapeIllegalXpathSearchChars(stringValue);
                    paramValue = '\'' + esc + '\'';
                    useContains = inputContainsWildCard(esc);
                }
            } else if (search instanceof Calendar) {
                paramValue =
                        "xs:dateTime('" + ISODateTimeFormat.dateTime().print(((Calendar) search).getTimeInMillis()) +
                                "')";
                useContains = false;
            } else {
                paramValue = stringValue;
                useContains = false;
            }
            builder.append("[");
            if (useContains) {
                builder.append("jcr:contains(").append(propertyAttribute).append(", ").append(paramValue).append(")");
            } else if (paramValue == null) {
                builder.append("@");
                builder.append(propertyAttribute);
            } else {
                builder.append("@").append(propertyAttribute).append(" ");
                switch (controls.getOperation()) {
                    case EQ:
                        builder.append("=");
                        break;
                    case GT:
                        builder.append(">");
                        break;
                    case LT:
                        builder.append("<");
                        break;
                    case GTE:
                        builder.append(">=");
                        break;
                    case LTE:
                        builder.append("<=");
                        break;
                }
                builder.append(" ").append(paramValue);
            }
            builder.append("]");
        } else if (StringUtils.isNotBlank(propertyAttribute)) {
            // Just check for existence of the property
            builder.append("[@");
            builder.append(propertyAttribute);
            builder.append("]");
        }

        String queryStr = builder.toString();
        QueryResult queryResult = getJcrService().executeQuery(JcrQuerySpec.xpath(queryStr).noLimit());
        List<GenericMetadataSearchResult<StatsInfo>> results = new ArrayList<GenericMetadataSearchResult<StatsInfo>>();
        RowIterator rows = queryResult.getRows();

        //Filter the results and if the search results are limited, stop when reached more than max results + 1
        while (rows.hasNext() && (!controls.isLimitSearchResults() || (results.size() < getMaxResults()))) {
            Row row = rows.nextRow();
            String path = row.getValue(JcrConstants.JCR_PATH).getString();
            String artifactPath = path.substring(0, path.lastIndexOf("/" + JcrTypes.NODE_ARTIFACTORY_METADATA));
            RepoPath repoPath = JcrPath.get().getRepoPath(artifactPath);
            if (repoPath == null) {
                continue; // probably deleted
            }
            LocalRepo localRepo = getRepoService().localOrCachedRepositoryByKey(repoPath.getRepoKey());
            if (!controls.isSpecificRepoSearch() && !isRepoPathValid(repoPath, localRepo)) {
                continue;
            }

            boolean canRead = getAuthService().canRead(repoPath);
            if (canRead) {
                JcrFsItem<?> jcrFsItem = localRepo.getJcrFsItem(repoPath);
                GenericMetadataSearchResult<StatsInfo> result =
                        new GenericMetadataSearchResult<StatsInfo>(jcrFsItem.getInfo(),
                                jcrFsItem.getMetadata(StatsInfo.class)) {
                            @Override
                            public String getMetadataName() {
                                return StatsInfo.ROOT;
                            }
                        };
                results.add(result);
            }
            //Release the read locks early
            LockingHelper.releaseReadLock(repoPath);
        }
        return new SearchResults<GenericMetadataSearchResult<StatsInfo>>(results, rows.getSize());
    }

}
