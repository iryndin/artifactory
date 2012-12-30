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

package org.artifactory.update.security.v7;

import org.apache.commons.io.FileUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Tests {@link DeleteForSecurityDumpConverter}
 *
 * @author Shay Yaakov
 */
@Test
public class DeleteForSecurityDumpConverterTest extends ArtifactoryHomeBoundTest {

    @AfterMethod
    public void cleanup() {
        FileUtils.deleteQuietly(new File(ArtifactoryHome.get().getDataDir(), ".deleteForSecurityMarker"));
    }

    public void testCreatingNewFile() throws Exception {
        assertFalse(new File(ArtifactoryHome.get().getDataDir(), ".deleteForSecurityMarker").exists());
        new DeleteForSecurityDumpConverter().convert(null);
        assertTrue(new File(ArtifactoryHome.get().getDataDir(), ".deleteForSecurityMarker").exists());
    }

    public void testFileExists() throws Exception {
        assertTrue(new File(ArtifactoryHome.get().getDataDir(), ".deleteForSecurityMarker").createNewFile());
        new DeleteForSecurityDumpConverter().convert(null);
        assertTrue(new File(ArtifactoryHome.get().getDataDir(), ".deleteForSecurityMarker").exists());
    }
}
