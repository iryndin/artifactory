/*
 * This file is part of Artifactory.
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

package org.artifactory.version;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 * Tests the ArtifactoryVersionReader.
 *
 * @author Yossi Shaul
 */
@Test
public class ArtifactoryVersionReaderTest {

    public void readFromFile() {
        URL resource = getClass().getResource("/version/artifactory1.2.5.properties");
        String fileName = resource.getFile();
        CompoundVersionDetails version = ArtifactoryVersionReader.read(new File(fileName));
        Assert.assertNotNull(version, "Version should have been resolved");
        Assert.assertEquals(version.getVersion(), ArtifactoryVersion.v125, "Unexpected version");
    }

    public void readFromStream() {
        InputStream propertiesFileStream = getClass().getResourceAsStream("/version/artifactory1.2.5.properties");
        CompoundVersionDetails version = ArtifactoryVersionReader.read(propertiesFileStream);
        Assert.assertNotNull(version, "Version should have been resolved");
        Assert.assertEquals(version.getVersion(), ArtifactoryVersion.v125, "Unexpected version");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void failIfNullStream() {
        ArtifactoryVersionReader.read((File) null);
    }

    public void readCurrentVersion() {
        ArtifactoryVersion current = ArtifactoryVersion.getCurrent();
        InputStream in = createInputStream(current.getValue(), current.getRevision());
        CompoundVersionDetails result = ArtifactoryVersionReader.read(in);
        Assert.assertEquals(result.getVersion(), current);
    }

    public void readDevelopmentVersion() {
        InputStream in = createInputStream("${project.version}", 123);
        CompoundVersionDetails result = ArtifactoryVersionReader.read(in);
        Assert.assertEquals(result.getVersion(), ArtifactoryVersion.getCurrent(),
                "Development version should always be the current version");
    }

    public void notExistingFutureVersion() {
        InputStream in = createInputStream("6.7.8.8", 123789);
        CompoundVersionDetails result = ArtifactoryVersionReader.read(in);
        Assert.assertNotNull(result, "Got null result but expected the current version to be returned");
        Assert.assertEquals(result.getVersion(), ArtifactoryVersion.getCurrent(),
                "Expected the current version to be returned");
    }

    private InputStream createInputStream(String version, int revision) {
        return new ByteArrayInputStream(String.format("artifactory.version=%s%n" +
                "artifactory.revision=%s%n", version, revision + "").getBytes());
    }

}
