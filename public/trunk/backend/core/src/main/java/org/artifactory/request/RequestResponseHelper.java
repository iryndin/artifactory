/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

import org.apache.http.HttpStatus;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.rest.constant.ArtifactRestConstants;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.fs.RepoResource;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.resource.RepoResourceInfo;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.resource.ZipEntryResource;
import org.artifactory.security.AccessLogger;
import org.artifactory.traffic.TrafficService;
import org.artifactory.traffic.entry.DownloadEntry;
import org.artifactory.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.artifactory.api.rest.constant.ArtifactRestConstants.MT_ITEM_PROPERTIES;

/**
 * @author yoavl
 */
public final class RequestResponseHelper {
    private static final Logger log = LoggerFactory.getLogger(RequestResponseHelper.class);

    private TrafficService trafficService;

    public RequestResponseHelper(TrafficService service) {
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
                    repoPath, res.getSize(), handle.getSize());
        }
        response.sendStream(inputStream);

        fireDownloadTrafficEvent(response, repoPath, handle.getSize(), start);
    }

    public void sendBodyResponse(ArtifactoryResponse response, RepoPath repoPath, String content)
            throws IOException {
        if (content == null) {
            RuntimeException exception = new RuntimeException("Cannot send null response");
            response.sendInternalError(exception, log);
            throw exception;
        }
        byte[] bytes = content.getBytes("utf-8");
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            String path = repoPath.getPath();
            String mimeType = NamingUtils.getMimeTypeByPathAsString(path);
            response.setContentType(mimeType);
            int bodySize = bytes.length;
            response.setContentLength(bodySize);
            response.setLastModified(System.currentTimeMillis());
            AccessLogger.downloaded(repoPath);
            final long start = System.currentTimeMillis();
            response.sendStream(is);
            fireDownloadTrafficEvent(response, repoPath, bodySize, start);
        }
    }

    private void fireDownloadTrafficEvent(ArtifactoryResponse response, RepoPath repoPath, long size,
            long start) {
        if (!(response instanceof InternalArtifactoryResponse)) {
            String remoteAddress = HttpUtils.getRemoteClientAddress();
            DownloadEntry downloadEntry = new DownloadEntry(
                    repoPath.getId(), size, System.currentTimeMillis() - start, remoteAddress);
            trafficService.handleTrafficEntry(downloadEntry);
        }
    }

    public void sendHeadResponse(ArtifactoryResponse response, RepoResource res) {
        log.debug("{}: Sending HEAD meta-information", res.getRepoPath());
        updateResponseFromRepoResource(response, res);
        response.sendSuccess();
    }

    public void sendNotModifiedResponse(ArtifactoryResponse response, RepoResource res) throws IOException {
        log.debug("{}: Sending NOT-MODIFIED response", res.toString());
        response.setContentLength(0);
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

    public void updateResponseForProperties(ArtifactoryResponse response, RepoResource res,
            String content, MediaType mediaType) throws IOException {
        RepoPath propsDownloadRepoPath;
        if (mediaType.equals(MediaType.APPLICATION_XML)) {
            propsDownloadRepoPath = RepoPathFactory.create(res.getRepoPath().getRepoKey(),
                    res.getRepoPath().getPath() + "?" + ArtifactRestConstants.PROPERTIES_XML_PARAM);
            response.setContentType(mediaType.getType());
        } else if (mediaType.equals(MediaType.APPLICATION_JSON)) {
            propsDownloadRepoPath = RepoPathFactory.create(res.getRepoPath().getRepoKey(),
                    res.getRepoPath().getPath() + "?" + ArtifactRestConstants.PROPERTIES_PARAM);
            response.setContentType(MT_ITEM_PROPERTIES);
        } else {
            response.sendError(HttpStatus.SC_BAD_REQUEST, "Media Type " + mediaType + " not supported!", log);
            return;
        }

        // props generated xml and json always browsable
        setBasicHeaders(response, res, false);
        noCache(response);
        byte[] bytes = content.getBytes("utf-8");
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            int bodySize = bytes.length;
            response.setContentLength(bodySize);
            AccessLogger.downloaded(propsDownloadRepoPath);
            final long start = System.currentTimeMillis();
            response.sendStream(is);
            fireDownloadTrafficEvent(response, propsDownloadRepoPath, bodySize, start);
        }
    }

    private void updateResponseFromRepoResource(ArtifactoryResponse response, RepoResource res) {
        String mimeType = res.getMimeType();
        response.setContentType(mimeType);
        if (!isContentLengthSet(response)) {
            //Only set the content length once
            response.setContentLength(res.getSize());
        }
        setBasicHeaders(response, res, contentBrowsingDisabled(res));
        if (res.isExpirable()) {
            noCache(response);
        }
    }

    private boolean isContentLengthSet(ArtifactoryResponse response) {
        return response.getContentLength() != -1;
    }

    private void setBasicHeaders(ArtifactoryResponse response, RepoResource res, boolean contentBrowsingDisabled) {
        response.setLastModified(res.getLastModified());
        RepoResourceInfo info = res.getInfo();

        // set the sha1 as the eTag and the sha1 header
        String sha1 = info.getSha1();
        response.setEtag(sha1);
        response.setSha1(sha1);

        // set the md5 header
        String md5 = info.getMd5();
        response.setMd5(md5);

        if (response instanceof ArtifactoryResponseBase) {
            String fileName = info.getName();
            if (!isNotZipResource(res)) {
                // The filename is the zip entry inside the zip
                ZipEntryResource zipEntryResource = (ZipEntryResource) res;
                fileName = zipEntryResource.getEntryPath();
            }
            ((ArtifactoryResponseBase) response).setFilename(fileName);

            // content disposition is not set only for archive resources when archived browsing is enabled
            if (contentBrowsingDisabled) {
                ((ArtifactoryResponseBase) response).setContentDispositionAttachment(fileName);
            }
        }
    }

    private void noCache(ArtifactoryResponse response) {
        response.setHeader("Cache-Control", "no-store");
    }

    private boolean isNotZipResource(RepoResource res) {
        return !(res instanceof ZipEntryResource);
    }

    private boolean contentBrowsingDisabled(RepoResource res) {
        boolean result = true;
        RepositoryService repositoryService = ContextHelper.get().getRepositoryService();
        String repoKey = res.getResponseRepoPath().getRepoKey();
        RepoDescriptor repoDescriptor = repositoryService.repoDescriptorByKey(repoKey);
        if (repoDescriptor != null) {
            if (repoDescriptor instanceof RealRepoDescriptor) {
                result = !((RealRepoDescriptor) repoDescriptor).isArchiveBrowsingEnabled();
            }
        }

        // We return true by default if we couldn't get the flag from the descriptor
        return result;
    }
}