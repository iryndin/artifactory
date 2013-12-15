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

package org.artifactory.repo.db.importexport;

import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.YumAddon;
import org.artifactory.api.config.ImportSettingsImpl;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.maven.MavenMetadataService;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.FolderInfo;
import org.artifactory.fs.MetadataEntryInfo;
import org.artifactory.fs.StatsInfo;
import org.artifactory.fs.WatcherInfo;
import org.artifactory.fs.WatchersInfo;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.md.MetadataDefinition;
import org.artifactory.md.MetadataDefinitionService;
import org.artifactory.md.PropertiesInfo;
import org.artifactory.model.common.RepoPathImpl;
import org.artifactory.model.xstream.fs.PropertiesImpl;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.interceptor.ImportInterceptors;
import org.artifactory.repo.interceptor.StorageAggregationInterceptors;
import org.artifactory.repo.local.ValidDeployPathContext;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.sapi.fs.MetadataReader;
import org.artifactory.sapi.fs.MutableVfsFile;
import org.artifactory.sapi.fs.MutableVfsFolder;
import org.artifactory.sapi.fs.MutableVfsItem;
import org.artifactory.schedule.TaskService;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.storage.StorageProperties;
import org.artifactory.storage.spring.ArtifactoryStorageContext;
import org.artifactory.storage.spring.StorageContextHelper;
import org.artifactory.update.md.MetadataVersion;
import org.artifactory.util.CollectionUtils;
import org.artifactory.util.GlobalExcludes;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;

import static org.artifactory.repo.db.importexport.ImportExportAccumulator.ProgressAccumulatorType.IMPORT;

/**
 * Controls the export of db repository.
 * The export is done outside of a transaction and is not locking files during its work.
 *
 * @author Yossi Shaul
 */
public class DbRepoImportHandler extends DbRepoImportExportBase {
    private static final Logger log = LoggerFactory.getLogger(DbRepoImportHandler.class);
    private static final int MAX_ITEMS_PER_TRANSACTION = 1000;
    private final LocalRepo<? extends LocalRepoDescriptor> repo;
    private final ImportSettings settings;
    private ImportExportAccumulator progressAccumulator;
    private MutableStatusHolder status;
    private TransactionStatus txStatus;

    public DbRepoImportHandler(LocalRepo<? extends LocalRepoDescriptor> repo, ImportSettings settings) {
        this.repo = repo;
        this.settings = settings;
        status = settings.getStatusHolder();
    }

    public void executeImport() {
        File fileSystemBaseDir = settings.getBaseDir();
        status.status(String.format("%s import started %s", repo.getKey(), fileSystemBaseDir), log);
        if (fileSystemBaseDir == null || !fileSystemBaseDir.isDirectory()) {
            status.error("Error Import: Cannot import null, non existent folder or non directory file '"
                    + fileSystemBaseDir + "'.", log);
            return;
        }

        RepoPath rootRepoPath = InternalRepoPathFactory.repoRootPath(repo.getKey());
        progressAccumulator = new ImportExportAccumulator(repo.getKey(), IMPORT);
        startTransaction();
        try {
            importRecursive(fileSystemBaseDir, rootRepoPath);
        } finally {
            commitTransaction(txStatus);
        }
        progressAccumulator.finished();

        if (!repo.isCache()) {
            ContextHelper.get().beanForType(MavenMetadataService.class).calculateMavenMetadataAsync(rootRepoPath, true);

            LocalRepoDescriptor descriptor = repo.getDescriptor();
            if (descriptor.isCalculateYumMetadata()) {
                AddonsManager addonsManager = StorageContextHelper.get().beanForType(AddonsManager.class);
                addonsManager.addonByType(YumAddon.class).requestAsyncRepositoryYumMetadataCalculation(descriptor);
            }
        }

        status.status(String.format("%s import finished: Items imported: %s (%s files %s folders). " +
                "Duration: %s IPS: %s Target: '%s'",
                repo.getKey(), progressAccumulator.getItemsCount(), progressAccumulator.getFilesCount(),
                progressAccumulator.getFoldersCount(),
                progressAccumulator.getDurationString(), progressAccumulator.getItemsPerSecond(), fileSystemBaseDir),
                log);

        if (progressAccumulator.getItemsCount() > 1) {
            StorageContextHelper.get().beanForType(StorageAggregationInterceptors.class).
                    afterAllImport(rootRepoPath, progressAccumulator.getItemsCount(), status);
        }
    }

    private void importRecursive(final File fileToImport, final RepoPath target) {
        TaskService taskService = InternalContextHelper.get().getTaskService();
        if (taskService.pauseOrBreak()) {
            status.error("Import of " + repo.getKey() + " was stopped", log);
            return;
        }

        if (progressAccumulator.getItemsCount() > 0 &&
                progressAccumulator.getItemsCount() % MAX_ITEMS_PER_TRANSACTION == 0) {
            log.debug("Committing transaction artifacts count: {}", progressAccumulator.getItemsCount());
            commitTransaction(txStatus);
            startTransaction();
        }

        if (!fileToImport.exists()) {
            // skeleton import? looks for file metadata
            File fileInfoMetadata = new File(
                    fileToImport.getAbsolutePath() + METADATA_FOLDER + "/" + FileInfo.ROOT + ".xml");
            if (fileInfoMetadata.exists() && isStorableFile(fileToImport.getName())) {
                importFile(fileToImport, target);
            } else {
                status.warn("File/metadata not found: " + fileToImport.getAbsolutePath(), log);
            }
        } else if (fileToImport.isFile() && isStorableFile(fileToImport.getName())) {
            importFile(fileToImport, target);
        } else if (isStorableFolder(fileToImport.getName())) {
            boolean folderExistAfterImport = importFolder(fileToImport, target);
            if (!folderExistAfterImport) {
                log.debug("Folder '{}' doesn't exist after import. Skipping import children of '{}'",
                        target, fileToImport);
                return;
            }
            File[] filesToImport = fileToImport.listFiles();
            if (filesToImport != null && filesToImport.length > 0) {
                Set<String> fileNames = Sets.newHashSetWithExpectedSize(filesToImport.length / 2);
                for (File childFile : filesToImport) {
                    String name = childFile.getName();
                    if (settings.isIncludeMetadata() && name.endsWith(METADATA_FOLDER)) {
                        fileNames.add(name.substring(0, name.length() - METADATA_FOLDER.length()));
                    } else if (isStorableFolder(name) && isStorableFile(name)
                            && !GlobalExcludes.isInGlobalExcludes(childFile)) {
                        fileNames.add(name);
                    }
                }
                for (String fileName : fileNames) {
                    RepoPathImpl targetChild = new RepoPathImpl(target, fileName);
                    importRecursive(new File(fileToImport, fileName), targetChild);
                }
            }
        }
    }

    private void importFile(final File fileToImport, final RepoPath target) {
        log.debug("Importing '{}'.", target);
        if (!settings.isIncludeMetadata() && !fileToImport.exists()) {
            status.error("Cannot import non existent file (metadata is excluded): " +
                    fileToImport.getAbsolutePath(), log);
            return;
        }
        ArtifactoryStorageContext context = StorageContextHelper.get();
        try {
            long length = -1L;
            if (fileToImport.exists()) {
                length = fileToImport.length();
            }
            InternalRepositoryService repositoryService = context.beanForType(InternalRepositoryService.class);
            String repoKey = target.getRepoKey();
            LocalRepo localRepo = repositoryService.localOrCachedRepositoryByKey(repoKey);
            if (localRepo == null) {
                throw new RepoRejectException("The repository '" + repoKey + "' is not configured.");
            }
            repositoryService.assertValidDeployPath(
                    new ValidDeployPathContext.Builder(localRepo, target).contentLength(length).build());
        } catch (RepoRejectException e) {
            status.error("Artifact rejected: " + e.getMessage(), log);
            return;
        }
        MutableVfsFile mutableFile = null;
        try {
            mutableFile = repo.createOrGetFile(target);
            importFileFrom(fileToImport, mutableFile);

            context.beanForType(ImportInterceptors.class).afterImport(mutableFile, status);
            log.debug("Imported '{}'.", target);
            AccessLogger.deployed(target);
            progressAccumulator.accumulateFile();
        } catch (Exception e) {
            status.error("Could not import file '" + fileToImport.getAbsolutePath() + "' into " + target + ".",
                    e, log);
            // mark the mutable item in error and let the session manager handle it
            if (mutableFile != null) {
                mutableFile.markError();
            }
        }
    }

    public void importFileFrom(File sourceFile, MutableVfsFile mutableFile) throws IOException, RepoRejectException {
        updateMutableFromImportedFile(sourceFile, mutableFile);

        boolean fileInfoImported = false;
        String expectedSha1 = null;
        String expectedMd5 = null;
        long expectedLength = -1L;

        RepoPath targetRepoPath = mutableFile.getRepoPath();
        if (settings.isIncludeMetadata()) {
            List<MetadataEntryInfo> metadataEntries = getMetadataEntryInfos(sourceFile, status);
            FileInfo fileInfoToImport = (FileInfo) readItemInfoMetadata(
                    FileInfo.ROOT, sourceFile, targetRepoPath, metadataEntries);
            if (fileInfoToImport != null) {
                mutableFile.fillInfo(fileInfoToImport);
                expectedSha1 = fileInfoToImport.getSha1();
                expectedMd5 = fileInfoToImport.getMd5();
                expectedLength = fileInfoToImport.getSize();
                fileInfoImported = true;
            }
            importProperties(sourceFile, mutableFile, metadataEntries);
            importWatches(sourceFile, mutableFile, metadataEntries);
            importStats(sourceFile, mutableFile, metadataEntries);
        }

        if (!fileInfoImported && !sourceFile.exists()) {
            throw new FileNotFoundException("Cannot import non existent file " + sourceFile.getAbsolutePath()
                    + " since metadata information was not found!");
        }

        if (!fileInfoImported) {
            // Couldn't import metadata, try to import client checksums from files (FILENAME.sha1, FILENAME.md5)
            expectedLength = sourceFile.length();
            String sha1FileValue = getOriginalChecksumFromFile(sourceFile, ChecksumType.sha1);
            if (StringUtils.isNotBlank(sha1FileValue)) {
                expectedSha1 = sha1FileValue;
                mutableFile.setClientSha1(sha1FileValue);
            }
            String md5FileValue = getOriginalChecksumFromFile(sourceFile, ChecksumType.md5);
            if (StringUtils.isNotBlank(md5FileValue)) {
                expectedMd5 = md5FileValue;
                mutableFile.setClientMd5(md5FileValue);
            }
        }

        //Stream the file directly into the storage
        boolean usedFileInfoImportedChecksums = false;
        if (fileInfoImported && ChecksumType.sha1.isValid(expectedSha1)) {
            status.debug("Using metadata import for " + sourceFile, log);
            if (sourceFile.exists() && settings.isExcludeContent()) {
                // If exclude content and file exists use it for the external filestore if it exists
                StorageProperties storageProperties = StorageContextHelper.get().beanForType(StorageProperties.class);
                String extFilestoreDir = storageProperties.getBinaryProviderExternalDir();
                if (StringUtils.isNotBlank(extFilestoreDir)) {
                    Path filePath = Paths.get(extFilestoreDir, expectedSha1.substring(0, 2), expectedSha1);
                    if (!Files.exists(filePath)) {
                        Files.move(sourceFile.toPath(), filePath, StandardCopyOption.ATOMIC_MOVE);
                    }
                }
            }
            // Found file info in metadata : Try deploy by checksum
            usedFileInfoImportedChecksums = mutableFile.useData(expectedSha1, expectedMd5, expectedLength);
            if (usedFileInfoImportedChecksums) {
                status.debug("Found existing binary in the filestore for " + expectedSha1, log);
            }
        }

        if (!usedFileInfoImportedChecksums) {
            if (!sourceFile.exists()) {
                throw new FileNotFoundException(sourceFile.getAbsolutePath() + ": File doesn't exist and matching " +
                        "binary either doesn't exist of settings are not configured to use it");
            }
            try (InputStream is = new BufferedInputStream(new FileInputStream(sourceFile))) {
                mutableFile.fillData(is);
            }
        }

        if (PathUtils.hasText(expectedSha1) && !mutableFile.getSha1().equals(expectedSha1)) {
            status.warn("Received file " + targetRepoPath + " with Checksum error on SHA1 " +
                    "actual=" + mutableFile.getSha1() + " expected=" + expectedSha1, log);
        }
        if (PathUtils.hasText(expectedMd5) && !mutableFile.getMd5().equals(expectedMd5)) {
            status.warn("Received file " + targetRepoPath + " with Checksum error on MD5 " +
                    "actual=" + mutableFile.getMd5() + " expected=" + expectedMd5, log);
        }
    }

    private List<MetadataEntryInfo> getMetadataEntryInfos(File sourceFile, MutableStatusHolder status) {
        File metadataFolder = getMetadataContainerFolder(sourceFile);
        if (!metadataFolder.exists()) {
            return null;
        }

        MetadataReader metadataReader = findBestMatchMetadataReader(settings, metadataFolder);
        return metadataReader.getMetadataEntries(metadataFolder, status);
    }

    //TORE: [by YS] requires refactoring
    private Object readItemInfoMetadata(String metadataName, File source, RepoPath target,
            List<MetadataEntryInfo> metadataEntries) {
        if (CollectionUtils.isNullOrEmpty(metadataEntries)) {
            if (!target.isRoot()) {
                status.debug("No Metadata entries found for " + source.getAbsolutePath(), log);
            }
            return null;
        }
        try {
            for (MetadataEntryInfo entry : metadataEntries) {
                if (metadataName.equals(entry.getMetadataName())) {
                    MetadataDefinitionService metadataDefinitionService = getMetadataDefinitionService();
                    MetadataDefinition definition = metadataDefinitionService.getMetadataDefinition(metadataName, true);
                    return definition.getXmlProvider().fromXml(entry.getXmlContent());
                }
            }
        } catch (Exception e) {
            String msg = "Failed to import metadata of " + source.getAbsolutePath() + " into '" + target + "'.";
            status.error(msg, e, log);
        }

        return null;
    }

    private void updateMutableFromImportedFile(File sourceFile, MutableVfsFile mutableFile) {
        // set basic data from imported file. most of it will be overridden when/if file metadata is imported
        String currentUser = getCurrentUsername();
        mutableFile.setCreatedBy(currentUser);
        mutableFile.setModifiedBy(currentUser);
        if (sourceFile.exists()) {
            mutableFile.setModified(sourceFile.lastModified());
        }
        mutableFile.setUpdated(System.currentTimeMillis());
    }

    private String getOriginalChecksumFromFile(File artifactFile, ChecksumType checksumType) throws IOException {
        //TORE: [by YS] check for file size and use a util method
        File checksumFile = new File(artifactFile.getParent(), artifactFile.getName() + checksumType.ext());
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(checksumFile));
            return Checksum.checksumStringFromStream(is);
        } catch (FileNotFoundException e) {
            log.debug("Couldn't find '{}' checksum file and Artifactory metadata doesn't exist.",
                    checksumFile.getName());
        } finally {
            IOUtils.closeQuietly(is);
        }

        return null;
    }

    private boolean importFolder(File sourceFolder, RepoPath target) {
        boolean folderExistAfterImport = false;
        if (!GlobalExcludes.isInGlobalExcludes(sourceFolder)) {
            // Create the folder and import its the metadata
            MutableVfsFolder mutableFolder = null;
            try {
                // First create the folder
                mutableFolder = repo.createOrGetFolder(target);
                //Read metadata into the node
                if (settings.isIncludeMetadata()) {
                    List<MetadataEntryInfo> metadataEntries = getMetadataEntryInfos(sourceFolder, status);
                    FolderInfo folderInfoToImport = (FolderInfo) readItemInfoMetadata(
                            FileInfo.ROOT, sourceFolder, target, metadataEntries);
                    if (folderInfoToImport != null) {
                        mutableFolder.fillInfo(folderInfoToImport);
                    }

                    importProperties(sourceFolder, mutableFolder, metadataEntries);
                    importWatches(sourceFolder, mutableFolder, metadataEntries);
                }
                folderExistAfterImport = true;
                progressAccumulator.accumulateFolder();
            } catch (Exception e) {
                // Just log an error and continue - will not import children
                String msg = "Failed to import folder " + sourceFolder.getAbsolutePath() + " into '" + target + "'.";
                status.error(msg, e, log);
                if (mutableFolder != null) {
                    mutableFolder.markError();
                }
            }
        }
        return folderExistAfterImport;
    }

    private void importProperties(File sourceFile, MutableVfsItem mutableItem,
            List<MetadataEntryInfo> metadataEntries) {
        PropertiesInfo propertiesInfo = (PropertiesInfo) readItemInfoMetadata(
                PropertiesInfo.ROOT, sourceFile, mutableItem.getRepoPath(), metadataEntries);
        if (propertiesInfo != null) {
            mutableItem.setProperties(new PropertiesImpl(propertiesInfo));
        }
    }

    private void importWatches(File sourceFile, MutableVfsItem mutableItem, List<MetadataEntryInfo> metadataEntries) {
        WatchersInfo watchersInfo = (WatchersInfo) readItemInfoMetadata(
                WatchersInfo.ROOT, sourceFile, mutableItem.getRepoPath(), metadataEntries);
        if (watchersInfo != null) {
            for (WatcherInfo watcherInfo : watchersInfo.getWatchers()) {
                mutableItem.addWatch(watcherInfo);
            }
        }
    }

    private void importStats(File sourceFile, MutableVfsFile mutableFile, List<MetadataEntryInfo> metadataEntries) {
        StatsInfo statsInfo = (StatsInfo) readItemInfoMetadata(
                StatsInfo.ROOT, sourceFile, mutableFile.getRepoPath(), metadataEntries);
        if (statsInfo != null) {
            mutableFile.setStats(statsInfo);
        }
    }

    public MetadataReader findBestMatchMetadataReader(ImportSettings importSettings, File metadataFolder) {
        ImportSettingsImpl settings = (ImportSettingsImpl) importSettings;
        MetadataReader metadataReader = settings.getMetadataReader();
        if (metadataReader == null) {
            if (settings.getExportVersion() != null) {
                metadataReader = MetadataVersion.findVersion(settings.getExportVersion());
            } else {
                //try to find the version from the format of the metadata folder
                metadataReader = MetadataVersion.findVersion(metadataFolder);
            }
            settings.setMetadataReader(metadataReader);
        }
        return metadataReader;
    }

    private String getCurrentUsername() {
        return InternalContextHelper.get().getAuthorizationService().currentUsername();
    }

    private void startTransaction() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("ImportTransaction");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        AbstractPlatformTransactionManager txManager = getTransactionManager();
        this.txStatus = txManager.getTransaction(def);
    }

    private void commitTransaction(TransactionStatus status) {
        getTransactionManager().commit(status);
    }

    private AbstractPlatformTransactionManager getTransactionManager() {
        return (AbstractPlatformTransactionManager) ContextHelper.get().getBean("artifactoryTransactionManager");
    }
}
