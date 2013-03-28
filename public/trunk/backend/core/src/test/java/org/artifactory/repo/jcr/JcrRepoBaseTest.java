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

package org.artifactory.repo.jcr;

import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.StatusHolder;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.snapshot.MavenSnapshotVersionAdapter;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.util.RepoLayoutUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertTrue;

/**
 * Unit test the JcrRepoBase.
 *
 * @author Yossi Shaul
 */
@Test
public class JcrRepoBaseTest {
    private AuthorizationService authService;
    private JcrRepoBase repo;

    @BeforeMethod
    public void setup() {
        ArtifactoryContext context = createNiceMock(InternalArtifactoryContext.class);
        authService = createNiceMock(AuthorizationService.class);
        expect(context.getAuthorizationService()).andReturn(authService);
        replay(context);
        ArtifactoryContextThreadBinder.bind(context);
        repo = createJcrRepoBase();
    }

    public void anonAccessDisabledAndNoReadPermissions() {
        setAnonAccessEnabled(repo, false);

        RepoPath dummyPath = InternalRepoPathFactory.create("target", "blabla");
        expect(authService.canRead(dummyPath)).andReturn(false);
        replay(authService);

        StatusHolder status = repo.checkDownloadIsAllowed(dummyPath);

        assertTrue(status.isError(), "Download is allowed: " + status.getStatusMsg());

        verify(authService);
    }

    public void anonAccessEnabledAndNoReadPermissions() {
        setAnonAccessEnabled(repo, true);

        RepoPath dummyPath = InternalRepoPathFactory.create("target", "blabla");
        expect(authService.canRead(dummyPath)).andReturn(false);
        replay(authService);

        StatusHolder status = repo.checkDownloadIsAllowed(dummyPath);

        assertTrue(status.isError(), "Download is allowed: " + status.getStatusMsg());

        verify(authService);
    }

    private void setAnonAccessEnabled(JcrRepoBase base, boolean enabled) {
        ReflectionTestUtils.setField(base, "anonAccessEnabled", enabled);
    }

    private JcrRepoBase<LocalRepoDescriptor> createJcrRepoBase() {
        LocalRepoDescriptor descriptor = new LocalRepoDescriptor();
        JcrRepoBase<LocalRepoDescriptor> repo = new JcrRepoBase<LocalRepoDescriptor>(null, null) {
            @Override
            public ChecksumPolicy getChecksumPolicy() {
                return null;
            }

            @Override
            public void onCreate(JcrFsItem fsItem) {
            }

            @Override
            public MavenSnapshotVersionAdapter getMavenSnapshotVersionAdapter() {
                return null;
            }

            @Override
            public boolean isSuppressPomConsistencyChecks() {
                return false;
            }
        };
        descriptor.setRepoLayout(RepoLayoutUtils.MAVEN_2_DEFAULT);
        repo.setDescriptor(descriptor);
        return repo;
    }
}
