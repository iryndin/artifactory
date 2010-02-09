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
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.repo.DeployableArtifact;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.webapp.wicket.component.FileUploadForm;
import org.artifactory.webapp.wicket.component.FileUploadParentPanel;
import org.artifactory.webapp.wicket.component.panel.titled.TitledPanel;

import java.io.File;

/**
 * @author Yoav Aharoni
 */
public class DeployArtifactPanel extends TitledPanel implements FileUploadParentPanel {
    private final static Logger LOGGER = Logger.getLogger(DeployArtifactPanel.class);

    @SpringBean
    private RepositoryService repositoryService;

    private DeployableArtifact deployableArtifact;

    private ArtifactForm artifactForm;

    private FileUploadForm uploadForm;

    public DeployArtifactPanel(String string) {
        super(string);

        //Add upload form with ajax progress bar
        uploadForm = new FileUploadForm("uploadForm", this);
        add(uploadForm);

        //Add the artifact details
        deployableArtifact = new DeployableArtifact();
        artifactForm = new ArtifactForm("artifactForm", this);
        artifactForm.enable(false);
        add(artifactForm);
    }

    public void onException() {
        artifactForm.setVisible(false);
    }

    public void removeUploadedFile() {
        if (uploadForm != null) {
            uploadForm.removeUploadedFile();
        }
    }

    public void onFileSaved() {
        deployableArtifact = deployableArtifactFromUploadedFile();
        artifactForm.update(deployableArtifact);
    }

    //Analyze the uploadedFile
    private DeployableArtifact deployableArtifactFromUploadedFile() {
        deployableArtifact.invalidate();
        //Try to guess the properties from pom/jar content
        try {
            deployableArtifact = repositoryService.getDeployableArtifact(getUploadedFile());
        } catch (Exception e) {
            String msg = "Unable to analyze uploaded file content. Cause: " + e.getMessage();
            LOGGER.debug(msg, e);
            error(msg);
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
