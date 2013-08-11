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

import org.artifactory.version.converter.XmlConverter;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

import java.util.List;

/**
 * Converts the logback configuration file to comply with the package changes that have occurred as a result of the
 * addition of the public API module.
 *
 * @author Noam Y. Tenne
 */
public class PublicApiPackageChangeLoggerConverter implements XmlConverter {

    @Override
    @SuppressWarnings({"unchecked"})
    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Namespace namespace = root.getNamespace();

        List<Element> loggerElements = root.getChildren("logger", namespace);
        if (loggerElements != null) {
            for (Element loggerElement : loggerElements) {
                Attribute nameAttribute = loggerElement.getAttribute("name");
                if (nameAttribute != null) {
                    String nameAttributeValue = nameAttribute.getValue();
                    if ("org.artifactory.api.common.StatusHolder".equals(nameAttributeValue)) {
                        nameAttribute.setValue("org.artifactory.api.common.BasicStatusHolder");
                    }
                }
            }
        }
    }
}
