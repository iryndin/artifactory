package org.artifactory.webapp.wicket.page.deploy;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusEntry;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.webapp.wicket.WicketProperty;
import org.artifactory.webapp.wicket.common.component.FileUploadForm;
import org.artifactory.webapp.wicket.common.component.FileUploadParentPanel;
import org.artifactory.webapp.wicket.common.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.common.component.help.HelpBubble;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledPanel;

import java.io.File;
import java.util.List;

/**
 * Gives the user an interface for deployment of artifacts which are stored in an archive
 *
 * @author Noam Tenne
 */
public class DeployFromZipPanel extends TitledPanel implements FileUploadParentPanel {

    @SpringBean
    private RepositoryService repositoryService;

    @SpringBean
    private SecurityService securityService;

    private FileUploadForm deployForm;

    @WicketProperty
    private LocalRepoDescriptor targetRepo;

    public DeployFromZipPanel(String id) {
        super(id);

        //Add upload form with ajax progress bar
        deployForm = new FileUploadForm("deployForm", this);

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
        final DropDownChoice targetRepo =
                new DropDownChoice("targetRepo", targetRepoModel, deployableRepos);
        if (deployableRepos.size() > 0) {
            LocalRepoDescriptor defaultTarget = deployableRepos.get(0);
            targetRepo.setModelObject(defaultTarget);
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
     */
    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    public void onFileSaved() {
        File uploadedFile = deployForm.getUploadedFile();
        MultiStatusHolder statusHolder = new MultiStatusHolder();
        try {
            repositoryService.deployBundle(uploadedFile, targetRepo, statusHolder);
            List<StatusEntry> warnings = statusHolder.getWarnings();
            if (statusHolder.isError()) {
                StatusEntry error = statusHolder.getLastError();
                Throwable throwable = error.getException();
                error(error.getStatusMessage() + (throwable != null ? ": " + throwable.getMessage() : ""));
            } else if (!warnings.isEmpty()) {
                warn("There were " + warnings.size() +
                        " warnings during import. Please see the log for more details.");
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