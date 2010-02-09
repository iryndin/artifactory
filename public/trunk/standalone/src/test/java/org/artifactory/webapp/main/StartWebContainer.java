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

package org.artifactory.webapp.main;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.mortbay.jetty.Server;
import org.mortbay.xml.XmlConfiguration;

import java.io.File;
import java.net.URL;

/**
 * @author yoavl
 */
public class StartWebContainer {

    public static final String DEFAULT_PREFIX = "..";

    /**
     * Main function, starts the jetty server.
     */
    public static void main(String... args) {
        System.setProperty("java.net.preferIPv4Stack", "true");

        String prefix = args.length == 0 ? DEFAULT_PREFIX : args[0];

        // set home dir - dev mode only!
        System.setProperty(ArtifactoryHome.SYS_PROP, new File(prefix + "/open/standalone/src").getAbsolutePath());
        System.setProperty(ConstantValues.dev.getPropertyName(), "true");

        //Manually set the selector (needed explicitly here before any logger kicks in)
        System.setProperty("logback.ContextSelector", "org.artifactory.log.logback.LogbackContextSelector");
        // create the logger only after artifactory.home is set
        Server server = null;
        try {
            URL configUrl = new URL("file:" + prefix + "/open/standalone/src/etc/jetty.xml");
            XmlConfiguration xmlConfiguration = new XmlConfiguration(configUrl);
            server = new Server();
            xmlConfiguration.configure(server);
            server.start();
        } catch (Exception e) {
            System.err.println("Could not start the Jetty server: " + e);
            if (server != null) {
                try {
                    server.stop();
                } catch (Exception e1) {
                    System.err.println("Unable to stop the jetty server: " + e1);
                }
            }
        }
    }
}
