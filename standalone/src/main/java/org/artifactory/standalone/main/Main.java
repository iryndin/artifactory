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

import org.apache.log4j.Logger;
import org.mortbay.jetty.Server;
import org.mortbay.xml.XmlConfiguration;

import java.net.URL;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class Main {
    private static final Logger log = Logger.getLogger(Main.class);

    /**
     * Main function, starts the jetty server.
     *
     * @param args
     */
    public static void main(String[] args) {
        Server server = null;
        try {
            String artifactoryHome = System.getProperty("artifactory.home");
            if (artifactoryHome != null) {
                artifactoryHome = artifactoryHome.replace('\\', '/');
            }
            URL configUrl = new URL("file:" + artifactoryHome + "/etc/jetty.xml");
            XmlConfiguration xmlConfiguration = new XmlConfiguration(configUrl);
            server = new Server();
            xmlConfiguration.configure(server);
            server.start();
        } catch (Exception e) {
            log.fatal("Could not start the Jetty server: " + e);
            if (server != null) {
                try {
                    server.stop();
                } catch (Exception e1) {
                    log.fatal("Unable to stop the jetty server: " + e1);
                }
            }
        }
    }
}
