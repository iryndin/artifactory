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

package org.artifactory.api.mime;

import org.artifactory.common.property.ArtifactorySystemProperties;
import org.artifactory.test.SystemPropertiesBoundTest;
import org.artifactory.util.ResourceUtils;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.*;

/**
 * @author Eli Givoni
 */
public class XmlAdditionalMimeTypePatternTest extends SystemPropertiesBoundTest {

    @Test
    public void getXmlAdditionalMimeTypeContentType() {
        File file = ResourceUtils.getResourceAsFile("/system/artifactory.system.2.properties");
        ArtifactorySystemProperties.get().loadArtifactorySystemProperties(file, null);
        NamingUtils.initializeContentTypesMap();

        ContentType contentType1 = NamingUtils.getContentType("path/1.0-SNAPSHOT/maven-metadata.myextension");
        assertContentType(contentType1);

        ContentType contentType2 = NamingUtils.getContentType("path/1.0-SNAPSHOT/maven-metadata.jfrog");
        assertContentType(contentType2);

        ContentType contentType3 = NamingUtils.getContentType("path/1.0-SNAPSHOT/maven-metadata.jfrog2");
        assertEquals(contentType3, ContentType.def, "Expecting default content type");
    }

    private void assertContentType(ContentType contentType) {
        assertNotNull(contentType, "Expected a content type object");
        assertEquals(contentType, ContentType.applicationXml, "Expected applicationXml contentType");
        assertTrue(contentType.isXml(), "Epected content type of xml type");
    }
}
