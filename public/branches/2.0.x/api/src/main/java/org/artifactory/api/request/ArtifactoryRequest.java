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
package org.artifactory.api.request;

import org.artifactory.api.repo.RepoPath;

import java.io.IOException;
import java.io.InputStream;

public interface ArtifactoryRequest {

    char REPO_SEP = '@';
    @Deprecated
    String ORIGIN_ARTIFACTORY = "Origin-Artifactory";
    String ARTIFACTORY_ORIGINATED = "X-Artifactory-Originated";

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

    int getContentLength();

    String getHeader(String headerName);

    boolean isWebdav();

    String getName();

    String getUri();

    boolean isChecksum();

    String getResourcePath();
}
