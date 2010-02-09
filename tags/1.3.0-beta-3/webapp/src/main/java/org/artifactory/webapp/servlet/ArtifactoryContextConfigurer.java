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

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.lang.reflect.Constructor;

public class ArtifactoryContextConfigurer implements ServletContextListener {

    private static final org.apache.log4j.Logger LOGGER =
            org.apache.log4j.Logger.getLogger(ArtifactoryContextConfigurer.class);

    public void contextInitialized(ServletContextEvent event) {
        LOGGER.info("Starting Artifactory...");
        ServletContext servletContext = event.getServletContext();

        ApplicationContext context;
        try {
            Constructor<?> constructor =
                    ClassUtils.forName("org.artifactory.spring.ArtifactoryApplicationContext")
                            .getConstructor(String.class);
            context = (ApplicationContext) constructor
                    .newInstance("/META-INF/spring/applicationContext.xml");
        } catch (Exception e) {
            LOGGER.error("Error creating spring context", e);
            throw new RuntimeException(e);
        }
        //Register the context for easy retreival for faster destroy
        servletContext.setAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
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
}
