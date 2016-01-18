package org.artifactory.version.converter.v169;

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
public class PasswordMaxAgeConverterTest extends XmlConverterTest {

    @Test
    public void convert() throws Exception {
        Document document = convertXml("/config/test/config-1.6.8-expires_in.xml", new PasswordMaxAgeConverter());
        Element rootElement = document.getRootElement();
        Namespace namespace = rootElement.getNamespace();
        Element child = rootElement.getChild("security", namespace).getChild("passwordSettings", namespace)
                .getChild("expirationPolicy", namespace);
        assertNotNull(child);
        assertEquals(child.getChildText("passwordMaxAge", namespace), "60");
    }
}