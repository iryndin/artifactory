package org.artifactory.repo;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.jcr.JcrRepo;
import org.artifactory.spring.InternalArtifactoryContext;
import org.easymock.EasyMock;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the JcrRepo.
 *
 * @author Noam Tenne
 */
public class JcrRepoTest {
    @Test
    public void testNoFilePermission() {
        AuthorizationService authService = EasyMock.createMock(AuthorizationService.class);

        InternalArtifactoryContext context = EasyMock.createMock(InternalArtifactoryContext.class);
        EasyMock.expect(context.getAuthorizationService()).andReturn(authService);
        EasyMock.replay(context);
        ArtifactoryContextThreadBinder.bind(context);

        InternalRepositoryService irs = EasyMock.createMock(InternalRepositoryService.class);
        EasyMock.replay(irs);

        LocalRepoDescriptor lrd = new LocalRepoDescriptor();
        lrd.setKey("libs");
        JcrRepo jcrRepo = new JcrRepo(irs, lrd);
        ReflectionTestUtils.setField(jcrRepo, "anonAccessEnabled", false);
        RepoPath path = new RepoPath("libs", "jfrog/settings/jfrog-settings-sources.zip");
        EasyMock.expect(authService.canRead(path)).andReturn(false);
        EasyMock.expect(authService.currentUsername()).andReturn("testUser");
        EasyMock.replay(authService);

        StatusHolder holder = jcrRepo.allowsDownload(path);
        Assert.assertTrue(holder.isError(), "User should not have access to src files");
        EasyMock.verify(context, authService, irs);
    }
}
