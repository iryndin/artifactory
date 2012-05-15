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

import org.artifactory.version.converter.XmlConverter;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

import java.util.ArrayList;
import java.util.List;

/**
 * Convert the old layout appender to the new Artifactory Pattern layout
 *
 * @author Tomer Cohen
 */
public class LineNumberLayoutLoggerConverter implements XmlConverter {

    private static final String ORIGINAL_CLASS = "ch.qos.logback.classic.PatternLayout";
    private static final String NEW_CLASS = "org.artifactory.logging.layout.BackTracePatternLayout";
    private static final String LAYOUT = "layout";
    private static final String APPENDER = "appender";
    private static final String APPENDER_CLASS = "class";
    private final List<String> excludedAppenders = new ArrayList<String>() {{
        add("REQUEST");
        add("ACCESS");
        add("TRAFFIC");
    }};


    @Override
    @SuppressWarnings({"unchecked"})
    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Namespace namespace = root.getNamespace();
        List<Element> appenders = root.getChildren(APPENDER, namespace);
        if (appenders != null && !appenders.isEmpty()) {
            for (Element appender : appenders) {
                Attribute appenderNameAttribute = appender.getAttribute("name", namespace);
                String appenderName = appenderNameAttribute.getValue();
                if (!excludedAppenders.contains(appenderName)) {
                    Element layoutElement = appender.getChild(LAYOUT, namespace);
                    Attribute appenderClassAttribute = layoutElement.getAttribute(APPENDER_CLASS, namespace);
                    String appenderClass = appenderClassAttribute.getValue();
                    if (ORIGINAL_CLASS.equals(appenderClass)) {
                        appenderClassAttribute.setValue(NEW_CLASS);
                    }
                }
            }
        }

    }
}
