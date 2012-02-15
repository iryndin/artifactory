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

package org.artifactory.logging.version.v2;

import org.artifactory.convert.XmlConverterTest;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Tests the Logback error MBean converter
 *
 * @author Noam Y. Tenne
 */
public class JackrabbitLoggerConverterTest extends XmlConverterTest {

    private static final String BDBPM_OLD_NAME = "org.apache.jackrabbit.core.persistence.bundle.BundleDbPersistenceManager";
    private static final String BDBPM_NEW_NAME = "org.apache.jackrabbit.core.persistence.pool.BundleDbPersistenceManager";
    private static final String MULTI_INDEX_NAME = "org.apache.jackrabbit.core.query.lucene.MultiIndex";
    private static final String XML_TEXT_EXTRACTOR_NAME = "org.apache.jackrabbit.extractor.XMLTextExtractor";

    /**
     * Tests that the jackrabbit loggers exist in the config after being converted
     */
    @Test
    public void testConversion() throws Exception {
        Document doc = convertXml("/org/artifactory/logging/version/v2/logback.xml", new JackrabbitLoggerConverter());

        Element docRoot = doc.getRootElement();
        Namespace rootNamespace = docRoot.getNamespace();

        @SuppressWarnings({"unchecked"})
        List<Element> loggers = docRoot.getChildren("logger", rootNamespace);

        boolean hasBDBPMLogger = false;
        boolean hasMultiIndexLogger = false;
        boolean hasXmlTextExtractorLogger = false;

        for (Element logger : loggers) {
            Attribute nameAttribute = logger.getAttribute("name", rootNamespace);
            Assert.assertNotNull(nameAttribute, "Found logger null 'name' attribute.");
            String nameValue = nameAttribute.getValue();
            if (!hasBDBPMLogger && (BDBPM_NEW_NAME.equals(nameValue))) {
                hasBDBPMLogger = true;
                continue;
            }
            if (!hasMultiIndexLogger && (MULTI_INDEX_NAME.equals(nameValue))) {
                hasMultiIndexLogger = true;
                continue;
            }
            if (!hasXmlTextExtractorLogger && (XML_TEXT_EXTRACTOR_NAME.equals(nameValue))) {
                hasXmlTextExtractorLogger = true;
            }
        }

        assertTrue(hasBDBPMLogger,
                "BundleDbPersistenceManager logger should exist in the logback config after conversion.");
        assertTrue(hasMultiIndexLogger, "MultiIndex logger should exist in the logback config after conversion.");
        assertTrue(hasXmlTextExtractorLogger,
                "XMLTextExtractor logger should exist in the logback config after conversion.");

        Element rootAppenderReference = docRoot.getChild("root", rootNamespace);
        Assert.assertNotNull(rootAppenderReference, "Found null root appender reference.");
    }

    /**
     * Tests renaming old BundleDbPersistenceManager package with the new one after conversion
     *
     * @throws Exception
     */
    @Test
    public void testConversionRenaming() throws Exception {
        Document doc = convertXml("/org/artifactory/logging/version/v4/logback.xml", new JackrabbitLoggerConverter());

        Element docRoot = doc.getRootElement();
        Namespace rootNamespace = docRoot.getNamespace();

        @SuppressWarnings({"unchecked"})
        List<Element> loggers = docRoot.getChildren("logger", rootNamespace);

        boolean hasNewBDBPMLogger = false;
        boolean hasOldBDBPMLogger = false;

        for (Element logger : loggers) {
            Attribute nameAttribute = logger.getAttribute("name", rootNamespace);
            Assert.assertNotNull(nameAttribute, "Found logger null 'name' attribute.");
            String nameValue = nameAttribute.getValue();
            if (!hasNewBDBPMLogger && (BDBPM_NEW_NAME.equals(nameValue))) {
                hasNewBDBPMLogger = true;
                continue;
            }
            if (!hasOldBDBPMLogger && (BDBPM_OLD_NAME.equals(nameValue))) {
                hasOldBDBPMLogger = true;
            }
        }

        assertTrue(hasNewBDBPMLogger,
                "new BundleDbPersistenceManager logger should exist in the logback config after conversion.");
        assertFalse(hasOldBDBPMLogger,
                "old BundleDbPersistenceManager logger should NOT exist in the logback config after conversion.");
    }
}
