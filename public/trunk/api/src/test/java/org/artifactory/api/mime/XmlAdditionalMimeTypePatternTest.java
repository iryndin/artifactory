package org.artifactory.api.mime;

import org.artifactory.common.property.ArtifactorySystemProperties;
import org.artifactory.test.TestUtils;
import static org.testng.Assert.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Eli Givoni
 */
public class XmlAdditionalMimeTypePatternTest {

    @BeforeMethod
    public void bindProperties() {
        ArtifactorySystemProperties.bind(new ArtifactorySystemProperties());
    }

    @AfterMethod
    public void unbindProperties() {
        ArtifactorySystemProperties.unbind();
    }

    @Test
    public void getXmlAdditionalMimeTypeContentType() {
        File file = TestUtils.getResourceAsFile("/system/artifactory.system.2.properties");
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
