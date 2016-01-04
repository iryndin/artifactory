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

package org.artifactory.version.converter.v167;

import com.google.common.collect.Lists;
import org.artifactory.descriptor.trashcan.TrashcanConfigDescriptor;
import org.artifactory.version.converter.XmlConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Adds the trashcan config section with default value where applicable
 *
 * @author Shay Yaakov
 */
public class TrashcanConfigConverter implements XmlConverter {

    private static final Logger log = LoggerFactory.getLogger(TrashcanConfigConverter.class);

    @Override
    public void convert(Document doc) {
        log.info("Starting default trashcan config conversion");
        Element rootElement = doc.getRootElement();
        Namespace namespace = rootElement.getNamespace();
        Element trashcanConfigElement = rootElement.getChild("trashcanConfig", namespace);
        if (trashcanConfigElement == null) {
            log.info("No trashcan config found - adding default one");
            addDefaultConfig(rootElement, namespace);
        }
        log.info("Finished default trashcan config conversion");
    }

    private void addDefaultConfig(Element rootElement, Namespace namespace) {
        TrashcanConfigDescriptor descriptor = new TrashcanConfigDescriptor();
        Element trashcan = new Element("trashcanConfig", namespace);
        Namespace trashcanConfigNs = trashcan.getNamespace();
        ArrayList<Element> elements = Lists.newArrayList();
        elements.add(new Element("enabled", trashcanConfigNs).setText(String.valueOf(descriptor.isEnabled())));
        elements.add(new Element("retentionPeriodDays", trashcanConfigNs).setText(String.valueOf(descriptor.getRetentionPeriodDays())));
        trashcan.addContent(elements);
        rootElement.addContent(rootElement.getContentSize(), trashcan);
    }
}
