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

package org.artifactory.webapp.servlet.logback;

import ch.qos.logback.classic.LoggerContext;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.log.BootstrapLogger;
import org.artifactory.log.LoggerFactory;
import org.artifactory.log.logback.LogbackContextHelper;
import org.artifactory.log.logback.LogbackContextSelector;
import org.artifactory.log.logback.LoggerConfigInfo;
import org.artifactory.util.FileWatchDog;
import org.artifactory.util.HttpUtils;
import org.slf4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Configured logback with the config file from etc directory.
 *
 * @author Yossi Shaul
 * @author Yoav Landman
 */
public class LogbackConfigListener implements ServletContextListener {

    private ArtifactoryHome home;
    LogbackConfigWatchDog configWatchDog;

    public void contextInitialized(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        home = (ArtifactoryHome) servletContext.getAttribute(ArtifactoryHome.SERVLET_CTX_ATTR);
        if (home == null) {
            throw new IllegalStateException("Artifactory home not initialized");
        }

        //Install the juli to slf4j bridge (disabled dur to RTFACT-1283)
        //SLF4JBridgeHandler.install();

        boolean selectorUsed = System.getProperty("logback.ContextSelector") != null;
        LoggerContext context;
        if (selectorUsed) {
            String contextId = HttpUtils.getContextId(servletContext);
            LoggerConfigInfo configInfo = new LoggerConfigInfo(contextId, home);
            LogbackContextSelector.bindConfig(configInfo);
            try {
                //This load should already use a context from the selector
                context = getOrInitLoggerContext();
            } finally {
                LogbackContextSelector.unbindConfig();
            }
        } else {
            context = getOrInitLoggerContext();
            LogbackContextHelper.configure(context, home);
        }

        //Configure and start the watchdog
        configWatchDog = new LogbackConfigWatchDog(context);
        configureWatchdog(servletContext);
        configWatchDog.start();
    }

    public void contextDestroyed(ServletContextEvent sce) {
        configWatchDog.interrupt();
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.stop();
    }

    private static LoggerContext getOrInitLoggerContext() {
        return (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    private void configureWatchdog(ServletContext servletContext) {
        String intervalString = servletContext.getInitParameter("logbackRefreshInterval");
        if (intervalString != null) {
            try {
                long refreshInterval = Long.parseLong(intervalString);
                configWatchDog.setDelay(refreshInterval);
            } catch (NumberFormatException e) {
                BootstrapLogger.error("Failed to parse logbackRefreshInterval. Log refresh will not be active.");
                getOrInitLoggerContext();
            }
        }
    }

    private class LogbackConfigWatchDog extends FileWatchDog {

        private final Logger log = LoggerFactory.getLogger(LogbackConfigListener.LogbackConfigWatchDog.class);

        private LoggerContext loggerContext;

        public LogbackConfigWatchDog(LoggerContext loggerContext) {
            super(home.getLogbackConfig(), false);
            setName("logback-watchdog");
            this.loggerContext = loggerContext;
            checkAndConfigure();
        }

        @Override
        protected void doOnChange() {
            LogbackContextHelper.configure(loggerContext, home);
            //Log after reconfig, since this class logger is constucted before config with the default warn level
            log.info("Reloaded logback config from: {}.", file.getAbsolutePath());
        }
    }
}
