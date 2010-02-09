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
package org.artifactory.webapp.wicket.page.deploy;

import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.webapp.wicket.common.component.FileUploadForm;
import org.artifactory.webapp.wicket.common.component.FileUploadParentPanel;
import org.artifactory.webapp.wicket.common.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author Yoav Aharoni
 */
public class DeployArtifactPanel extends TitledPanel implements FileUploadParentPanel {
    private static final Logger log = LoggerFactory.getLogger(DeployArtifactPanel.class);

    @SpringBean
    private RepositoryService repositoryService;

    private MavenArtifactInfo artifactInfo;

    private ArtifactForm artifactForm;

    private FileUploadForm uploadForm;

    public DeployArtifactPanel(String id) {
        super(id);

        //Add upload form with ajax progress bar
        uploadForm = new FileUploadForm("uploadForm", this);

        TitledBorder uploadBorder = new TitledBorder("uploadBorder") {
            @Override
            public boolean isVisible() {
                return super.isVisible() && uploadForm.isVisible();
            }
        };
        add(uploadBorder);
        uploadBorder.add(uploadForm);

        //Add the artifact details
        artifactInfo = new MavenArtifactInfo();
        artifactForm = new ArtifactForm("artifactForm", this);
        artifactForm.enable(false);

        TitledBorder artifactBorder = new TitledBorder("artifactBorder") {
            @Override
            public boolean isVisible() {
                return super.isVisible() && artifactForm.isVisible();
            }
        };
        add(artifactBorder);
        artifactBorder.add(artifactForm);
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
        artifactInfo = artifactInfoFromUploadedFile();
        artifactForm.update(artifactInfo);
    }

    //Analyze the uploadedFile
    private MavenArtifactInfo artifactInfoFromUploadedFile() {
        artifactInfo.invalidate();
        //Try to guess the properties from pom/jar content
        try {
            artifactInfo = repositoryService.getArtifactInfo(getUploadedFile());
        } catch (Exception e) {
            String msg = "Unable to analyze uploaded file content. Cause: " + e.getMessage();
            log.debug(msg, e);
            error(msg);
        }
        return artifactInfo;
    }

    MavenArtifactInfo getDeployableArtifact() {
        return artifactInfo;
    }

    File getUploadedFile() {
        return this.uploadForm.getUploadedFile();
    }

    public FileUploadForm getUploadForm() {
        return uploadForm;
    }
}
