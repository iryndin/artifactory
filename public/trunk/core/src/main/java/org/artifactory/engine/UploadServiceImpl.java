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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.fs.ChecksumsInfo;
import org.artifactory.api.fs.FileInfoImpl;
import org.artifactory.api.fs.MetadataInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.md.Properties;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.request.ArtifactoryRequest;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.MavenModelUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.MetadataResource;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.search.InternalSearchService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import static org.apache.commons.httpclient.HttpStatus.SC_CONFLICT;
import static org.apache.commons.httpclient.HttpStatus.SC_NOT_FOUND;
import static org.artifactory.descriptor.repo.SnapshotVersionBehavior.*;

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

    public void process(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException {
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

        StatusHolder statusHolder = repoService.assertValidDeployPath(localRepo, request.getPath());
        if (statusHolder.isError()) {
            //Test if we need to require http authorization
            int returnCode = statusHolder.getStatusCode();
            if (returnCode == HttpStatus.SC_FORBIDDEN && authService.isAnonymous()) {
                //Transform a forbidden to unauthorized if received for an anonymous user
                String msg = statusHolder.getStatusMsg();
                String realmName = authenticationEntryPoint.getRealmName();
                response.sendAuthorizationRequired(msg, realmName);
            } else {
                response.sendError(statusHolder);
            }
            return;
        }

        //Get the internal implementation since the method is annotated with org.artifactory.api.repo.Request
        getInternalMe().doProcess(request, response, localRepo);
    }

    public void doProcess(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo)
            throws IOException {
        String path = request.getPath();

        if (NamingUtils.isChecksum(path)) {
            processChecksumUploadRequest(request, response, repo);
            return;
        }

        InputStream is = request.getInputStream();
        MavenArtifactInfo mavenInfo = ArtifactResource.getMavenInfo(new RepoPath(repo.getKey(), path));
        if (mavenInfo.isValid() && mavenInfo.isSnapshot()) {
            SnapshotVersionBehavior snapshotBehavior = repo.getSnapshotVersionBehavior();
            if (MavenNaming.isMavenMetadata(path) && !snapshotBehavior.equals(DEPLOYER)) {
                // if the snapshot behavior is not DEPLOYER just skip the metadata deployment
                // it was probably recalculated after the pom deploy and we want to use the calculated one
                log.trace("Skipping deployment of maven metadata file {}", path);
                response.sendOk();
                return;
            }
            //Adjust snapshot path if needed
            path = adjustSnapshotPath(repo, path, mavenInfo, snapshotBehavior);
            //Adjust snapshot metadata if needed
            if (MavenNaming.isSnapshotMavenMetadata(path)) {
                Metadata metadata = adjustSnapshotMetadata(request, repo, path, snapshotBehavior);
                String metadataStr = MavenModelUtils.mavenMetadataToString(metadata);
                is = new ByteArrayInputStream(metadataStr.getBytes("utf-8"));
            }
        }

        RepoResource res;
        RepoPath repoPath = new RepoPath(repo.getKey(), path);
        Properties properties = null;
        if (NamingUtils.isMetadata(path)) {
            res = new MetadataResource(new MetadataInfo(repoPath));
        } else {
            FileInfoImpl fileInfo = new FileInfoImpl(repoPath);
            //fileInfo.createTrustedChecksums();
            res = new FileResource(fileInfo);
            if (authService.canAnnotate(repoPath)) {
                properties = request.getProperties();
            }
        }

        //Update the last modified
        long lastModified = request.getLastModified() > 0 ? request.getLastModified() : System.currentTimeMillis();
        res.getInfo().setLastModified(lastModified);

        try {
            RepoResource resource = repo.saveResource(res, is, properties);
            if (!resource.isFound()) {
                response.sendError(SC_NOT_FOUND, ((UnfoundRepoResource) resource).getReason(), log);
                return;
            }
            //Async index the uploaded file if needed
            ContentType contentType = NamingUtils.getContentType(repoPath.getPath());
            if (contentType.isJarVariant()) {
                boolean index;
                try {
                    index = !Boolean.parseBoolean(request.getParameter(ArtifactoryRequest.SKIP_JAR_INDEXING));
                } catch (IllegalStateException ise) {
                    log.debug("Unable to retrieve parameter '" + ArtifactoryRequest.SKIP_JAR_INDEXING + "'.", ise);
                    index = true;
                }
                if (index) {
                    searchService.asyncIndex(repoPath);
                }
            }
        } finally {
            IOUtils.closeQuietly(is);
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
            checksumPath = adjustSnapshotPath(repo, checksumPath, mavenInfo, repo.getSnapshotVersionBehavior());
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

        if (checksum.equalsIgnoreCase(checksumInfo.getActual())) {
            response.sendOk();
        } else {
            response.sendError(SC_CONFLICT, "Checksum error: received '" + checksum +
                    "' but actual is '" + checksumInfo.getActual() + "'", log);
        }
    }

    private String adjustSnapshotPath(LocalRepo repo, String path, MavenArtifactInfo mavenInfo,
            SnapshotVersionBehavior snapshotVersionBehavior) {
        String adjustedPath = path;
        boolean notMetadataArtifact = !NamingUtils.isMetadata(path) && !NamingUtils.isMetadataChecksum(path);
        if (notMetadataArtifact) {
            if (snapshotVersionBehavior.equals(NONUNIQUE)) {
                //Replace version timestamp with SNAPSHOT for non-unique snapshot repo
                String classifier = mavenInfo.getClassifier();
                String fileExtension = getFileExtensionForModifiedPath(path, mavenInfo);
                adjustedPath = path.substring(0, getLastPathSeparatorIndex(path) + 1) +
                        mavenInfo.getArtifactId() + "-" + mavenInfo.getVersion() +
                        (StringUtils.isEmpty(classifier) ? "" : "-" + classifier) + "." + fileExtension;
            } else if (snapshotVersionBehavior.equals(UNIQUE)) {
                //Replace SNAPSHOT with version timestamp for unique snapshot repo
                String fileName = PathUtils.getName(path);
                if (!MavenNaming.isUniqueSnapshotFileName(fileName)) {
                    adjustedPath = adjustNonUniqueSnapshotToUnique(repo, path, mavenInfo);
                }
            } // else it is deployer - don't change the path
        }
        if (!adjustedPath.equals(path)) {
            log.debug("Snapshot file path '{}' adjusted to: '{}'", path, adjustedPath);
        }

        return adjustedPath;
    }

    private int getLastPathSeparatorIndex(String filePath) {
        return filePath.lastIndexOf('/');
    }

    private String adjustNonUniqueSnapshotToUnique(LocalRepo repo, String filePath, MavenArtifactInfo mavenInfo) {
        //Get the latest build number from the metadata
        String versionMetadataPath = PathUtils.getParent(filePath) + "/maven-metadata.xml";
        int metadataBuildNumber = getLastBuildNumber(repo, versionMetadataPath);
        int nextBuildNumber = metadataBuildNumber + 1;

        RepoPath parentPath = new RepoPath(repo.getKey(), PathUtils.getParent(filePath));

        // determine if the next build number should be the one read from the metadata
        String classifier = mavenInfo.getClassifier();
        if (metadataBuildNumber > 0 && (StringUtils.isNotBlank(classifier) || MavenNaming.isChecksum(filePath))) {
            // checksums and artifacts with classifier are always deployed after the pom (which triggers the
            // maven-metadata.xml calculation) so use the metadata build number
            nextBuildNumber = metadataBuildNumber;
        }
        if (metadataBuildNumber > 0 && MavenNaming.isPom(filePath)) {
            // the metadata might already represent an existing main artifact (in case the
            // maven-metadata.xml was deployed after the main artifact and before the pom/classifier)
            // so first check if there's already a file with the next build number
            if (getSnapshotFile(parentPath, metadataBuildNumber + 1) == null) {
                // no files deployed with the next build number so either this is a pom deployment (parent pom)
                // or this is a pom of a main artifact for which the maven-metadata.xml was deployed before this pom
                if (getSnapshotPomFile(parentPath, metadataBuildNumber) == null) {
                    // this is a pom attached to a main artifact deployed after maven-metadata.xml
                    nextBuildNumber = metadataBuildNumber;
                }
            }
        }

        String timestamp = getSnapshotTimestamp(parentPath, nextBuildNumber);
        if (timestamp == null) {
            // probably the first deployed file for this build, use now for the timestamp
            timestamp = MavenModelUtils.dateToUniqueSnapshotTimestamp(new Date());
        }

        // replace the SNAPSHOT string with timestamp-buildNumber
        String version = mavenInfo.getVersion();
        String uniqueVersion = version.substring(0, version.lastIndexOf("SNAPSHOT")) +
                timestamp + "-" + nextBuildNumber;

        // compose the path extension (special case when it's a checksum path - name.jar.sha1)
        String fileExtension = getFileExtensionForModifiedPath(filePath, mavenInfo);

        String adjustedPath = FilenameUtils.getPath(filePath) +
                mavenInfo.getArtifactId() + "-" + uniqueVersion +
                (StringUtils.isEmpty(classifier) ? "" : "-" + classifier) + "." + fileExtension;
        return adjustedPath;
    }

    private String getFileExtensionForModifiedPath(String filePath, MavenArtifactInfo mavenInfo) {
        String fileExtension;
        if (MavenNaming.isChecksum(filePath)) {
            String[] extensions = filePath.split("\\.");
            fileExtension = extensions[extensions.length - 1];
            if (extensions.length >= 2) {
                fileExtension = extensions[extensions.length - 2] + "." + fileExtension;
            }
        } else {
            fileExtension = mavenInfo.getType();
        }
        return fileExtension;
    }

    private Metadata adjustSnapshotMetadata(ArtifactoryRequest request, LocalRepo repo, String snapshotMetadataPath,
            SnapshotVersionBehavior snapshotVersionBehavior) throws IOException {
        //See if the user requested deployment path is for a non-unique snapshot
        boolean deployerUsedNonUniqueSnapshot = MavenNaming.isNonUniqueSnapshot(snapshotMetadataPath);
        boolean nonUniqueSnapshotVersions =
                snapshotVersionBehavior.equals(NONUNIQUE) ||
                        (deployerUsedNonUniqueSnapshot && snapshotVersionBehavior.equals(DEPLOYER));
        Metadata metadata = MavenModelUtils.toMavenMetadata(request.getInputStream());
        Versioning versioning = metadata.getVersioning();
        if (versioning != null) {
            Snapshot snap = versioning.getSnapshot();
            if (nonUniqueSnapshotVersions && snap != null && snap.getTimestamp() != null) {
                //For specific version metadata, remove the <timestamp> tag. This is
                //necessary for plugin dependencies with no specified version, to be
                //able to resolve the latest artifact (otherwise they will try to locate
                //a file name based on the timestamp).
                snap.setTimestamp(null);
            } else if (snapshotVersionBehavior.equals(UNIQUE)) {
                //Update the build number if we force unique snapshot versions
                if (snap == null) {
                    snap = new Snapshot();
                }

                int metadataBuildNumber = getLastBuildNumber(repo, snapshotMetadataPath);
                int nextBuildNumber = metadataBuildNumber + 1;
                snap.setBuildNumber(nextBuildNumber);

                // If the timestamp is null (for example if distribution management uniqueVersion set to false)
                if (snap.getTimestamp() == null) {
                    String parentPath = PathUtils.getParent(snapshotMetadataPath);
                    RepoPath repoPath = new RepoPath(repo.getKey(), parentPath);
                    // get the timestamp from the deployed filename with the same build number
                    String timestamp = getSnapshotTimestamp(repoPath, nextBuildNumber);
                    snap.setTimestamp(timestamp);
                }
            }
        }
        return metadata;
    }

    /**
     * @param repo                The repository to deploy to
     * @param versionMetadataPath Path to a snapshot maven metadata (ie ends with '-SNAPSHOT/maven-metadata.xml')
     * @return The last build number for snapshot version. 0 if maven-metadata not found for the path.
     */
    private int getLastBuildNumber(LocalRepo repo, String versionMetadataPath) {
        int buildNumber = 0;
        try {
            // get the parent path (remove the maven-metadata.xml)
            String parentPath = PathUtils.getParent(versionMetadataPath);
            RepoPath repoPath = new RepoPath(repo.getKey(), parentPath);
            if (repoService.exists(repoPath) &&
                    repoService.hasMetadata(repoPath, MavenNaming.MAVEN_METADATA_NAME)) {
                String mavenMetadataStr = repoService.getXmlMetadata(repoPath, MavenNaming.MAVEN_METADATA_NAME);
                Metadata metadata = MavenModelUtils.toMavenMetadata(mavenMetadataStr);
                Versioning versioning = metadata.getVersioning();
                if (versioning != null) {
                    Snapshot snapshot = versioning.getSnapshot();
                    if (snapshot != null) {
                        buildNumber = snapshot.getBuildNumber();
                    }
                }
            } else {
                // ok probably not found. just log
                log.debug("No maven metadata found for {}.", versionMetadataPath);
            }
        } catch (Exception e) {
            log.error("Cannot obtain build number from metadata.", e);
        }
        return buildNumber;
    }

    /**
     * @param snapshotDirectoryPath Path to a repository snapshot directory (eg, /a/path/1.0-SNAPSHOT)
     * @param buildNum              The file with build number to search for
     * @return The timestamp of the unique snapshot file with the input build number.
     */
    private String getSnapshotTimestamp(RepoPath snapshotDirectoryPath, int buildNum) {
        String snapshotFile = getSnapshotFile(snapshotDirectoryPath, buildNum);
        if (snapshotFile != null) {
            int childBuildNumber = MavenNaming.getUniqueSnapshotVersionBuildNumber(snapshotFile);
            if (childBuildNumber == buildNum) {
                String timestamp = MavenNaming.getUniqueSnapshotVersionTimestamp(snapshotFile);
                log.debug("Extracted timestamp {} from {}", timestamp, snapshotFile);
                return timestamp;
            }
        }
        log.debug("Snapshot timestamp not found in {} for build number {}", snapshotDirectoryPath, buildNum);
        return null;
    }

    /**
     * @param snapshotDirectoryPath Path to a repository snapshot directory (eg, /a/path/1.0-SNAPSHOT)
     * @param buildNum              The file with build number to search for
     * @return The path of the first unique snapshot file with the input build number.
     */
    private String getSnapshotFile(RepoPath snapshotDirectoryPath, int buildNum) {
        return getSnapshotFile(snapshotDirectoryPath, buildNum, null);
    }

    /**
     * @param snapshotDirectoryPath Path to a repository snapshot directory (eg, /a/path/1.0-SNAPSHOT)
     * @param buildNum              The file with build number to search for
     * @return The path of the unique snapshot pom file with the input build number.
     */
    private String getSnapshotPomFile(RepoPath snapshotDirectoryPath, int buildNum) {
        return getSnapshotFile(snapshotDirectoryPath, buildNum, "pom");
    }

    /**
     * @param snapshotDirectoryPath Path to a repository snapshot directory (eg, /a/path/1.0-SNAPSHOT)
     * @param buildNum              The file with build number to search for
     * @param fileType              The file type to search for. Use null for any type
     * @return The path of the first unique snapshot file with the input build number.
     */
    private String getSnapshotFile(RepoPath snapshotDirectoryPath, int buildNum, String fileType) {
        log.debug("Searching for unique snapshot file in {} with build number {}",
                snapshotDirectoryPath, buildNum);
        if (repoService.exists(snapshotDirectoryPath)) {
            List<String> children = repoService.getChildrenNames(snapshotDirectoryPath);
            for (String child : children) {
                if ((fileType == null || fileType.equals(PathUtils.getExtension(child))) &&
                        //In all cases - child must be a unique snapshot for the build number extraction
                        MavenNaming.isUniqueSnapshotFileName(child)) {
                    int childBuildNumber = MavenNaming.getUniqueSnapshotVersionBuildNumber(child);
                    if (childBuildNumber == buildNum) {
                        log.debug("Found unique snapshot with build number {}: {}", buildNum, child);
                        return child;
                    }
                }
            }
        }
        log.debug("Unique snapshot file not found in {} for build number {}", snapshotDirectoryPath, buildNum);
        return null;
    }

    /**
     * Returns the internal interface of the service
     *
     * @return InternalUploadService
     */
    private InternalUploadService getInternalMe() {
        return InternalContextHelper.get().beanForType(InternalUploadService.class);
    }
}
