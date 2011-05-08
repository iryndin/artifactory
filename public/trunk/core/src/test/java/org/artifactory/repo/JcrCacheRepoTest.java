/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

import org.artifactory.api.fs.FileInfoImpl;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.descriptor.repo.ChecksumPolicyType;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.MetadataResource;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.artifactory.test.mock.MockUtils;
import org.artifactory.util.RepoLayoutUtils;
import org.easymock.EasyMock;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Yoav Landman
 */
public class JcrCacheRepoTest extends ArtifactoryHomeBoundTest {

    @Test
    public void testExpiry() {
        FileResource releaseRes = new FileResource(new FileInfoImpl(new RepoPathImpl("repox", "g1/g2/g3/a/v1/a.jar")));
        releaseRes.getInfo().setLastModified(0);
        FileResource snapRes = new FileResource(
                new FileInfoImpl(new RepoPathImpl("repox", "g1/g2/g3/a/v1-SNAPSHOT/a-v1-SNAPSHOT.pom")));
        snapRes.getInfo().setLastModified(0);
        FileResource uniqueSnapRes =
                new FileResource(new FileInfoImpl(
                        new RepoPathImpl("repox", "g1/g2/g3/a/v1-SNAPSHOT/a-v1-20081214.090217-4.pom")));
        uniqueSnapRes.getInfo().setLastModified(0);
        FileResource nonSnapRes = new FileResource(
                new FileInfoImpl(new RepoPathImpl("repox", "g1/g2/g3/a/v1/aSNAPSHOT.pom")));
        nonSnapRes.getInfo().setLastModified(0);
        MetadataResource relMdRes = new MetadataResource(new RepoPathImpl("repox", "g1/g2/g3/a/v1/maven-metadata.xml"));
        relMdRes.getInfo().setLastModified(0);
        FileResource snapMdRes =
                new FileResource(
                        new FileInfoImpl(new RepoPathImpl("repox", "g1/g2/g3/a/v1-SNAPSHOT/maven-metadata.xml")));
        snapMdRes.getInfo().setLastModified(0);
        FileResource nonsnapMdRes =
                new FileResource(new FileInfoImpl(new RepoPathImpl("repox", "g1/g2/g3/a/v1/maven-metadata.metadata")));
        nonsnapMdRes.getInfo().setLastModified(0);
        FileResource indexRes = new FileResource(
                new FileInfoImpl(new RepoPathImpl("repox", MavenNaming.NEXUS_INDEX_GZ)));
        indexRes.getInfo().setLastModified(0);

        RemoteRepo<?> remoteRepo = createRemoteRepoMock(0L);

        InternalArtifactoryContext context = MockUtils.getThreadBoundedMockContext();
        EasyMock.replay(context);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        JcrCacheRepo cacheRepo = new JcrCacheRepo(remoteRepo, null);

        assertFalse(cacheRepo.isExpired(releaseRes));
        assertTrue(cacheRepo.isExpired(snapRes));
        assertFalse(cacheRepo.isExpired(uniqueSnapRes));
        assertFalse(cacheRepo.isExpired(nonSnapRes));
        assertTrue(cacheRepo.isExpired(relMdRes));
        assertTrue(cacheRepo.isExpired(snapMdRes));
        assertFalse(cacheRepo.isExpired(nonsnapMdRes));
        assertTrue(cacheRepo.isExpired(indexRes));

        remoteRepo = createRemoteRepoMock(10L);
        cacheRepo = new JcrCacheRepo(remoteRepo, null);

        assertFalse(cacheRepo.isExpired(indexRes));
    }

    private RemoteRepo<?> createRemoteRepoMock(long expiry) {
        RemoteRepo remoteRepo = EasyMock.createMock(RemoteRepo.class);
        EasyMock.expect(remoteRepo.getRepositoryService()).andReturn(null).anyTimes();
        HttpRepoDescriptor httpRepoDescriptor = new HttpRepoDescriptor();
        httpRepoDescriptor.setChecksumPolicyType(ChecksumPolicyType.FAIL);
        httpRepoDescriptor.setRepoLayout(RepoLayoutUtils.MAVEN_2_DEFAULT);
        EasyMock.expect(remoteRepo.getDescriptor()).andReturn(httpRepoDescriptor).anyTimes();
        EasyMock.expect(remoteRepo.getDescription()).andReturn("desc").anyTimes();
        EasyMock.expect(remoteRepo.getKey()).andReturn("repox").anyTimes();
        EasyMock.expect(remoteRepo.getRetrievalCachePeriodSecs()).andReturn(expiry).anyTimes();
        EasyMock.expect(remoteRepo.getUrl()).andReturn("http://jfrog").anyTimes();
        EasyMock.replay(remoteRepo);
        return remoteRepo;
    }
}