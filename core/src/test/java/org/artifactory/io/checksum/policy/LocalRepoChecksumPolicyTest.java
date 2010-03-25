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

package org.artifactory.io.checksum.policy;

import com.google.common.collect.Sets;
import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.descriptor.repo.LocalRepoChecksumPolicyType;
import org.artifactory.test.SystemPropertiesBoundTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Set;

import static org.artifactory.api.mime.ChecksumType.md5;
import static org.artifactory.api.mime.ChecksumType.sha1;
import static org.testng.Assert.*;

/**
 * Tests the LocalRepoChecksumPolicyTest class.
 *
 * @author Yossi Shaul
 */
@Test
public class LocalRepoChecksumPolicyTest extends SystemPropertiesBoundTest {
    private Set<ChecksumInfo> checksums;

    @BeforeClass
    public void createChecksumPolicy() {
        checksums = Sets.newHashSet(
                new ChecksumInfo(sha1, "clientsha1", "serversha1"),
                new ChecksumInfo(md5, "clientmd5", "servermd5"));
    }

    public void clientPolicyType() {
        LocalRepoChecksumPolicy policy = new LocalRepoChecksumPolicy(); // client type is the default
        assertEquals(policy.getChecksum(sha1, checksums), "clientsha1");
        assertEquals(policy.getChecksum(md5, checksums), "clientmd5");
    }

    public void serverPolicyType() {
        LocalRepoChecksumPolicy policy = new LocalRepoChecksumPolicy();
        policy.setPolicyType(LocalRepoChecksumPolicyType.SERVER);
        assertEquals(policy.getChecksum(sha1, checksums), "serversha1");
        assertEquals(policy.getChecksum(md5, checksums), "servermd5");
    }

    public void checksumTypeNotFound() {
        LocalRepoChecksumPolicy policy = new LocalRepoChecksumPolicy();
        Set<ChecksumInfo> empty = Sets.newHashSet();
        assertNull(policy.getChecksum(sha1, empty));
        assertNull(policy.getChecksum(md5, empty));
    }

    public void checksumValueNotFound() {
        LocalRepoChecksumPolicy policy = new LocalRepoChecksumPolicy();
        Set<ChecksumInfo> noOriginal = Sets.newHashSet(new ChecksumInfo(sha1, null, "serversha1"));
        assertNull(policy.getChecksum(sha1, noOriginal));

        policy.setPolicyType(LocalRepoChecksumPolicyType.SERVER);
        assertNotNull(policy.getChecksum(sha1, noOriginal), "serversha1");
    }

    public void checksumOfMetadata() {
        LocalRepoChecksumPolicy policy = new LocalRepoChecksumPolicy();
        RepoPath metadataPath = new RepoPath("repo", "test/test/1.0/maven-metadata.xml");
        assertEquals(policy.getChecksum(sha1, checksums, metadataPath), "serversha1");

        policy.setPolicyType(LocalRepoChecksumPolicyType.SERVER);
        assertNotNull(policy.getChecksum(sha1, checksums, metadataPath), "serversha1");
    }

    public void checksumOfSnapshotMetadata() {
        LocalRepoChecksumPolicy policy = new LocalRepoChecksumPolicy();
        RepoPath metadataPath = new RepoPath("repo", "test/test/1.0-SNAPSHOT/maven-metadata.xml");
        assertEquals(policy.getChecksum(sha1, checksums, metadataPath), "serversha1");

        policy.setPolicyType(LocalRepoChecksumPolicyType.SERVER);
        assertNotNull(policy.getChecksum(sha1, checksums, metadataPath), "serversha1");
    }
}