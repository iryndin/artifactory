/*
 * Copyright (c) 2004-2005, by OpenXource, LLC. All rights reserved.
 * 
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF OPENXOURCE
 *  
 * The copyright notice above does not evidence any          
 * actual or intended publication of such source code. 
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.webapp.servlet;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantsValue;
import org.artifactory.config.SpringConfResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.JdkVersion;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.lang.reflect.Constructor;

public class ArtifactoryContextConfigurer implements ServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryContextConfigurer.class);

    public void contextInitialized(ServletContextEvent event) {
        long start = System.currentTimeMillis();
        ArtifactoryHome.assertInitialized();

        String versionNumber = ConstantsValue.artifactoryVersion.getString();
        String revision = ConstantsValue.artifactoryRevision.getString();
        log.info(
                "\n" +
                        "               _   _  __           _\n" +
                        "    /\\        | | (_)/ _|         | |\n" +
                        "   /  \\   _ __| |_ _| |_ __ _  ___| |_ ___  _ __ _   _\n" +
                        "  / /\\ \\ | '__| __| |  _/ _` |/ __| __/ _ \\| '__| | | |\n" +
                        " / ____ \\| |  | |_| | || (_| | (__| || (_) | |  | |_| |\n" +
                        "/_/    \\_\\_|   \\__|_|_| \\__,_|\\___|\\__\\___/|_|   \\__, |\n" +
                        String.format(" Version: %-19s Revision: %-9s __/ |\n", versionNumber,
                                revision) +
                        "                                                 |___/\n");

        if (!isJvmSupported()) {
            String message = "\n\n***************************************************************************\n" +
                    "*** You have started Artifactory with an unsupported version of Java 6! ***\n" +
                    "***                Please use Java 6 update 4 and above.                ***\n" +
                    "***************************************************************************\n";
            log.warn(message);
        }

        ServletContext servletContext = event.getServletContext();

        ApplicationContext context;
        try {
            Constructor<?> constructor =
                    ClassUtils.forName("org.artifactory.spring.ArtifactoryApplicationContext")
                            .getConstructor(String[].class);
            context = (ApplicationContext) constructor
                    .newInstance((Object) SpringConfResourceLoader.getConfigurationPaths());

        } catch (Exception e) {
            log.error("Error creating spring context", e);
            throw new RuntimeException(e);
        }
        //Register the context for easy retreival for faster destroy
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
        log.info("\n" +
                "###########################################################\n" +
                "### Artifactory successfully started (" +
                String.format("%-17s", (DurationFormatUtils.formatPeriod(start, System.currentTimeMillis(), "s")) +
                        " seconds)") + " ###\n" +
                "###########################################################\n");
    }

    public void contextDestroyed(ServletContextEvent event) {
        AbstractApplicationContext context =
                (AbstractApplicationContext) event.getServletContext().getAttribute(
                        WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        if (context != null) {
            context.destroy();
        }
        event.getServletContext()
                .removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    }

    /**
     * Checks if the user if running JVM 6. If true, make sure it is comptaible (update 4+)
     *
     * @return
     */
    //TODO [by noam]: find a better way to check the minor versions when on different vendors of the JVM
    @SuppressWarnings({"EmptyCatchBlock"})
    private boolean isJvmSupported() {
        //Make sure to warn user if he is using Java 6 with an update earlier than 4
        boolean supported = true;
        if (JdkVersion.getMajorJavaVersion() == JdkVersion.JAVA_16) {
            String javaVersion = JdkVersion.getJavaVersion();
            int underscoreIndex = javaVersion.indexOf("_");
            if (underscoreIndex == -1) {
                supported = false;
            } else {
                int minorVersion = -1;
                try {
                    minorVersion = Integer.parseInt(javaVersion.substring(underscoreIndex + 1));
                } catch (Exception e) {
                }
                if (minorVersion < 4) {
                    supported = false;
                }
            }
        }
        return supported;
    }
}
