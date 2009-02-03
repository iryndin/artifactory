package org.artifactory.webapp.wicket.deploy;

import org.apache.log4j.Logger;
import org.artifactory.deploy.DeployableArtifact;
import org.artifactory.maven.MavenUtils;
import org.artifactory.webapp.wicket.ArtifactoryApp;
import org.artifactory.webapp.wicket.ArtifactoryPage;
import org.codehaus.plexus.util.StringUtils;
import wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import wicket.extensions.ajax.markup.html.form.upload.UploadProgressBar;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.upload.FileUpload;
import wicket.markup.html.form.upload.FileUploadField;
import wicket.markup.html.panel.FeedbackPanel;
import wicket.util.file.Files;
import wicket.util.lang.Bytes;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;

@AuthorizeInstantiation("ADMIN")
public class DeployArtifactPage extends ArtifactoryPage {

    @SuppressWarnings({"UnusedDeclaration", "UNUSED_SYMBOL"})
    private final static Logger LOGGER = Logger.getLogger(DeployArtifactPage.class);

    private DeployableArtifact deployableArtifact;
    private ArtifactForm artifactForm;
    private FileUploadForm uploadForm;
    private File uploadedFile;

    /**
     * Constructor.
     */
    public DeployArtifactPage() {
        // Create feedback panel
        final FeedbackPanel uploadFeedback = new FeedbackPanel("uploadFeedback");
        add(uploadFeedback);

        // Add upload form with ajax progress bar
        uploadForm = new FileUploadForm("uploadForm");
        uploadForm.add(new UploadProgressBar("progress", uploadForm));
        add(uploadForm);

        //Add the artifact details
        deployableArtifact = new DeployableArtifact();
        artifactForm = new ArtifactForm("artifactForm", this);
        artifactForm.setOutputMarkupId(false);
        add(artifactForm);
        artifactForm.enable(false);
    }

    protected String getPageName() {
        return "Artifact Deployer";
    }

    /**
     * Check whether the file allready exists, and if so, try to delete it.
     */
    void removeUploadedFile() {
        if (uploadedFile.exists()) {
            //Try to delete the file
            if (!Files.remove(uploadedFile)) {
                throw new IllegalStateException("Unable to remove/overwrite "
                        + uploadedFile.getAbsolutePath());
            }
        }
    }

    /**
     * Form for uploads.
     */
    class FileUploadForm extends Form {
        private FileUploadField fileUploadField;

        /**
         * Construct.
         *
         * @param name Component name
         */
        public FileUploadForm(String name) {
            super(name);
            // set this form to multipart mode (allways needed for uploads!)
            setMultiPart(true);
            // Add one file input field
            add(fileUploadField = new FileUploadField("fileInput"));
            // Set maximum size to 100K for demo purposes
            setMaxSize(Bytes.megabytes(10));
        }

        /**
         * @see wicket.markup.html.form.Form#onSubmit()
         */
        protected void onSubmit() {
            final FileUpload upload = fileUploadField.getFileUpload();
            if (upload != null) {
                // Create a new file
                uploadedFile = new File(ArtifactoryApp.UPLOAD_FOLDER, upload.getClientFileName());
                //Check new file, delete if it allready existed
                removeUploadedFile();
                try {
                    // Save to new file
                    uploadedFile.createNewFile();
                    upload.writeTo(uploadedFile);
                    DeployArtifactPage.this.info(
                            "Successfully uploaded file: '" + upload.getClientFileName()
                                    + "' into '" + ArtifactoryApp.UPLOAD_FOLDER.getAbsolutePath()
                                    + "'.");
                }
                catch (Exception e) {
                    DeployArtifactPage.this.artifactForm.setVisible(false);
                    throw new IllegalStateException("Unable to write file to '"
                            + ArtifactoryApp.UPLOAD_FOLDER.getAbsolutePath() + "'.");
                }
                deployableArtifact = deployableArtifactFromUploadedFile();
                artifactForm.update(deployableArtifact);
            }
        }

        //Analyze the uploadedFile
        private DeployableArtifact deployableArtifactFromUploadedFile() {
            deployableArtifact.invalidate();
            //Try to guess the properties from pom/jar content
            try {
                deployableArtifact.update(uploadedFile);
            } catch (IOException e) {
                error("Unable to analyze uploaded file content. Cause: " + e.getMessage());
            }
            String fileName = uploadedFile.getName();
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
                deployableArtifact.setArtifact(uploadedFile.getName());
            }
            if (StringUtils.isEmpty(deployableArtifact.getGroup())) {
                //If we have no group, set it to be the same as the artifact name
                deployableArtifact.setGroup(deployableArtifact.getArtifact());
            }
            if (StringUtils.isEmpty(deployableArtifact.getVersion())) {
                deployableArtifact.setVersion(uploadedFile.getName());
            }
            return deployableArtifact;
        }
    }

    DeployableArtifact getDeployableArtifact() {
        return deployableArtifact;
    }

    FileUploadForm getUploadForm() {
        return uploadForm;
    }

    File getUploadedFile() {
        return uploadedFile;
    }
}
