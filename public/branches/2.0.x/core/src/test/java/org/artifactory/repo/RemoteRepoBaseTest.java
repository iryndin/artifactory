package org.artifactory.repo;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalArtifactoryContext;
import org.easymock.EasyMock;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * This class tests the behaviour of the allowsDownloadMethod and checks that it returns the proper results on different
 * scenarios
 *
 * @author Noam Tenne
 */
public class RemoteRepoBaseTest {
    AuthorizationService authService = EasyMock.createMock(AuthorizationService.class);
    InternalRepositoryService internalRepoService = EasyMock.createMock(InternalRepositoryService.class);
    InternalArtifactoryContext context = EasyMock.createMock(InternalArtifactoryContext.class);
    HttpRepoDescriptor httpRepoDescriptor = new HttpRepoDescriptor();
    HttpRepo httpRepo = new HttpRepo(internalRepoService, httpRepoDescriptor, false);
    RepoPath repoPath = new RepoPath("remote-repo-cache", "test/test/1.0/test-1.0.jar");

    /**
     * Try to download with ordinary path
     */
    @Test
    public void testDownloadWithCachePrefix() {
        expectCanRead();
        createJcrCacheRepo(false);
        StatusHolder statusHolder = tryAllowsDownload(repoPath);
        Assert.assertFalse(statusHolder.isError());
    }

    /**
     * Try to download while supplying a path with no "-cache" prefix
     */
    @Test
    public void testDownloadWithNoCachePrefix() {
        RepoPath repoPathNoPrefix = new RepoPath("remote-repo", "test/test/1.0/test-1.0.jar");
        expectCanRead();
        createJcrCacheRepo(false);
        StatusHolder statusHolder = tryAllowsDownload(repoPathNoPrefix);
        Assert.assertFalse(statusHolder.isError());
    }

    /**
     * Try to download when Anonymous mode is enabled
     */
    @Test
    public void testDownloadWithAnonymousEnabled() {
        createJcrCacheRepo(true);
        StatusHolder statusHolder = tryAllowsDownload(repoPath);
        Assert.assertFalse(statusHolder.isError());
    }

    /**
     * Try to download when the repo is blacked out
     */
    @Test
    public void testDownloadWithBlackedOut() {
        httpRepoDescriptor.setBlackedOut(true);
        createJcrCacheRepo(false);
        StatusHolder statusHolder = tryAllowsDownload(repoPath);
        Assert.assertTrue(statusHolder.isError());
        httpRepoDescriptor.setBlackedOut(false);
    }

    /**
     * Try to download when the repo does not serve releases
     */
    @Test
    public void testDownloadWithNoReleases() {
        httpRepoDescriptor.setHandleReleases(false);
        createJcrCacheRepo(false);
        StatusHolder statusHolder = tryAllowsDownload(repoPath);
        Assert.assertTrue(statusHolder.isError());
        httpRepoDescriptor.setHandleReleases(true);
    }

    /**
     * Try to download when requested resource is excluded
     */
    @Test
    public void testDownloadWithExcludes() {
        httpRepoDescriptor.setExcludesPattern("test/test/1.0/**");
        createJcrCacheRepo(false);
        StatusHolder statusHolder = tryAllowsDownload(repoPath);
        Assert.assertTrue(statusHolder.isError());
        httpRepoDescriptor.setExcludesPattern("");
    }

    /**
     * Create a JcrCacheRepo object ready for the test
     *
     * @param anonymousEnabled True if to enable the Anonymous mode on the repo, false if not
     */
    private void createJcrCacheRepo(boolean anonymousEnabled) {
        httpRepoDescriptor.setKey("remote-repo");
        JcrCacheRepo localCacheRepo = new JcrCacheRepo(httpRepo);
        ReflectionTestUtils.setField(localCacheRepo, "anonAccessEnabled", anonymousEnabled);
        ReflectionTestUtils.setField(httpRepo, "localCacheRepo", localCacheRepo);
    }

    /**
     * Setup the authentication service and the context to expect a canRead request
     */
    private void expectCanRead() {
        EasyMock.expect(authService.canRead(repoPath)).andReturn(true);
        EasyMock.replay(authService);
        EasyMock.expect(context.getAuthorizationService()).andReturn(authService);
        EasyMock.replay(context);
        ArtifactoryContextThreadBinder.bind(context);
    }

    /**
     * Try to download the the given path
     *
     * @param path Repopath to test the download with
     * @return StatusHolder - Cotains any errors which might be monitored
     */
    private StatusHolder tryAllowsDownload(RepoPath path) {
        StatusHolder statusHolder = httpRepo.allowsDownload(path);
        return statusHolder;
    }

    /**
     * Resets the mock objects after every test method to make sure the numbers of the expected method calls are exact
     */
    @AfterMethod
    private void resetMocks() {
        EasyMock.reset(authService, internalRepoService, context);
        EasyMock.verify();
    }
}
