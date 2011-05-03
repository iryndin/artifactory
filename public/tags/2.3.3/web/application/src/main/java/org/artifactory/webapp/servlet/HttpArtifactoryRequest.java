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

package org.artifactory.webapp.servlet;

import org.artifactory.api.request.ArtifactoryRequestBase;
import org.artifactory.repo.RepoPath;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.PathUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

public class HttpArtifactoryRequest extends ArtifactoryRequestBase {

    private final HttpServletRequest httpRequest;

    public HttpArtifactoryRequest(HttpServletRequest httpRequest) {
        this.httpRequest = httpRequest;
        String servletPath = RequestUtils.getServletPathFromRequest(httpRequest);
        RepoPath repoPath = calculateRepoPath(servletPath);
        setRepoPath(repoPath);
    }

    public long getLastModified() {
        return httpRequest.getDateHeader("Last-Modified");
    }

    public boolean isHeadOnly() {
        return "HEAD".equalsIgnoreCase(httpRequest.getMethod());
    }

    public String getClientAddress() {
        return HttpUtils.getRemoteClientAddress(httpRequest);
    }

    public String getServletContextUrl() {
        return HttpUtils.getServletContextUrl(httpRequest);
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

    public InputStream getInputStream() throws IOException {
        return httpRequest.getInputStream();
    }

    /**
     * @see javax.servlet.ServletRequest#getContentLength()
     */
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
    public String getParameter(String name) {
        return httpRequest.getParameter(name);
    }

    @Override
    public String[] getParameterValues(String name) {
        return httpRequest.getParameterValues(name);
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