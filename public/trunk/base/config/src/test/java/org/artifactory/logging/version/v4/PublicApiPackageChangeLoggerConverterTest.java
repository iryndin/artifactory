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

package org.artifactory.logging.version.v4;

import org.apache.commons.lang.StringUtils;
import org.artifactory.convert.XmlConverterTest;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Tests that the logback configuration file was properly converted to comply with the package changes that followed the
 * addition of the public API module.
 *
 * @author Noam Y. Tenne
 */
@Test
public class PublicApiPackageChangeLoggerConverterTest extends XmlConverterTest {

    @SuppressWarnings({"unchecked"})
    public void testStatusHolderPackageConversion() throws Exception {
        Document doc = convertXml("/org/artifactory/logging/version/v2/logback.xml",
                new PublicApiPackageChangeLoggerConverter());

        Element root = doc.getRootElement();
        Namespace namespace = root.getNamespace();
        List<Element> loggerElements = root.getChildren("logger", namespace);

        boolean foundStatusHolderLogger = false;
        if (loggerElements != null) {
            for (Element loggerElement : loggerElements) {
                Attribute nameAttribute = loggerElement.getAttribute("name");
                if (nameAttribute != null) {
                    String nameAttributeValue = nameAttribute.getValue();
                    if (StringUtils.isNotBlank(nameAttributeValue) && nameAttributeValue.contains("StatusHolder")) {
                        foundStatusHolderLogger = true;
                        Assert.assertEquals(nameAttributeValue, "org.artifactory.api.common.BasicStatusHolder",
                                "The status holder logger does not reference the valid package after conversion.");
                    }
                }
            }
        }

        Assert.assertTrue(foundStatusHolderLogger,
                "Could not find the status holder logger within the logback configuration file.");
    }
}
