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

package org.artifactory.search.build;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.api.search.SearchControls;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.util.Pair;
import org.artifactory.build.InternalBuildService;
import org.artifactory.build.api.Build;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.log.LoggerFactory;
import org.artifactory.search.SearcherBase;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds the build search logic
 *
 * @author Noam Y. Tenne
 */
public class BuildSearcher extends SearcherBase {

    private static final Logger log = LoggerFactory.getLogger(BuildSearcher.class);

    private InternalBuildService buildService;

    /**
     * Default constructor
     */
    public BuildSearcher() {
        buildService = ContextHelper.get().beanForType(InternalBuildService.class);
    }

    /**
     * Returns a list of build concentrated by name and latest date
     *
     * @return List of latest builds by name
     * @throws RepositoryException Any exception that might occur while executing the query
     */
    public List<Build> getLatestBuildsByName() throws Exception {
        JcrPath jcrPath = JcrPath.get();

        Map<String, Pair<Calendar, Build>> map = Maps.newHashMap();

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("/jcr:root").append(jcrPath.getBuildsJcrRootPath()).append("/*/*/.");

        QueryResult queryResult = getJcrService().executeQuery(JcrQuerySpec.xpath(queryBuilder.toString()).noLimit());

        NodeIterator nodes = queryResult.getNodes();

        while (nodes.hasNext()) {
            try {
                Node node = nodes.nextNode();

                String buildPath = node.getPath();
                Calendar buildCreated = node.getProperty(JcrTypes.PROP_ARTIFACTORY_CREATED).getDate();

                String buildName = jcrPath.getBuildNameFromPath(buildPath);

                if (!map.containsKey(buildName) ||
                        map.get(buildName).getFirst().before(buildCreated)) {

                    Build build = buildService.getBuild(node);

                    if (build != null) {
                        map.put(buildName, new Pair<Calendar, Build>(buildCreated, build));
                    }
                }
            } catch (RepositoryException re) {
                handleNotFoundException(re);
            }
        }

        List<Build> buildsToReturn = Lists.newArrayList();
        for (Pair<Calendar, Build> buildPair : map.values()) {
            buildsToReturn.add(buildPair.getSecond());
        }

        return buildsToReturn;
    }

    /**
     * Locates builds with deployed artifacts that have the given checksum
     *
     * @param sha1 SHA1 checksum to search for. Can be blank.
     * @param md5  MD5 checksum to search for. Can be blank.
     * @return List of builds that deployed at least one artifact with the given checksum
     */
    public List<Build> findBuildsByArtifactChecksum(String sha1, String md5) throws RepositoryException {
        return findBuildsByItemChecksums(JcrTypes.PROP_BUILD_ARTIFACT_CHECKSUMS, sha1, md5);
    }

    /**
     * Locates builds with dependencies that have the given checksum
     *
     * @param sha1 SHA1 checksum to search for. Can be blank.
     * @param md5  MD5 checksum to search for. Can be blank.
     * @return List of builds that depend on the artifact with the given checksum
     */
    public List<Build> findBuildsByDependencyChecksum(String sha1, String md5) throws RepositoryException {
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
    private List<Build> findBuildsByItemChecksums(String itemTypeProp, String sha1, String md5)
            throws RepositoryException {
        List<Build> results = Lists.newArrayList();

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
    private void findBuildsByItemChecksum(String itemTypeProp, String sha1, String md5, List<Build> results)
            throws RepositoryException {
        boolean validSha1 = StringUtils.isNotBlank(sha1);
        boolean validMd5 = StringUtils.isNotBlank(md5);

        if (!validSha1 && !validMd5) {
            return;
        }

        StringBuilder queryBuilder = new StringBuilder().append("//. ").append("[");

        if (validSha1) {
            queryBuilder.append("@").append(itemTypeProp).append(" = '").append(BuildService.BUILD_CHECKSUM_PREFIX_SHA1)
                    .append(sha1).append("'");
        }
        if (validMd5) {
            if (validSha1) {
                queryBuilder.append(" and ");
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
            Build build = buildService.getBuild(node);
            if (build != null) {
                results.add(build);
            }
        }
    }
}