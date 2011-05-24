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

package org.artifactory.resource;

import org.artifactory.api.fs.FileInfoImpl;
import org.artifactory.api.fs.RepoResource;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.fs.FileInfo;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.Checksums;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

/**
 * Tests the auto checksum recalculation of the resolved resource
 *
 * @author Noam Y. Tenne
 */
@Test
public class ResolvedResourceTest {

    public void testAutoChecksumReCalculation() throws IOException {
        String content = "dfadfgadfgadfgkadmgwtw45624563454k  fsg w5yk54ymkwmtk   tk4t5k mkqr q24kmfkmc";
        Checksum[] contentChecksums = calcChecksum(content);

        FileInfo fileInfo = new FileInfoImpl(new RepoPathImpl("demo", "demo"));

        RepoResource originalResource = prepareOriginalRequestMock(fileInfo);

        new ResolvedResource(originalResource, content);
        assertFileInfoChecksums(contentChecksums, fileInfo);
    }

    public void testExplicitChecksumReCalculation() throws IOException {
        String content = "dfgl4kmaewawm3,2r3 , awfm,q4t,m5y l,blt,h5664,3,';. p32.42,35346346";
        Checksum[] contentChecksums = calcChecksum(content);

        FileInfo fileInfo = new FileInfoImpl(new RepoPathImpl("demo", "demo"));

        RepoResource originalResource = prepareOriginalRequestMock(fileInfo);

        new ResolvedResource(originalResource, content, true);
        assertFileInfoChecksums(contentChecksums, fileInfo);
    }

    public void testNoChecksumReCalculation() throws IOException {
        String content = "fsdgdhthjtjml dfglmr5ouyw432qq ./ad,fgaw;l3tmr23 freg g";
        FileInfo fileInfo = new FileInfoImpl(new RepoPathImpl("demo", "demo"));

        RepoResource originalResource = prepareOriginalRequestMock(fileInfo);

        new ResolvedResource(originalResource, content, false);
        Assert.assertTrue(fileInfo.getChecksums().isEmpty(), "Resource checksums should not have been recalculated.");
    }

    private RepoResource prepareOriginalRequestMock(FileInfo fileInfo) {
        RepoResource originalResource = EasyMock.createMock(RepoResource.class);
        EasyMock.expect(originalResource.getInfo()).andReturn(fileInfo).times(2);
        EasyMock.replay(originalResource);
        return originalResource;
    }

    private void assertFileInfoChecksums(Checksum[] contentChecksums, FileInfo fileInfo) {
        Set<ChecksumInfo> recalculatedChecksums = fileInfo.getChecksums();
        assertNotNull(recalculatedChecksums, "Resource checksums should have been recalculated.");
        assertFalse(recalculatedChecksums.isEmpty(), "Resource checksums should have been recalculated.");

        for (Checksum expectedChecksum : contentChecksums) {

            boolean foundRecalculated = false;

            for (ChecksumInfo recalculatedChecksum : recalculatedChecksums) {
                if (expectedChecksum.getType().equals(recalculatedChecksum.getType()) &&
                        expectedChecksum.getChecksum().equals(recalculatedChecksum.getActual()) &&
                        expectedChecksum.getChecksum().equals(recalculatedChecksum.getOriginal())) {
                    foundRecalculated = true;
                }
            }

            Assert.assertTrue(foundRecalculated, "Resource checksums should have been recalculated.");
        }
    }

    private Checksum[] calcChecksum(String content) throws IOException {
        return Checksums.calculate(new ByteArrayInputStream(content.getBytes("utf-8")), ChecksumType.values());
    }
}
