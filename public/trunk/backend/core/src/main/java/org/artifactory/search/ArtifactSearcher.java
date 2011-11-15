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

package org.artifactory.search;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.artifact.ArtifactSearchControls;
import org.artifactory.api.search.artifact.ArtifactSearchResult;
import org.artifactory.api.search.artifact.ChecksumSearchControls;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumStorageHelper;
import org.artifactory.common.ConstantValues;
import org.artifactory.io.checksum.ChecksumPaths;
import org.artifactory.jcr.factory.VfsItemFactory;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.PathFactory;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.sapi.data.VfsDataService;
import org.artifactory.sapi.data.VfsNode;
import org.artifactory.sapi.data.VfsNodeType;
import org.artifactory.sapi.search.VfsComparatorType;
import org.artifactory.sapi.search.VfsQueryResult;
import org.artifactory.sapi.search.VfsRepoQuery;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import static org.artifactory.storage.StorageConstants.PROP_ARTIFACTORY_NAME;

/**
 * User: freds Date: Jul 27, 2008 Time: 6:04:39 PM
 */
public class ArtifactSearcher extends SearcherBase<ArtifactSearchControls, ArtifactSearchResult> {

    @Override
    public ItemSearchResults<ArtifactSearchResult> doSearch(ArtifactSearchControls controls) {
        String providedQuery = controls.getQuery();
        String relativePath = controls.getRelativePath();
        List<ArtifactSearchResult> results = Lists.newArrayList();
        long resultCount;
        boolean limit = controls.isLimitSearchResults();
        PathFactory pathFactory = PathFactoryHolder.get();
        if (ConstantValues.searchArtifactSearchUseV2Storage.getBoolean()) {
            ChecksumPaths checksumPaths = ContextHelper.get().beanForType(ChecksumPaths.class);
            //Users might enter JCR wildcards, as they've been used here for quite long. Replace with SQL equivalents
            providedQuery = StringUtils.replaceChars(StringUtils.replaceChars(controls.getQuery(), '?', '_'), '*', '%');
            ImmutableCollection<String> rawResults = checksumPaths.getFileOrPathsLike(
                    Lists.<String>newArrayList(providedQuery), createPathExpressions(controls, pathFactory));

            VfsDataService vfsDataService = getVfsDataService();
            for (String rawResult : rawResults) {
                if (limit && results.size() >= getMaxResults()) {
                    break;
                }
                if (vfsDataService.pathExists(rawResult)) {
                    appendResult(results, pathFactory, rawResult);
                }
            }

            resultCount = rawResults.size();
        } else {
            VfsRepoQuery query = createRepoQuery(controls);
            if (StringUtils.isNotBlank(relativePath)) {
                query.addRelativePathFilter(relativePath);
            }
            query.addAllSubPathFilter();
            query.setNodeTypeFilter(VfsNodeType.FILE);
            query.addCriterion(PROP_ARTIFACTORY_NAME, VfsComparatorType.CONTAINS, providedQuery);
            VfsQueryResult queryResult = query.execute(limit);
            for (VfsNode vfsNode : queryResult.getNodes()) {
                if (limit && results.size() >= getMaxResults()) {
                    break;
                }
                appendResult(results, pathFactory, vfsNode.absolutePath());
            }
            resultCount = queryResult.getCount();
        }

        return new ItemSearchResults<ArtifactSearchResult>(results, resultCount);
    }

    private List<String> createPathExpressions(ArtifactSearchControls controls, PathFactory pathFactory) {
        List<String> pathExpressions = Lists.newArrayList();
        String relativePath = controls.getRelativePath();
        if (controls.isSpecificRepoSearch()) {
            List<String> repoKeys = controls.getSelectedRepoForSearch();
            for (String repoKey : repoKeys) {
                pathExpressions.add(createPathExpression(pathFactory, repoKey, relativePath));
            }
        } else {
            pathExpressions.add(createPathExpression(pathFactory, null, relativePath));
        }
        return pathExpressions;
    }

    private String createPathExpression(PathFactory pathFactory, @Nullable String repoKey, String relativePath) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(pathFactory.getRepoRootPath(repoKey)).append("/");

        if (StringUtils.isNotBlank(relativePath)) {
            if (StringUtils.isBlank(repoKey)) {
                queryBuilder.append("%/");
            }
            queryBuilder.append(relativePath);
        }
        return queryBuilder.append("%").toString();
    }

    private void appendResult(List<ArtifactSearchResult> results, PathFactory pathFactory, String nodePath) {
        RepoPath repoPath = pathFactory.getRepoPath(nodePath);
        if (!isResultAcceptable(repoPath) || nodePath.contains(NamingUtils.METADATA_PREFIX)) {
            return;
        }
        results.add(new ArtifactSearchResult(VfsItemFactory.createFileInfoProxy(repoPath)));
    }

    /**
     * Searches for artifacts by their checksum values
     *
     * @param searchControls Search controls
     * @return Set of repo paths that comply with the given checksums
     */
    public Set<RepoPath> searchArtifactsByChecksum(ChecksumSearchControls searchControls) {
        Set<RepoPath> repoPathSet = Sets.newHashSet();

        Set<ChecksumInfo> checksums = searchControls.getChecksums();
        for (ChecksumInfo checksumInfo : checksums) {
            if (repoPathSet.isEmpty() && StringUtils.isNotBlank(checksumInfo.getActual())) {
                findArtifactsByChecksum(checksumInfo, searchControls, repoPathSet);
            }
        }
        return repoPathSet;
    }

    /**
     * Locates artifacts by the given checksum value and adds them to the given list
     *
     * @param checksumInfo           Checksum info to search for
     * @param checksumSearchControls controls
     * @param repoPathSet            Set of repo paths to append the results to
     */
    private void findArtifactsByChecksum(ChecksumInfo checksumInfo, ChecksumSearchControls checksumSearchControls,
            Set<RepoPath> repoPathSet) {
        VfsRepoQuery query = createRepoQuery(checksumSearchControls);
        query.addAllSubPathFilter();
        query.setNodeTypeFilter(VfsNodeType.FILE);
        query.addCriterion(ChecksumStorageHelper.getActualPropName(checksumInfo.getType()), VfsComparatorType.EQUAL,
                checksumInfo.getActual());
        VfsQueryResult queryResult = query.execute(false);
        PathFactory pathFactory = PathFactoryHolder.get();
        String allRepoRootPath = pathFactory.getAllRepoRootPath();
        for (VfsNode vfsNode : queryResult.getNodes()) {
            String fullPath = vfsNode.absolutePath();
            //Make sure the path is of an artifact within an actual repository. Results may include trash and builds
            if (fullPath.startsWith(allRepoRootPath)) {
                RepoPath repoPath = pathFactory.getRepoPath(fullPath);
                LocalRepo localRepo = getRepoService().localOrCachedRepositoryByKey(repoPath.getRepoKey());
                if (localRepo == null) {
                    // Some left over in JCR or the node is in a virtual repo
                    continue;
                }
                if (NamingUtils.isChecksum(repoPath.getPath())) {
                    // don't show checksum files
                    continue;
                }
                repoPathSet.add(repoPath);
            }
        }
    }
}
