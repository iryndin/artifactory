package org.artifactory.version.converter.v167;

import org.artifactory.convert.XmlConverterTest;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Shay Yaakov
 */
@Test
public class TrashcanConfigConverterTest extends XmlConverterTest {

    @Test
    public void convert() throws Exception {
        Document document = convertXml("/config/test/config-1.5.13-multi_repo_type.xml", new TrashcanConfigConverter());
        Element rootElement = document.getRootElement();
        Namespace namespace = rootElement.getNamespace();
        validateDefaultsAdded(rootElement, namespace);
    }

    private void validateDefaultsAdded(Element rootElement, Namespace namespace) {
        Element trashcanConfig = rootElement.getChild("trashcanConfig", namespace);
        assertNotNull(trashcanConfig);
        Namespace trashcanConfigNamespace = trashcanConfig.getNamespace();
        assertEquals(trashcanConfig.getChildText("enabled", trashcanConfigNamespace), "true");
        assertEquals(trashcanConfig.getChildText("retentionPeriodDays", trashcanConfigNamespace), "14");
    }
}