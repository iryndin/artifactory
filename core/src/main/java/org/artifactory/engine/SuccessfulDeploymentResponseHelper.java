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

package org.artifactory.engine;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.jackson.JacksonFactory;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.rest.artifact.RestFileInfo;
import org.artifactory.api.rest.constant.ArtifactRestConstants;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.fs.FileInfo;
import org.artifactory.md.MetadataInfo;
import org.artifactory.mime.MimeType;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.ArtifactoryRequest;
import org.codehaus.jackson.JsonGenerator;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.io.Writer;

/**
 * Implements the spec of the HTTP 201 (Created) response by adding the final file URI to the Location header and
 * including general file information in the response body.
 */
public class SuccessfulDeploymentResponseHelper {

    /**
     * Writes the spec implementation response
     *
     * @param repoService Instance of repo service
     * @param request
     * @param response    Response object
     * @param repoPath    Repo path of deployed artifact
     * @param url         Final reachable URI of deployed file
     */
    public void writeSuccessfulDeploymentResponse(InternalRepositoryService repoService,
                                                  ArtifactoryRequest request, ArtifactoryResponse response,
                                                  RepoPath repoPath, String url) throws IOException {

        if (clientHandlesCreatedStatus(request)) {
            sendCreated(repoService, response, repoPath, url);
        } else {
            response.sendSuccess();
        }
    }

    private boolean clientHandlesCreatedStatus(ArtifactoryRequest request) {
        // for backward compatibility, we return 201 only for clients that we know will handle such response code
        // this should be removed in a few moths from now (RTFACT-3620)
        String userAgent = request.getHeader("User-Agent");
        if (StringUtils.isBlank(userAgent)) {
            return false;
        }
        userAgent = userAgent.toLowerCase();
        return userAgent.contains("ivy") || userAgent.contains("maven") || userAgent.contains("ArtifactoryBuildClient");
    }

    private void sendCreated(InternalRepositoryService repoService, ArtifactoryResponse response, RepoPath repoPath,
                             String url) throws IOException {
        Writer writer = response.getWriter();
        response.setHeader("Location", url);
        response.setStatus(HttpStatus.SC_CREATED);
        response.setContentType(ArtifactRestConstants.MT_ITEM_CREATED);
        RestFileInfo fileInfo;
        if (NamingUtils.isMetadata(repoPath.getPath())) {
            fileInfo = getMetadataFileInfo(repoService, repoPath, url);
        } else {
            fileInfo = getFileInfo(repoService, repoPath, url);
        }

        JsonGenerator jsonGenerator = JacksonFactory.createJsonGenerator(writer);
        jsonGenerator.writeObject(fileInfo);
        jsonGenerator.close();
        writer.flush();
    }

    private RestFileInfo getFileInfo(InternalRepositoryService repoService, RepoPath repoPath, String url) {
        RestFileInfo fileInfo = new RestFileInfo();
        FileInfo deployedInfo = repoService.getFileInfo(repoPath);
        ChecksumsInfo checksumsInfo = deployedInfo.getChecksumsInfo();
        fileInfo.checksums = getActualChecksums(checksumsInfo);
        fileInfo.created = ISODateTimeFormat.dateTime().print(deployedInfo.getCreated());
        fileInfo.createdBy = deployedInfo.getCreatedBy();
        fileInfo.downloadUri = url;
        fileInfo.mimeType = deployedInfo.getMimeType();
        fileInfo.originalChecksums = getOriginalChecksums(checksumsInfo);
        fileInfo.path = "/" + repoPath.getPath();
        fileInfo.repo = repoPath.getRepoKey();
        fileInfo.size = deployedInfo.getSize();
        fileInfo.slf = url;
        return fileInfo;
    }

    private RestFileInfo getMetadataFileInfo(InternalRepositoryService repoService, RepoPath repoPath, String url) {
        RestFileInfo fileInfo = new RestFileInfo();

        MetadataInfo deployedInfo = repoService.getMetadataInfo(
                new RepoPathImpl(repoPath.getRepoKey(), NamingUtils.getMetadataParentPath(repoPath.getPath())),
                NamingUtils.getMetadataName(repoPath.getPath()));

        ChecksumsInfo checksumsInfo = deployedInfo.getChecksumsInfo();
        fileInfo.checksums = getActualChecksums(checksumsInfo);
        fileInfo.created = ISODateTimeFormat.dateTime().print(deployedInfo.getCreated());
        fileInfo.createdBy = deployedInfo.getLastModifiedBy();
        fileInfo.downloadUri = url;
        fileInfo.mimeType = MimeType.applicationXml;
        fileInfo.originalChecksums = getOriginalChecksums(checksumsInfo);
        fileInfo.path = "/" + repoPath.getPath();
        fileInfo.repo = repoPath.getRepoKey();
        fileInfo.size = deployedInfo.getSize();
        fileInfo.slf = url;
        return fileInfo;
    }

    private RestFileInfo.Checksums getActualChecksums(ChecksumsInfo checksumsInfo) {
        String md5 = null;
        String sha1 = null;

        ChecksumInfo md5Checksums = checksumsInfo.getChecksumInfo(ChecksumType.md5);
        if (md5Checksums != null) {
            String actualMd5 = md5Checksums.getActual();
            if (StringUtils.isNotBlank(actualMd5)) {
                md5 = actualMd5;
            }
        }
        ChecksumInfo sha1Checksums = checksumsInfo.getChecksumInfo(ChecksumType.sha1);
        if (sha1Checksums != null) {
            String actualSha1 = sha1Checksums.getActual();
            if (StringUtils.isNotBlank(actualSha1)) {
                sha1 = actualSha1;
            }
        }

        return new RestFileInfo.Checksums(sha1, md5);
    }

    private RestFileInfo.Checksums getOriginalChecksums(ChecksumsInfo checksumsInfo) {
        String md5 = null;
        String sha1 = null;

        ChecksumInfo md5Checksums = checksumsInfo.getChecksumInfo(ChecksumType.md5);
        if (md5Checksums != null) {
            String originalMd5 = md5Checksums.getOriginal();
            if (StringUtils.isNotBlank(originalMd5)) {
                md5 = originalMd5;
            }
        }
        ChecksumInfo sha1Checksums = checksumsInfo.getChecksumInfo(ChecksumType.sha1);
        if (sha1Checksums != null) {
            String originalSha1 = sha1Checksums.getOriginal();
            if (StringUtils.isNotBlank(originalSha1)) {
                sha1 = originalSha1;
            }
        }

        return new RestFileInfo.Checksums(sha1, md5);
    }
}