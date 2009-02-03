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

import org.artifactory.security.RepoPath;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

public class HttpArtifactoryRequest extends ArtifactoryRequestBase {

    private final HttpServletRequest httpRequest;

    public HttpArtifactoryRequest(HttpServletRequest httpRequest, String prefix) {
        this.httpRequest = httpRequest;
        //Look for repo-key@repo
        int idx = prefix.indexOf(REPO_SEP);
        String targetRepo = idx > 0 ? prefix.substring(0, idx) : prefix;
        int startIdx = prefix.length() + 2;
        //Strip any trailing '/'
        String servletPath = httpRequest.getServletPath();
        int endIdx = (servletPath.endsWith("/") ? servletPath.length() - 1 : servletPath.length());
        String path = startIdx < endIdx ? servletPath.substring(startIdx, endIdx) : "";
        RepoPath repoPath = new RepoPath(targetRepo, path);
        setRepoPath(repoPath);
    }

    public long getLastModified() {
        long dateHeader = httpRequest.getDateHeader("Last-Modified");
        return round(dateHeader);
    }

    public boolean isHeadOnly() {
        return httpRequest.getMethod().equalsIgnoreCase("HEAD");
    }

    public String getSourceDescription() {
        return httpRequest.getRemoteAddr();
    }

    public long getIfModifiedSince() {
        return httpRequest.getDateHeader("If-Modified-Since");
    }

    public boolean isFromAnotherArtifactory() {
        String origin = httpRequest.getHeader(ORIGIN_ARTIFACTORY);
        return origin != null;
    }

    public boolean isRecursive() {
        String origin = httpRequest.getHeader(ORIGIN_ARTIFACTORY);
        return origin != null && origin.equals(HOST_ID);
    }

    public InputStream getInputStream() throws IOException {
        return httpRequest.getInputStream();
    }

    @Override
    public String toString() {
        return httpRequest.getRequestURI();
    }
}