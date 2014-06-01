/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.FileFilter;

public class StartDevMode {
    private static File openDir = null;

    public static void main(String[] args) {
        setupParameters();
        startJettyServer();
    }

    private static void setupParameters() {
        // Find the open standalone folder should be just below or just above
        File curDir = new File("").getAbsoluteFile();
        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() && pathname.getName().equals("open");
            }
        };
        File[] files = curDir.listFiles(fileFilter);
        if (files != null && files.length == 1) {
            openDir = files[0];
        }
        if (openDir == null) {
            // Try father
            files = curDir.getParentFile().listFiles(fileFilter);
            if (files != null && files.length == 1) {
                openDir = files[0];
            }
        }
        if (openDir == null) {
            throw new RuntimeException("Did not find open directory in " + curDir.getAbsolutePath() + " and " +
                    curDir.getParentFile().getAbsolutePath());
        }
        // set home dir - dev mode only!
        System.setProperty(ArtifactoryHome.SYS_PROP, new File(openDir, "standalone/src").getAbsolutePath());
        System.setProperty(ConstantValues.dev.getPropertyName(), "true");
    }

    private static void startJettyServer() {
        Server server = new Server();
        SocketConnector connector = new SocketConnector();
        connector.setMaxIdleTime(1000 * 60 * 60);
        connector.setSoLingerTime(-1);
        connector.setPort(8080);
        server.setConnectors(new Connector[]{connector});

        WebAppContext appContext = new WebAppContext();
        appContext.setServer(server);
        appContext.setContextPath("/artifactory");
        appContext.setWar(new File(openDir, "web/war/src/main/webapp/").getAbsolutePath());

        //server.addHandler(appContext);
        try {
            server.start();
            while (System.in.available() == 0) {
                Thread.sleep(5000);
            }
            server.stop();
            server.join();
        } catch (Exception e) {
            System.exit(100);
        }
    }
}