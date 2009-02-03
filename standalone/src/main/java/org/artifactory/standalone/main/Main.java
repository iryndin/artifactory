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
package org.artifactory.standalone.main;

import org.artifactory.ArtifactoryHome;
import org.mortbay.jetty.Server;
import org.mortbay.xml.XmlConfiguration;

import java.io.File;
import java.net.URL;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class Main {
    /**
     * Main function, starts the jetty server. The first parameter can be the jetty.xml
     * configuration file. If not provided ${artifactory.home}/etc/jetty.xml will be used.
     *
     * @param args
     */
    public static void main(String[] args) {
        Server server = null;
        try {
            URL configUrl;
            if (args != null && args.length > 0) {
                // Jetty configuration file is the only param allowed
                if (args.length != 1) {
                    System.err.println("Usage java " + Main.class.getName() +
                            " [URL path to jetty xml conf]");
                    System.exit(1);
                }
                File jettyConfFile = new File(args[0]);
                if (jettyConfFile.exists() && jettyConfFile.isFile()) {
                    System.out.println("Starting jetty from configuration file " + jettyConfFile);
                    configUrl = jettyConfFile.toURI().toURL();
                } else {
                    System.out.println("Starting jetty from URL configuration " + args[0]);
                    configUrl = new URL(args[0]);
                }
            } else {
                //Trying to find Artifactory home and then /etc/jetty.xml under it
                ArtifactoryHome.create();
                File jettyConfFile = new File(ArtifactoryHome.getEtcDir(), "jetty.xml");
                if (jettyConfFile.exists() && jettyConfFile.isFile()) {
                    System.out.println("Starting jetty from configuration file " + jettyConfFile);
                    configUrl = jettyConfFile.toURI().toURL();
                } else {
                    System.err.println("No jetty configuration file found at " + jettyConfFile);
                    System.exit(1);
                    return;
                }
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
}
