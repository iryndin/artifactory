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

import org.apache.commons.io.FileUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.util.ResourceUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.xml.XmlConfiguration;

import java.io.File;
import java.io.IOException;
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
        String devEtcDir = prefix + "/open/standalone/src/etc";

        // set home dir - dev mode only!
        System.setProperty(ArtifactoryHome.SYS_PROP, new File(prefix + "/open/standalone/src").getAbsolutePath());
        System.setProperty(ConstantValues.dev.getPropertyName(), "true");

        updateDefaultResources(devEtcDir);

        //Manually set the selector (needed explicitly here before any logger kicks in)
        // create the logger only after artifactory.home is set
        Server server = null;
        try {
            URL configUrl = new URL("file:" + devEtcDir + "/jetty.xml");
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

    private static void updateDefaultResources(String devEtcDir) {
        File defaultMimeTypes = ResourceUtils.getResourceAsFile("/META-INF/default/mimetypes.xml");
        File devMimeTypes = new File(devEtcDir, "mimetypes.xml");
        if (!devMimeTypes.exists() || defaultMimeTypes.lastModified() > devMimeTypes.lastModified()) {
            // override developer mimetypes file with newer default mimetypes file
            try {
                FileUtils.copyFile(defaultMimeTypes, devMimeTypes);
            } catch (IOException e) {
                System.err.println("Failed to copy default mime types file: " + e.getMessage());
            }
        }

    }
}
