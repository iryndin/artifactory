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

import org.artifactory.ArtifactoryHome;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactoryHomeConfigurer implements ServletContextListener {

    public void contextInitialized(ServletContextEvent event) {
        if (ArtifactoryHome.getEtcDir() == null) {
            // Artifactory home not initialized
            ArtifactoryHome.findArtifactoryHome(new ServletLogger(event.getServletContext()));
            ArtifactoryHome.create();
        }
    }

    private static class ServletLogger implements ArtifactoryHome.SimpleLog {
        private final ServletContext servletContext;

        private ServletLogger(ServletContext servletContext) {
            this.servletContext = servletContext;
        }

        public void log(String message) {
            servletContext.log(message);
        }
    }

    public void contextDestroyed(ServletContextEvent sce) {
    }
}
