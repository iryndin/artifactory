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

package org.artifactory.search;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.artifact.ArtifactSearchControls;
import org.artifactory.api.search.artifact.ArtifactSearchResult;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.FileInfoProxy;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.jcr.JcrHelper;
import org.artifactory.resource.ArtifactResource;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.List;

/**
 * User: freds Date: Jul 27, 2008 Time: 6:04:39 PM
 */
public class ArtifactSearcher extends SearcherBase<ArtifactSearchControls, ArtifactSearchResult> {

    @Override
    public SearchResults<ArtifactSearchResult> doSearch(ArtifactSearchControls controls) throws RepositoryException {
        String exp = stringToJcrSearchExp(controls.getQuery());

        // select all the elements of type artifactory:file with element artifactory:name
        // that contains the search term. we use jackrabbit specific function fn:lower-case to allow jcr:like
        // queries that are not case sensitive.
        StringBuilder queryBuilder = getPathQueryBuilder(controls);
        queryBuilder.append("/element(*, ").append(JcrFile.NT_ARTIFACTORY_FILE).append(") [jcr:like(fn:lower-case(@");
        queryBuilder.append(JcrHelper.PROP_ARTIFACTORY_NAME).append("),").append(exp.toLowerCase())
                .append(")] order by @").append(JcrHelper.PROP_ARTIFACTORY_NAME).append(" ascending");
        String queryStr = queryBuilder.toString();

        QueryResult queryResult = getJcrService().executeXpathQuery(queryStr);
        List<ArtifactSearchResult> results = new ArrayList<ArtifactSearchResult>();
        NodeIterator nodes = queryResult.getNodes();

        //Filter the results and if the search results are limited, stop when reached more than max results + 1
        while (nodes.hasNext() && (!controls.isLimitSearchResults() || (results.size() < getMaxResults()))) {
            Node artifactNode = nodes.nextNode();
            RepoPath repoPath = JcrPath.get().getRepoPath(artifactNode.getPath());
            if (!isResultRepoPathValid(repoPath)) {
                continue;
            }

            FileInfoProxy fileInfo = new FileInfoProxy(repoPath);
            ArtifactResource artifact = new ArtifactResource(fileInfo.getRepoPath());
            boolean canRead = getAuthService().canRead(fileInfo.getRepoPath());
            MavenArtifactInfo mavenInfo = artifact.getMavenInfo();
            if (canRead && mavenInfo.isValid()) {
                ArtifactSearchResult result = new ArtifactSearchResult(fileInfo, mavenInfo);
                results.add(result);
            }
        }
        return new SearchResults<ArtifactSearchResult>(results, nodes.getSize());
    }

    /**
     * Searches for artifacts by their checksum values
     *
     * @param sha1 SHA1 checksum to search for. Can be blank.
     * @param md5  MD5 checksum to search for. Can be blank.
     * @return List of repo paths that comply with the given checksums
     */
    public List<RepoPath> searchArtifactsByChecksum(String sha1, String md5) throws RepositoryException {
        List<RepoPath> repoPathList = Lists.newArrayList();

        if (StringUtils.isNotBlank(sha1)) {
            findArtifactsByChecksum(sha1, repoPathList);
        }

        if (repoPathList.isEmpty() && StringUtils.isNotBlank(md5)) {
            findArtifactsByChecksum(md5, repoPathList);
        }

        return repoPathList;
    }

    /**
     * Locates artifacts by the given checksum value and adds them to the given list
     *
     * @param checksumValue Checksum value to search for
     * @param repoPathList  List of result repo paths
     */
    private void findArtifactsByChecksum(String checksumValue, List<RepoPath> repoPathList) throws RepositoryException {
        String queryStr = getChecksumQuery(checksumValue);

        QueryResult queryResult = getJcrService().executeXpathQuery(queryStr);

        NodeIterator nodes = queryResult.getNodes();

        while (nodes.hasNext()) {
            Node artifactNode = nodes.nextNode();
            String fullPath = artifactNode.getPath();
            int metadataIndex = fullPath.indexOf("/" + JcrService.NODE_ARTIFACTORY_METADATA);
            if (metadataIndex != -1) {
                fullPath = fullPath.substring(0, metadataIndex);
            }
            RepoPath repoPath = JcrPath.get().getRepoPath(fullPath);
            LocalRepo localRepo = getRepoService().localOrCachedRepositoryByKey(repoPath.getRepoKey());
            if (localRepo == null) {
                // Some left over in JCR of non configured repo
                continue;
            }
            if (NamingUtils.isChecksum(repoPath.getPath())) {
                // don't show checksum files
                continue;
            }
            if (!repoPathList.contains(repoPath)) {
                repoPathList.add(repoPath);
            }
        }
    }

    /**
     * Returns the JCR XPath query for locating artifacts with the given checksum
     *
     * @param checksumValue Checksum value to search for
     * @return JCR Xpath query
     */
    private String getChecksumQuery(String checksumValue) {
        return new StringBuilder().
                append("/jcr:root/repositories//artifactory-file-ext/checksumsInfo/checksums//jcr:xmltext ").
                append("[@jcr:xmlcharacters = '").append(checksumValue).append("']").toString();
    }
}