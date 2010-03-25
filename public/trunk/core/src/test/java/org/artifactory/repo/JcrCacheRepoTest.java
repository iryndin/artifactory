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

import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.common.property.ArtifactorySystemProperties;
import org.artifactory.descriptor.repo.ChecksumPolicyType;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.MetadataResource;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.test.mock.MockUtils;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Yoav Landman
 */
public class JcrCacheRepoTest {
    InternalRepositoryService internalRepoService = EasyMock.createMock(InternalRepositoryService.class);

    @SuppressWarnings({"unchecked"})
    @Test
    public void testExpiry() {
        FileResource releaseRes = new FileResource(new RepoPath("repox", "g1/g2/g3/a/v1/a.jar"));
        releaseRes.getInfo().setLastModified(0);
        FileResource snapRes = new FileResource(new RepoPath("repox", "g1/g2/g3/a/v1-SNAPSHOT/a-v1-SNAPSHOT.pom"));
        snapRes.getInfo().setLastModified(0);
        FileResource uniqueSnapRes =
                new FileResource(new RepoPath("repox", "g1/g2/g3/a/v1-SNAPSHOT/a-v1-20081214.090217-4.pom"));
        uniqueSnapRes.getInfo().setLastModified(0);
        FileResource nonSnapRes = new FileResource(new RepoPath("repox", "g1/g2/g3/a/v1/aSNAPSHOT.pom"));
        nonSnapRes.getInfo().setLastModified(0);
        MetadataResource relMdRes = new MetadataResource(new RepoPath("repox", "g1/g2/g3/a/v1/maven-metadata.xml"));
        relMdRes.getInfo().setLastModified(0);
        FileResource snapMdRes = new FileResource(new RepoPath("repox", "g1/g2/g3/a/v1-SNAPSHOT/maven-metadata.xml"));
        snapMdRes.getInfo().setLastModified(0);
        FileResource nonsnapMdRes = new FileResource(new RepoPath("repox", "g1/g2/g3/a/v1/maven-metadata.metadata"));
        nonsnapMdRes.getInfo().setLastModified(0);
        FileResource indexRes = new FileResource(new RepoPath("repox", MavenNaming.NEXUS_INDEX_GZ));
        indexRes.getInfo().setLastModified(0);

        RemoteRepo remoteRepo = createRemoreRepoMock(0L);

        InternalArtifactoryContext context = MockUtils.getThreadBoundedMockContext();
        EasyMock.replay(context);

        JcrCacheRepo cacheRepo = new JcrCacheRepo(remoteRepo, null);

        Assert.assertFalse(cacheRepo.isExpired(releaseRes));
        Assert.assertTrue(cacheRepo.isExpired(snapRes));
        Assert.assertFalse(cacheRepo.isExpired(uniqueSnapRes));
        Assert.assertFalse(cacheRepo.isExpired(nonSnapRes));
        Assert.assertTrue(cacheRepo.isExpired(relMdRes));
        Assert.assertTrue(cacheRepo.isExpired(snapMdRes));
        Assert.assertFalse(cacheRepo.isExpired(nonsnapMdRes));
        Assert.assertTrue(cacheRepo.isExpired(indexRes));

        remoteRepo = createRemoreRepoMock(10L);
        cacheRepo = new JcrCacheRepo(remoteRepo, null);

        Assert.assertFalse(cacheRepo.isExpired(indexRes));
    }

    private RemoteRepo createRemoreRepoMock(long expiry) {
        RemoteRepo remoteRepo = EasyMock.createMock(RemoteRepo.class);
        EasyMock.expect(remoteRepo.getRepositoryService()).andReturn(null).anyTimes();
        HttpRepoDescriptor httpRepoDescriptor = new HttpRepoDescriptor();
        httpRepoDescriptor.setChecksumPolicyType(ChecksumPolicyType.FAIL);
        EasyMock.expect(remoteRepo.getDescriptor()).andReturn(httpRepoDescriptor).anyTimes();
        EasyMock.expect(remoteRepo.getDescription()).andReturn("desc").anyTimes();
        EasyMock.expect(remoteRepo.getKey()).andReturn("repox").anyTimes();
        EasyMock.expect(remoteRepo.getRetrievalCachePeriodSecs()).andReturn(expiry).anyTimes();
        EasyMock.expect(remoteRepo.getUrl()).andReturn("http://jfrog").anyTimes();
        EasyMock.replay(remoteRepo);
        return remoteRepo;
    }

    @BeforeMethod
    void bind() {
        ArtifactorySystemProperties artifactorySystemProperties = new ArtifactorySystemProperties();
        artifactorySystemProperties.loadArtifactorySystemProperties(null, null);
        ArtifactorySystemProperties.bind(artifactorySystemProperties);
    }

    @AfterMethod
    void unbind() {
        ArtifactorySystemProperties.unbind();
    }
}