/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.request;

import org.artifactory.repo.RepoPath;
import org.artifactory.resource.RepoResource;

import java.io.IOException;
import java.io.InputStream;

public interface ArtifactoryRequest {

    char REPO_SEP = '@';

    String getRepoKey();

    long getLastModified();

    long getIfModifiedSince();

    String getPath();

    String getSourceDescription();

    InputStream getInputStream() throws IOException;

    boolean isSnapshot();

    boolean isMetaData();

    boolean isPom();

    String getDir();

    /**
     * This feels a bit dirty, but it represents a request where we don't want the actual file, just
     * the meta information about last update etc.
     *
     * @return
     */
    boolean isHeadOnly();

    /**
     * Indicates whether the request is coming back to the same proxy as a result of reverse
     * mirroring
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
     * Returns true if the request specification is newer than the resource. This will occur if the
     * client has a newer version of the artifact than we can provide.
     *
     * @param res
     * @return
     */
    boolean isNewerThanResource(RepoResource res);

    RepoPath getRepoPath();

    int getContentLength();

    String getHeader(String headerName);

    boolean isWebdav();

    String getName();

    String getUri();

    /**
     * Is this a request for a resource property (such as checksum), rather than an independent
     * resource request
     *
     * @return
     */
    boolean isResourceProperty();

    boolean isChecksum();
}
