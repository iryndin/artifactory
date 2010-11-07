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

package org.artifactory.request;

import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.api.fs.RepoResource;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.security.AccessLogger;
import org.artifactory.traffic.InternalTrafficService;
import org.artifactory.traffic.entry.DownloadEntry;
import org.joda.time.Duration;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author yoavl
 */
public final class RequestResponseHelper {
    private static final Logger log = LoggerFactory.getLogger(RequestResponseHelper.class);

    private InternalTrafficService trafficService;

    private static final long CACHE_YEAR_SECS = Duration.standardDays(365).getStandardSeconds();

    public RequestResponseHelper(InternalTrafficService service) {
        trafficService = service;
    }

    public void sendBodyResponse(ArtifactoryResponse response, RepoResource res, ResourceStreamHandle handle)
            throws IOException {
        RepoPath repoPath = res.getRepoPath();
        //First, update the real length
        updateResponseActualLength(response, handle);
        updateResponseFromRepoResource(response, res);
        AccessLogger.downloaded(repoPath);
        InputStream inputStream = handle.getInputStream();
        final long start = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("Sending back body response for '{}'. Original resource size: {}, actual size: {}.",
                    new Object[]{repoPath, res.getSize(), handle.getSize()});
        }
        response.sendStream(inputStream);
        final DownloadEntry downloadEntry =
                new DownloadEntry(repoPath.getId(), res.getSize(), System.currentTimeMillis() - start);
        trafficService.handleTrafficEntry(downloadEntry);
    }

    public void sendBodyResponse(ArtifactoryResponse response, RepoPath repoPath, String content)
            throws IOException {
        if (content == null) {
            RuntimeException exception = new RuntimeException("Cannot send null response");
            response.sendInternalError(exception, log);
            throw exception;
        }
        byte[] bytes = content.getBytes("utf-8");
        InputStream is = new ByteArrayInputStream(bytes);
        try {
            String path = repoPath.getPath();
            String mimeType = NamingUtils.getMimeTypeByPathAsString(path);
            response.setContentType(mimeType);
            int bodySize = bytes.length;
            response.setContentLength(bodySize);
            response.setLastModified(System.currentTimeMillis());
            AccessLogger.downloaded(repoPath);
            final long start = System.currentTimeMillis();
            response.sendStream(is);
            final DownloadEntry downloadEntry =
                    new DownloadEntry(repoPath.getId(), bodySize, System.currentTimeMillis() - start);
            trafficService.handleTrafficEntry(downloadEntry);
        } finally {
            is.close();
        }
    }

    public void sendHeadResponse(ArtifactoryResponse response, RepoResource res) {
        log.debug("{}: Sending HEAD meta-information", res.getRepoPath());
        updateResponseFromRepoResource(response, res);
        response.sendSuccess();
    }

    public void sendNotModifiedResponse(ArtifactoryResponse response, RepoResource res) throws IOException {
        log.debug("{}: Sending NOT-MODIFIED response", res.toString());
        updateResponseFromRepoResource(response, res);
        response.setStatus(HttpStatus.SC_NOT_MODIFIED);
    }

    /**
     * Update the actual size according to the size of the content being sent. This may be different from the size
     * contained in the RepoResource, which was retrieved using getInfo() on the repository, and may have changed in the
     * repo just before sending back the stream, since we do not lock the repository item between getInfo() and
     * getResourceStreamHandle().
     *
     * @param response
     * @param handle
     */
    private void updateResponseActualLength(ArtifactoryResponse response, ResourceStreamHandle handle) {
        long actualSize = handle.getSize();
        if (actualSize > 0) {
            response.setContentLength(actualSize);
        }
    }

    private void updateResponseFromRepoResource(ArtifactoryResponse response, RepoResource res) {
        String mimeType = res.getMimeType();
        response.setContentType(mimeType);
        if (!response.isContentLengthSet()) {
            //Only set the content length once
            response.setContentLength(res.getSize());
        }
        response.setLastModified(res.getLastModified());
        response.setEtag(res.getInfo().getSha1());

        //TODO: [by yl] Should be in a HttpRequestInterceptor instance #processResponse
        if (res instanceof ArtifactResource) {
            boolean snapshot = ((ArtifactResource) res).getMavenInfo().isSnapshot();
            if (snapshot) {
                //Do not cache snapshot artifacts
                response.setHeader("Cache-Control", "no-cache");
            } else {
                //Set the cache in the far future for releases
                response.setHeader("Cache-Control", "public, max-age=" + CACHE_YEAR_SECS);
            }
        }
    }
}
