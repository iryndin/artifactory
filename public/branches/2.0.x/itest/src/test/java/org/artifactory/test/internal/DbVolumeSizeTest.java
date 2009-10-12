package org.artifactory.test.internal;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.cli.main.ArtifactoryCli;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.repo.service.InternalRepositoryService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URL;

/**
 * A test that does a full system import and un deploying the imported Data repeatedly for a number of times, then
 * compresses the database table via the CLI compress command, And checks to see if the data folder has reduced in
 * size.
 *
 * @author Noam Tenne
 */
public class DbVolumeSizeTest extends ArtifactoryTestBase {
    private InternalRepositoryService repositoryService;

    /**
     * Execute before all tests - initialize the central config service impl
     */
    @BeforeMethod
    public void setUp() {
        repositoryService = context.beanForType(InternalRepositoryService.class);
    }

    @Test
    public void test() throws Exception {
        URL resourceURL = ImportExportTest.class.getResource("/export/VersionTest/");
        File resourceDir = new File(resourceURL.getFile());
        ImportSettings importSettings = new ImportSettings(resourceDir);
        importSettings.setCopyToWorkingFolder(false);
        importSettings.setFailFast(true);
        importSettings.setFailIfEmpty(true);
        importSettings.setVerbose(true);
        context.importFrom(importSettings, new StatusHolder());

        LocalRepoDescriptor descriptor =
                repositoryService.localOrCachedRepoDescriptorByKey("libs-releases-local");
        RepoPath path = new RepoPath(descriptor.getKey(), "/");
        repositoryService.undeploy(path);

        context.importFrom(importSettings, new StatusHolder());

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        File dataDir = ArtifactoryHome.getDataDir();
        long sizeAfterOperations = 0;
        sizeAfterOperations = getFolderTotalSize(dataDir, sizeAfterOperations);

        tearDown();

        long sizeAfterCompress = 0;
        sizeAfterCompress = getFolderTotalSize(dataDir, sizeAfterCompress);

        Assert.assertTrue(sizeAfterCompress < sizeAfterOperations,
                "The data folder should have been compressed");
    }

    /**
     * Overriding the tearDown method of ArtifactoryTestBase, because we need to make sure that All the system is shut
     * down before we attempt to compress the database tables
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        ArtifactoryCli.main(new String[]{"compress", super.getHomePath(), "--url",
                "http://localhost:8080/artifactory/api/",
                "--username", "admin",
                "--password", "password"});
    }

    /**
     * Returns the size of the file\folder which was recieved (Recursive)
     *
     * @param location The location needed of size measuring
     * @param size     An initialized long variable
     * @return long - Variable which contains the size of the given location
     */
    private long getFolderTotalSize(File location, long size) {
        size += location.length();

        for (File child : location.listFiles()) {
            if (child.isFile()) {
                size += child.length();
            } else {
                size = getFolderTotalSize(child, size);
            }
        }

        return size;
    }
}