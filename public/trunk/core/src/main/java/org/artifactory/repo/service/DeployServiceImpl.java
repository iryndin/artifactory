package org.artifactory.repo.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.model.Model;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.maven.MavenUnitInfo;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.DeployService;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.FolderExpectedException;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.repo.exception.maven.BadPomException;
import org.artifactory.api.request.InternalArtifactoryRequest;
import org.artifactory.api.request.UploadService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.Maven;
import org.artifactory.maven.MavenModelUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.request.InternalArtifactoryResponse;
import org.artifactory.search.InternalSearchService;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.PathUtils;
import org.artifactory.util.ZipUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    public void deploy(RepoDescriptor targetRepo, MavenArtifactInfo artifactInfo,
                       File file, boolean forceDeployPom, boolean partOfBundleDeploy) throws RepoAccessException {
        String pomString = getModelString(artifactInfo);
        deploy(targetRepo, artifactInfo, file, pomString, forceDeployPom, partOfBundleDeploy);
    }

    public void deploy(RepoDescriptor targetRepo, MavenArtifactInfo artifactInfo,
                       final File file, String pomString, boolean forceDeployPom, boolean partOfBundleDeploy)
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
        File pomFile = null;
        try {
            InternalArtifactoryContext context = InternalContextHelper.get();
            Artifact artifact = null;
            RepoPath repoPath = new RepoPath(targetRepo.getKey(), path);
            if (!authService.canDeploy(repoPath)) {
                AccessLogger.deployDenied(repoPath);
                throw new RepoAccessException(
                        "Not enough permissions to deploy artifact '" + artifact + "'.",
                        repoPath, "deploy", authService.currentUsername());
            }

            Model model = null;
            Maven maven = null;
            if (artifactInfo.isAutoCalculate()) {
                maven = context.beanForType(Maven.class);
                artifact = maven.createArtifact(artifactInfo);
                model = MavenModelUtils.getMavenModel(artifactInfo);
                artifactInfo.setModelAsString(pomString);
            }

            //Handle extra pom deployment - add the metadata with the gnerated pom file to the artifact
            ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
            if (forceDeployPom && !MavenArtifactInfo.POM.equalsIgnoreCase(artifactInfo.getType())) {
                pomFile = MavenModelUtils
                        .addPomFileMetadata(file, artifact, pomString, artifactoryHome.getTmpUploadsDir());
            }

            //Add plugin metadata
            if (model != null && "maven-plugin".equals(model.getPackaging())) {
                addPluginVersioningMetadata(artifactInfo.getVersion(), artifact);
            }
            if (maven != null) {
                maven.deploy(file, artifact, localRepo, artifactoryHome.getTmpUploadsDir());
            } else {
                RepoPath uploadPath = new RepoPath(targetRepo.getKey(), artifactInfo.getPath());
                final FileInputStream is = new FileInputStream(file);
                InternalArtifactoryResponse response = new InternalArtifactoryResponse();
                InternalArtifactoryRequest request = new InternalArtifactoryRequest(uploadPath) {
                    @Override
                    public InputStream getInputStream() {
                        return is;
                    }

                    @Override
                    public long getLastModified() {
                        return file.lastModified();
                    }

                    @Override
                    public int getContentLength() {
                        return (int) file.length();
                    }
                };

                try {
                    if (repositoryService.exists(request.getRepoPath())) {
                        ItemInfo itemInfo = repositoryService.getItemInfo(request.getRepoPath());
                        if (itemInfo.isFolder()) {
                            throw new RuntimeException(
                                    "Cannot deploy an artifact file over an existing directory.");
                        }
                    }
                    uploadService.process(request, response);
                } catch (Exception e) {
                    Throwable rootCause = ExceptionUtils.getRootCause(e);
                    if (rootCause instanceof FolderExpectedException) {
                        throw new RuntimeException(
                                "Target path includes an existing file - cannot overwrite a file with a directory.",
                                rootCause);
                    }
                    throw new RuntimeException(e.getMessage(), rootCause);
                } finally {
                    IOUtils.closeQuietly(is);
                }
                if (!response.isSuccessful()) {
                    log.error("Unable to upload file. ", response.getException());
                }
            }
            if (!partOfBundleDeploy) {
                //Since we are dealing with a single deployed file, check whether it's a candiate for indexing to save
                //the extra marked files search
                ContentType contentType = NamingUtils.getContentType(file.getPath());
                if (contentType.isJarVariant()) {
                    //Trigger indexing for the deployed file directly. We cannot query for it, since query must be
                    //delayed until after commit which we cannot do, since the search marked method is out of tx
                    searchService.index(Collections.singletonList(repoPath));
                }
            }
        } catch (ArtifactDeploymentException e) {
            String msg = "Cannot deploy file " + file.getName() + ". Cause: " + e.getMessage();
            log.debug(msg, e);
            throw new RepositoryRuntimeException(msg, e);
        } catch (IOException e) {
            String msg = "Cannot deploy file " + file.getName() + ". Cause: " + e.getMessage();
            log.debug(msg, e);
            throw new RepositoryRuntimeException(msg, e);
        } finally {
            FileUtils.deleteQuietly(pomFile);
        }
    }

    private void validatePath(MavenUnitInfo artifactInfo) {
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
                    extractFolder, deployableFilesFilter, DirectoryFileFilter.INSTANCE);
            List<File> deployFailedList = new ArrayList<File>();
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
                        getTransactionalMe().deploy(targetRepo, artifactInfo, file, false, true);
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
                status.setStatus("Succesfully deployed " + archiveContentSize + " artifacts from archive: " + bundleName
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

    public String getModelString(MavenArtifactInfo artifactInfo) {
        if (artifactInfo == null || !artifactInfo.isAutoCalculate()) {
            return "";
        }
        Model model = MavenModelUtils.getMavenModel(artifactInfo);
        return MavenModelUtils.mavenModelToString(model);
    }

    private void addPluginVersioningMetadata(String version, Artifact artifact) {
        //Add the latest version metadata for plugins.
        //With regular maven deploy this is handled automatically by the
        //AddPluginArtifactMetadataMojo, as part of the "maven-plugin" packaging lifecycle.
        Versioning versioning = new Versioning();
        versioning.setLatest(version); //Set the current deployed version as the latest
        versioning.updateTimestamp();
        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata(artifact, versioning);
        artifact.addMetadata(metadata);
    }

    private File extractArchive(StatusHolder status, File archive) throws Exception {
        String archiveName = archive.getName();
        String fixedArchiveName = new String(archiveName.getBytes("UTF-8"));
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
