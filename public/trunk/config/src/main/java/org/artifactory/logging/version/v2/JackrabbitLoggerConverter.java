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

package org.artifactory.logging.version.v2;

import com.google.common.collect.Lists;
import org.artifactory.version.converter.XmlConverter;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

import java.util.List;

import static org.artifactory.version.XmlConverterUtils.getAddedComment;
import static org.artifactory.version.XmlConverterUtils.getNewLine;

/**
 * Adds the jackrabbit loggers to the logback configuration
 *
 * @author Noam Y. Tenne
 */
public class JackrabbitLoggerConverter implements XmlConverter {

    private static final String LOGGER = "logger";
    private static final String NAME = "name";
    private static final String BDBPM_NAME = "org.apache.jackrabbit.core.persistence.bundle.BundleDbPersistenceManager";
    private static final String MULTI_INDEX_NAME = "org.apache.jackrabbit.core.query.lucene.MultiIndex";
    private static final String XML_TEXT_EXTRACTOR_NAME = "org.apache.jackrabbit.extractor.XMLTextExtractor";
    private static final String INFO = "INFO";

    /**
     * Converts the XML doc
     *
     * @param doc Doc to convert
     */
    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Namespace namespace = root.getNamespace();

        addLoggers(root, namespace);
    }

    /**
     * Adds the jackrabbit loggers if the aren't configured
     *
     * @param root      Doc root element
     * @param namespace Doc namespace
     */
    private void addLoggers(Element root, Namespace namespace) {
        @SuppressWarnings({"unchecked"})
        List<Element> loggers = root.getChildren(LOGGER, namespace);

        //BundleDbPersistenceManager
        boolean hasBDBPMLogger = false;
        boolean hasMultiIndexLogger = false;
        boolean hasXmlTextExtractorLogger = false;

        for (Element logger : loggers) {
            Attribute loggerName = logger.getAttribute(NAME, namespace);
            if (loggerName != null) {
                String nameValue = loggerName.getValue();
                if (!hasBDBPMLogger && (BDBPM_NAME.equals(nameValue))) {
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
        }

        if (hasBDBPMLogger || hasMultiIndexLogger || hasXmlTextExtractorLogger) {
            root.addContent(getNewLine());
        }
        if (!hasBDBPMLogger) {
            root.addContent(Lists.newArrayList(getAddedComment(), getNewLine(),
                    createAndGetLogger(namespace, BDBPM_NAME, INFO), getNewLine()));
        }

        if (!hasMultiIndexLogger) {
            root.addContent(Lists.newArrayList(getAddedComment(), getNewLine(),
                    createAndGetLogger(namespace, MULTI_INDEX_NAME, INFO), getNewLine()));
        }

        if (!hasXmlTextExtractorLogger) {
            root.addContent(Lists.newArrayList(getAddedComment(), getNewLine(),
                    createAndGetLogger(namespace, XML_TEXT_EXTRACTOR_NAME, "ERROR"), getNewLine()));
        }
    }

    /**
     * Creates and returns a logger element
     *
     * @param namespace Doc namespace
     * @param name      Logger name
     * @param level     Logger level
     * @return Logger declaration element
     */
    private Element createAndGetLogger(Namespace namespace, String name, String level) {
        Element levelElement = new Element("level", namespace);
        levelElement.setAttribute("value", level);

        Element logger = new Element(LOGGER, namespace);
        logger.setAttribute(NAME, name);
        logger.addContent(Lists.newArrayList(getNewLine(), levelElement, getNewLine()));

        return logger;
    }
}