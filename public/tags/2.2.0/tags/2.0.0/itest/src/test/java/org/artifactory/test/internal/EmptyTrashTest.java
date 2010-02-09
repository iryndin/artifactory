package org.artifactory.test.internal;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrSession;
import org.artifactory.repo.service.InternalRepositoryService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.io.File;
import java.net.URL;

/**
 * Imports a some artifacts, and deletes them. Then checks to see if the EmptyTrashJob has worked properly by looking
 * For TrashFolders which might have been left over
 *
 * @author Noam Tenne
 */
public class EmptyTrashTest extends ArtifactoryTestBase {

    private InternalRepositoryService repositoryService;

    /**
     * Execute before all tests - initialize the repository service
     */
    @BeforeMethod
    public void setUp() {
        repositoryService = context.beanForType(InternalRepositoryService.class);
    }

    /**
     * Try to import a couple of artifacts, delete them, wait for 10 seconds, and check that the trash is empty
     *
     * @throws Exception Exception
     */
    @Test
    public void testTrashIsEmpty() throws Exception {

        String releasesResourcePath = "/export/VersionTest/repositories/libs-releases-local";
        String releasesTargetRepo = "libs-releases-local";
        importAndDelete(releasesResourcePath, releasesTargetRepo);

        String snapshotsResourcePath = "/export/VersionTest/repositories/libs-snapshots-local";
        String snapshotsTargetRepo = "libs-snapshots-local";
        importAndDelete(snapshotsResourcePath, snapshotsTargetRepo);

        stall();

        checkForTrash();
    }

    /**
     * Import and delete the from the given resource path to the given target repo
     *
     * @param resourcePath Artifacts to import
     * @param targetRepo   Target repo to import into
     * @throws InterruptedException Interrupted exception
     */
    private void importAndDelete(String resourcePath, String targetRepo) throws InterruptedException {
        URL resourceURL = ImportExportTest.class.getResource(resourcePath);
        File resourceDir = new File(resourceURL.getFile());
        ImportSettings importSettings = new ImportSettings(resourceDir);
        importSettings.setCopyToWorkingFolder(false);
        importSettings.setFailFast(true);
        importSettings.setFailIfEmpty(true);
        importSettings.setVerbose(true);
        repositoryService.importRepo(targetRepo, importSettings, new StatusHolder());

        stall();

        LocalRepoDescriptor descriptor =
                repositoryService.localOrCachedRepoDescriptorByKey(targetRepo);
        RepoPath path = new RepoPath(descriptor.getKey(), "/");
        repositoryService.undeploy(path);
    }

    /**
     * Checks the to see if there is any trash left over
     *
     * @throws RepositoryException Repository exception
     */
    private void checkForTrash() throws RepositoryException {
        JcrSession session = context.getJcrService().getUnmanagedSession();
        String trashRootPath = JcrPath.get().getTrashJcrRootPath();
        Node trashNode = (Node) session.getItem(trashRootPath);
        NodeIterator trashFolderNodes = trashNode.getNodes();
        Assert.assertTrue(trashFolderNodes.getSize() == 0, "Trash folder should be empty after deletion");
    }

    /**
     * Wait for ten seconds
     *
     * @throws InterruptedException Interrupted exception
     */
    private void stall() throws InterruptedException {
        Thread.sleep(10000);
    }
}
