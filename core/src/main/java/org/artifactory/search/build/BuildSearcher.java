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

package org.artifactory.search.build;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import org.artifactory.api.build.BasicBuildInfo;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.api.search.SearchControls;
import org.artifactory.api.search.SearchResults;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.log.LoggerFactory;
import org.artifactory.search.SearcherBase;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.util.List;
import java.util.Set;

/**
 * Holds the build search logic
 *
 * @author Noam Y. Tenne
 */
public class BuildSearcher extends SearcherBase {

    private static final Logger log = LoggerFactory.getLogger(BuildSearcher.class);

    /**
     * Returns a set of build concentrated by name and latest date
     *
     * @return Set of latest builds by name
     * @throws RepositoryException Any exception that might occur while executing the query
     */
    public Set<BasicBuildInfo> getLatestBuildsByName() throws Exception {

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("/jcr:root").append(JcrPath.get().getBuildsJcrRootPath()).append("/element(*, ").
                append(JcrConstants.NT_UNSTRUCTURED).append(")");

        QueryResult queryResult = getJcrService().executeQuery(JcrQuerySpec.xpath(queryBuilder.toString()).noLimit());

        NodeIterator nodes = queryResult.getNodes();

        Set<BasicBuildInfo> buildsToReturn = Sets.newHashSet();

        while (nodes.hasNext()) {
            try {
                Node node = nodes.nextNode();
                String buildName = Text.unescapeIllegalJcrChars(node.getName());

                Property latestNumber = node.getProperty(BuildService.PROP_BUILD_LATEST_NUMBER);
                Property latestStartTime = node.getProperty(BuildService.PROP_BUILD_LATEST_START_TIME);

                buildsToReturn.add(new BasicBuildInfo(buildName, latestNumber.getString(),
                        latestStartTime.getDate().getTime()));
            } catch (RepositoryException re) {
                handleNotFoundException(re);
            }
        }

        return buildsToReturn;
    }

    /**
     * Locates builds with deployed artifacts that have the given checksum
     *
     * @param sha1 SHA1 checksum to search for. Can be blank.
     * @param md5  MD5 checksum to search for. Can be blank.
     * @return List of basic build infos that deployed at least one artifact with the given checksum
     */
    public List<BasicBuildInfo> findBuildsByArtifactChecksum(String sha1, String md5) throws RepositoryException {
        return findBuildsByItemChecksums(JcrTypes.PROP_BUILD_ARTIFACT_CHECKSUMS, sha1, md5);
    }

    /**
     * Locates builds with dependencies that have the given checksum
     *
     * @param sha1 SHA1 checksum to search for. Can be blank.
     * @param md5  MD5 checksum to search for. Can be blank.
     * @return List of basic build infos that depend on the artifact with the given checksum
     */
    public List<BasicBuildInfo> findBuildsByDependencyChecksum(String sha1, String md5) throws RepositoryException {
        return findBuildsByItemChecksums(JcrTypes.PROP_BUILD_DEPENDENCY_CHECKSUMS, sha1, md5);
    }

    /**
     * DO NOT USE - NOT IMPLEMENTED
     */
    @Override
    public SearchResults doSearch(SearchControls controls) throws RepositoryException {
        return null;
    }

    /**
     * Locates builds that produced or depended on an item with the given checksums and adds them to the given list
     *
     * @param itemTypeProp Item property type. May be dependency or artifact
     * @param sha1         SHA1 checksum. May be blank
     * @param md5          MD5 checksum. May be blank
     * @return List of results
     */
    private List<BasicBuildInfo> findBuildsByItemChecksums(String itemTypeProp, String sha1, String md5)
            throws RepositoryException {
        List<BasicBuildInfo> results = Lists.newArrayList();

        findBuildsByItemChecksum(itemTypeProp, sha1, md5, results);

        return results;
    }

    /**
     * Locates builds that produced or depended on an item with the given checksum and adds them to the given list
     *
     * @param itemTypeProp Item property type. May be dependency or artifact
     * @param sha1         SHA1 checksum value
     * @param md5          MD5 checksum value
     * @param results      List of results to append to
     */
    private void findBuildsByItemChecksum(String itemTypeProp, String sha1, String md5, List<BasicBuildInfo> results)
            throws RepositoryException {
        boolean validSha1 = StringUtils.isNotBlank(sha1);
        boolean validMd5 = StringUtils.isNotBlank(md5);

        if (!validSha1 && !validMd5) {
            return;
        }

        StringBuilder queryBuilder =
                new StringBuilder().append("//element(*, ").append(JcrConstants.NT_UNSTRUCTURED).append(") [");

        if (validSha1) {
            queryBuilder.append("@").append(itemTypeProp).append(" = '").append(BuildService.BUILD_CHECKSUM_PREFIX_SHA1)
                    .append(sha1).append("'");
        }
        if (validMd5) {
            if (validSha1) {
                queryBuilder.append(" or ");
            }
            queryBuilder.append("@").append(itemTypeProp).append(" = '").append(BuildService.BUILD_CHECKSUM_PREFIX_MD5).
                    append(md5).append("'");
        }
        queryBuilder.append("]");

        QueryResult queryResult = getJcrService().executeQuery(JcrQuerySpec.xpath(queryBuilder.toString()).noLimit());

        NodeIterator nodeIterator = queryResult.getNodes();

        Set<Node> nodeSet = Sets.newHashSet();
        while (nodeIterator.hasNext()) {
            try {
                Node node = nodeIterator.nextNode();
                String path = node.getPath();

                //Make sure the path is of a build. Results may include trash and artifacts
                if (path.startsWith(JcrPath.get().getBuildsJcrRootPath())) {
                    nodeSet.add(node);
                }
            } catch (RepositoryException re) {
                handleNotFoundException(re);
            }
        }

        for (Node node : nodeSet) {
            String nodePath = node.getPath();
            String[] splitPath = nodePath.split("/");
            if (splitPath.length < 5) {
                log.debug("Build by item checksum search result '{}' path hierarchy does not contain sufficient " +
                        "info.", nodePath);
                continue;
            }
            String decodedBuildName = Text.unescapeIllegalJcrChars(splitPath[2]);
            String decodedBuildNumber = Text.unescapeIllegalJcrChars(splitPath[3]);
            String decodedBuildStarted = Text.unescapeIllegalJcrChars(splitPath[4]);
            results.add(new BasicBuildInfo(decodedBuildName, decodedBuildNumber, decodedBuildStarted));
        }
    }
}