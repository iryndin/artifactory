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
package org.artifactory.webapp.servlet;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.request.ArtifactoryRequest;
import org.artifactory.api.request.ArtifactoryRequestBase;
import org.artifactory.common.ArtifactoryProperties;
import org.artifactory.utils.PathUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

public class HttpArtifactoryRequest extends ArtifactoryRequestBase {

    private final HttpServletRequest httpRequest;

    public HttpArtifactoryRequest(HttpServletRequest httpRequest) {
        this.httpRequest = httpRequest;
        //Look for the deprecated legacy format of repo-key@repo
        String servletPath = RequestUtils.getServletPathFromRequest(httpRequest);
        String prefix = PathUtils.getPathFirstPart(servletPath);
        int idx = prefix.indexOf(ArtifactoryRequest.REPO_SEP);
        String targetRepo = idx > 0 ? prefix.substring(0, idx) : prefix;
        //Test if we need to substitue the targetRepo due to system prop existence
        String substTargetRepo =
                ArtifactoryProperties.get().getSubstituteRepoKeys().get(targetRepo);
        if (substTargetRepo != null) {
            targetRepo = substTargetRepo;
        }
        int startIdx = prefix.length() + 2;
        //Strip any trailing '/'
        int endIdx = (servletPath.endsWith("/") ? servletPath.length() - 1 : servletPath.length());
        String path = startIdx < endIdx ? servletPath.substring(startIdx, endIdx) : "";
        RepoPath repoPath = new RepoPath(targetRepo, path);
        setRepoPath(repoPath);
    }

    public long getLastModified() {
        long dateHeader = httpRequest.getDateHeader("Last-Modified");
        return ArtifactoryRequestBase.round(dateHeader);
    }

    public boolean isHeadOnly() {
        return "HEAD".equalsIgnoreCase(httpRequest.getMethod());
    }

    public String getSourceDescription() {
        return httpRequest.getRemoteAddr();
    }

    public long getIfModifiedSince() {
        return httpRequest.getDateHeader("If-Modified-Since");
    }

    public boolean isFromAnotherArtifactory() {
        String origin = getOrigin();
        return origin != null;
    }

    public boolean isRecursive() {
        String origin = getOrigin();
        return origin != null && origin.equals(PathUtils.getHostId());
    }

    public boolean isWebdav() {
        return RequestUtils.isWebdavRequest(httpRequest);
    }

    public InputStream getInputStream() throws IOException {
        return httpRequest.getInputStream();
    }

    public int getContentLength() {
        return httpRequest.getContentLength();
    }

    public String getHeader(String headerName) {
        return httpRequest.getHeader(headerName);
    }

    public String getUri() {
        return httpRequest.getRequestURI();
    }

    @Override
    public String toString() {
        return getUri();
    }

    @SuppressWarnings({"deprecation"})
    private String getOrigin() {
        String origin = httpRequest.getHeader(ArtifactoryRequest.ARTIFACTORY_ORIGINATED);
        if (origin == null) {
            origin = httpRequest.getHeader(ArtifactoryRequest.ORIGIN_ARTIFACTORY);
        }
        return origin;
    }
}