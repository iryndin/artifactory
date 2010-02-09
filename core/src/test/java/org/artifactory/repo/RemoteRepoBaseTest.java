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

package org.artifactory.repo;

import org.apache.commons.io.IOUtils;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.jcr.md.MetadataService;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalArtifactoryContext;
import org.easymock.EasyMock;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.lang.reflect.Method;

/**
 * This class tests the behaviour of the allowsDownloadMethod and checks that it returns the proper results on different
 * scenarios
 *
 * @author Noam Tenne
 */
public class RemoteRepoBaseTest {
    AuthorizationService authService = EasyMock.createNiceMock(AuthorizationService.class);
    InternalRepositoryService internalRepoService = EasyMock.createMock(InternalRepositoryService.class);
    InternalArtifactoryContext context = EasyMock.createMock(InternalArtifactoryContext.class);
    MetadataService metadataService = EasyMock.createMock(MetadataService.class);

    {
        resetMocks();
    }

    HttpRepoDescriptor httpRepoDescriptor = new HttpRepoDescriptor();
    HttpRepo httpRepo = new HttpRepo(internalRepoService, httpRepoDescriptor, false, null);
    RepoPath repoPath = new RepoPath("remote-repo-cache", "test/test/1.0/test-1.0.jar");

    public RemoteRepoBaseTest() {
    }

    /**
     * Try to download with ordinary path
     */
    @Test
    public void testDownloadWithCachePrefix() {
        expectCanRead(true);
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
        expectCanRead(true);
        createJcrCacheRepo(false);
        StatusHolder statusHolder = tryAllowsDownload(repoPathNoPrefix);
        Assert.assertFalse(statusHolder.isError());
    }

    /**
     * Try to download when Anonymous mode is enabled
     */
    @Test
    public void testDownloadWithAnonymousEnabledAndNoReadPermissions() {
        expectCanRead(false);       // don't give read access
        createJcrCacheRepo(true);   // and enable anon
        StatusHolder statusHolder = tryAllowsDownload(repoPath);
        Assert.assertTrue(statusHolder.isError());
    }

    /**
     * Try to download when the repo is blacked out
     */
    @Test
    public void testDownloadWithBlackedOut() {
        EasyMock.replay(context);
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
        EasyMock.replay(context);
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
        EasyMock.replay(context);
        httpRepoDescriptor.setExcludesPattern("test/test/1.0/**");
        createJcrCacheRepo(false);
        StatusHolder statusHolder = tryAllowsDownload(repoPath);
        Assert.assertTrue(statusHolder.isError());
        httpRepoDescriptor.setExcludesPattern("");
    }

    /**
     * Verify reading of valid checksum file to test the {@link RemoteRepoBase#readAndFormatChecksum(java.io.InputStream)}
     * method
     *
     * @throws Exception
     */
    @Test
    public void testReadValidChecksum() throws Exception {
        invokeReadChecksum("valid", "dasfasdf4r234234q32asdfadfasasdfasdf");
    }

    /**
     * Verify reading of a checksum file containing comments to test the {@link RemoteRepoBase#readAndFormatChecksum(java.io.InputStream)}
     * method
     *
     * @throws Exception
     */
    @Test
    public void testReadCommentChecksum() throws Exception {
        invokeReadChecksum("comment", "asdfaeaef435345435asdf");
    }

    /**
     * Verify reading of a checksum file containing file description to test the {@link
     * RemoteRepoBase#readAndFormatChecksum(java.io.InputStream)} method
     *
     * @throws Exception
     */
    @Test
    public void testReadDescChecksum() throws Exception {
        invokeReadChecksum("desc", "dasfasdf4r234234q32asdfadfasdf");
    }

    /**
     * Verify reading of an empty checksum file to test the {@link RemoteRepoBase#readAndFormatChecksum(java.io.InputStream)}
     * method
     *
     * @throws Exception
     */
    @Test
    public void testReadEmptyChecksum() throws Exception {
        invokeReadChecksum("empty", "");
    }

    /**
     * Invoke the {@link RemoteRepoBase#readAndFormatChecksum(java.io.InputStream)} method and check the different
     * values that are read from the given checksum are as expected
     *
     * @param checksumType  Type of checksum test file to read
     * @param expectedValue Expected checksum value
     * @throws Exception
     */
    private void invokeReadChecksum(String checksumType, String expectedValue) throws Exception {
        InputStream stream = getClass().getResourceAsStream("/org/artifactory/repo/test-" + checksumType + ".md5");
        Method method = RemoteRepoBase.class.getDeclaredMethod("readAndFormatChecksum", InputStream.class);
        method.setAccessible(true);
        try {
            String checksum = (String) method.invoke(httpRepo, stream);
            Assert.assertEquals(expectedValue, checksum, "Incorrect checksum value returned.");
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    /**
     * Create a JcrCacheRepo object ready for the test
     *
     * @param anonymousEnabled True if to enable the Anonymous mode on the repo, false if not
     */
    private void createJcrCacheRepo(boolean anonymousEnabled) {
        httpRepoDescriptor.setKey("remote-repo");
        JcrCacheRepo localCacheRepo = new JcrCacheRepo(httpRepo, null);
        ReflectionTestUtils.setField(localCacheRepo, "anonAccessEnabled", anonymousEnabled);
        ReflectionTestUtils.setField(httpRepo, "localCacheRepo", localCacheRepo);
    }

    /**
     * Setup the authentication service and the context to expect a canRead request
     *
     * @param canRead
     */
    private void expectCanRead(boolean canRead) {
        EasyMock.expect(authService.canRead(repoPath)).andReturn(canRead);
        EasyMock.expect(context.getAuthorizationService()).andReturn(authService);
        EasyMock.replay(authService, context);
        ArtifactoryContextThreadBinder.bind(context);
    }

    /**
     * Try to download the the given path
     *
     * @param path Repopath to test the download with
     * @return StatusHolder - Cotains any errors which might be monitored
     */
    private StatusHolder tryAllowsDownload(RepoPath path) {
        StatusHolder statusHolder = httpRepo.checkDownloadIsAllowed(path);
        return statusHolder;
    }

    /**
     * Resets the mock objects after every test method to make sure the numbers of the expected method calls are exact
     */
    @BeforeMethod
    private void resetMocks() {
        EasyMock.reset(authService, internalRepoService, context);
        EasyMock.expect(context.beanForType(MetadataService.class)).andReturn(metadataService).anyTimes();
        ArtifactoryContextThreadBinder.bind(context);
    }
}
