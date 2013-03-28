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

package org.artifactory.jcr.version.v228

import com.google.common.io.Files
import org.artifactory.common.ArtifactoryHome
import org.artifactory.io.checksum.ChecksumPathsImpl
import org.artifactory.test.ArtifactoryHomeBoundTest
import org.artifactory.test.ArtifactoryHomeStub
import org.easymock.EasyMock
import org.testng.annotations.Test

public class DeleteForConsistencyFixConverterTest extends ArtifactoryHomeBoundTest {

    def artifactoryHomeMock

    @Override
    protected ArtifactoryHomeStub getOrCreateArtifactoryHomeStub() {

        artifactoryHomeMock = EasyMock.createMock ArtifactoryHomeStub.class
        artifactoryHomeMock
    }

    @Test
    public void testFileExists() {
        def tempDataDir = Files.createTempDir()
        assert new File(tempDataDir, ChecksumPathsImpl.DELETE_FOR_CONSISTENCY_FIX_FILENAME).createNewFile()
        EasyMock.reset artifactoryHomeMock
        EasyMock.expect artifactoryHomeMock.getDataDir() andReturn tempDataDir anyTimes()
        EasyMock.expect artifactoryHomeMock.isNewDataDir() andReturn false anyTimes()
        EasyMock.replay artifactoryHomeMock
        new DeleteForConsistencyFixConverter().convert null
        def dataDir = ArtifactoryHome.get().getDataDir()
        assert new File(dataDir, ChecksumPathsImpl.DELETE_FOR_CONSISTENCY_FIX_FILENAME).exists()
    }

    @Test
    public void testExistingDataDir() {
        def tempDataDir = Files.createTempDir()
        assert !new File(tempDataDir, ChecksumPathsImpl.DELETE_FOR_CONSISTENCY_FIX_FILENAME).exists()
        EasyMock.reset artifactoryHomeMock
        EasyMock.expect artifactoryHomeMock.getDataDir() andReturn tempDataDir anyTimes()
        EasyMock.expect artifactoryHomeMock.isNewDataDir() andReturn false anyTimes()
        EasyMock.replay artifactoryHomeMock
        new DeleteForConsistencyFixConverter().convert null
        def dataDir = ArtifactoryHome.get().getDataDir()
        assert new File(dataDir, ChecksumPathsImpl.DELETE_FOR_CONSISTENCY_FIX_FILENAME).exists()
    }

    @Test
    public void testNewDataDir() {
        def tempDataDir = Files.createTempDir()
        EasyMock.reset artifactoryHomeMock
        EasyMock.expect artifactoryHomeMock.getDataDir() andReturn tempDataDir anyTimes()
        EasyMock.expect artifactoryHomeMock.isNewDataDir() andReturn true anyTimes()
        EasyMock.replay artifactoryHomeMock
        new DeleteForConsistencyFixConverter().convert null
        def dataDir = ArtifactoryHome.get().getDataDir()
        assert new File(dataDir, ChecksumPathsImpl.DELETE_FOR_CONSISTENCY_FIX_FILENAME).exists()
    }
}