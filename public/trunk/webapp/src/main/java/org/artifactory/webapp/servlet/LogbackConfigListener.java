package org.artifactory.webapp.servlet;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import org.artifactory.common.ArtifactoryHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;

/**
 * Configured logback with the config file from etc directory.
 *
 * @author Yossi Shaul
 */
public class LogbackConfigListener implements ServletContextListener {

    public void contextInitialized(ServletContextEvent event) {
        ArtifactoryHome.assertInitialized();
        File logbackConfigFile = ArtifactoryHome.getLogbackConfig();

        //TODO: [by yl] Is this necessary (RTFACT-1283)
        // install the java.utils.logging to slf4j bridge
        //SLF4JBridgeHandler.install();

        String intervalString =
                event.getServletContext().getInitParameter("logbackRefreshInterval");
        if (intervalString != null) {
            try {
                long refreshInterval = Long.parseLong(intervalString);
                LogbackConfigWatchDog configWatchDog =
                        new LogbackConfigWatchDog(logbackConfigFile.getAbsolutePath());
                configWatchDog.setDelay(refreshInterval);
                configWatchDog.start();
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse logbackRefreshInterval. Log refresh will not be active.");
                loadLogbackConfiguration(logbackConfigFile);
            }
        } else {
            loadLogbackConfiguration(logbackConfigFile);
        }

    }

    private static void loadLogbackConfiguration(File logbackConfigFile) {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(lc);
            lc.shutdownAndReset();
            configurator.doConfigure(logbackConfigFile);
        } catch (JoranException je) {
            StatusPrinter.print(lc);
        }
    }

    public void contextDestroyed(ServletContextEvent sce) {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.shutdownAndReset();
    }

    private static class LogbackConfigWatchDog extends FileWatchDog {
        private final static Logger log = LoggerFactory.getLogger(
                LogbackConfigListener.LogbackConfigWatchDog.class);

        public LogbackConfigWatchDog(String filename) {
            super(filename);
            setName("logback-watchdog");
        }

        @Override
        protected void doOnChange() {
            log.info("Reloading logback configuration.");
            loadLogbackConfiguration(new File(filename));
        }
    }
}
