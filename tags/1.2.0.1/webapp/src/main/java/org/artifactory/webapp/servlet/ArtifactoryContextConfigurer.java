/*
 * Copyright (c) 2004-2005, by OpenXource, LLC. All rights reserved.
 * 
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF OPENXOURCE
 *  
 * The copyright notice above does not evidence any          
 * actual or intended publication of such source code. 
 */
package org.artifactory.webapp.servlet;

import org.artifactory.webapp.spring.ArtifactoryWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ArtifactoryContextConfigurer implements ServletContextListener {

    private static final org.apache.log4j.Logger LOGGER =
            org.apache.log4j.Logger.getLogger(ArtifactoryContextConfigurer.class);

    public void contextInitialized(ServletContextEvent event) {
        LOGGER.info("Starting Artifactory...");
        ServletContext servletContext = event.getServletContext();
        XmlWebApplicationContext context = new ArtifactoryWebApplicationContext();
        context.setServletContext(servletContext);
        context.refresh();
        servletContext.setAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
    }

    public void contextDestroyed(ServletContextEvent event) {
        XmlWebApplicationContext context =
                (XmlWebApplicationContext) event.getServletContext().getAttribute(
                        WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        if (context != null) {
            context.destroy();
        }
        event.getServletContext()
                .removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    }
}
