/*
 * This file is part of Artifactory.
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

package org.artifactory.webapp.wicket.page.deploy;

import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.panel.upload.FileUploadForm;
import org.artifactory.common.wicket.panel.upload.FileUploadParentPanel;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.panel.upload.DefaultFileUploadForm;
import org.slf4j.Logger;

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

    private DefaultFileUploadForm uploadForm;

    public DeployArtifactPanel(String id) {
        super(id);

        //Add upload form with ajax progress bar
        uploadForm = new DefaultFileUploadForm("uploadForm", this);

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
