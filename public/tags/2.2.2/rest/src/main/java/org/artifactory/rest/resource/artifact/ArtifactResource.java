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

package org.artifactory.rest.resource.artifact;

import com.google.common.collect.Iterables;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.fs.ChecksumsInfo;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.md.Properties;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.DirectoryItem;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.VirtualRepoItem;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.api.rest.artifact.ItemMetadata;
import org.artifactory.api.rest.artifact.ItemMetadataNames;
import org.artifactory.api.rest.artifact.ItemProperties;
import org.artifactory.api.rest.artifact.RestBaseStorageInfo;
import org.artifactory.api.rest.artifact.RestFileInfo;
import org.artifactory.api.rest.artifact.RestFolderInfo;
import org.artifactory.api.rest.constant.ArtifactRestConstants;
import org.artifactory.api.rest.constant.RestConstants;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.rest.common.list.StringList;
import org.artifactory.rest.util.RestUtils;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.PathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.artifactory.api.rest.constant.ArtifactRestConstants.*;

/**
 * @author Eli Givoni
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path(PATH_ROOT)
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
public class ArtifactResource {

    @Context
    private HttpServletRequest request;

    @Context
    private HttpServletResponse response;

    @Autowired
    private RepositoryService repositoryService;

    @GET
    @Path("{path: .+}")
    @Produces({MT_FOLDER_INFO, MT_FILE_INFO, MT_ITEM_METADATA_NAMES, MT_ITEM_PROPERTIES, MT_ITEM_METADATA})
    public Object getStorageInfo(@PathParam("path") String path,
            @QueryParam("mdns") String mdns,
            @QueryParam("md") StringList md,
            @QueryParam("properties") StringList properties) throws IOException {
        String accept = request.getHeader("Accept");
        boolean isTypeNotBlank = StringUtils.isNotBlank(accept);
        if (isItemMetadataNameRequest(mdns, md, properties)) {
            //get all metadata names on requested path
            if (isTypeNotBlank && MT_ITEM_METADATA_NAMES.equals(accept)) {
                ItemMetadataNames res = getItemMetadataNames(path, mdns);
                return Response.ok(res, MT_ITEM_METADATA_NAMES).build();
            } else {
                return Response.status(HttpStatus.SC_NOT_ACCEPTABLE).build();
            }
        } else if (isItemMetadataRequest(mdns, md)) {
            //get metadata storage info on requested specific metadata
            if (isTypeNotBlank && MT_ITEM_METADATA.equals(accept)) {
                ItemMetadata res = getItemMetadata(path, md);
                return Response.ok(res, MT_ITEM_METADATA).build();
            } else {
                return Response.status(HttpStatus.SC_NOT_ACCEPTABLE).build();
            }
        } else if (properties != null && !properties.isEmpty()) {
            // get property storage info on the requested specific property
            if (isTypeNotBlank && MT_ITEM_PROPERTIES.equals(accept)) {
                ItemProperties res = getItemProperties(path, properties);
                return Response.ok(res, MT_ITEM_PROPERTIES).build();
            } else {
                return Response.status(HttpStatus.SC_NOT_ACCEPTABLE).build();
            }
        } else {
            //get folderInfo or FileInfo on requested path
            return processStorageInfoRequest(isTypeNotBlank, accept, path);
        }
    }

    private ItemMetadataNames getItemMetadataNames(String path, String mdns) throws IOException {
        //validate method call is valid (the mdns parameter value is empty and not virtual repo)
        RepoPath repoPath = calcRepPath(path);
        if (StringUtils.isWhitespace(mdns) && isLocalRepo(repoPath.getRepoKey())) {
            ItemMetadataNames itemMetadataNames;
            List<String> metadataNameList = repositoryService.getMetadataNames(repoPath);
            if (metadataNameList != null && !metadataNameList.isEmpty()) {
                itemMetadataNames = new ItemMetadataNames();
                itemMetadataNames.slf = buildMetadataUri(path, "").trim();
                for (String name : metadataNameList) {
                    String uri = buildMetadataUri(path, "?md=") + name;
                    ItemMetadataNames.MetadataNamesInfo info = new ItemMetadataNames.MetadataNamesInfo(uri);
                    itemMetadataNames.metadata.put(name, info);
                }
                return itemMetadataNames;
            }
        }
        RestUtils.sendNotFoundResponse(response);
        return null;
    }

    /**
     * Get the property values from a certain path.
     *
     * @param path       The path from where to find the properties.
     * @param properties The property keys to find the values for.
     * @return A JSON object that contains the URI of the artifact as well as the property values.
     * @throws IOException In case of IO problems.
     */
    private ItemProperties getItemProperties(String path, StringList properties) throws IOException {
        RepoPath repoPath = calcRepPath(path);
        if (isLocalRepo(repoPath.getRepoKey())) {
            ItemProperties itemProperties = new ItemProperties();
            //add properties
            if (properties != null && !properties.isEmpty()) {
                Properties pathProperties = repositoryService.getMetadata(repoPath, Properties.class);
                if (pathProperties != null) {
                    for (String propertyName : properties) {
                        Set<String> propertySet = pathProperties.get(propertyName);
                        if (!propertySet.isEmpty()) {
                            itemProperties.properties.put(propertyName, Iterables.toArray(propertySet, String.class));
                        }
                    }
                }
            }
            if (!itemProperties.properties.isEmpty()) {
                itemProperties.slf = request.getRequestURL().toString();
                return itemProperties;
            }
        }
        RestUtils.sendNotFoundResponse(response);
        return null;
    }


    public ItemMetadata getItemMetadata(String path, StringList md) throws IOException {
        RepoPath repoPath = calcRepPath(path);
        //not supporting virtual repo metadata
        if (isLocalRepo(repoPath.getRepoKey())) {
            ItemMetadata itemMetadata = new ItemMetadata();
            //add metadata
            if (md != null) {
                for (String metadataName : md) {
                    if (StringUtils.isNotBlank(metadataName)) {
                        String metadata = repositoryService.getXmlMetadata(repoPath, metadataName);
                        if (metadata != null) {
                            itemMetadata.metadata.put(metadataName, metadata);
                        }
                    }

                }
            }
            if (!itemMetadata.metadata.isEmpty()) {
                itemMetadata.slf = request.getRequestURL().toString();
                return itemMetadata;
            }
        }
        RestUtils.sendNotFoundResponse(response);
        return null;
    }

    private Response processStorageInfoRequest(boolean isTypeBlank, String accept, String requestPath)
            throws IOException {
        RepoPath repoPath = calcRepPath(requestPath);
        String repoKey = repoPath.getRepoKey();
        RestBaseStorageInfo storageInfoRest;
        ItemInfo itemInfo = null;
        if (isLocalRepo(repoKey)) {
            try {
                itemInfo = repositoryService.getItemInfo(repoPath);
            } catch (ItemNotFoundRuntimeException e) {
                //no item found, will send 404
            }
        } else if (isVirtualRepo(repoKey)) {
            VirtualRepoItem virtualRepoItem = repositoryService.getVirtualRepoItem(repoPath);
            itemInfo = virtualRepoItem.getItem();
        }

        if (itemInfo == null) {
            RestUtils.sendNotFoundResponse(response);
            return null;
        }

        storageInfoRest = createStorageInfoData(itemInfo, requestPath);
        // we don't use the repo key from the item info because we want to set the virtual repo key if it came
        // from a virtual repository
        storageInfoRest.repo = repoKey;
        if (itemInfo.isFolder()) {
            if (isTypeBlank && MT_FOLDER_INFO.equals(accept)) {
                return Response.ok(storageInfoRest, MT_FOLDER_INFO).build();
            } else {
                return Response.status(HttpStatus.SC_NOT_ACCEPTABLE).build();

            }
        } else {
            if (isTypeBlank && MT_FILE_INFO.equals(accept)) {
                return Response.ok(storageInfoRest, MT_FILE_INFO).build();
            } else {
                return Response.status(HttpStatus.SC_NOT_ACCEPTABLE).build();
            }
        }
    }

    private RestBaseStorageInfo createStorageInfoData(ItemInfo itemInfo, String requestPath) {
        if (itemInfo.isFolder()) {
            return createFolderInfoData((FolderInfo) itemInfo, requestPath);
        } else {
            return createFileInfoData((FileInfo) itemInfo, requestPath);
        }
    }

    private boolean isVirtualRepo(String repoKey) {
        VirtualRepoDescriptor virtualRepoDescriptor = repositoryService.virtualRepoDescriptorByKey(repoKey);
        return virtualRepoDescriptor != null;
    }

    private boolean isLocalRepo(String repoKey) {
        LocalRepoDescriptor descriptor = repositoryService.localOrCachedRepoDescriptorByKey(repoKey);
        if (descriptor == null) {
            return false;
        }
        if (descriptor.isCache() && !descriptor.getKey().equals(repoKey)) {
            return false;
        }
        return true;
    }

    private String buildDownloadUri(String path) {
        String servletContextUrl = HttpUtils.getServletContextUrl(request);
        StringBuilder sb = new StringBuilder(servletContextUrl);
        sb.append("/").append(path);
        return sb.toString();
    }

    private String buildDownloadUrl(FileInfo fileInfo) {
        LocalRepoDescriptor descriptor = repositoryService.localOrCachedRepoDescriptorByKey(fileInfo.getRepoKey());
        if (descriptor == null || !descriptor.isCache()) {
            return null;
        }
        RemoteRepoDescriptor remoteRepoDescriptor = ((LocalCacheRepoDescriptor) descriptor).getRemoteRepo();
        StringBuilder sb = new StringBuilder(remoteRepoDescriptor.getUrl());
        sb.append("/").append(fileInfo.getRelPath());
        return sb.toString();
    }


    private String buildMetadataUri(String path, String addedProperty) {
        String servletContextUrl = HttpUtils.getServletContextUrl(request);
        StringBuilder sb = new StringBuilder(servletContextUrl);
        sb.append("/").append(RestConstants.PATH_API).append("/").append(ArtifactRestConstants.PATH_ROOT).append("/")
                .append(path).append(addedProperty);
        return sb.toString();
    }

    private RestFileInfo createFileInfoData(FileInfo itemInfo, String requestPath) {
        RestFileInfo fileInfo = new RestFileInfo();
        setBaseStorageInfo(fileInfo, itemInfo, requestPath);

        fileInfo.mimeType = NamingUtils.getMimeTypeByPathAsString(requestPath);
        fileInfo.downloadUri = buildDownloadUri(requestPath);
        fileInfo.remoteUrl = buildDownloadUrl(itemInfo);
        //set checksums
        ChecksumsInfo checksumInfo = itemInfo.getChecksumsInfo();
        ChecksumInfo sh1 = checksumInfo.getChecksumInfo(ChecksumType.sha1);
        ChecksumInfo md5 = checksumInfo.getChecksumInfo(ChecksumType.md5);
        String originalSh1 = sh1 != null ? sh1.getOriginal() : checksumInfo.getSha1();
        String originalMd5 = md5 != null ? md5.getOriginal() : checksumInfo.getMd5();
        fileInfo.checksums = new RestFileInfo.Checksums(checksumInfo.getSha1(), checksumInfo.getMd5());
        fileInfo.originalChecksums = new RestFileInfo.Checksums(originalSh1, originalMd5);
        return fileInfo;
    }

    private void setBaseStorageInfo(RestBaseStorageInfo storageInfoRest, ItemInfo itemInfo, String requestPath) {
        storageInfoRest.slf = request.getRequestURL().toString();
        storageInfoRest.path = "/" + itemInfo.getRelPath();
        storageInfoRest.created = RestUtils.toIsoDateString(itemInfo.getCreated());
        storageInfoRest.createdBy = itemInfo.getCreatedBy();
        storageInfoRest.lastModified = RestUtils.toIsoDateString(itemInfo.getLastModified());
        storageInfoRest.modifiedBy = itemInfo.getModifiedBy();
        storageInfoRest.lastUpdated = RestUtils.toIsoDateString(itemInfo.getLastUpdated());
        storageInfoRest.metadataUri = buildMetadataUri(requestPath, "?mdns");
    }

    private RestFolderInfo createFolderInfoData(FolderInfo itemInfo, String requestPath) {
        RestFolderInfo folderInfo = new RestFolderInfo();
        setBaseStorageInfo(folderInfo, itemInfo, requestPath);
        RepoPath folderRepoPath = itemInfo.getRepoPath();
        //if local or cache repo
        if (isLocalRepo(itemInfo.getRepoKey())) {
            List<DirectoryItem> directoryItems = repositoryService.getDirectoryItems(folderRepoPath, false);
            for (DirectoryItem item : directoryItems) {
                folderInfo.children.add(new RestFolderInfo.DirItem("/" + item.getName(), item.isFolder()));
            }
            //for virtual repo
        } else {
            List<VirtualRepoItem> virtualRepoItems = repositoryService.getVirtualRepoItems(folderRepoPath);
            for (VirtualRepoItem item : virtualRepoItems) {
                folderInfo.children.add(new RestFolderInfo.DirItem("/" + item.getName(), item.isFolder()));
            }
        }
        return folderInfo;
    }

    private RepoPath calcRepPath(String path) {
        String repoKey = PathUtils.getFirstPathElements(path);
        String relPath = PathUtils.getRelativePath(repoKey, path);
        if (relPath.endsWith("/")) {
            int index = relPath.length() - 1;
            relPath = relPath.substring(0, index);
        }
        return new RepoPath(repoKey, relPath);
    }

    /**
     * @param mdns
     * @param md
     * @param properties
     * @return url has only one query parameter named "mdns"
     */
    private boolean isItemMetadataNameRequest(String mdns, List<String> md, List<String> properties) {

        return StringUtils.isWhitespace(mdns) && (md == null || md.isEmpty()) &&
                (properties == null || properties.isEmpty());
    }

    /**
     * @param mdns
     * @param md
     * @return one or more md or properties parameter and no mdns
     */
    private boolean isItemMetadataRequest(String mdns, List md) {
        return mdns == null && ((md != null && !md.isEmpty()));
    }
}
