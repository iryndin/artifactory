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
package org.artifactory.test.mock;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Server;
import org.mortbay.xml.XmlConfiguration;

import java.io.File;
import java.net.URL;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class StartDummyRepo {
    private static final Log log = LogFactory.getLog(StartDummyRepo.class);
    private static final String LOCAL_HOST = "localhost";

    /**
     * Main function, starts the jetty server.
     */
    public static void main(String[] args) {
        Server server = null;

        try {
            log.info("Starting the Mock Server");
            URL configUrl = StartDummyRepo.class.getResource("/mock/jetty.xml");
            XmlConfiguration xmlConfiguration = new XmlConfiguration(configUrl);
            server = new Server();
            xmlConfiguration.configure(server);
            server.start();
            Thread.sleep(1000L);
            MockServer mockServer = new MockServer(LOCAL_HOST);
            int index = 0;
            while (!mockServer.isServerActive()) {
                Thread.sleep(1000L);
                index++;
                if (index == 30) {
                    throw new Exception("Server was unable to start after 30 sec");
                }
            }
            log.info("Mock Server started");
            if (args != null && args.length > 0) {
                for (String arg : args) {
                    File xml = new File(arg);
                    if (xml.exists()) {
                        String fileName = xml.getName();
                        log.info("Adding test from " + fileName);
                        mockServer.addTest(fileName.substring(0, (fileName.length() - 4)),
                                FileUtils.readFileToString(xml, "utf-8"));
                    }
                }
            }

        } catch (Exception e) {
            log.fatal("Could not start the Jetty server: " + e, e);
            if (server != null) {
                try {
                    server.stop();
                } catch (Exception e1) {
                    log.fatal("Unable to stop the jetty server: " + e1, e1);
                }
            }
        }
    }
}