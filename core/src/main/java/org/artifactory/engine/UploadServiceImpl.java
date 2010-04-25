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
import org.apache.commons.io.IOUtils;
import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.fs.ChecksumsInfo;
import org.artifactory.api.fs.FileInfoImpl;
import org.artifactory.api.fs.MetadataInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.md.Properties;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepoRejectionException;
import org.artifactory.api.request.ArtifactoryRequest;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.request.InternalArtifactoryRequest;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.io.checksum.policy.LocalRepoChecksumPolicy;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.MetadataResource;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.search.InternalSearchService;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static org.apache.commons.httpclient.HttpStatus.SC_CONFLICT;
import static org.apache.commons.httpclient.HttpStatus.SC_NOT_FOUND;
import static org.artifactory.descriptor.repo.LocalRepoChecksumPolicyType.SERVER;
import static org.artifactory.descriptor.repo.SnapshotVersionBehavior.DEPLOYER;

@Service
public class UploadServiceImpl implements InternalUploadService {
    private static final Logger log = LoggerFactory.getLogger(UploadServiceImpl.class);

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private InternalRepositoryService repoService;

    @Autowired
    private BasicAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private InternalSearchService searchService;

    public void process(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException,
            RepoRejectionException {
        log.debug("Request: {}", request);

        String repoKey = request.getRepoKey();
        if (repoKey == null) {
            String msg = "No target local repository specified in deploy request.";
            response.sendError(HttpStatus.SC_BAD_REQUEST, msg, log);
            return;
        }

        //Get the proper file repository for deployment from the path
        LocalRepo localRepo = repoService.localRepositoryByKey(repoKey);
        if (localRepo == null) {
            String msg = "Could not find a local repository named " + repoKey + " to deploy to.";
            response.sendError(HttpStatus.SC_BAD_REQUEST, msg, log);
            return;
        }

        try {
            repoService.assertValidDeployPath(localRepo, request.getPath());
        } catch (RepoRejectionException e) {
            //Test if we need to require http authorization
            int returnCode = e.getErrorCode();
            if (returnCode == HttpStatus.SC_FORBIDDEN && authService.isAnonymous()) {
                //Transform a forbidden to unauthorized if received for an anonymous user
                String realmName = authenticationEntryPoint.getRealmName();
                response.sendAuthorizationRequired(e.getMessage(), realmName);
            } else {
                response.sendError(returnCode, e.getMessage(), log);
            }
            return;
        }

        //Get the internal implementation since the method is annotated with org.artifactory.api.repo.Request
        getInternalMe().doProcess(request, response, localRepo);
    }

    public void doProcess(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo)
            throws IOException, RepoRejectionException {
        String path = request.getPath();

        if (NamingUtils.isChecksum(path)) {
            processChecksumUploadRequest(request, response, repo);
            return;
        }

        MavenArtifactInfo mavenInfo = ArtifactResource.getMavenInfo(new RepoPath(repo.getKey(), path));
        SnapshotVersionBehavior snapshotBehavior = repo.getSnapshotVersionBehavior();
        // skip snapshot adapters if not valid maven info or using deployer policy
        if (!snapshotBehavior.equals(DEPLOYER) && mavenInfo.isValid() && mavenInfo.isSnapshot()) {
            if (MavenNaming.isMavenMetadata(path)) {
                // if the snapshot behavior is not DEPLOYER just skip the metadata deployment
                // it is recalculated after the pom deploy and we want to use the calculated one
                log.trace("Skipping deployment of maven metadata file {}", path);
                response.sendOk();
                return;
            }
            //Adjust snapshot path if needed
            path = adjustSnapshotPath(repo, path);
        }

        RepoResource res;
        RepoPath repoPath = new RepoPath(repo.getKey(), path);
        Properties properties = null;
        if (NamingUtils.isMetadata(path)) {
            res = new MetadataResource(new MetadataInfo(repoPath));
        } else {
            FileInfoImpl fileInfo = new FileInfoImpl(repoPath);
            if (request instanceof InternalArtifactoryRequest) {
                if (((InternalArtifactoryRequest) request).isTrustServerChecksums()) {
                    fileInfo.createTrustedChecksums();
                }
            }
            res = new FileResource(fileInfo);
            if (authService.canAnnotate(repoPath)) {
                properties = request.getProperties();
            }
        }

        //Update the last modified
        long lastModified = request.getLastModified() > 0 ? request.getLastModified() : System.currentTimeMillis();
        res.getInfo().setLastModified(lastModified);

        try {
            RepoResource resource = repo.saveResource(res, request.getInputStream(), properties);
            if (!resource.isFound()) {
                response.sendError(SC_NOT_FOUND, ((UnfoundRepoResource) resource).getReason(), log);
                return;
            }
            //Async index the uploaded file if needed
            if (NamingUtils.isJarVariant(repoPath.getPath())) {
                boolean indexJar = true;
                if (request instanceof InternalArtifactoryRequest) {
                    indexJar = ((InternalArtifactoryRequest) request).isSkipJarIndexing();
                }
                if (indexJar) {
                    searchService.asyncIndex(repoPath);
                }
            }
        } finally {
            IOUtils.closeQuietly(request.getInputStream());
        }
        //Send ok. Also for those artifacts that the wagon sent and we ignore (checksums etc.)
        response.sendOk();
    }

    /**
     * This method processes a checksum upload. Since checksums are stored as files' metadata (part of the file info),
     * we have to locate the file and update it's 'original' checksum with the value read from the request body.
     */
    private void processChecksumUploadRequest(ArtifactoryRequest request,
            ArtifactoryResponse response, LocalRepo repo) throws IOException {

        String checksumPath = request.getPath();
        MavenArtifactInfo mavenInfo = ArtifactResource.getMavenInfo(new RepoPath(repo.getKey(), checksumPath));
        if (mavenInfo.isValid() && mavenInfo.isSnapshot()) {
            checksumPath = adjustSnapshotPath(repo, checksumPath);
        }

        String checksumTargetFile = MavenNaming.getChecksumTargetFile(checksumPath);
        if (NamingUtils.isMetadata(checksumTargetFile)) {
            // (for now) we always return calculated checksums of metadata
            response.sendOk();
            return;
        }

        RepoPath filePath = new RepoPath(repo.getKey(), checksumTargetFile);
        JcrFsItem fsItem = repo.getLockedJcrFsItem(filePath);

        if (fsItem == null) {
            response.sendError(SC_NOT_FOUND, "Target file to set checksum on doesn't exist: " + filePath, log);
            return;
        }

        if (!fsItem.isFile()) {
            response.sendError(SC_CONFLICT, "Checksum only supported for files (but found folder): " + filePath, log);
            return;
        }

        if (request.getContentLength() > 1024) {
            // something is fishy, checksum file should not be so big...
            response.sendError(SC_CONFLICT, "Suspicious checksum file, content length of " + request.getContentLength()
                    + " bytes is bigger than allowed.", log);
            return;
        }

        String checksum;
        try {
            checksum = IOUtils.toString(request.getInputStream(), "UTF-8");
        } catch (IOException e) {
            response.sendError(SC_CONFLICT, "Failed to read checksum from file: " + e.getMessage(), log);
            return;
        }

        // ok everything looks good, lets set the checksum original value
        JcrFile jcrFile = (JcrFile) fsItem;
        ChecksumsInfo checksums = jcrFile.getInfo().getChecksumsInfo();
        ChecksumType checksumType = ChecksumType.forFilePath(checksumPath);
        if (!checksumType.isValid(checksum)) {
            log.warn("Uploading non valid original checksum for {}", filePath);
        }
        ChecksumInfo checksumInfo = checksums.getChecksumInfo(checksumType);
        if (checksumInfo == null) {
            checksumInfo = new ChecksumInfo(checksumType, checksum, null);
        } else {
            checksumInfo = new ChecksumInfo(checksumType, checksum, checksumInfo.getActual());
        }
        checksums.addChecksumInfo(checksumInfo);
        // check whether to verify the checksum sent by the client in accordance to the policy.
        if (checksum.equalsIgnoreCase(checksumInfo.getActual())) {
            response.sendOk();
        } else {
            String message = String.format("Checksum error: received '%s' but actual is '%s'",
                    checksum, checksumInfo.getActual());
            ChecksumPolicy checksumPolicy = repo.getChecksumPolicy();
            if (checksumPolicy instanceof LocalRepoChecksumPolicy &&
                    ((LocalRepoChecksumPolicy) checksumPolicy).getPolicyType().equals(SERVER)) {
                log.debug(message);
                response.sendOk();
            } else {
                response.sendError(SC_CONFLICT, message, log);
            }
        }
    }

    private String adjustSnapshotPath(LocalRepo repo, String path) {
        String adjustedPath = repo.getSnapshotVersionAdapter().adaptSnapshotPath(new RepoPath(repo.getKey(), path));
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
