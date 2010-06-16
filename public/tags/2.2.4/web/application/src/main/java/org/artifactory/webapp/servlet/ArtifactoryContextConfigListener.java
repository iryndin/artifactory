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

package org.artifactory.webapp.servlet;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.log.LoggerFactory;
import org.artifactory.log.logback.LogbackContextSelector;
import org.artifactory.log.logback.LoggerConfigInfo;
import org.artifactory.spring.SpringConfigPaths;
import org.artifactory.spring.SpringConfigResourceLoader;
import org.artifactory.util.HttpUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.JdkVersion;
import org.springframework.util.ClassUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ArtifactoryContextConfigListener implements ServletContextListener {

    public void contextInitialized(ServletContextEvent event) {
        final ServletContext servletContext = event.getServletContext();

        final Thread initThread = new Thread("art-init") {
            boolean success = true;

            @SuppressWarnings({"unchecked"})
            @Override
            public void run() {
                try {
                    //Use custom logger
                    String contextId = HttpUtils.getContextId(servletContext);
                    //Build a partial config, since we expect the logger-context to exit in the selector cache by only contextId
                    LoggerConfigInfo configInfo = new LoggerConfigInfo(contextId);
                    LogbackContextSelector.bindConfig(configInfo);
                    //No log field since needs to lazy initialize only after logback customization listener has run
                    Logger log = getLogger();
                    configure(servletContext, log);

                    LogbackContextSelector.unbindConfig();
                } catch (Exception e) {
                    getLogger().error("Application could not be initialized.", e);
                    success = false;
                } finally {
                    if (success) {
                        //Run the waiting filters
                        BlockingQueue<DelayedInit> waitingFiltersQueue = (BlockingQueue<DelayedInit>) servletContext
                                .getAttribute(DelayedInit.APPLICATION_CONTEXT_LOCK_KEY);
                        List<DelayedInit> waitingInits = new ArrayList<DelayedInit>();
                        waitingFiltersQueue.drainTo(waitingInits);
                        for (DelayedInit filter : waitingInits) {
                            try {
                                filter.delayedInit();
                            } catch (ServletException e) {
                                getLogger().error("Could not init {}.", filter.getClass().getName(), e);
                                success = false;
                                break;
                            }
                        }
                    }
                    //Remove the lock and open the app to requests
                    servletContext.removeAttribute(DelayedInit.APPLICATION_CONTEXT_LOCK_KEY);
                }
            }
        };
        initThread.setDaemon(true);
        servletContext.setAttribute(DelayedInit.APPLICATION_CONTEXT_LOCK_KEY, new LinkedBlockingQueue<DelayedInit>());
        initThread.start();
        if (Boolean.getBoolean("artifactory.init.useServletContext")) {
            try {
                getLogger().info("Waiting for servlet context initialization ...");
                initThread.join();
            } catch (InterruptedException e) {
                getLogger().error("Artifactory initialization thread got interrupted", e);
            }
        }
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(ArtifactoryContextConfigListener.class);
    }

    private void configure(ServletContext servletContext, Logger log) {
        long start = System.currentTimeMillis();

        ArtifactoryHome artifactoryHome =
                (ArtifactoryHome) servletContext.getAttribute(ArtifactoryHome.SERVLET_CTX_ATTR);
        if (artifactoryHome == null) {
            throw new IllegalStateException("Artifactory home not initialized.");
        }

        CompoundVersionDetails runningVersionDetails = artifactoryHome.getRunningVersionDetails();
        String versionNumber = runningVersionDetails.getVersionName();
        String revision = runningVersionDetails.getRevision();
        versionNumber = fixVersion(versionNumber);
        revision = fixVersion(revision);

        log.info(
                "\n" +
                        "               _   _  __           _\n" +
                        "    /\\        | | (_)/ _|         | |\n" +
                        "   /  \\   _ __| |_ _| |_ __ _  ___| |_ ___  _ __ _   _\n" +
                        "  / /\\ \\ | '__| __| |  _/ _` |/ __| __/ _ \\| '__| | | |\n" +
                        " / ____ \\| |  | |_| | || (_| | (__| || (_) | |  | |_| |\n" +
                        "/_/    \\_\\_|   \\__|_|_| \\__,_|\\___|\\__\\___/|_|   \\__, |\n" +
                        String.format(" Version: %-19s Revision: %-9s __/ |\n", versionNumber, revision) +
                        "                                                 |___/\n" +
                        " Artifactory Home: '" + artifactoryHome.getHomeDir().getAbsolutePath() + "'\n"
        );

        if (!isSupportedJava6()) {
            String message = "\n\n***************************************************************************\n" +
                    "*** You have started Artifactory with an unsupported version of Java 6! ***\n" +
                    "***                Please use Java 6 update 4 and above.                ***\n" +
                    "***************************************************************************\n";
            log.warn(message);
        }

        ApplicationContext context;
        try {
            ArtifactoryHome.bind(artifactoryHome);

            Class<?> contextClass = ClassUtils.forName(
                    "org.artifactory.spring.ArtifactoryApplicationContext", ClassUtils.getDefaultClassLoader());
            Constructor<?> constructor = contextClass.
                    getConstructor(String.class, SpringConfigPaths.class, ArtifactoryHome.class);
            //Construct the context name based on the context path
            //(will not work with multiple servlet containers on the same vm!)
            String contextUniqueName = HttpUtils.getContextId(servletContext);
            SpringConfigPaths springConfigPaths = SpringConfigResourceLoader.getConfigurationPaths(artifactoryHome);
            context = (ApplicationContext) constructor.newInstance(
                    contextUniqueName, springConfigPaths, artifactoryHome);
            ArtifactoryHome.unbind();

        } catch (Exception e) {
            log.error("Error creating spring context", e);
            throw new RuntimeException(e);
        }
        log.info("\n" +
                "###########################################################\n" +
                "### Artifactory successfully started (" +
                String.format("%-17s", (DurationFormatUtils.formatPeriod(start, System.currentTimeMillis(), "s")) +
                        " seconds)") + " ###\n" +
                "###########################################################\n");
        //Register the context for easy retrieval for faster destroy
        servletContext.setAttribute(ArtifactoryContext.APPLICATION_CONTEXT_KEY, context);
    }

    private String fixVersion(String version) {
        if (version.startsWith("${")) {
            return "Unknown";
        }
        return version;
    }

    public void contextDestroyed(ServletContextEvent event) {
        AbstractApplicationContext context = (AbstractApplicationContext) event.getServletContext().getAttribute(
                ArtifactoryContext.APPLICATION_CONTEXT_KEY);
        try {
            if (context != null) {
                context.destroy();
            }
        } finally {
            event.getServletContext().removeAttribute(ArtifactoryContext.APPLICATION_CONTEXT_KEY);
        }
    }

    /**
     * @return True if the current jvm version is supported (the unsupported versions are java 6 upto java 6 update 4)
     */
    //TODO [by noam]: find a better way to check the minor versions when on different vendors of the JVM
    private boolean isSupportedJava6() {
        //Make sure to warn user if he is using Java 6 with an update earlier than 4
        boolean supported = true;
        if (JdkVersion.getMajorJavaVersion() == JdkVersion.JAVA_16) {
            String javaVersion = JdkVersion.getJavaVersion();
            int underscoreIndex = javaVersion.indexOf('_');
            if (underscoreIndex == -1) {
                supported = false;
            } else {
                try {
                    int minorVersion = Integer.parseInt(javaVersion.substring(underscoreIndex + 1));
                    if (minorVersion < 4) {
                        supported = false;
                    }
                } catch (Exception e) {
                    supported = false;
                }
            }
        }
        return supported;
    }
}
