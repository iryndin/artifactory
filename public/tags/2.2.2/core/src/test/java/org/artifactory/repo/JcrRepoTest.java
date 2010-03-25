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

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.repo.jcr.JcrLocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.test.SystemPropertiesBoundTest;
import org.easymock.EasyMock;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the JcrRepo.
 *
 * @author Noam Tenne
 */
public class JcrRepoTest extends SystemPropertiesBoundTest {
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
        JcrLocalRepo jcrLocalRepo = new JcrLocalRepo(irs, lrd, null);
        ReflectionTestUtils.setField(jcrLocalRepo, "anonAccessEnabled", false);
        RepoPath path = new RepoPath("libs", "jfrog/settings/jfrog-settings-sources.zip");
        EasyMock.expect(authService.canRead(path)).andReturn(false);
        EasyMock.expect(authService.currentUsername()).andReturn("testUser");
        EasyMock.replay(authService);

        StatusHolder holder = jcrLocalRepo.checkDownloadIsAllowed(path);
        Assert.assertTrue(holder.isError(), "User should not have access to src files");
        EasyMock.verify(context, authService, irs);
    }
}
