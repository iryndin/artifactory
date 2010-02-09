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

package org.artifactory.api.search;

import org.artifactory.api.repo.Async;
import org.artifactory.api.repo.Lock;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.search.archive.ArchiveSearchControls;
import org.artifactory.api.search.archive.ArchiveSearchResult;
import org.artifactory.api.search.artifact.ArtifactSearchControls;
import org.artifactory.api.search.artifact.ArtifactSearchResult;
import org.artifactory.api.search.gavc.GavcSearchControls;
import org.artifactory.api.search.gavc.GavcSearchResult;
import org.artifactory.api.search.metadata.GenericMetadataSearchControls;
import org.artifactory.api.search.metadata.GenericMetadataSearchResult;
import org.artifactory.api.search.metadata.MetadataSearchControls;
import org.artifactory.api.search.metadata.MetadataSearchResult;
import org.artifactory.api.search.metadata.pom.PomSearchResult;
import org.artifactory.api.search.property.PropertySearchControls;
import org.artifactory.api.search.property.PropertySearchResult;
import org.artifactory.api.util.Pair;
import org.artifactory.build.api.Build;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

/**
 * @author Noam Tenne
 */
public interface SearchService {
    /**
     * @param controls Search data (mainly the search term)
     * @return Artifacts found by the search, empty list if nothing was found
     */
    @Lock(transactional = true)
    SearchResults<ArtifactSearchResult> searchArtifacts(ArtifactSearchControls controls);

    /**
     * Searches for artifacts by their checksum values
     *
     * @param sha1 SHA1 checksum to search for. Can be blank.
     * @param md5  MD5 checksum to search for. Can be blank.
     * @return Set of repo paths that comply with the given checksums
     */
    @Lock(transactional = true)
    Set<RepoPath> searchArtifactsByChecksum(String sha1, String md5);

    /**
     * @param from The time to start the search exclusive (eg, >). If empty will start from 1st Jan 1970
     * @param to   The time to end search inclusive (eg, <=), If empty, will not use current time as the limit
     * @return List of file repo paths that were created or modifies between the input time range and the date the file
     *         was modified. Empty if none is found.
     */
    @Lock(transactional = true)
    List<Pair<RepoPath, Calendar>> searchArtifactsCreatedOrModifiedInRange(
            @Nullable Calendar from, @Nullable Calendar to);

    @Lock(transactional = true)
    SearchResults<ArchiveSearchResult> searchArchiveContent(ArchiveSearchControls controls);

    @Lock(transactional = true)
    SearchResults<MetadataSearchResult> searchMetadata(MetadataSearchControls controls);

    @Lock(transactional = true)
    <T> SearchResults<GenericMetadataSearchResult<T>> searchGenericMetadata(GenericMetadataSearchControls<T> controls);

    @Lock(transactional = true)
    SearchResults<GavcSearchResult> searchGavc(GavcSearchControls controls);

    @Lock(transactional = true)
    SearchResults<PomSearchResult> searchXmlContent(MetadataSearchControls controls);

    @Lock(transactional = true)
    SearchResults<PropertySearchResult> searchProperty(PropertySearchControls controls);

    @Async(delayUntilAfterCommit = true)
    void indexMarkedArchives();

    //TODO: [by yl] Move all these methods to the InternalBuildService!

    @Lock(transactional = true)
    List<Build> getLatestBuildsByName() throws RepositoryRuntimeException;

    @Lock(transactional = true)
    List<Build> findBuildsByArtifactChecksum(String sha1, String md5) throws RepositoryRuntimeException;

    @Lock(transactional = true)
    List<Build> findBuildsByDependencyChecksum(String sha1, String md5) throws RepositoryRuntimeException;
}
