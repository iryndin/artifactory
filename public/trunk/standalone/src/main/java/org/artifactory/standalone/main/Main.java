/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.standalone.main;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.xml.XmlConfiguration;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Manually initialize and starts Jetty Web Server. NOTE: No logger here, since it needs to be initialized after.
 *
 * @author Yoav Landman
 */
public class Main {
    static {
        // Initialize logback as soon as possible without using ArtifactoryHome :(
        String artHome = findArtifactoryHome();
        File logbackFile = new File(artHome, "etc/logback.xml");
        if (logbackFile.exists()) {
            System.setProperty("logback.configurationFile", logbackFile.getAbsolutePath());
        }
        //Initialize ipv4 pref, unless previously configured otherwise
        String ipv4 = System.getProperty("java.net.preferIPv4Stack");
        if (ipv4 == null) {
            System.setProperty("java.net.preferIPv4Stack", "true");
        }
    }

    /**
     * Main function, starts the jetty server. The first parameter can be the jetty.xml configuration file. If not
     * provided ${artifactory.home}/etc/jetty.xml will be used.
     *
     * @param args Alternative location of jetty configuration file
     */
    public static void main(String[] args) {
        Server server = null;
        try {
            URL configUrl;
            if (args != null && args.length > 0) {
                // Jetty configuration file is the only param allowed
                if (args.length != 1) {
                    System.err.println("Usage java " + Main.class.getName() + " [URL path to jetty xml conf]");
                    System.exit(1);
                }
                String jettyXmlUrl = args[0];
                configUrl = getConfigUrl(jettyXmlUrl);
            } else {
                // use etc/jetty.xml under artifactory home directory
                String artHome = findArtifactoryHome();
                File jettyConfFile = new File(artHome, "etc/jetty.xml");
                configUrl = getConfigUrl(jettyConfFile, null);
            }
            XmlConfiguration xmlConfiguration = new XmlConfiguration(configUrl);
            server = new Server();
            xmlConfiguration.configure(server);
            server.start();
        } catch (Exception e) {
            System.err.println("Could not start the Jetty server: " + e);
            e.printStackTrace();
            if (server != null) {
                try {
                    server.stop();
                } catch (Exception e1) {
                    System.err.println("Unable to stop the jetty server: " + e1);
                }
            }
        }
    }

    private static URL getConfigUrl(String jettyXmlUrl) throws MalformedURLException {
        File jettyConfFile = new File(jettyXmlUrl);
        return getConfigUrl(jettyConfFile, jettyXmlUrl);
    }

    private static URL getConfigUrl(File jettyConfFile, String jettyXmlUrl) throws MalformedURLException {
        URL configUrl;
        if (jettyConfFile.exists() && jettyConfFile.isFile()) {
            System.out.println("Starting jetty from configuration file " + jettyConfFile.getAbsolutePath());
            configUrl = new URL("file:" + jettyConfFile.toURI().getPath());
        } else if (jettyXmlUrl != null) {
            System.out.println("Starting jetty from URL configuration " + jettyXmlUrl);
            configUrl = new URL(jettyXmlUrl);
        } else {
            System.err.println("No jetty configuration file found at " + jettyConfFile.getAbsolutePath());
            System.exit(1);
            return null;
        }
        return configUrl;
    }

    private static String findArtifactoryHome() {
        String home = System.getProperty("artifactory.home");
        if (home == null) {
            home = System.getenv("ARTIFACTORY_HOME");
            if (home == null) {
                home = new File(System.getProperty("user.home", "."), ".artifactory").getAbsolutePath();
            }
        }
        home = home.replace('\\', '/');
        return home;
    }

}