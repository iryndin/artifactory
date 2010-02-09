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
package org.artifactory.webapp.wicket.deploy;

import org.apache.log4j.Logger;
import org.artifactory.deploy.DeployableArtifact;
import org.artifactory.maven.MavenUtils;
import org.artifactory.webapp.wicket.components.FileUploadForm;
import org.artifactory.webapp.wicket.components.FileUploadParentPanel;
import org.artifactory.webapp.wicket.panel.WindowPanel;
import org.codehaus.plexus.util.StringUtils;
import wicket.markup.html.panel.FeedbackPanel;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;

/**
 * @author Yoav Aharoni
 */
public class DeployArtifactPanel extends WindowPanel implements FileUploadParentPanel {

    @SuppressWarnings({"UnusedDeclaration", "UNUSED_SYMBOL"})
    private final static Logger LOGGER = Logger.getLogger(DeployArtifactPanel.class);

    private DeployableArtifact deployableArtifact;

    private ArtifactForm artifactForm;

    private FileUploadForm uploadForm;

    public DeployArtifactPanel(String string) {
        super(string);

        //Create feedback panel
        final FeedbackPanel uploadFeedback = new FeedbackPanel("uploadFeedback");
        add(uploadFeedback);

        //Add upload form with ajax progress bar
        uploadForm = new FileUploadForm("uploadForm", this);
        add(uploadForm);

        //Add the artifact details
        deployableArtifact = new DeployableArtifact();
        artifactForm = new ArtifactForm("artifactForm", this);
        artifactForm.setOutputMarkupId(false);
        add(artifactForm);
        artifactForm.enable(false);
    }

    public void onException() {
        artifactForm.setVisible(false);
    }

    public void removeUploadedFile() {
        if (uploadForm != null) {
            uploadForm.removeUploadedFile();
        }
    }

    public void callbackFileSaved() {
        deployableArtifact = deployableArtifactFromUploadedFile();
        artifactForm.update(deployableArtifact);
    }

    //Analyze the uploadedFile
    private DeployableArtifact deployableArtifactFromUploadedFile() {
        deployableArtifact.invalidate();
        //Try to guess the properties from pom/jar content
        try {
            deployableArtifact.update(getUploadedFile());
        } catch (IOException e) {
            error("Unable to analyze uploaded file content. Cause: " + e.getMessage());
        }
        String fileName = getUploadedFile().getName();
        //Try to guess the artifactId and version properties from the uploadedFile name
        if (!deployableArtifact.hasArtifact() || !deployableArtifact.hasVersion() ||
                !deployableArtifact.hasClassifer()) {
            Matcher matcher = MavenUtils.artifactMatcher(fileName);
            if (matcher.matches()) {
                if (!deployableArtifact.hasClassifer()) {
                    deployableArtifact.setClassifier(matcher.group(5));
                }
                if (!deployableArtifact.hasArtifact()) {
                    deployableArtifact.setArtifact(matcher.group(1));
                }
                if (!deployableArtifact.hasVersion()) {
                    deployableArtifact.setVersion(matcher.group(2));
                }
            }
        }
        //Complete values by falling back to dumb defaults
        if (StringUtils.isEmpty(deployableArtifact.getArtifact())) {
            deployableArtifact.setArtifact(fileName);
        }
        if (StringUtils.isEmpty(deployableArtifact.getGroup())) {
            //If we have no group, set it to be the same as the artifact name
            deployableArtifact.setGroup(deployableArtifact.getArtifact());
        }
        if (StringUtils.isEmpty(deployableArtifact.getVersion())) {
            deployableArtifact.setVersion(fileName);
        }
        return deployableArtifact;
    }

    DeployableArtifact getDeployableArtifact() {
        return deployableArtifact;
    }

    File getUploadedFile() {
        return this.uploadForm.getUploadedFile();
    }

    public FileUploadForm getUploadForm() {
        return uploadForm;
    }
}
