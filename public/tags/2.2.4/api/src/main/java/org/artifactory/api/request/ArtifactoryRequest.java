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

package org.artifactory.api.request;

import org.artifactory.api.md.Properties;
import org.artifactory.api.repo.RepoPath;

import java.io.IOException;
import java.io.InputStream;

public interface ArtifactoryRequest {

    @Deprecated
    char LEGACY_REPO_SEP = '@';

    @Deprecated
    String ORIGIN_ARTIFACTORY = "Origin-Artifactory";

    String ARTIFACTORY_ORIGINATED = "X-Artifactory-Originated";

    String MATRIX_PARAMS_SEP = ";";

    String SKIP_JAR_INDEXING = "skipJarIndexing";

    String FORCE_DOWNLOAD_IF_NEWER = "forceDownloadIfNewer";

    String getRepoKey();

    long getLastModified();

    long getIfModifiedSince();

    String getPath();

    String getSourceDescription();

    InputStream getInputStream() throws IOException;

    boolean isSnapshot();

    boolean isMetadata();

    String getDir();

    /**
     * This feels a bit dirty, but it represents a request where we don't want the actual file, just the meta
     * information about last update etc.
     *
     * @return
     */
    boolean isHeadOnly();

    /**
     * Indicates whether the request is coming back to the same proxy as a result of reverse mirroring
     *
     * @return
     */
    boolean isRecursive();

    /**
     * Checks if the request originated from another artifactory
     *
     * @return
     */
    boolean isFromAnotherArtifactory();

    long getModificationTime();

    /**
     * Returns true if the request specification is newer than the resource. This will occur if the client has a newer
     * version of the artifact than we can provide.
     *
     * @param resourceLastModified
     * @return
     */
    boolean isNewerThanResource(long resourceLastModified);

    RepoPath getRepoPath();

    /**
     * @return an integer containing the length in bytes of the request body or -1 if the length is not known
     */
    int getContentLength();

    String getHeader(String headerName);

    boolean isWebdav();

    String getName();

    String getUri();

    boolean isChecksum();

    Properties getProperties();

    boolean hasMatrixProperties();

    String getServletContextUrl();

    public String getParameter(String name);
}
