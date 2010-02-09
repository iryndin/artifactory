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
package org.jfrog.maven.viewer.ui.command;

import org.jfrog.maven.viewer.common.Config;
import org.jfrog.maven.viewer.domain.Artifact;
import org.jfrog.maven.viewer.domain.exception.ArtifactCreationException;
import org.springframework.richclient.application.ApplicationException;
import org.springframework.richclient.filechooser.DefaultFileFilter;

import javax.swing.*;
import java.io.File;

/**
 * User: Dror Bereznitsky
 * Date: 15/03/2007
 * Time: 19:08:31
 */
public class CreateGraphFromFileCommand extends CreateGraphCommand {

    public CreateGraphFromFileCommand() {
        super("createGraphFromFile");
    }

    protected void doExecuteCommand() {
        DefaultFileFilter fileFilter = new DefaultFileFilter(new String[]{".pom", ".xml"}, "POM files");
        File lastOpenFolder = new File(Config.getLastOpenFolder());
        if (!lastOpenFolder.exists() || !lastOpenFolder.isDirectory()) {
            lastOpenFolder = new File(System.getProperty("use.dir"));
        }
        JFileChooser fc = new JFileChooser(lastOpenFolder);

        fc.setFileFilter(fileFilter);

        int returnVal = fc.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            File pomFile = fc.getSelectedFile();

            Config.setLastOpenFolder(pomFile.getParentFile().getAbsolutePath());

            try {
                Artifact artifact = getArtifactFactory().createArtifact(pomFile);
                createGraph(artifact);
            } catch (ArtifactCreationException e) {
                throw new ApplicationException("Could not process POM file: " + e.getMessage(), e);
            }
        }
    }
}
