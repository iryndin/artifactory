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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.artifactory.convert.XmlConverterTest;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * A logback config converter to test the addition of the request trace appender and logger
 *
 * @author Noam Y. Tenne
 */
@Test
public class RequestTraceLoggerConverterTest extends XmlConverterTest {

    private Element root;
    private Namespace namespace;

    @BeforeClass
    public void setUp() throws Exception {
        Document doc = convertXml("/org/artifactory/logging/version/v4/logback.xml",
                new RequestTraceLoggerConverter());

        root = doc.getRootElement();
        namespace = root.getNamespace();
    }

    public void testAppenderWasAdded() throws Exception {
        List<Element> allAppenders = root.getChildren("appender", namespace);
        Iterable<Element> requestTraceAppenders = Iterables.filter(allAppenders, new Predicate<Element>() {
            @Override
            public boolean apply(@Nullable Element input) {
                return (input != null) && ("REQUEST_TRACE".equals(input.getAttributeValue("name", namespace)));
            }
        });

        assertEquals(Iterables.size(requestTraceAppenders), 1, "Only 1 request trace appender expected");
        Element appender = requestTraceAppenders.iterator().next();
        assertEquals(appender.getAttributeValue("class", namespace),
                "ch.qos.logback.core.rolling.RollingFileAppender", "Unexpected appender class");

        assertEquals(appender.getChildText("File", namespace), "${artifactory.home}/logs/request_trace.log",
                "Unexpected appender file path");

        Element layout = appender.getChild("layout", namespace);
        assertEquals(layout.getAttributeValue("class", namespace),
                "ch.qos.logback.classic.PatternLayout", "Unexpected appender layout class");

        assertEquals(layout.getChildText("pattern", namespace), "%date %message%n",
                "Unexpected appender layout pattern");

        Element rollingPolicy = appender.getChild("rollingPolicy", namespace);
        assertEquals(rollingPolicy.getAttributeValue("class", namespace),
                "ch.qos.logback.core.rolling.FixedWindowRollingPolicy", "Unexpected appender rolling policy class");

        assertEquals(rollingPolicy.getChildText("FileNamePattern", namespace),
                "${artifactory.home}/logs/request_trace.%i.log",
                "Unexpected appender rolling policy file name pattern");

        assertEquals(rollingPolicy.getChildText("maxIndex", namespace), "13",
                "Unexpected appender rolling policy max index");

        Element triggeringPolicy = appender.getChild("triggeringPolicy", namespace);
        assertEquals(triggeringPolicy.getAttributeValue("class", namespace),
                "ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy", "Unexpected appender triggering policy class");

        assertEquals(triggeringPolicy.getChildText("MaxFileSize", namespace), "10MB",
                "Unexpected appender triggering policy max file size");
    }

    public void testLoggerWasAdded() throws Exception {
        List<Element> allLoggers = root.getChildren("logger", namespace);
        Iterable<Element> requestTraceLoggers = Iterables.filter(allLoggers, new Predicate<Element>() {
            @Override
            public boolean apply(@Nullable Element input) {
                return (input != null) && ("org.artifactory.request.RequestTraceLogger".
                        equals(input.getAttributeValue("name", namespace)));
            }
        });
        assertEquals(Iterables.size(requestTraceLoggers), 1, "Only 1 request trace logger expected");
        Element appender = requestTraceLoggers.iterator().next();
        assertEquals(appender.getAttributeValue("additivity", namespace), "false", "Unexpected logger additivity");

        Element level = appender.getChild("level", namespace);
        assertEquals(level.getAttributeValue("value", namespace), "INFO", "Unexpected logger level value");

        Element appenderRef = appender.getChild("appender-ref", namespace);
        assertEquals(appenderRef.getAttributeValue("ref", namespace), "REQUEST_TRACE",
                "Unexpected logger appender ref");
    }
}
