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

package org.artifactory.repo.service;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.artifactory.api.artifact.ArtifactInfo;
import org.artifactory.api.artifact.UnitInfo;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.repo.DeployService;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.repo.exception.maven.BadPomException;
import org.artifactory.api.request.UploadService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.MavenModelUtils;
import org.artifactory.maven.PomTargetPathValidator;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoPath;
import org.artifactory.request.InternalArtifactoryResponse;
import org.artifactory.search.InternalSearchService;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.PathMatcher;
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
import java.util.Collections;
import java.util.List;

/**
 * Provides artifacts deploy services from the UI.
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

    public void deploy(RepoDescriptor targetRepo, UnitInfo artifactInfo, File file) throws RepoRejectException {
        String pomString = getPomModelString(file);
        deploy(targetRepo, artifactInfo, file, pomString, false, false);
    }

    public void deploy(RepoDescriptor targetRepo, UnitInfo artifactInfo, File fileToDeploy, String pomString,
            boolean forceDeployPom, boolean partOfBundleDeploy) throws RepoRejectException {
        String path = artifactInfo.getPath();
        if (!artifactInfo.isValid()) {
            throw new IllegalArgumentException("Invalid unit info for '" + path + "'.");
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
        //TODO: [by yl] assertValidDeployPath is already called by the upload service, including security checks!
        repositoryService.assertValidDeployPath(localRepo, path);
        RepoPath repoPath = new RepoPathImpl(targetRepo.getKey(), path);
        //TODO: [by yl] assertValidDeployPath is already checking deploy permissions!
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
            assertNotFailedRequest(fileToDeploy.getName(), response);
        } catch (IOException e) {
            String msg = "Cannot deploy file " + fileToDeploy.getName() + ". Cause: " + e.getMessage();
            log.debug(msg, e);
            throw new RepositoryRuntimeException(msg, e);
        }

        //Handle extra pom deployment - add the metadata with the generated pom file to the artifact
        if (forceDeployPom && artifactInfo.isMavenArtifact() && StringUtils.isNotBlank(pomString)) {
            MavenArtifactInfo mavenArtifactInfo = (MavenArtifactInfo) artifactInfo;
            RepoPath pomPath = new RepoPathImpl(repoPath.getParent(),
                    mavenArtifactInfo.getArtifactId() + "-" + mavenArtifactInfo.getVersion() + ".pom");
            RepoPath uploadPomPath = new RepoPathImpl(targetRepo.getKey(), pomPath.getPath());
            try {
                ArtifactoryDeployRequest pomRequest = new ArtifactoryDeployRequest(
                        uploadPomPath, IOUtils.toInputStream(pomString), fileToDeploy.length(),
                        fileToDeploy.lastModified());
                InternalArtifactoryResponse pomResponse = new InternalArtifactoryResponse();
                // upload the POM if needed
                uploadService.process(pomRequest, pomResponse);
                assertNotFailedRequest(fileToDeploy.getName(), pomResponse);
            } catch (IOException e) {
                String msg = "Cannot deploy file " + pomPath.getName() + ". Cause: " + e.getMessage();
                log.debug(msg, e);
                throw new RepositoryRuntimeException(msg, e);
            }
        }
    }

    private void assertNotFailedRequest(String deployedFileName, InternalArtifactoryResponse response)
            throws RepoRejectException {
        if (response.getException() != null) {
            throw new RuntimeException("Cannot deploy file " + deployedFileName, response.getException());
        } else if (!response.isSuccessful()) {
            StringBuilder errorMessageBuilder = new StringBuilder("Cannot deploy file '").append(deployedFileName).
                    append("'. ");
            String statusMessage = response.getStatusMessage();
            if (StringUtils.isNotBlank(statusMessage)) {
                errorMessageBuilder.append(statusMessage);
                if (!StringUtils.endsWith(statusMessage, ".")) {
                    errorMessageBuilder.append(".");
                }
            } else {
                errorMessageBuilder.append("Please view the logs for further information.");
            }
            throw new RepoRejectException(errorMessageBuilder.toString());
        }
    }

    @SuppressWarnings({"unchecked"})
    public void deployBundle(File bundle, RealRepoDescriptor targetRepo, final BasicStatusHolder status) {
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
                    if (NamingUtils.isSystem(file.getAbsolutePath()) || PathMatcher.isInDefaultExcludes(file) ||
                            file.getName().contains(MavenNaming.MAVEN_METADATA_NAME)) {
                        status.setDebug("Excluding '" + file.getAbsolutePath() + "' from bundle deployment.", log);
                        return false;
                    }

                    return true;
                }
            };
            List<File> archiveContent = Lists.newArrayList(FileUtils.listFiles(
                    extractFolder, deployableFilesFilter, DirectoryFileFilter.DIRECTORY));
            Collections.sort(archiveContent);

            Repo repo = repositoryService.repositoryByKey(targetRepo.getKey());
            for (File file : archiveContent) {
                String parentPath = extractFolder.getAbsolutePath();
                String filePath = file.getAbsolutePath();
                String relPath = PathUtils.getRelativePath(parentPath, filePath);

                ModuleInfo moduleInfo = repo.getItemModuleInfo(relPath);
                if (MavenNaming.isPom(file.getName())) {
                    try {
                        validatePom(file, relPath, moduleInfo, targetRepo.isSuppressPomConsistencyChecks());
                    } catch (Exception e) {
                        String msg = "The pom: " + file.getName() +
                                " could not be validated, and thus was not deployed.";
                        status.setWarning(msg, log);
                        continue;
                    }
                }

                try {
                    UnitInfo artifactInfo = null;

                    /**
                     * No use in creating a maven artifact info if it's not a valid module. Will just create a weird
                     * path
                     */
                    if (targetRepo.isMavenRepoLayout() && moduleInfo.isValid()) {
                        artifactInfo = MavenArtifactInfo.fromRepoPath(new RepoPathImpl(targetRepo.getKey(), relPath));
                    }

                    if ((artifactInfo == null) || !artifactInfo.isValid()) {
                        artifactInfo = new ArtifactInfo(relPath);
                    }

                    getTransactionalMe().deploy(targetRepo, artifactInfo, file, null, false, true);
                } catch (IllegalArgumentException iae) {
                    status.setWarning(iae.getMessage(), log);
                } catch (Exception e) {
                    status.setError("Error during deployment: " + e.getMessage(), e, log);
                }
            }

            String bundleName = bundle.getName();
            String timeTaken = DurationFormatUtils.formatPeriod(start, System.currentTimeMillis(), "s");
            int archiveContentSize = archiveContent.size();

            status.setStatus("Successfully deployed " + archiveContentSize + " artifacts from archive: " + bundleName
                    + " (" + timeTaken + " seconds).", log);
            //Trigger indexing for marked files
            searchService.asyncIndexMarkedArchives();
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
            try {
                MavenModelUtils.stringToMavenModel(pomFromJar);
                return pomFromJar;
            } catch (RepositoryRuntimeException rre) {
                log.error("Failed to validate the model of the POM file within '{}'.", file.getAbsolutePath());
            }
        }
        MavenArtifactInfo model = getArtifactInfo(file);
        return MavenModelUtils.mavenModelToString(MavenModelUtils.toMavenModel(model));
    }

    private File extractArchive(BasicStatusHolder status, File archive) throws Exception {
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

    public void validatePom(String pomContent, String relPath, ModuleInfo moduleInfo,
            boolean suppressPomConsistencyChecks) throws IOException {
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        File tempFile = File.createTempFile("pom.validation", ".tmp", artifactoryHome.getWorkTmpDir());
        try {
            FileUtils.writeStringToFile(tempFile, pomContent, "utf-8");
            validatePom(tempFile, relPath, moduleInfo, suppressPomConsistencyChecks);
        } finally {
            FileUtils.forceDelete(tempFile);
        }
    }

    private static void validatePom(File pomFile, String relPath, ModuleInfo moduleInfo,
            boolean suppressPomConsistencyChecks) throws BadPomException {
        InputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(pomFile));
            new PomTargetPathValidator(relPath, moduleInfo).validate(inputStream, suppressPomConsistencyChecks);
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
