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

package org.artifactory.rest.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.artifact.RestFileInfo;
import org.artifactory.api.rest.constant.ArtifactRestConstants;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.fs.FileInfo;
import org.artifactory.md.Properties;
import org.artifactory.mime.NamingUtils;
import org.artifactory.request.ArtifactoryRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * Helper class for returning search results (quick, gavc, property, checksum, xpath)
 * with optionally full info (same as file info REST API) and properties.
 *
 * @author Shay Yaakov
 */
public class FileStorageInfoHelper {

    private static final String INCLUDE_INFO_PARAM = "info";
    private static final String INCLUDE_PROPERTIES_PARAM = "properties";

    private HttpServletRequest request;
    private RepositoryService repositoryService;
    private FileInfo itemInfo;

    public FileStorageInfoHelper(HttpServletRequest request, RepositoryService repositoryService, FileInfo itemInfo) {
        this.request = request;
        this.repositoryService = repositoryService;
        this.itemInfo = itemInfo;
    }

    public RestFileInfo createFileInfoData() {
        RestFileInfo fileInfo = new RestFileInfo();
        String uri = RestUtils.buildStorageInfoUri(request, itemInfo.getRepoKey(), itemInfo.getRelPath());
        fileInfo.slf = uri;
        addExtraFileInfo(fileInfo, uri);
        addFileInfoProperties(fileInfo);
        return fileInfo;
    }

    private void addExtraFileInfo(RestFileInfo fileInfo, String uri) {
        if (!isIncludeExtraInfo()) {
            return;
        }

        fileInfo.path = "/" + itemInfo.getRelPath();
        fileInfo.repo = itemInfo.getRepoKey();
        fileInfo.created = RestUtils.toIsoDateString(itemInfo.getCreated());
        fileInfo.createdBy = itemInfo.getCreatedBy();
        fileInfo.lastModified = RestUtils.toIsoDateString(itemInfo.getLastModified());
        fileInfo.modifiedBy = itemInfo.getModifiedBy();
        fileInfo.lastUpdated = RestUtils.toIsoDateString(itemInfo.getLastUpdated());
        fileInfo.metadataUri = uri + "?" + ArtifactRestConstants.PARAM_METADATA_NAMES_PREFIX;
        fileInfo.mimeType = NamingUtils.getMimeTypeByPathAsString(itemInfo.getRelPath());
        fileInfo.downloadUri = RestUtils.buildDownloadUri(request, itemInfo.getRepoKey(), itemInfo.getRelPath());
        fileInfo.remoteUrl = buildDownloadUrl();
        fileInfo.size = String.valueOf(itemInfo.getSize());
        ChecksumsInfo checksumInfo = itemInfo.getChecksumsInfo();
        ChecksumInfo sha1 = checksumInfo.getChecksumInfo(ChecksumType.sha1);
        ChecksumInfo md5 = checksumInfo.getChecksumInfo(ChecksumType.md5);
        String originalSha1 = sha1 != null ? sha1.getOriginal() : checksumInfo.getSha1();
        String originalMd5 = md5 != null ? md5.getOriginal() : checksumInfo.getMd5();
        fileInfo.checksums = new RestFileInfo.Checksums(checksumInfo.getSha1(), checksumInfo.getMd5());
        fileInfo.originalChecksums = new RestFileInfo.Checksums(originalSha1, originalMd5);
    }

    private String buildDownloadUrl() {
        LocalRepoDescriptor descriptor = repositoryService.localOrCachedRepoDescriptorByKey(itemInfo.getRepoKey());
        if (descriptor == null || !descriptor.isCache()) {
            return null;
        }
        RemoteRepoDescriptor remoteRepoDescriptor = ((LocalCacheRepoDescriptor) descriptor).getRemoteRepo();
        StringBuilder sb = new StringBuilder(remoteRepoDescriptor.getUrl());
        sb.append("/").append(itemInfo.getRelPath());
        return sb.toString();
    }

    private void addFileInfoProperties(RestFileInfo fileInfo) {
        if (!isIncludeProperties()) {
            return;
        }

        // Outside the loop since we want Jackson to parse it as an empty list if there aren't any properties
        fileInfo.properties = Maps.newTreeMap();

        Properties propertiesAnnotatingItem = repositoryService.getMetadata(itemInfo.getRepoPath(), Properties.class);
        if (propertiesAnnotatingItem != null && !propertiesAnnotatingItem.isEmpty()) {
            for (String propertyName : propertiesAnnotatingItem.keySet()) {
                fileInfo.properties.put(propertyName,
                        Iterables.toArray(propertiesAnnotatingItem.get(propertyName), String.class));
            }
        }
    }

    private boolean isIncludeExtraInfo() {
        return resultDetailHeaderContainsKey(INCLUDE_INFO_PARAM);
    }

    private boolean isIncludeProperties() {
        return resultDetailHeaderContainsKey(INCLUDE_PROPERTIES_PARAM);
    }

    private boolean resultDetailHeaderContainsKey(String key) {
        String resultDetailHeader = request.getHeader(ArtifactoryRequest.RESULT_DETAIL);
        return StringUtils.contains(resultDetailHeader, key);
    }
}
