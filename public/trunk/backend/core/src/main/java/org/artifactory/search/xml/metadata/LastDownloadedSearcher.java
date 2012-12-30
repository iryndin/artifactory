/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.xml.metadata.GenericMetadataSearchResult;
import org.artifactory.api.search.xml.metadata.stats.StatsSearchControls;
import org.artifactory.fs.StatsInfo;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.PathFactory;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.sapi.data.VfsNode;
import org.artifactory.sapi.data.VfsNodeType;
import org.artifactory.sapi.search.VfsBoolType;
import org.artifactory.sapi.search.VfsComparatorType;
import org.artifactory.sapi.search.VfsQueryCriterion;
import org.artifactory.sapi.search.VfsQueryResult;
import org.artifactory.sapi.search.VfsRepoQuery;
import org.artifactory.schedule.TaskInterruptedException;
import org.artifactory.schedule.TaskUtils;
import org.artifactory.search.SearcherBase;

import java.util.Calendar;
import java.util.List;

import static org.artifactory.storage.StorageConstants.*;

/**
 * @author Noam Y. Tenne
 */
public class LastDownloadedSearcher extends SearcherBase<StatsSearchControls, GenericMetadataSearchResult<StatsInfo>> {

    @Override
    public ItemSearchResults<GenericMetadataSearchResult<StatsInfo>> doSearch(StatsSearchControls controls) {
        if (!"lastDownloaded".equals(controls.getPropertyName())) {
            throw new IllegalArgumentException("This stats searcher supports only queries for last download time."
                    + " Please use the org.artifactory.search.xml.metadata.GenericStatsInfoSearcher");
        }
        VfsRepoQuery repoQuery = createRepoQuery(controls);
        repoQuery.addAllSubPathFilter();
        repoQuery.setNodeTypeFilter(VfsNodeType.FILE);
        Calendar since = (Calendar) controls.getValue();
        Calendar createdBefore = controls.getCreatedBefore();
        if (createdBefore == null) {
            createdBefore = since;
        }

        repoQuery.addCriterion(PROP_ARTIFACTORY_STATS_LAST_DOWNLOADED, VfsComparatorType.LOWER_THAN, since)
                .addPropertySubPath(NODE_ARTIFACTORY_METADATA, StatsInfo.ROOT).nextBool(VfsBoolType.OR);

        VfsQueryCriterion noStatsCriterion = repoQuery
                .addCriterion(PROP_ARTIFACTORY_STATS_LAST_DOWNLOADED, VfsComparatorType.NONE, "")
                .addPropertySubPath(NODE_ARTIFACTORY_METADATA, StatsInfo.ROOT);
        //Skip artifacts that have only been created after the since date
        VfsQueryCriterion artifactCreatedBeforeCriterion =
                repoQuery.addCriterion(PROP_ARTIFACTORY_CREATED, VfsComparatorType.LOWER_THAN, createdBefore)
                        .addPropertySubPath(NODE_ARTIFACTORY_METADATA, StatsInfo.ROOT);
        repoQuery.group(noStatsCriterion, artifactCreatedBeforeCriterion);

        VfsQueryResult queryResult = repoQuery.execute(controls.isLimitSearchResults());

        // TODO: [by fsi] It was limited in this loop but not in JCR query => removed the limit
        List<GenericMetadataSearchResult<StatsInfo>> results = Lists.newArrayList();
        int iterationCount = 0;
        PathFactory pathFactory = PathFactoryHolder.get();
        for (VfsNode node : queryResult.getNodes()) {
            if ((++iterationCount % 10 == 0) && TaskUtils.pauseOrBreak()) {
                throw new TaskInterruptedException();
            }
            RepoPath repoPath = pathFactory.getRepoPath(node.absolutePath());
            if (repoPath == null) {
                continue; // probably deleted
            }
            LocalRepo localRepo = getRepoService().localOrCachedRepositoryByKey(repoPath.getRepoKey());
            if (localRepo == null || !isResultAcceptable(repoPath, localRepo)) {
                continue;
            }

            // TODO: Use the vfs node to create this
            JcrFsItem<?, ?> jcrFsItem = localRepo.getJcrFsItem(repoPath);
            GenericMetadataSearchResult<StatsInfo> result =
                    new GenericMetadataSearchResult<StatsInfo>(jcrFsItem.getInfo(),
                            jcrFsItem.getMetadata(StatsInfo.class)) {
                        @Override
                        public String getMetadataName() {
                            return StatsInfo.ROOT;
                        }
                    };
            results.add(result);
            //Release the read locks early
            LockingHelper.releaseReadLock(repoPath);
        }
        return new ItemSearchResults<GenericMetadataSearchResult<StatsInfo>>(results, queryResult.getCount());
    }
}
