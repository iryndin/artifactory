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

package org.artifactory.logging.version.v6;

import com.google.common.collect.Lists;
import org.artifactory.version.converter.XmlConverter;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

import java.util.List;


/**
 * Adds the request tracer appender and logger to the logback config
 *
 * @author Noam Y. Tenne
 */
public class RequestTraceLoggerConverter implements XmlConverter {

    @Override
    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Namespace namespace = root.getNamespace();

        addAppender(root, namespace);
        addLogger(root, namespace);
    }

    private void addAppender(Element root, Namespace namespace) {
        Element requestTraceAppenderElement = new Element("appender", namespace);
        requestTraceAppenderElement.setAttribute("name", "REQUEST_TRACE", namespace);
        requestTraceAppenderElement.setAttribute("class", "ch.qos.logback.core.rolling.RollingFileAppender", namespace);

        Element fileElement = new Element("File", namespace);
        fileElement.setText("${artifactory.home}/logs/request_trace.log");
        requestTraceAppenderElement.addContent(fileElement);

        Element layoutElement = new Element("layout", namespace);
        layoutElement.setAttribute("class", "ch.qos.logback.classic.PatternLayout", namespace);
        Element layoutPatternElement = new Element("pattern", namespace);
        layoutPatternElement.setText("%date %message%n");
        layoutElement.addContent(layoutPatternElement);
        requestTraceAppenderElement.addContent(layoutElement);

        Element rollingPolicyElement = new Element("rollingPolicy", namespace);
        rollingPolicyElement.setAttribute("class", "ch.qos.logback.core.rolling.FixedWindowRollingPolicy", namespace);
        Element rollingPolicyFileNamePatternElement = new Element("FileNamePattern", namespace);
        rollingPolicyFileNamePatternElement.setText("${artifactory.home}/logs/request_trace.%i.log");
        rollingPolicyElement.addContent(rollingPolicyFileNamePatternElement);
        Element rollingPolicyMaxIndexElement = new Element("maxIndex", namespace);
        rollingPolicyMaxIndexElement.setText("13");
        rollingPolicyElement.addContent(rollingPolicyMaxIndexElement);
        requestTraceAppenderElement.addContent(rollingPolicyElement);

        Element triggeringPolicyElement = new Element("triggeringPolicy", namespace);
        triggeringPolicyElement.setAttribute("class", "ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy",
                namespace);
        Element triggeringPolicyMaxFileSizeElement = new Element("MaxFileSize", namespace);
        triggeringPolicyMaxFileSizeElement.setText("10MB");
        triggeringPolicyElement.addContent(triggeringPolicyMaxFileSizeElement);
        requestTraceAppenderElement.addContent(triggeringPolicyElement);

        int insertionIndex = 0;
        List<Element> allAppenders = root.getChildren("appender", namespace);
        if ((allAppenders != null) && !allAppenders.isEmpty()) {
            Element lastAppender = allAppenders.get(allAppenders.size() - 1);
            insertionIndex = root.getContent().indexOf(lastAppender) + 1;
        }
        root.getContent().addAll(insertionIndex,
                Lists.newArrayList(requestTraceAppenderElement
                ));
    }

    private void addLogger(Element root, Namespace namespace) {
        Element requestTraceLoggerElement = new Element("logger", namespace);
        requestTraceLoggerElement.setAttribute("name", "org.artifactory.request.RequestTraceLogger", namespace);
        requestTraceLoggerElement.setAttribute("additivity", "false", namespace);

        Element loggerLevelElement = new Element("level", namespace);
        loggerLevelElement.setAttribute("value", "INFO", namespace);
        requestTraceLoggerElement.addContent(loggerLevelElement);

        Element loggerAppenderRefElement = new Element("appender-ref", namespace);
        loggerAppenderRefElement.setAttribute("ref", "REQUEST_TRACE", namespace);
        requestTraceLoggerElement.addContent(loggerAppenderRefElement);

        int insertionIndex = 0;
        List<Element> allLoggers = root.getChildren("logger", namespace);
        if ((allLoggers != null) && !allLoggers.isEmpty()) {
            Element lastLogger = allLoggers.get(allLoggers.size() - 1);
            insertionIndex = root.getContent().indexOf(lastLogger) + 1;
        }
        root.getContent().addAll(insertionIndex,
                Lists.newArrayList(requestTraceLoggerElement));
    }
}
