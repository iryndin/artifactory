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

package org.artifactory.logging.version.v3;

import org.artifactory.convert.XmlConverterTest;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * A test for LineNumberLayoutLoggerConverter
 *
 * @author Tomer Cohen
 * @see LineNumberLayoutLoggerConverter
 */
@Test
public class LineNumberLayoutLoggerConverterTest extends XmlConverterTest {

    private static final String NEW_CLASS = "org.artifactory.logging.layout.BackTracePatternLayout";
    private static final String ORIGINAL_CLASS = "ch.qos.logback.classic.PatternLayout";

    @SuppressWarnings({"unchecked"})
    public void testConversion() throws Exception {
        Document doc =
                convertXml("/org/artifactory/logging/version/v2/logback.xml", new LineNumberLayoutLoggerConverter());

        Element docRoot = doc.getRootElement();
        Namespace rootNamespace = docRoot.getNamespace();

        List<Element> appenders = docRoot.getChildren("appender", rootNamespace);
        Assert.assertFalse(appenders.isEmpty(), "should have at least one appender");
        for (Element appender : appenders) {
            String appenderName = appender.getAttribute("name").getValue();
            Element layout = appender.getChild("layout", rootNamespace);
            Attribute appenderClassAttribute = layout.getAttribute("class");
            if ("REQUEST".equals(appenderName) || "ACCESS".equals(appenderName) || "TRAFFIC".equals(appenderName)) {
                Assert.assertEquals(appenderClassAttribute.getValue(), ORIGINAL_CLASS);
            } else {
                Assert.assertEquals(appenderClassAttribute.getValue(), NEW_CLASS);
            }
        }
    }
}
