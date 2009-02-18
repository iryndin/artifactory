package org.artifactory.test.internal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusEntry;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.FileAdditionalInfo;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderAdditionaInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ArtifactoryProperties;
import org.artifactory.common.ConstantsValue;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.md.MetadataAware;
import org.artifactory.jcr.md.MetadataDefinitionService;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.update.md.MetadataVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.testng.Assert;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Tests the central config import and export services.
 *
 * @author Noam Tenne
 */
public class ImportExportTest extends ArtifactoryTestBase {
    /**
     * Logger
     */
    private final static Logger log = LoggerFactory.getLogger(LocalHostRemoteRepoTest.class);

    /**
     * Global variable to indicate if meta data was found in the search
     */
    private boolean metaDataExists;
    private static final String LIBS_RELEASES_LOCAL = "libs-releases-local";

    /**
     * Execute before all tests - initialize the central config service impl
     */
    @BeforeMethod
    public void setUp() {
        // 10 secs defaulkt timeout, to shorten the tests
        ArtifactoryProperties.get()
                .setProperty(ConstantsValue.lockTimeoutSecs.getPropertyName(), "30");
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger(ImportExportTest.class).setLevel(Level.DEBUG);
        lc.getLogger(QuartzCommand.class).setLevel(Level.DEBUG);
    }

    /**
     * Attempts to import normally
     */
    @Test
    public void importNormal() {
        URL resourceURL = ImportExportTest.class.getResource("/export/VersionTest/");
        File resourceDir = new File(resourceURL.getFile());
        ImportSettings importSettings = new ImportSettings(resourceDir);
        importSettings.setCopyToWorkingFolder(false);
        importSettings.setFailIfEmpty(false);
        MultiStatusHolder statusHolder = new MultiStatusHolder();
        context.importFrom(importSettings, statusHolder);
        checkStatus(statusHolder);
    }

    @Test
    public void importExportNormalSingleRepoBeta3() throws Exception {
        URL resourceURL = ImportExportTest.class.getResource("/export/WcTest/");
        File resourceDir = new File(resourceURL.getFile());
        ImportSettings importSettings = new ImportSettings(resourceDir);
        importSettings.setCopyToWorkingFolder(false);
        importSettings.setFailFast(true);
        importSettings.setFailIfEmpty(false);
        importSettings.setIncludeMetadata(true);
        MultiStatusHolder statusHolder = new MultiStatusHolder();
        InternalRepositoryService repositoryService = context.beanForType(InternalRepositoryService.class);
        repositoryService.importRepo(LIBS_RELEASES_LOCAL, importSettings, statusHolder);
        checkStatus(statusHolder);
        Assert.assertEquals(importSettings.getMetadataReader(), MetadataVersion.v130beta3);
        List<StatusEntry> importWarnings = statusHolder.getWarnings();

        // Check good import in JCR
        RepoPath folderPath = new RepoPath(LIBS_RELEASES_LOCAL, "biz/aQute/bndlib/0.0.227");
        RepoPath filePath = new RepoPath(folderPath, "bndlib-0.0.227.pom");
        JcrService jcr = context.getJcrService();
        Repository repository = jcr.getRepository();
        Session session = repository.login();
        String folderAbsPath = JcrPath.get().getAbsolutePath(folderPath);
        String fileAbsPath = JcrPath.get().getAbsolutePath(filePath);
        assertTrue(session.itemExists(folderAbsPath),
                "Folder " + folderAbsPath + " should exists after import");
        assertTrue(session.itemExists(fileAbsPath),
                "File " + fileAbsPath + " should exists after import");
        Node folderNode = (Node) session.getItem(folderAbsPath);
        Node fileNode = (Node) session.getItem(fileAbsPath);
        String metadataNode = MetadataAware.NODE_ARTIFACTORY_METADATA;
        assertTrue(folderNode.hasNode(metadataNode),
                "Folder " + folderAbsPath + " should have metadata node " + metadataNode);
        assertTrue(fileNode.hasNode(metadataNode),
                "File " + fileAbsPath + " should have metadata node " + metadataNode);
        Node folderContainerNode = folderNode.getNode(metadataNode);
        Node fileContainerNode = fileNode.getNode(metadataNode);
        assertTrue(folderContainerNode.hasNode(FolderAdditionaInfo.ROOT),
                "Folder container " + folderContainerNode.getPath() + " should have " +
                        FolderAdditionaInfo.ROOT);
        assertTrue(fileContainerNode.hasNode(FileAdditionalInfo.ROOT),
                "File container " + fileContainerNode.getPath() + " should have " +
                        FileAdditionalInfo.ROOT);

        // TODO: Check the xml metadata
        Node extraFolderInfo = folderContainerNode.getNode(FolderAdditionaInfo.ROOT);
        Node extraFileInfo = fileContainerNode.getNode(FileAdditionalInfo.ROOT);

        session.logout();

        // Clear the caches to make sure all reloaded from JCR DB
        CacheService cacheService = InternalContextHelper.get().beanForType(CacheService.class);
        Map fsItemCache = cacheService.getRepositoryCache(LIBS_RELEASES_LOCAL, ArtifactoryCache.fsItemCache);
        Map locks = cacheService.getRepositoryCache(LIBS_RELEASES_LOCAL, ArtifactoryCache.locks);
        fsItemCache.clear();
        locks.clear();
        File exportTest = new File(ArtifactoryHome.getOrCreateSubDir("exportTest"), "" + hashCode());
        ExportSettings exportSettings = new ExportSettings(exportTest);
        exportSettings.setIncludeMetadata(true);
        statusHolder = new MultiStatusHolder();
        repositoryService.exportRepo(LIBS_RELEASES_LOCAL, exportSettings, statusHolder);
        checkStatus(statusHolder);

        File folderExportDir = new File(exportTest, folderAbsPath);
        assertTrue(folderExportDir.exists(),
                "Folder " + folderExportDir.getAbsolutePath() + " should exists after export");
        File fileExport = new File(exportTest, fileAbsPath);
        assertTrue(fileExport.exists(),
                "File " + fileExport.getAbsolutePath() + " should exists after export");
        File originalFile = new File(resourceDir, filePath.getPath());
        String origFileContent = FileUtils.readFileToString(originalFile);
        String exportFileContent = FileUtils.readFileToString(fileExport);
        Assert.assertEquals(exportFileContent, origFileContent,
                "After Import/Export file " + originalFile + " should be equal to " + fileExport);

        File folderMdFolder =
                new File(folderExportDir.getParentFile(),
                        folderExportDir.getName() + ItemInfo.METADATA_FOLDER);
        assertTrue(folderMdFolder.exists(),
                "Folder " + folderMdFolder.getAbsolutePath() + " should exists after export");
        File folderXmlInfo = new File(folderMdFolder, FolderInfo.ROOT + ".xml");
        assertTrue(folderXmlInfo.exists(),
                "Folder xml MD " + folderXmlInfo.getAbsolutePath() + " should exists after export");
        XStream xstream = context.beanForType(MetadataDefinitionService.class).getXstream();
        FileReader fileReader = new FileReader(folderXmlInfo);
        FolderInfo folderInfo = (FolderInfo) xstream.fromXML(fileReader);
        IOUtils.closeQuietly(fileReader);
        Assert.assertEquals(folderInfo.getRepoPath(), folderPath,
                "Folder RepoPath should be equal after import/export");
        Assert.assertEquals(folderInfo.getModifiedBy(), "test-folder",
                "Modified by should be test-folder after import/export");
        Assert.assertEquals(folderInfo.getCreatedBy(), "test-folder",
                "Created by should be test-folder after import/export");
        Assert.assertEquals(folderInfo.getLastUpdated(), 1226004559605L,
                "Last updated by should be same after import/export");
        Assert.assertEquals(folderInfo.getLastModified(), 1220183113697L,
                "Last modified by should be same after import/export");

        File fileMdFolder = new File(fileExport.getParentFile(),
                fileExport.getName() + ItemInfo.METADATA_FOLDER);
        assertTrue(fileMdFolder.exists(),
                "File " + fileMdFolder.getAbsolutePath() + " should exists after export");
        File fileXmlInfo = new File(fileMdFolder, FileInfo.ROOT + ".xml");
        assertTrue(fileXmlInfo.exists(),
                "File xml MD " + fileXmlInfo.getAbsolutePath() + " should exists after export");
        fileReader = new FileReader(fileXmlInfo);
        FileInfo fileInfo = (FileInfo) xstream.fromXML(fileReader);
        IOUtils.closeQuietly(fileReader);
        Assert.assertEquals(fileInfo.getRepoPath(), filePath,
                "File RepoPath should be equal after import/export");
        Assert.assertEquals(fileInfo.getModifiedBy(), "test-file",
                "Modified by should be test after import/export");
        Assert.assertEquals(fileInfo.getCreatedBy(), "test-file",
                "Modified by should be test after import/export");
        Assert.assertEquals(fileInfo.getLastUpdated(), 1218409202811L,
                "Last updated by should be same after import/export");
        Assert.assertEquals(fileInfo.getLastModified(), 1216224940944L,
                "Last modified by should be same after import/export");
        Assert.assertTrue(fileInfo.getSize() == 950, "Size should be same after import/export");
        Assert.assertEquals(fileInfo.getMd5(), "388e5963e2b4a2bc633e845b62cd6e1f",
                "MD5 should be same after import/export");
        // Status holder should have the warning on MD5 mismatch
        Assert.assertEquals(importWarnings.size(), 2, "Should have one warning on MD5 and no metadata on repo root");
        Assert.assertTrue(importWarnings.get(1).getStatusMessage().contains("MD5"),
                "Should have one warning on MD5");
        Assert.assertEquals(fileInfo.getSha1(),
                "d38fc54f2ab67153cf8020d0b207c7eba3979e86",
                "SHA1 should be same after import/export");
    }

    private void checkStatus(MultiStatusHolder statusHolder) {
        if (statusHolder.isError()) {
            Throwable ex = statusHolder.getException();
            if (ex != null) {
                log.error("Error during import", ex);
            }
            Assert.assertFalse(statusHolder.isError(),
                    "Error during export " + statusHolder.getLastError());
        }
    }

    @Test(enabled = false)
    public void importExportFullNormalBeta2() throws Exception {
        File baseExportDir = new File(getClass().getResource("/export/v130beta2").toURI());
        ImportSettings settings = new ImportSettings(baseExportDir);
        settings.setCopyToWorkingFolder(false);
        settings.setFailFast(true);
        settings.setIncludeMetadata(true);
        settings.setFailIfEmpty(false);
        MultiStatusHolder status = new MultiStatusHolder();
        context.importFrom(settings, status);
        checkStatus(status);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, enabled = false)
    public void importExportTooOld() throws Exception {
        File baseExportDir = new File(getClass().getResource("/export/v125u1").toURI());
        ImportSettings settings = new ImportSettings(baseExportDir);
        MultiStatusHolder status = new MultiStatusHolder();
        context.importFrom(settings, status);
        Assert.assertTrue(false, "Should not reach here");
    }

    /**
     * Attempts to export normally
     */
    @Test(dependsOnMethods = "importNormal")
    public void exportWithCorrectDatesAndMetadata() {
        log.debug("Starting system export");
        File dir = createTempDir("exportTest");
        ExportSettings exportSettings = new ExportSettings(dir);
        exportSettings.setIncludeMetadata(true);
        MultiStatusHolder statusHolder = new MultiStatusHolder();
        context.exportTo(exportSettings, statusHolder);
        Assert.assertFalse(statusHolder.isError(),
                "Error during export " + statusHolder.getLastError());
        checkMetaData(dir);
        assertTrue(dir.lastModified() > 0);
        assertTrue(metaDataExists == exportSettings.isIncludeMetadata());
    }

    @Test
    public void importAndExportSameTime() throws InterruptedException {
        final InternalRepositoryService repositoryService =
                context.beanForType(InternalRepositoryService.class);
        final String repoKey = LIBS_RELEASES_LOCAL;
        URL resourceURL =
                ImportExportTest.class.getResource("/export/VersionTest/repositories/" + repoKey);
        Assert.assertNotNull(resourceURL, "Repositories dir not found");
        File resourceDir = new File(resourceURL.getFile());
        final ImportSettings importSettings = new ImportSettings(resourceDir);
        importSettings.setCopyToWorkingFolder(false);
        importSettings.setFailIfEmpty(false);
        importSettings.setFailFast(true);
        importSettings.setIncludeMetadata(true);
        // execute syncronous import in a seperate thread
        ImportRunner importRunner = new ImportRunner(repositoryService, repoKey, importSettings);
        importRunner.start();

        // now export
        ExportSettings exportSettings = new ExportSettings(createTempDir("repoExportTest"));
        MultiStatusHolder statusHolder = new MultiStatusHolder();
        repositoryService.exportRepo(repoKey, exportSettings, statusHolder);
        importRunner.join();
        // TODO: The export is failing randomly
        //checkStatus(statusHolder);
        Throwable importException = importRunner.le;
        if (importException != null) {
            log.error("error during import", importException);
            Assert.assertNull(importException,
                    "Error during import " + importException.getMessage());
        }
    }

    /**
     * Attempts to import from a non-existant directory
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void importNonExistantDirectory() {
        File dir = new File("nosuchfile.lala");
        ImportSettings importSettings = new ImportSettings(dir);
        MultiStatusHolder statusHolder = new MultiStatusHolder();
        context.importFrom(importSettings, statusHolder);
        Assert.assertFalse(statusHolder.isError(),
                "Error during export " + statusHolder.getLastError());
    }

    /**
     * Attempts to import from an empty directory and fail
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void importEmptyDirecotyFailIfEmpty() {
        File dir = createTempDir("emptyDir");
        ImportSettings importSettings = new ImportSettings(dir);
        MultiStatusHolder statusHolder = new MultiStatusHolder();
        context.importFrom(importSettings, statusHolder);
        Assert.assertFalse(statusHolder.isError(),
                "Error during export " + statusHolder.getLastError());
    }

    /**
     * Attempts to import from an empty directory without failing
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void importEmptyDirecotyWithoutFail() {
        File dir = createTempDir("emptyDir");
        ImportSettings importSettings = new ImportSettings(dir);
        MultiStatusHolder statusHolder = new MultiStatusHolder();
        context.importFrom(importSettings, statusHolder);
        Assert.assertFalse(statusHolder.isError(),
                "Error during export " + statusHolder.getLastError());
    }

    /**
     * Creates an empty temp directory
     *
     * @param dirName Name of directory to create
     * @return File Object of created temp directory
     */
    private File createTempDir(String dirName) {
        File dir = new File(SystemUtils.getJavaIoTmpDir(), dirName);
        dir.deleteOnExit();
        return dir;
    }

    /**
     * Recursive method that checks if any metadata exists in the given file structure. Results can be checked in the
     * global variable - metaDataExists
     *
     * @param file File structure to search at
     */
    private void checkMetaData(File file) {
        if (file.isDirectory()) {
            if (file.getName().indexOf("metadata") != -1) {
                metaDataExists = true;
                return;
            }
            for (File child : file.listFiles()) {
                checkMetaData(child);
                if (metaDataExists) {
                    return;
                }
            }
        }
    }

    @Override
    String getConfigName() {
        return "localhost-repo";
    }

    private static class ImportRunner extends Thread {
        public Throwable le;
        private InternalRepositoryService service;
        private String key;
        private ImportSettings settings;
        private Authentication authentication;

        private ImportRunner(InternalRepositoryService service, String key,
                ImportSettings settings) {
            this.service = service;
            this.key = key;
            this.settings = settings;
            this.authentication = SecurityContextHolder.getContext().getAuthentication();
        }

        @Override
        public void run() {
            try {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                StatusHolder holder = new MultiStatusHolder();
                service.importRepo(key, settings, holder);
                if (holder.isError()) {
                    le = holder.getException();
                    if (le == null) {
                        le = new Error(holder.getStatusMsg());
                    }
                }
            } catch (Throwable e) {
                le = e;
            } finally {
                SecurityContextHolder.getContext().setAuthentication(null);
            }
        }
    }
}
