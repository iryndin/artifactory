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

package org.artifactory.webapp.wicket.page.deploy.fromzip;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.repo.DeployService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.common.StatusEntry;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.panel.upload.FileUploadForm;
import org.artifactory.common.wicket.panel.upload.UploadListener;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.webapp.wicket.page.logs.SystemLogsPage;
import org.artifactory.webapp.wicket.panel.upload.DefaultFileUploadForm;

import java.io.File;
import java.util.List;

/**
 * Gives the user an interface for deployment of artifacts which are stored in an archive
 *
 * @author Noam Tenne
 */
public class DeployFromZipPanel extends TitledPanel implements UploadListener {

    @SpringBean
    private AuthorizationService authorizationService;

    @SpringBean
    private RepositoryService repositoryService;

    @SpringBean
    private DeployService deployService;

    @SpringBean
    private SecurityService securityService;

    private DefaultFileUploadForm deployForm;

    @WicketProperty
    private LocalRepoDescriptor targetRepo;

    public DeployFromZipPanel(String id) {
        super(id);

        //Add upload form with ajax progress bar
        deployForm = new DefaultFileUploadForm("deployForm", this);

        TitledBorder uploadBorder = new TitledBorder("uploadBorder") {
            @Override
            public boolean isVisible() {
                return super.isVisible() && deployForm.isVisible();
            }
        };
        add(uploadBorder);
        uploadBorder.add(deployForm);

        PropertyModel targetRepoModel = new PropertyModel(this, "targetRepo");
        List<LocalRepoDescriptor> deployableRepos = getDeployableRepos();
        final DropDownChoice targetRepo = new DropDownChoice("targetRepo", targetRepoModel, deployableRepos);
        if (deployableRepos.size() > 0) {
            LocalRepoDescriptor defaultTarget = deployableRepos.get(0);
            targetRepo.setDefaultModelObject(defaultTarget);
        } //Else - BUG!
        deployForm.add(targetRepo);

        deployForm.add(new HelpBubble("deployHelp", getDeployHelp()));
    }

    /**
     * Returns the content string of the archive selection help bubble
     *
     * @return String - Text for archive selection help buble
     */
    private String getDeployHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("When deploying an artifacts bundle, the file structure within the archive you select should be\n");
        sb.append("similar to:\n");
        sb.append("SELECTED_ARCHIVE.zip\n");
        sb.append(" |\n");
        sb.append(" |--org\n");
        sb.append(" |--|--apache\n");
        sb.append("\n");
        sb.append("Please note that artifacts need to be stored in the archive in a Maven repository structure,\n");
        sb.append("with no extra folders between the archive root and the artifact's first group directory.");
        return sb.toString();
    }

    public void onException() {
    }

    /**
     * Remove uploaded file from the form
     */
    public void removeUploadedFile() {
        if (deployForm != null) {
            deployForm.removeUploadedFile();
        }
    }

    /**
     * Executes when the uploaded file is saved
     *
     * @param file
     */
    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    public void onFileSaved(File file) {
        File uploadedFile = deployForm.getUploadedFile();
        MultiStatusHolder statusHolder = new MultiStatusHolder();
        try {
            deployService.deployBundle(uploadedFile, targetRepo, statusHolder);
            List<StatusEntry> errors = statusHolder.getErrors();
            List<StatusEntry> warnings = statusHolder.getWarnings();

            String logs;
            if (authorizationService.isAdmin()) {
                CharSequence systemLogsPage = WicketUtils.mountPathForPage(SystemLogsPage.class);
                logs = "<a href=\"" + systemLogsPage + "\">log</a>";
            } else {
                logs = "log";
            }

            if (!errors.isEmpty()) {
                error("There were " + errors.size() + " errors during import. Please review the " + logs +
                        " for more details.");
            } else if (!warnings.isEmpty()) {
                warn("There were " + warnings.size() + " warnings during import. Please review the " + logs +
                        " for more details.");
            } else {
                info(statusHolder.getStatusMsg());
            }
        } finally {
            //Delete the uploaded file
            FileUtils.deleteQuietly(uploadedFile);
        }
    }

    /**
     * Returns the uploaded file from the form
     *
     * @return File - The uploaded file from the form
     */
    public File getUploadedFile() {
        return this.deployForm.getUploadedFile();
    }

    /**
     * Returns the upload form
     *
     * @return FileUploadForm - The used upload form
     */
    public FileUploadForm getUploadForm() {
        return deployForm;
    }

    /**
     * Returns all the deployable repos from the repository service
     *
     * @return List - All deployable repos
     */
    private List<LocalRepoDescriptor> getDeployableRepos() {
        return repositoryService.getDeployableRepoDescriptors();
    }
}
