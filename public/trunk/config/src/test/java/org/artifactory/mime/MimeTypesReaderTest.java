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

package org.artifactory.mime;

import org.artifactory.util.ResourceUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Set;

import static org.testng.Assert.*;

/**
 * Tests the {@link MimeTypesReader}
 *
 * @author Yossi Shaul
 */
public class MimeTypesReaderTest {
    private MimeTypes mimeTypes;

    @BeforeClass
    public void setup() {
        MimeTypesReader reader = new MimeTypesReader();
        mimeTypes = reader.read(ResourceUtils.getResourceAsFile("/META-INF/default/mimetypes-test.xml"));
        assertNotNull(mimeTypes, "Should not return null");
        assertEquals(mimeTypes.getMimeTypes().size(), 5, "Unexpected count of mime types");
    }

    @Test
    public void checkArchiveMime() throws Exception {
        MimeType archive = mimeTypes.getByMime(MimeType.javaArchive);
        assertNotNull(archive, "Couldn't find application/java-archive mime");
        assertEquals(archive.getExtensions().size(), 5, "Unexpected file extensions count");
        assertFalse(archive.isViewable(), "Should not be viewable");
        assertFalse(archive.isXml(), "Should be marked as xml");
        assertFalse(archive.isArchive(), "Should be marked as archive");
        assertNull(archive.getSyntax(), "No syntax configured for this type");
        assertNull(archive.getCss(), "No css class configured for this type");
    }

    @Test
    public void trimmedFileExtensions() throws Exception {
        // the file extensions list is usually with spaces that should be trimmed
        MimeType archive = mimeTypes.getByMime(MimeType.javaArchive);
        Set<String> extensions = archive.getExtensions();
        assertTrue(extensions.contains("war"), "war extension not found in: " + extensions);
        assertTrue(extensions.contains("jar"), "jar extension not found in: " + extensions);
    }
}
