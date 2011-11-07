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

package org.artifactory.api.search;

import org.artifactory.api.repo.Async;
import org.artifactory.api.search.archive.ArchiveSearchControls;
import org.artifactory.api.search.archive.ArchiveSearchResult;
import org.artifactory.api.search.artifact.ArtifactSearchControls;
import org.artifactory.api.search.artifact.ArtifactSearchResult;
import org.artifactory.api.search.artifact.ChecksumSearchControls;
import org.artifactory.api.search.gavc.GavcSearchControls;
import org.artifactory.api.search.gavc.GavcSearchResult;
import org.artifactory.api.search.property.PropertySearchControls;
import org.artifactory.api.search.property.PropertySearchResult;
import org.artifactory.api.search.xml.XmlSearchResult;
import org.artifactory.api.search.xml.metadata.GenericMetadataSearchControls;
import org.artifactory.api.search.xml.metadata.GenericMetadataSearchResult;
import org.artifactory.api.search.xml.metadata.MetadataSearchControls;
import org.artifactory.api.search.xml.metadata.MetadataSearchResult;
import org.artifactory.api.search.xml.metadata.stats.StatsSearchControls;
import org.artifactory.build.BuildRun;
import org.artifactory.fs.StatsInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.Lock;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.util.SerializablePair;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author Noam Tenne
 */
public interface SearchService {
    /**
     * @param controls Search data (mainly the search term)
     * @return Artifacts found by the search, empty list if nothing was found
     */
    @Lock(transactional = true)
    ItemSearchResults<ArtifactSearchResult> searchArtifacts(ArtifactSearchControls controls);

    /**
     * Searches for artifacts by their checksum values
     *
     * @param searchControls Search controls
     * @return Set of repo paths that comply with the given checksums
     */
    @Lock(transactional = true)
    Set<RepoPath> searchArtifactsByChecksum(ChecksumSearchControls searchControls);

    /**
     * @param from          The time to start the search exclusive (eg, >). If empty will start from 1st Jan 1970
     * @param to            The time to end search inclusive (eg, <=), If empty, will not use current time as the limit
     * @param reposToSearch Lists of repositories to search within
     * @return List of file repo paths that were created or modifies between the input time range and the date the file
     *         was modified. Empty if none is found.
     */
    @Lock(transactional = true)
    List<SerializablePair<RepoPath, Calendar>> searchArtifactsCreatedOrModifiedInRange(
            @Nullable Calendar from, @Nullable Calendar to, List<String> reposToSearch);

    @Lock(transactional = true)
    ItemSearchResults<GenericMetadataSearchResult<StatsInfo>> searchArtifactsNotDownloadedSince(
            StatsSearchControls controls);

    @Lock(transactional = true)
    ItemSearchResults<ArchiveSearchResult> searchArchiveContent(ArchiveSearchControls controls);

    @Lock(transactional = true)
    ItemSearchResults<MetadataSearchResult> searchMetadata(MetadataSearchControls controls);

    @Lock(transactional = true)
    <T> ItemSearchResults<GenericMetadataSearchResult<T>> searchGenericMetadata(
            GenericMetadataSearchControls<T> controls);

    @Lock(transactional = true)
    ItemSearchResults<GavcSearchResult> searchGavc(GavcSearchControls controls);

    @Lock(transactional = true)
    ItemSearchResults<XmlSearchResult> searchXmlContent(MetadataSearchControls controls);

    @Lock(transactional = true)
    ItemSearchResults<PropertySearchResult> searchProperty(PropertySearchControls controls);

    @Async(delayUntilAfterCommit = true)
    void asyncIndexMarkedArchives();

    @Lock(transactional = true)
    Set<BuildRun> getLatestBuilds() throws RepositoryRuntimeException;

    @Lock(transactional = true)
    List<BuildRun> findBuildsByArtifactChecksum(@Nullable String sha1, @Nullable String md5)
            throws RepositoryRuntimeException;

    @Lock(transactional = true)
    List<BuildRun> findBuildsByDependencyChecksum(@Nullable String sha1, @Nullable String md5)
            throws RepositoryRuntimeException;

    /**
     * Search for artifacts within a repository matching a given pattern.<br> The pattern should be like
     * repo-key:this/is/a/pattern
     *
     * @param pattern Pattern to search for
     * @return Set of matching artifact paths relative to the repo
     */
    Set<String> searchArtifactsByPattern(String pattern) throws ExecutionException, InterruptedException,
            TimeoutException;
}
