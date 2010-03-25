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

package org.artifactory.repo.service;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.artifactory.api.artifact.UnitInfo;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.DeployService;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.repo.exception.maven.BadPomException;
import org.artifactory.api.request.UploadService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.MavenModelUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.request.InternalArtifactoryResponse;
import org.artifactory.search.InternalSearchService;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.PathUtils;
import org.artifactory.util.ZipUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * Provides artifacts deploy services.
 *
 * @author Yossi Shaul
 */
@Service
public class DeployServiceImpl implements DeployService {
    private static final Logger log = LoggerFactory.getLogger(DeployServiceImpl.class);

    @Autowired
    private InternalRepositoryService repositoryService;

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private InternalSearchService searchService;

    @Autowired
    private UploadService uploadService;

    public void deploy(RepoDescriptor targetRepo, UnitInfo artifactInfo, File file) throws RepoAccessException {
        String pomString = getPomModelString(file);
        deploy(targetRepo, artifactInfo, file, pomString, false, false);
    }

    public void deploy(RepoDescriptor targetRepo, UnitInfo artifactInfo,
            final File fileToDeploy, String pomString, boolean forceDeployPom, boolean partOfBundleDeploy)
            throws RepoAccessException {

        validatePath(artifactInfo);

        if (!artifactInfo.isValid()) {
            throw new IllegalArgumentException("Invalid artifact details.");
        }
        //Sanity check
        if (targetRepo == null) {
            throw new IllegalArgumentException("No target repository selected for deployment.");
        }
        final LocalRepo localRepo = repositoryService.localRepositoryByKey(targetRepo.getKey());
        if (localRepo == null) {
            throw new IllegalArgumentException("No target repository found for deployment.");
        }
        //Check acceptance according to include/exclude patterns
        String path = artifactInfo.getPath();
        StatusHolder statusHolder = repositoryService.assertValidDeployPath(localRepo, path);
        if (statusHolder.isError()) {
            throw new IllegalArgumentException(statusHolder.getStatusMsg());
        }
        RepoPath repoPath = new RepoPath(targetRepo.getKey(), path);
        if (!authService.canDeploy(repoPath)) {
            AccessLogger.deployDenied(repoPath);
            throw new RepoAccessException("Not enough permissions to deploy artifact '" + fileToDeploy + "'.", repoPath,
                    "deploy", authService.currentUsername());
        }
        // upload the main file
        try {
            ArtifactoryDeployRequest request = new ArtifactoryDeployRequest(repoPath, fileToDeploy);
            request.setSkipJarIndexing(partOfBundleDeploy);
            InternalArtifactoryResponse response = new InternalArtifactoryResponse();
            uploadService.process(request, response);
            if (response.getException() != null) {
                throw new RuntimeException("Cannot deploy file " + fileToDeploy.getName(), response.getException());
            }
        } catch (IOException e) {
            String msg = "Cannot deploy file " + fileToDeploy.getName() + ". Cause: " + e.getMessage();
            log.debug(msg, e);
            throw new RepositoryRuntimeException(msg, e);
        }

        //Handle extra pom deployment - add the metadata with the generated pom file to the artifact
        if (forceDeployPom && artifactInfo.isMavenArtifact()) {
            MavenArtifactInfo mavenArtifactInfo = (MavenArtifactInfo) artifactInfo;
            RepoPath pomPath = new RepoPath(repoPath.getParent(),
                    mavenArtifactInfo.getArtifactId() + "-" + mavenArtifactInfo.getVersion() + ".pom");
            RepoPath uploadPomPath = new RepoPath(targetRepo.getKey(), pomPath.getPath());
            try {
                ArtifactoryDeployRequest pomRequest = new ArtifactoryDeployRequest(
                        uploadPomPath, IOUtils.toInputStream(pomString), fileToDeploy.length(),
                        fileToDeploy.lastModified());
                InternalArtifactoryResponse pomResponse = new InternalArtifactoryResponse();
                // upload the POM if needed
                uploadService.process(pomRequest, pomResponse);
                if (pomResponse.getException() != null) {
                    throw new RuntimeException("Cannot deploy file " + fileToDeploy.getName(),
                            pomResponse.getException());
                }
            } catch (IOException e) {
                String msg = "Cannot deploy file " + pomPath.getName() + ". Cause: " + e.getMessage();
                log.debug(msg, e);
                throw new RepositoryRuntimeException(msg, e);
            }
        }
    }

    private void validatePath(UnitInfo artifactInfo) {
        if (PathUtils.isDirectoryPath(artifactInfo.getPath())) {
            throw new IllegalArgumentException("Cannot deploy an artifact file using a directory path.");
        }
    }

    @SuppressWarnings({"unchecked"})
    public void deployBundle(File bundle, RealRepoDescriptor targetRepo, StatusHolder status) {
        long start = System.currentTimeMillis();
        if (!bundle.exists()) {
            String message =
                    "Specified location '" + bundle + "' does not exist. Deployment aborted.";
            status.setError(message, log);
            return;
        }
        File extractFolder;
        try {
            extractFolder = extractArchive(status, bundle);
        } catch (Exception e) {
            if (!status.isVerbose()) {
                status.setVerbose(true);
            }
            status.setError("A problem has occurred during extraction", e, log);
            return;
        }
        if (extractFolder == null) {
            //We have errors
            return;
        }
        try {
            IOFileFilter deployableFilesFilter = new AbstractFileFilter() {
                @Override
                public boolean accept(File file) {
                    return !MavenNaming.isChecksum(file) && !NamingUtils.isSystem(file.getAbsolutePath());
                }
            };
            Collection<File> archiveContent = FileUtils.listFiles(
                    extractFolder, deployableFilesFilter, DirectoryFileFilter.DIRECTORY);
            List<File> deployFailedList = Lists.newArrayList();
            for (File file : archiveContent) {
                String parentPath = extractFolder.getAbsolutePath();
                String pomPath = file.getAbsolutePath();
                String relPath = PathUtils.getRelativePath(parentPath, pomPath);
                if (MavenNaming.isPom(file.getName())) {
                    try {
                        validatePom(file, relPath, targetRepo.isSuppressPomConsistencyChecks());
                    } catch (Exception e) {
                        String msg =
                                "The pom: " + file.getName() +
                                        " could not be validated, and thus was not deployed.";
                        status.setWarning(msg, log);
                        continue;
                    }
                }
                MavenArtifactInfo artifactInfo =
                        MavenArtifactInfo.fromRepoPath(new RepoPath(targetRepo.getKey(), relPath));
                if (!artifactInfo.isValid()) {
                    deployFailedList.add(file);
                } else {
                    try {
                        String pomString = getPomModelString(file);
                        getTransactionalMe().deploy(targetRepo, artifactInfo, file, pomString, false, true);
                    } catch (IllegalArgumentException iae) {
                        status.setWarning(iae.getMessage(), log);
                    }
                    catch (Exception e) {
                        String msg = "Error during deployment";
                        status.setError(msg, e, log);
                    }
                }
            }

            String bundleName = bundle.getName();
            String timeTaken = DurationFormatUtils.formatPeriod(start, System.currentTimeMillis(), "s");
            int artifactsFailed = deployFailedList.size();
            int archiveContentSize = archiveContent.size();

            if (artifactsFailed == 0) {
                status.setStatus(
                        "Successfully deployed " + archiveContentSize + " artifacts from archive: " + bundleName
                                + " (" + timeTaken + " seconds).", log);
            } else if ((artifactsFailed > 0) && (artifactsFailed < archiveContentSize)) {
                status.setWarning(artifactsFailed + " out of " + archiveContentSize +
                        " artifacts have failed to deploy.", log);
            } else {
                status.setError("Deployment of archive: " + bundleName + " has failed: no valid artifacts found", log);
            }
            //Trigger indexing for marked files
            searchService.indexMarkedArchives();
        } catch (Exception e) {
            status.setError(e.getMessage(), e, log);
        } finally {
            FileUtils.deleteQuietly(extractFolder);
        }
    }

    public MavenArtifactInfo getArtifactInfo(File uploadedFile) {
        return MavenModelUtils.artifactInfoFromFile(uploadedFile);
    }

    public String getPomModelString(File file) {
        if (MavenNaming.isPom(file.getAbsolutePath())) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                return IOUtils.toString(fileInputStream);
            } catch (IOException e) {
                log.error("The following error occurred while reading {} {}", file.getAbsolutePath(), e);
            }
        }
        String pomFromJar = MavenModelUtils.getPomFileAsStringFromJar(file);
        if (StringUtils.isNotBlank(pomFromJar)) {
            return pomFromJar;
        }
        MavenArtifactInfo model = getArtifactInfo(file);
        return MavenModelUtils.mavenModelToString(MavenModelUtils.toMavenModel(model));
    }

    private File extractArchive(StatusHolder status, File archive) throws Exception {
        String archiveName = archive.getName();
        String fixedArchiveName = new String(archiveName.getBytes("utf-8"));
        File fixedArchive = new File(archive.getParentFile(), fixedArchiveName);
        boolean isRenamed = archive.renameTo(fixedArchive);
        if (!isRenamed) {
            throw new Exception("Could not encode archive name to UTF-8.");
        }
        File extractFolder = new File(ContextHelper.get().getArtifactoryHome().getTmpUploadsDir(),
                fixedArchive.getName() + "_extracted_" + System.currentTimeMillis());
        if (extractFolder.exists()) {
            //Clean up any existing folder
            try {
                FileUtils.deleteDirectory(extractFolder);
            } catch (IOException e) {
                status.setError("Could not delete existing extracted archive folder: " +
                        extractFolder.getAbsolutePath() + ".", e, log);
                return null;
            }
        }
        try {
            FileUtils.forceMkdir(extractFolder);
        } catch (IOException e) {
            log.error("Could not created the extracted archive folder: " +
                    extractFolder.getAbsolutePath() + ".", log);
            return null;
        }

        try {
            ZipUtils.extract(fixedArchive, extractFolder);
        } catch (Exception e) {
            FileUtils.deleteQuietly(extractFolder);
            if (e.getMessage() == null) {
                String errorMessage;
                if (e instanceof IllegalArgumentException) {
                    errorMessage =
                            "Please make sure the textual values in the archive are encoded in UTF-8.";
                } else {
                    errorMessage = "Please ensure the integrity of the selected archive";
                }
                throw new Exception(errorMessage, e);
            }
            throw e;
        }
        return extractFolder;
    }

    public void validatePom(String pomContent, String relPath, boolean suppressPomConsistencyChecks)
            throws IOException {
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        File tempFile = File.createTempFile("pom.validation", ".tmp", artifactoryHome.getWorkTmpDir());
        try {
            FileUtils.writeStringToFile(tempFile, pomContent, "utf-8");
            validatePom(tempFile, relPath, suppressPomConsistencyChecks);
        } finally {
            FileUtils.forceDelete(tempFile);
        }
    }

    private static void validatePom(File pomFile, String relPath, boolean suppressPomConsistencyChecks)
            throws BadPomException {
        InputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(pomFile));
            MavenModelUtils.validatePomTargetPath(inputStream, relPath, suppressPomConsistencyChecks);
        } catch (Exception e) {
            String message = "Error while validating POM for path: " + relPath +
                    ". Please assure the validity of the POM file.";
            throw new BadPomException(message);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private static DeployService getTransactionalMe() {
        return InternalContextHelper.get().beanForType(DeployService.class);
    }
}
