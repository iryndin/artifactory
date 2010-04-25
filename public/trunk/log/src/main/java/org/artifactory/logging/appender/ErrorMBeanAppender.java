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

package org.artifactory.logging.appender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Customized appender that records errors to the ErrorMBean
 *
 * @author Noam Y. Tenne
 */
public class ErrorMBeanAppender extends AppenderBase<ILoggingEvent> {
    public static final String APPENDER_NAME = "ERROR";

    ObjectName errorBeanName = null;

    /**
     * Default constructor
     */
    public ErrorMBeanAppender() {
        try {
            errorBeanName = new ObjectName("org.jfrog.artifactory:instance=Artifactory, type=Error");
        } catch (MalformedObjectNameException e) {
            addError("Error locating ErrorMBean: " + e.getMessage());
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (errorBeanName == null) {
            return;
        }
        try {
            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
            if (!beanServer.isRegistered(errorBeanName)) {
                return;
            }
            if (eventObject.getLevel().toInt() == Level.ERROR_INT) {
                beanServer.invoke(errorBeanName, "addError", new Object[]{eventObject.getFormattedMessage()},
                        new String[]{"java.lang.String"});
            }
        } catch (Exception e) {
            addError("Error appending content to ErrorMBean: " + e.getMessage());
        }
    }
}