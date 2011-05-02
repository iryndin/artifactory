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

package org.artifactory.api.request;

import org.artifactory.api.mime.NamingUtils;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.util.PathUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Noam Y. Tenne
 */
public class TranslatedArtifactoryRequest implements ArtifactoryRequest {

    private RepoPath translatedRepoPath;
    private ArtifactoryRequest originalRequest;

    public TranslatedArtifactoryRequest(RepoPath translatedRepoPath, ArtifactoryRequest originalRequest) {
        this.translatedRepoPath = translatedRepoPath;
        this.originalRequest = originalRequest;
    }

    public String getRepoKey() {
        return translatedRepoPath.getRepoKey();
    }

    public String getPath() {
        return translatedRepoPath.getPath();
    }

    public String getClientAddress() {
        return originalRequest.getClientAddress();
    }

    public boolean isMetadata() {
        return NamingUtils.isMetadata(getPath());
    }

    public boolean isRecursive() {
        return originalRequest.isRecursive();
    }

    public long getModificationTime() {
        return originalRequest.getModificationTime();
    }

    public String getName() {
        return PathUtils.getName(getPath());
    }

    public RepoPath getRepoPath() {
        return translatedRepoPath;
    }

    public boolean isChecksum() {
        return NamingUtils.isChecksum(getPath());
    }

    public boolean isFromAnotherArtifactory() {
        return originalRequest.isFromAnotherArtifactory();
    }

    public boolean isHeadOnly() {
        return originalRequest.isHeadOnly();
    }

    public long getLastModified() {
        return originalRequest.getLastModified();
    }

    public long getIfModifiedSince() {
        return originalRequest.getIfModifiedSince();
    }

    public boolean isNewerThan(long time) {
        return originalRequest.isNewerThan(time);
    }

    public String getHeader(String headerName) {
        return originalRequest.getHeader(headerName);
    }

    public String getServletContextUrl() {
        return originalRequest.getServletContextUrl();
    }

    public String getUri() {
        return originalRequest.getUri();
    }

    public Properties getProperties() {
        return originalRequest.getProperties();
    }

    public boolean hasProperties() {
        return originalRequest.hasProperties();
    }

    public String getParameter(String name) {
        return originalRequest.getParameter(name);
    }

    public String[] getParameterValues(String name) {
        return originalRequest.getParameterValues(name);
    }

    public InputStream getInputStream() throws IOException {
        return originalRequest.getInputStream();
    }

    public int getContentLength() {
        return originalRequest.getContentLength();
    }

    @Override
    public String toString() {
        return "source=" + getClientAddress()
                + ", path=" + getPath() + ", lastModified=" + getLastModified()
                + ", ifModifiedSince=" + getIfModifiedSince();
    }
}
