package org.artifactory.jcr.version.v228

import com.google.common.io.Files
import org.artifactory.common.ArtifactoryHome
import org.artifactory.test.ArtifactoryHomeBoundTest
import org.artifactory.test.ArtifactoryHomeStub
import org.easymock.EasyMock
import org.testng.annotations.Test

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
public class MarkerFileConverterTest extends ArtifactoryHomeBoundTest {

    @Override
    protected ArtifactoryHomeStub getOrCreateArtifactoryHomeStub() {

        def mock = EasyMock.createMock ArtifactoryHomeStub.class

        def tempDataDir = Files.createTempDir()
        EasyMock.expect mock.getDataDir() andReturn tempDataDir anyTimes()
        EasyMock.replay mock

        return mock
    }

    @Test
    public void testMarkerFileCreation() {
        new MarkerFileConverter().convert null
        def dataDir = ArtifactoryHome.get().getDataDir()
        assert new File(dataDir, MarkerFileConverter.CREATE_DEFAULT_STATS_MARKER_FILENAME).exists()
        assert new File(dataDir, MarkerFileConverter.REPAIR_WATCHERS_MARKER_FILENAME).exists()
    }
}