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

package org.artifactory.search;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.artifact.ArtifactSearchControls;
import org.artifactory.api.search.artifact.ArtifactSearchResult;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.jcr.fs.FileInfoProxy;
import org.artifactory.repo.LocalRepo;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.util.List;
import java.util.Set;

/**
 * User: freds Date: Jul 27, 2008 Time: 6:04:39 PM
 */
public class ArtifactSearcher extends SearcherBase<ArtifactSearchControls, ArtifactSearchResult> {

    @Override
    public SearchResults<ArtifactSearchResult> doSearch(ArtifactSearchControls controls) throws RepositoryException {
        String exp = Text.escapeIllegalXpathSearchChars(controls.getQuery());

        // select all the elements of type artifactory:file with element artifactory:name that contains the search term.
        StringBuilder queryBuilder = getPathQueryBuilder(controls);
        queryBuilder.append("/element(*, ").append(JcrTypes.NT_ARTIFACTORY_FILE).append(") [jcr:contains(@");
        queryBuilder.append(JcrTypes.PROP_ARTIFACTORY_NAME).append(", '").append(exp).append("')]");

        QueryResult queryResult = performQuery(controls.isLimitSearchResults(), queryBuilder.toString());
        List<ArtifactSearchResult> results = Lists.newArrayList();
        NodeIterator nodes = queryResult.getNodes();

        //Filter the results and if the search results are limited, stop when reached more than max results + 1
        while (nodes.hasNext() && (!controls.isLimitSearchResults() || (results.size() < getMaxResults()))) {
            try {
                Node artifactNode = nodes.nextNode();
                RepoPath repoPath = JcrPath.get().getRepoPath(artifactNode.getPath());
                if ((repoPath == null) || (!controls.isSpecificRepoSearch() && !isResultRepoPathValid(repoPath))) {
                    continue;
                }

                FileInfoProxy fileInfo = new FileInfoProxy(repoPath);
                boolean canRead = getAuthService().canRead(fileInfo.getRepoPath());
                if (canRead) {
                    ArtifactSearchResult result = new ArtifactSearchResult(fileInfo);
                    results.add(result);
                }
            } catch (RepositoryException re) {
                handleNotFoundException(re);
            }
        }
        return new SearchResults<ArtifactSearchResult>(results, nodes.getSize());
    }

    /**
     * Searches for artifacts by their checksum values
     *
     * @param sha1 SHA1 checksum to search for. Can be blank.
     * @param md5  MD5 checksum to search for. Can be blank.
     * @return Set of repo paths that comply with the given checksums
     */
    public Set<RepoPath> searchArtifactsByChecksum(String sha1, String md5) throws RepositoryException {
        Set<RepoPath> repoPathSet = Sets.newHashSet();

        if (StringUtils.isNotBlank(sha1)) {
            findArtifactsByChecksum(ChecksumType.sha1, sha1, repoPathSet);
        }

        if (repoPathSet.isEmpty() && StringUtils.isNotBlank(md5)) {
            findArtifactsByChecksum(ChecksumType.md5, md5, repoPathSet);
        }

        return repoPathSet;
    }

    /**
     * Locates artifacts by the given checksum value and adds them to the given list
     *
     * @param checksumValue Checksum value to search for
     * @param repoPathSet   Set of repo paths to append the results to
     */
    private void findArtifactsByChecksum(ChecksumType type, String checksumValue, Set<RepoPath> repoPathSet)
            throws RepositoryException {
        //Make a general search which might include results from the trash or the builds, but should save a lot of time
        String queryStr = new StringBuilder().append("//element(*, ").append(JcrTypes.NT_ARTIFACTORY_FILE).
                append(") [@").append(type.getActualPropName()).append(" = '").append(checksumValue).append("']").
                toString();

        QueryResult queryResult = getJcrService().executeQuery(JcrQuerySpec.xpath(queryStr).noLimit());

        NodeIterator nodes = queryResult.getNodes();

        while (nodes.hasNext()) {
            Node artifactNode = nodes.nextNode();
            String fullPath = artifactNode.getPath();

            //Make sure the path is of an artifact within an actual repository. Results may include trash and builds
            if (fullPath.startsWith(JcrPath.get().getRepoJcrRootPath())) {
                RepoPath repoPath = JcrPath.get().getRepoPath(fullPath);
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