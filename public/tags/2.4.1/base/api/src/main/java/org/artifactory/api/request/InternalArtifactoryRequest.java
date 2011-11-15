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

import org.artifactory.repo.RepoPath;

import java.io.InputStream;

/**
 * An internal resource request that is sent by Artifactory itself to the DownloadService asking for a resource.
 *
 * @author Yossi Shaul
 */
public class InternalArtifactoryRequest extends ArtifactoryRequestBase {

    private boolean skipJarIndexing;
    // set this flag to true if Artifactory should mark uploaded artifacts with trusted checksums mark
    private boolean trustServerChecksums;

    private boolean forceDownloadIfNewer;

    private Boolean searchForExistingResourceOnRemoteRequest;

    public InternalArtifactoryRequest(RepoPath repoPath) {
        setRepoPath(repoPath);
    }

    public long getLastModified() {
        return 0;
    }

    /**
     * @return false - internal requests are sent to download resources
     */
    public boolean isHeadOnly() {
        return false;
    }

    public String getClientAddress() {
        return null;
    }

    public long getIfModifiedSince() {
        return 0;
    }

    public boolean isFromAnotherArtifactory() {
        return false;
    }

    public boolean isRecursive() {
        return false;
    }

    /**
     * @return null - the internal request has no input stream
     */
    public InputStream getInputStream() {
        return null;
    }

    /**
     * @return 0 - the internal request has no content (only url and headers)
     */
    public int getContentLength() {
        return 0;
    }

    public String getHeader(String headerName) {
        return null;
    }

    public String getUri() {
        return "";
    }

    public String getServletContextUrl() {
        return "";
    }

    public void setSkipJarIndexing(boolean skipJarIndexing) {
        this.skipJarIndexing = skipJarIndexing;
    }

    public void setTrustServerChecksums(boolean trustServerChecksums) {
        this.trustServerChecksums = trustServerChecksums;
    }

    public boolean isSkipJarIndexing() {
        return skipJarIndexing;
    }

    public boolean isTrustServerChecksums() {
        return trustServerChecksums;
    }

    public boolean isForceDownloadIfNewer() {
        return forceDownloadIfNewer;
    }

    public void setForceDownloadIfNewer(boolean forceDownloadIfNewer) {
        this.forceDownloadIfNewer = forceDownloadIfNewer;
    }

    public void setSearchForExistingResourceOnRemoteRequest(boolean searchForExistingResourceOnRemoteRequest) {
        this.searchForExistingResourceOnRemoteRequest = searchForExistingResourceOnRemoteRequest;
    }

    @Override
    public void setZipResourcePath(String zipResourcePath) {
        super.setZipResourcePath(zipResourcePath);
    }

    @Override
    public String getParameter(String name) {
        if (SKIP_JAR_INDEXING.equals(name)) {
            return String.valueOf(skipJarIndexing);
        }
        if (FORCE_DOWNLOAD_IF_NEWER.equals(name)) {
            return String.valueOf(forceDownloadIfNewer);
        }
        if (SEARCH_FOR_EXISTING_RESOURCE_ON_REMOTE_REQUEST.equals(name) &&
                searchForExistingResourceOnRemoteRequest != null) {
            return String.valueOf(searchForExistingResourceOnRemoteRequest);
        }
        return null;
    }

    @Override
    public String[] getParameterValues(String name) {
        String val = getParameter(name);
        if (val != null) {
            return new String[]{val};
        } else {
            return super.getParameterValues(name);
        }
    }
}
