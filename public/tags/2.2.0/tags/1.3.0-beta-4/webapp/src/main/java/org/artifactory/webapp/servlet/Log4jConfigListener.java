/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.artifactory.common.ArtifactoryHome;
import org.springframework.web.util.Log4jWebConfigurer;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class Log4jConfigListener implements ServletContextListener {

    public void contextInitialized(ServletContextEvent event) {
        ArtifactoryHome.checkLog4j(event.getServletContext().getInitParameter(
                Log4jWebConfigurer.CONFIG_LOCATION_PARAM));
        // TODO: make it work even with init param null
        Log4jWebConfigurer.initLogging(event.getServletContext());
    }

    public void contextDestroyed(ServletContextEvent event) {
        Log4jWebConfigurer.shutdownLogging(event.getServletContext());
    }
}