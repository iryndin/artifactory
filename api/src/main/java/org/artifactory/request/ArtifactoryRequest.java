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

package org.artifactory.request;

public interface ArtifactoryRequest extends Request {

    @Deprecated
    char LEGACY_REPO_SEP = '@';

    @Deprecated
    String ORIGIN_ARTIFACTORY = "Origin-Artifactory";

    String ARTIFACTORY_ORIGINATED = "X-Artifactory-Originated";

    String CHECKSUM_SHA1 = "X-Checksum-Sha1";

    String CHECKSUM_MD5 = "X-Checksum-Md5";

    String SKIP_JAR_INDEXING = "skipJarIndexing";

    String FORCE_DOWNLOAD_IF_NEWER = "forceDownloadIfNewer";

    /**
     * The path prefix name for list browsing.
     */
    String LIST_BROWSING_PATH = "list";


    String getRepoKey();

    String getPath();

    String getSourceDescription();

    boolean isMetadata();

    /**
     * Indicates whether the request is coming back to the same proxy as a result of reverse mirroring
     */
    boolean isRecursive();

    long getModificationTime();

    String getName();
}
