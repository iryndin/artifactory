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

package org.artifactory.engine;

import com.google.common.collect.Sets;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.RestCoreAddon;
import org.artifactory.addon.replication.ReplicationAddon;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.repo.DeployService;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.api.repo.exception.maven.BadPomException;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.request.InternalArtifactoryRequest;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.MutableFileInfo;
import org.artifactory.fs.RepoResource;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.io.checksum.policy.LocalRepoChecksumPolicy;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.factory.JcrFsItemFactory;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.MetadataInfo;
import org.artifactory.md.Properties;
import org.artifactory.mime.MavenNaming;
import org.artifactory.mime.MimeType;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.SaveResourceContext;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.snapshot.MavenSnapshotVersionAdapter;
import org.artifactory.repo.snapshot.MavenSnapshotVersionAdapterContext;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.MetadataResource;
import org.artifactory.resource.MutableRepoResourceInfo;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.search.InternalSearchService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.traffic.TrafficService;
import org.artifactory.traffic.entry.UploadEntry;
import org.artifactory.util.CollectionUtils;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.PathUtils;
import org.artifactory.webapp.servlet.DelayedHttpResponse;
import org.artifactory.webapp.servlet.HttpArtifactoryResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.apache.commons.httpclient.HttpStatus.SC_CONFLICT;
import static org.apache.commons.httpclient.HttpStatus.SC_NOT_FOUND;
import static org.artifactory.descriptor.repo.LocalRepoChecksumPolicyType.SERVER;
import static org.artifactory.descriptor.repo.SnapshotVersionBehavior.DEPLOYER;

/**
 * @author Yoav Landman
 */

@Service
public class UploadServiceImpl implements InternalUploadService {
    private static final Logger log = LoggerFactory.getLogger(UploadServiceImpl.class);

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private InternalRepositoryService repoService;

    @Autowired
    private JcrService jcrService;

    @Autowired
    private BasicAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private InternalSearchService searchService;

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private TrafficService trafficService;

    @Autowired
    private DeployService deployService;

    private SuccessfulDeploymentResponseHelper successfulDeploymentResponseHelper =
            new SuccessfulDeploymentResponseHelper();

    @Override
    public void upload(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException,
            RepoRejectException {
        log.debug("Request: {}", request);

        addonsManager.interceptResponse(response);
        if (responseWasIntercepted(response)) {
            return;
        }

        validateRequestAndUpload(request, response);
    }

    @Override
    public void uploadWithinTransaction(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo)
            throws IOException, RepoRejectException {
        if (request.isDirectoryRequest()) {
            createDirectory(request, response);
        } else if (request.isChecksum()) {
            validateAndUploadChecksum(request, response, repo);
        } else {
            uploadArtifact(request, response, repo);
        }
    }

    private boolean responseWasIntercepted(ArtifactoryResponse response) {
        return response.isError();
    }

    private void validateRequestAndUpload(ArtifactoryRequest request, ArtifactoryResponse response)
            throws IOException {
        if (isRequestedRepoKeyInvalid(request)) {
            response.sendError(HttpStatus.SC_NOT_FOUND, "No target local repository specified in deploy request.", log);
            return;
        }

        LocalRepo targetRepository = getTargetRepository(request);
        if (isTargetRepositoryInvalid(targetRepository)) {
            sendInvalidTargetRepositoryError(request, response);
            return;
        }

        try {
            // Servlet container doesn't support long values so we take it manually from the header
            String contentLengthHeader = request.getHeader("Content-Length");
            long contentLength = StringUtils.isBlank(contentLengthHeader) ? -1 : Long.parseLong(contentLengthHeader);
            repoService.assertValidDeployPath(targetRepository, request.getPath(), contentLength);
        } catch (RepoRejectException e) {
            handleInvalidDeployPathError(response, e);
            return;
        }

        adjustResponseAndUpload(request, response, targetRepository);
    }

    private boolean isRequestedRepoKeyInvalid(ArtifactoryRequest request) {
        return StringUtils.isBlank(request.getRepoKey());
    }

    private LocalRepo getTargetRepository(ArtifactoryRequest request) {
        return repoService.localRepositoryByKey(request.getRepoKey());
    }

    private boolean isTargetRepositoryInvalid(LocalRepo targetRepository) {
        return targetRepository == null;
    }

    private void sendInvalidTargetRepositoryError(ArtifactoryRequest request, ArtifactoryResponse response)
            throws IOException {
        int responseStatus;
        String responseMessage;
        String repoKey = request.getRepoKey();

        if (isKeyOfVirtualRepository(repoKey)) {

            response.setHeader("Allow", "GET");
            responseStatus = HttpStatus.SC_METHOD_NOT_ALLOWED;
            responseMessage = "A virtual repository cannot be used for deployment (" + repoKey +
                    "). Use a local repository as deployment target.";
        } else {

            responseStatus = HttpStatus.SC_NOT_FOUND;
            responseMessage = "Could not find a local repository named " + repoKey + " to deploy to.";
        }
        response.sendError(responseStatus, responseMessage, log);
    }

    private boolean isKeyOfVirtualRepository(String repoKey) {
        return repoService.virtualRepoDescriptorByKey(repoKey) != null;
    }

    private void adjustResponseAndUpload(ArtifactoryRequest request, ArtifactoryResponse response,
            LocalRepo targetRepository) throws IOException {
        if (processOriginatedExternally(response)) {
            response = new DelayedHttpResponse((HttpArtifactoryResponse) response);
        }
        try {
            getInternalMe().uploadWithinTransaction(request, response, targetRepository);
        } catch (RepoRejectException e) {
            //Catch rejections on save
            response.sendError(e.getErrorCode(), e.getMessage(), log);
            return;
        }
        commitResponseIfDelayed(response);
    }

    private void handleInvalidDeployPathError(ArtifactoryResponse response, RepoRejectException rejectionException)
            throws IOException {
        if (rejectionSignifiesRequiredAuthorization(rejectionException)) {
            String realmName = authenticationEntryPoint.getRealmName();
            response.sendAuthorizationRequired(rejectionException.getMessage(), realmName);
        } else {
            response.sendError(rejectionException.getErrorCode(), rejectionException.getMessage(), log);
        }
    }

    private boolean rejectionSignifiesRequiredAuthorization(RepoRejectException rejectionException) {
        return (rejectionException.getErrorCode() == HttpStatus.SC_FORBIDDEN) && authService.isAnonymous();
    }

    private boolean processOriginatedExternally(ArtifactoryResponse response) {
        //Must check the type of the response instead of the request since the HTTP request object isn't accessible here
        return response instanceof HttpArtifactoryResponse;
    }

    private void commitResponseIfDelayed(ArtifactoryResponse response) throws IOException {
        if (response instanceof DelayedHttpResponse) {
            ((DelayedHttpResponse) response).commitResponseCode();
        }
    }

    private void createDirectory(ArtifactoryRequest request, ArtifactoryResponse response)
            throws IOException {
        RepoPath repoPath = request.getRepoPath();
        log.info("MKDir request to '{}'", request.getRepoPath());

        repoService.mkdirs(repoPath);
        annotateWithRequestPropertiesIfPermitted(request, repoPath);

        sendSuccessfulResponse(request, response, repoPath, true);
        log.info("Directory '{}' was created successfully.", request.getRepoPath());
    }

    private void annotateWithRequestPropertiesIfPermitted(ArtifactoryRequest request, RepoPath repoPath) {
        if (authService.canAnnotate(repoPath)) {
            Properties properties = request.getProperties();
            repoService.setMetadata(repoPath, Properties.class, properties);
        }
    }

    private void validateAndUploadChecksum(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo)
            throws IOException {
        int length = request.getContentLength();
        if (isAbnormalChecksumContentLength(length)) {
            // something is fishy, checksum file should not be so big...
            response.sendError(SC_CONFLICT, "Suspicious checksum file, content length of " + length +
                    " bytes is bigger than allowed.", log);
            return;
        }

        log.info("Deploy to '{}' Content-Length: {}", request.getRepoPath(), length < 0 ? "unspecified" : length);

        String checksumPath = request.getPath();
        if (NamingUtils.isMetadataChecksum(checksumPath)) {
            //Ignore request - we maintain our self-calculated checksums for metadata
            consumeContentAndRespondWithSuccess(request, response);
            return;
        }

        validatePathAndUploadChecksum(request, response, repo);
    }

    private boolean isAbnormalChecksumContentLength(int length) {
        return length > 1024;
    }

    private void consumeContentAndRespondWithSuccess(ArtifactoryRequest request, ArtifactoryResponse response)
            throws IOException {
        IOUtils.copy(request.getInputStream(), new NullOutputStream());
        response.sendSuccess();
    }

    private void validatePathAndUploadChecksum(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo)
            throws IOException {
        RepoPath targetFileRepoPath = adjustAndGetChecksumTargetRepoPath(request, repo);
        JcrFsItem fsItem = repo.getLockedJcrFsItem(targetFileRepoPath);

        if (fsItem == null) {
            response.sendError(SC_NOT_FOUND, "Target file to set checksum on doesn't exist: " + targetFileRepoPath,
                    log);
            return;
        }

        if (!fsItem.isFile()) {
            response.sendError(SC_CONFLICT, "Checksum only supported for files (but found folder): " +
                    targetFileRepoPath, log);
            return;
        }

        uploadChecksum(request, response, (JcrFile) fsItem);
    }

    private RepoPath adjustAndGetChecksumTargetRepoPath(ArtifactoryRequest request, LocalRepo repo) {
        String checksumTargetFile = request.getPath();
        if (isMavenRepo(repo)) {
            checksumTargetFile = adjustMavenSnapshotPath(repo, request);
        }
        return repo.getRepoPath(PathUtils.stripExtension(checksumTargetFile));
    }

    private void uploadChecksum(ArtifactoryRequest request, ArtifactoryResponse response, JcrFile jcrFile)
            throws IOException {
        String uploadedChecksum;
        try {
            uploadedChecksum = getChecksumContentAsString(request);
        } catch (IOException e) {
            response.sendError(SC_CONFLICT, "Failed to read checksum from file: " + e.getMessage() +
                    " for path " + request.getRepoPath(), log);
            return;
        }

        updateChecksumInfoAndRespond(request, response, jcrFile, uploadedChecksum);
    }

    private String getChecksumContentAsString(ArtifactoryRequest request)
            throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = request.getInputStream();
            return Checksum.checksumStringFromStream(inputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void updateChecksumInfoAndRespond(ArtifactoryRequest request, ArtifactoryResponse response, JcrFile jcrFile,
            String uploadedChecksum) throws IOException {
        ChecksumInfo checksumInfo = updateAndGetFileChecksumInfo(request, jcrFile, uploadedChecksum);
        if (isChecksumValidAccordingToPolicy(uploadedChecksum, checksumInfo)) {
            sendUploadedChecksumResponse(request, response, jcrFile.getRepoPath());
        } else {
            String message = String.format("Checksum error for '%s': received '%s' but actual is '%s'",
                    request.getPath(), uploadedChecksum, checksumInfo.getActual());

            sendInvalidUploadedChecksumResponse(request, response, jcrFile, message);
        }
    }

    private ChecksumInfo updateAndGetFileChecksumInfo(ArtifactoryRequest request, JcrFile jcrFile, String checksum) {
        FileInfo fileInfo = jcrFile.getInfo();
        ChecksumsInfo checksums = fileInfo.getChecksumsInfo();
        ChecksumType checksumType = ChecksumType.forFilePath(request.getPath());
        if (!checksumType.isValid(checksum)) {
            log.warn("Uploading non valid original checksum for {}", jcrFile.getRepoPath());
        }
        ChecksumInfo checksumInfo = checksums.getChecksumInfo(checksumType);
        if (checksumInfo == null) {
            checksumInfo = new ChecksumInfo(checksumType, checksum, null);
        } else {
            checksumInfo = new ChecksumInfo(checksumType, checksum, checksumInfo.getActual());
        }
        checksums.addChecksumInfo(checksumInfo);
        return checksumInfo;
    }

    private boolean isChecksumValidAccordingToPolicy(String checksum, ChecksumInfo checksumInfo) {
        return checksum.equalsIgnoreCase(checksumInfo.getActual());
    }

    private void sendInvalidUploadedChecksumResponse(ArtifactoryRequest request, ArtifactoryResponse response,
            JcrFile jcrFile, String errorMessage) throws IOException {
        JcrFsItemFactory repo = jcrFile.getRepo();
        ChecksumPolicy checksumPolicy = repo.getChecksumPolicy();
        if (checksumPolicy instanceof LocalRepoChecksumPolicy &&
                ((LocalRepoChecksumPolicy) checksumPolicy).getPolicyType().equals(SERVER)) {
            log.debug(errorMessage);
            sendUploadedChecksumResponse(request, response, jcrFile.getRepoPath());
        } else {
            response.sendError(SC_CONFLICT, errorMessage, log);
        }
    }

    private void sendUploadedChecksumResponse(ArtifactoryRequest request, ArtifactoryResponse response,
            RepoPath targetFileRepoPath) {
        response.setHeader("Location", buildArtifactUrl(request, targetFileRepoPath));
        response.setStatus(HttpStatus.SC_CREATED);
        response.sendSuccess();
        addonsManager.addonByType(ReplicationAddon.class).offerLocalReplicationDeploymentEvent(request.getRepoPath());
    }

    private void uploadArtifact(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo)
            throws IOException, RepoRejectException {
        if (isDeployArchiveBundle(request)) {
            RestCoreAddon restCoreAddon = addonsManager.addonByType(RestCoreAddon.class);
            restCoreAddon.deployArchiveBundle(request, response, repo);
            return;
        }

        int length = request.getContentLength();
        log.info("Deploy to '{}' Content-Length: {}", request.getRepoPath(), length < 0 ? "unspecified" : length);
        if (NamingUtils.isMetadata(request.getPath())) {
            uploadMetadata(request, response, repo);
        } else {
            uploadFile(request, response, repo);
        }
    }

    private void uploadMetadata(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo)
            throws IOException, RepoRejectException {
        String path = request.getPath();

        if (isMavenRepo(repo)) {

            if (isRepoSnapshotPolicyNotDeployer(repo) && MavenNaming.isSnapshotMavenMetadata(path)) {
                // Skip the maven metadata deployment - use the metadata calculated after the pom is deployed
                consumeContentAndRespondAccepted(request, response);
                return;
            }

            path = adjustMavenSnapshotPath(repo, request);
        }

        MetadataInfo metadataInfo = InfoFactoryHolder.get().createMetadata(repo.getRepoPath(path));
        MetadataResource metadataResource = new MetadataResource(metadataInfo);

        uploadItem(request, response, repo, metadataResource);
    }

    private void uploadFile(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo)
            throws RepoRejectException, IOException {
        String path = request.getPath();
        if (isMavenRepo(repo)) {
            path = adjustMavenSnapshotPath(repo, request);
        }

        RepoPath fileRepoPath = repo.getRepoPath(path);
        MutableFileInfo fileInfo = InfoFactoryHolder.get().createFileInfo(fileRepoPath);
        boolean isChecksumDeploy = isChecksumDeploy(request);
        setFileInfoChecksums(request, fileInfo, isChecksumDeploy);
        FileResource fileResource = new FileResource(fileInfo);

        uploadItem(request, response, repo, fileResource);
    }

    private boolean isMavenRepo(LocalRepo repo) {
        return repo.getDescriptor().isMavenRepoLayout();
    }

    private boolean isRepoSnapshotPolicyNotDeployer(LocalRepo repo) {
        SnapshotVersionBehavior mavenSnapshotVersionBehavior = repo.getMavenSnapshotVersionBehavior();
        return !mavenSnapshotVersionBehavior.equals(DEPLOYER);
    }

    private void consumeContentAndRespondAccepted(ArtifactoryRequest request, ArtifactoryResponse response)
            throws IOException {
        log.trace("Skipping deployment of maven metadata file {}", request.getPath());
        IOUtils.copy(request.getInputStream(), new NullOutputStream());
        response.setStatus(HttpStatus.SC_ACCEPTED);
    }

    private void uploadItem(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo, RepoResource res)
            throws IOException, RepoRejectException {

        if (isChecksumDeploy(request)) {
            uploadItemWithReusedContent(request, response, repo, res);
        } else if (ConstantValues.httpUseExpectContinue.getBoolean() && HttpUtils.isExpectedContinue(request)) {
            uploadItemWithReusedOrProvidedContent(request, response, repo, res);
        } else {
            uploadItemWithProvidedContent(request, response, repo, res);
        }
    }

    private boolean isChecksumDeploy(ArtifactoryRequest request) {
        return Boolean.parseBoolean(request.getHeader(ArtifactoryRequest.CHECKSUM_DEPLOY));
    }

    private boolean isDeployArchiveBundle(ArtifactoryRequest request) {
        return Boolean.parseBoolean(request.getHeader(ArtifactoryRequest.EXPLODE_ARCHIVE));
    }

    private void uploadItemWithReusedContent(ArtifactoryRequest request, ArtifactoryResponse response,
            LocalRepo repo, RepoResource res) throws IOException, RepoRejectException {

        String sha1 = HttpUtils.getSha1Checksum(request);
        if (StringUtils.isBlank(sha1)) {
            response.sendError(SC_NOT_FOUND, "Checksum deploy failed. SHA1 header '" +
                    ArtifactoryRequest.CHECKSUM_SHA1 + "' doesn't exist", log);
            return;
        }
        log.debug("Checksum deploy to '{}' with SHA1: {}", res.getRepoPath(), sha1);
        if (!ChecksumType.sha1.isValid(sha1)) {
            response.sendError(SC_NOT_FOUND, "Checksum deploy failed. Invalid SHA1: " + sha1, log);
            return;
        }
        InputStream inputStream = null;
        try {
            inputStream = jcrService.getDataStreamBySha1Checksum(sha1);
            if (inputStream == null) {
                response.sendError(SC_NOT_FOUND, "Checksum deploy failed. No existing file with SHA1: " + sha1, log);
                return;
            }
            uploadItemWithContent(request, response, repo, res, inputStream);
        } catch (DataStoreException e) {
            log.error("Failed to read stream for SHA1: " + sha1, e);
            response.sendError(SC_NOT_FOUND, "Checksum deploy failed. View log for more details.", log);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void uploadItemWithReusedOrProvidedContent(ArtifactoryRequest request, ArtifactoryResponse response,
            LocalRepo repo, RepoResource res) throws IOException, RepoRejectException {

        log.debug("Client '{}' supports Expect 100/continue", request.getHeader("User-Agent"));
        String sha1 = HttpUtils.getSha1Checksum(request);
        if (StringUtils.isNotBlank(sha1)) {
            log.debug("Expect continue deploy to '{}' with SHA1: {}", res.getRepoPath(), sha1);
            if (ChecksumType.sha1.isValid(sha1)) {
                InputStream inputStream = null;
                try {
                    inputStream = jcrService.getDataStreamBySha1Checksum(sha1);
                    if (inputStream != null) {
                        uploadItemWithContent(request, response, repo, res, inputStream);
                        return;
                    }
                } catch (DataStoreException e) {
                    log.warn("Could not get original stream from with SHA1 '{}': {}", sha1, e.getMessage());
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }
            }
        }
        uploadItemWithProvidedContent(request, response, repo, res);
    }

    private void uploadItemWithProvidedContent(ArtifactoryRequest request, ArtifactoryResponse response,
            LocalRepo repo, RepoResource res) throws IOException, RepoRejectException {
        InputStream inputStream = null;
        try {
            long remoteUploadStartTime = System.currentTimeMillis();
            inputStream = request.getInputStream();
            uploadItemWithContent(request, response, repo, res, inputStream);
            fireUploadTrafficEvent(res, remoteUploadStartTime);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void uploadItemWithContent(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo,
            RepoResource res, InputStream inputStream) throws RepoRejectException, IOException {
        //Update the last modified
        long lastModified = request.getLastModified() > 0 ? request.getLastModified() : System.currentTimeMillis();
        ((MutableRepoResourceInfo) res.getInfo()).setLastModified(lastModified);

        Properties properties = null;
        RepoPath repoPath = res.getRepoPath();
        if (authService.canAnnotate(repoPath)) {
            properties = request.getProperties();
        }
        SaveResourceContext.Builder contextBuilder = new SaveResourceContext.Builder(res, inputStream)
                .properties(properties);
        populateItemInfoFromHeaders(request, res, contextBuilder);
        try {
            RepoResource resource = repo.saveResource(contextBuilder.build());
            if (!resource.isFound()) {
                response.sendError(SC_NOT_FOUND, ((UnfoundRepoResource) resource).getReason(), log);
                return;
            }

            indexJarIfNeeded(request, repoPath);
            sendSuccessfulResponse(request, response, repoPath, false);
        } catch (BadPomException bpe) {
            response.sendError(HttpStatus.SC_CONFLICT, bpe.getMessage(), log);
        }
    }

    private void sendSuccessfulResponse(ArtifactoryRequest request, ArtifactoryResponse response, RepoPath repoPath,
            boolean isDirectory) throws IOException {
        String url = buildArtifactUrl(request, repoPath);
        successfulDeploymentResponseHelper.writeSuccessfulDeploymentResponse(repoService, response, repoPath,
                url, isDirectory);
    }

    private void fireUploadTrafficEvent(RepoResource resource, long remoteUploadStartTime) {
        if (remoteUploadStartTime > 0) {
            // fire upload event only if the resource is really uploaded from the remote client
            UploadEntry uploadEntry = new UploadEntry(resource.getRepoPath().getId(),
                    resource.getSize(), System.currentTimeMillis() - remoteUploadStartTime);
            trafficService.handleTrafficEntry(uploadEntry);
        }
    }

    private String buildArtifactUrl(ArtifactoryRequest request, RepoPath repoPath) {
        return request.getServletContextUrl() + "/" + repoPath.getRepoKey() + "/" + repoPath.getPath();
    }

    private void populateItemInfoFromHeaders(ArtifactoryRequest request, RepoResource res,
            SaveResourceContext.Builder contextBuilder) {
        if (authService.isAdmin()) {

            setItemLastModifiedInfoFromHeaders(request, res);
            setItemCreatedInfoFromHeaders(request, contextBuilder);
            setItemCreatedByInfoFromHeaders(request, contextBuilder);
            setItemModifiedInfoFromHeaders(request, contextBuilder);
        }
    }

    private void setItemLastModifiedInfoFromHeaders(ArtifactoryRequest request, RepoResource res) {
        String lastModifiedString = request.getHeader(ArtifactoryRequest.LAST_MODIFIED);
        if (StringUtils.isNotBlank(lastModifiedString)) {
            long lastModified = Long.parseLong(lastModifiedString);
            if (lastModified > 0) {
                ((MutableRepoResourceInfo) res.getInfo()).setLastModified(lastModified);
            }
        }
    }

    private void setItemCreatedInfoFromHeaders(ArtifactoryRequest request, SaveResourceContext.Builder contextBuilder) {
        String createdString = request.getHeader(ArtifactoryRequest.CREATED);
        if (StringUtils.isNotBlank(createdString)) {
            long created = Long.parseLong(createdString);
            if (created > 0) {
                contextBuilder.created(created);
            }
        }
    }

    private void setItemCreatedByInfoFromHeaders(ArtifactoryRequest request,
            SaveResourceContext.Builder contextBuilder) {
        String createBy = request.getHeader(ArtifactoryRequest.CREATED_BY);
        if (StringUtils.isNotBlank(createBy)) {
            contextBuilder.createdBy(createBy);
        }
    }

    private void setItemModifiedInfoFromHeaders(ArtifactoryRequest request,
            SaveResourceContext.Builder contextBuilder) {
        String modifiedBy = request.getHeader(ArtifactoryRequest.MODIFIED_BY);
        if (StringUtils.isNotBlank(modifiedBy)) {
            contextBuilder.modifiedBy(modifiedBy);
        }
    }

    private void setFileInfoChecksums(ArtifactoryRequest request, MutableFileInfo fileInfo, boolean checksumDeploy) {
        if (checksumDeploy || (request instanceof InternalArtifactoryRequest &&
                ((InternalArtifactoryRequest) request).isTrustServerChecksums())) {
            fileInfo.createTrustedChecksums();
            return;
        }

        // set checksums if attached to the request headers
        String sha1 = HttpUtils.getSha1Checksum(request);
        String md5 = HttpUtils.getMd5Checksum(request);
        if (StringUtils.isNotBlank(sha1) || StringUtils.isNotBlank(md5)) {
            Set<ChecksumInfo> checksums = Sets.newHashSet();
            if (StringUtils.isNotBlank(sha1)) {
                log.debug("Found sha1 '{}' for file '{}", sha1, fileInfo.getRepoPath());
                checksums.add(new ChecksumInfo(ChecksumType.sha1, sha1, null));
            }
            if (StringUtils.isNotBlank(md5)) {
                log.debug("Found md5 '{}' for file '{}", md5, fileInfo.getRepoPath());
                checksums.add(new ChecksumInfo(ChecksumType.md5, md5, null));
            }
            fileInfo.setChecksums(checksums);
        }
    }

    private void indexJarIfNeeded(ArtifactoryRequest request, RepoPath repoPath) {
        //Async index the uploaded file if needed
        MimeType ct = NamingUtils.getMimeType(repoPath.getPath());
        boolean indexJar = ct.isArchive() && ct.isIndex();
        // internal request can flag not to index (to allow manual control)
        if (indexJar && request instanceof InternalArtifactoryRequest) {
            indexJar = !((InternalArtifactoryRequest) request).isSkipJarIndexing();
        }

        if (indexJar) {
            searchService.asyncIndex(repoPath);
        }
    }

    private String adjustMavenSnapshotPath(LocalRepo repo, ArtifactoryRequest request) {
        String path = request.getPath();
        ModuleInfo itemModuleInfo = repo.getItemModuleInfo(path);
        MavenSnapshotVersionAdapter adapter = repo.getMavenSnapshotVersionAdapter();
        MavenSnapshotVersionAdapterContext context = new MavenSnapshotVersionAdapterContext(
                repo.getRepoPath(path), itemModuleInfo);

        Properties properties = request.getProperties();
        if (properties != null) {
            Set<String> timestamps = properties.get("build.timestamp");
            if (CollectionUtils.notNullOrEmpty(timestamps)) {
                context.setTimestamp(timestamps.iterator().next());
            }
        }
        String adjustedPath = adapter.adaptSnapshotPath(context);
        if (!adjustedPath.equals(path)) {
            log.debug("Snapshot file path '{}' adjusted to: '{}'", path, adjustedPath);
        }
        return adjustedPath;
    }

    /**
     * Returns the internal interface of the service for transaction management.
     */
    private InternalUploadService getInternalMe() {
        return InternalContextHelper.get().beanForType(InternalUploadService.class);
    }
}