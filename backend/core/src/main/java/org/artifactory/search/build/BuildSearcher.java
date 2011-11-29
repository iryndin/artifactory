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
import org.artifactory.api.build.BuildService;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.SearchControls;
import org.artifactory.build.BuildRun;
import org.artifactory.log.LoggerFactory;
import org.artifactory.sapi.common.PathFactory;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.sapi.data.VfsNode;
import org.artifactory.sapi.data.VfsNodeType;
import org.artifactory.sapi.search.VfsBoolType;
import org.artifactory.sapi.search.VfsComparatorType;
import org.artifactory.sapi.search.VfsQuery;
import org.artifactory.sapi.search.VfsQueryResult;
import org.artifactory.search.SearcherBase;
import org.artifactory.storage.StorageConstants;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import static org.artifactory.api.build.BuildService.PROP_BUILD_LATEST_NUMBER;
import static org.artifactory.api.build.BuildService.PROP_BUILD_LATEST_START_TIME;

/**
 * Holds the build search logic
 *
 * @author Noam Y. Tenne
 */
public class BuildSearcher extends SearcherBase {

    private static final Logger log = LoggerFactory.getLogger(BuildSearcher.class);
    private static final String MISSING_BUILD_LATEST_NUMBER = "_latest_";

    /**
     * Returns a set of build concentrated by name and latest date
     *
     * @return Set of latest builds by name
     * @throws RepositoryException Any exception that might occur while executing the query
     */
    public Set<BuildRun> getLatestBuildsByName() throws Exception {
        VfsQuery query = getVfsQueryService().createQuery();
        query.setRootPath(PathFactoryHolder.get().getBuildsRootPath());
        query.setNodeTypeFilter(VfsNodeType.UNSTRUCTURED);
        VfsQueryResult queryResult = query.execute(false);

        Set<BuildRun> buildsToReturn = Sets.newHashSet();
        for (VfsNode node : queryResult.getNodes()) {
            String buildName = PathFactoryHolder.get().unEscape(node.getName());
            boolean bereaved = false;
            String latestNumber = node.getStringProperty(PROP_BUILD_LATEST_NUMBER);
            if (latestNumber == null) {
                latestNumber = MISSING_BUILD_LATEST_NUMBER;
                bereaved = true;
            }
            Calendar latestStartTime;
            if (node.hasProperty(PROP_BUILD_LATEST_START_TIME)) {
                latestStartTime = node.getDateProperty(PROP_BUILD_LATEST_START_TIME);
            } else {
                latestStartTime = Calendar.getInstance();
                bereaved = true;
            }
            if (bereaved) {
                log.warn("The build '{}/{}' might contain partial data. You may wish to delete this build.", buildName,
                        latestNumber);
            }
            buildsToReturn.add(new BuildRun(buildName, latestNumber, latestStartTime.getTime()));
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
    public List<BuildRun> findBuildsByArtifactChecksum(String sha1, String md5) throws RepositoryException {
        return findBuildsByItemChecksums(StorageConstants.PROP_BUILD_ARTIFACT_CHECKSUMS, sha1, md5);
    }

    /**
     * Locates builds with dependencies that have the given checksum
     *
     * @param sha1 SHA1 checksum to search for. Can be blank.
     * @param md5  MD5 checksum to search for. Can be blank.
     * @return List of basic build infos that depend on the artifact with the given checksum
     */
    public List<BuildRun> findBuildsByDependencyChecksum(String sha1, String md5) throws RepositoryException {
        return findBuildsByItemChecksums(StorageConstants.PROP_BUILD_DEPENDENCY_CHECKSUMS, sha1, md5);
    }

    /**
     * DO NOT USE - NOT IMPLEMENTED
     */
    @Override
    public ItemSearchResults doSearch(SearchControls controls) {
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
    private List<BuildRun> findBuildsByItemChecksums(String itemTypeProp, String sha1, String md5)
            throws RepositoryException {
        List<BuildRun> results = Lists.newArrayList();

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
    private void findBuildsByItemChecksum(String itemTypeProp, String sha1, String md5, List<BuildRun> results) {
        boolean validSha1 = StringUtils.isNotBlank(sha1);
        boolean validMd5 = StringUtils.isNotBlank(md5);

        if (!validSha1 && !validMd5) {
            return;
        }

        VfsQuery query = getVfsQueryService().createQuery();
        query.setRootPath(PathFactoryHolder.get().getBuildsRootPath());
        query.addAllSubPathFilter();
        query.setNodeTypeFilter(VfsNodeType.UNSTRUCTURED);
        if (validSha1) {
            query.addCriterion(itemTypeProp, VfsComparatorType.EQUAL,
                    BuildService.BUILD_CHECKSUM_PREFIX_SHA1 + sha1).nextBool(VfsBoolType.OR);
        }
        if (validMd5) {
            query.addCriterion(itemTypeProp, VfsComparatorType.EQUAL,
                    BuildService.BUILD_CHECKSUM_PREFIX_MD5 + md5).nextBool(VfsBoolType.OR);
        }
        VfsQueryResult queryResult = query.execute(false);

        PathFactory pathFactory = PathFactoryHolder.get();
        for (VfsNode node : queryResult.getNodes()) {
            String path = node.absolutePath();
            //Make sure the path is of a build => FRED with root search should never happen
            if (!path.startsWith(pathFactory.getBuildsRootPath())) {
                continue;
            }
            String[] splitPath = path.split("/");
            if (splitPath.length < 5) {
                log.debug("Build by item checksum search result '{}' path hierarchy does not contain sufficient " +
                        "info.", path);
                continue;
            }
            String decodedBuildName = pathFactory.unEscape(splitPath[2]);
            String decodedBuildNumber = pathFactory.unEscape(splitPath[3]);
            String decodedBuildStarted = pathFactory.unEscape(splitPath[4]);
            results.add(new BuildRun(decodedBuildName, decodedBuildNumber, decodedBuildStarted));
        }
    }
}