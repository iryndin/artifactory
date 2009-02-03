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
        ServletContext servletContext = event.getServletContext();
        servletContext.log("Determining " + ArtifactoryHome.SYS_PROP + "...");
        servletContext.log("Looking for \"-D" + ArtifactoryHome.SYS_PROP +
                "=<path>\" vm parameter.");
        String home = System.getProperty(ArtifactoryHome.SYS_PROP);
        if (home == null) {
            servletContext.log("Could not find vm parameter. Defaulting to user.dir...");
            home = System.getProperty("user.dir", ".");
            System.setProperty(ArtifactoryHome.SYS_PROP, home);
        }
        ArtifactoryHome.create();
    }

    public void contextDestroyed(ServletContextEvent sce) {
    }
}
