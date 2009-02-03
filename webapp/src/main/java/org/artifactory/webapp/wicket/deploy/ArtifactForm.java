package org.artifactory.webapp.wicket.deploy;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.artifactory.deploy.DeployableArtifact;
import org.artifactory.maven.Maven;
import org.artifactory.repo.CentralConfig;
import org.artifactory.repo.LocalRepo;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.PackagingType;
import org.artifactory.security.SecurityHelper;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.webapp.wicket.ArtifactoryApp;
import org.artifactory.webapp.wicket.home.HomePage;
import wicket.ajax.AjaxRequestTarget;
import wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import wicket.ajax.markup.html.AjaxFallbackLink;
import wicket.markup.html.form.CheckBox;
import wicket.markup.html.form.DropDownChoice;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.SubmitLink;
import wicket.markup.html.form.TextField;
import wicket.markup.html.link.Link;
import wicket.model.CompoundPropertyModel;
import wicket.model.PropertyModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactForm extends Form {
    @SuppressWarnings({"UnusedDeclaration", "UNUSED_SYMBOL"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactForm.class);

    private final DeployArtifactPanel parent;
    private SubmitLink artifactSubmit;
    private LocalRepo targetRepo;
    private CheckBox deployPomCheckbox;
    private boolean deployPom;

    public ArtifactForm(final String id, DeployArtifactPanel parent) {
        super(id, new CompoundPropertyModel(parent.getDeployableArtifact()));
        this.parent = parent;
        setOutputMarkupId(true);
        TextField groupTf = new TextField("group");
        groupTf.add(new ValidateArtifactFormBehavior("onKeyup"));
        add(groupTf);
        TextField artifactTf = new TextField("artifact");
        artifactTf.add(new ValidateArtifactFormBehavior("onKeyup"));
        add(artifactTf);
        TextField versionTf = new TextField("version");
        versionTf.add(new ValidateArtifactFormBehavior("onKeyup"));
        add(versionTf);
        TextField classifierTf = new TextField("classifier");
        classifierTf.add(new ValidateArtifactFormBehavior("onKeyup"));
        add(classifierTf);
        //Add the dropdown choice for the packaging
        DeployableArtifact deployableArtifact = parent.getDeployableArtifact();
        PropertyModel packagingModel = new PropertyModel(deployableArtifact, "packaging");
        DropDownChoice packaging =
                new DropDownChoice("packaging", packagingModel, PackagingType.LIST);
        packaging.add(new ValidateArtifactFormBehavior("onChange"));
        add(packaging);
        //Add the deploy pom checkbox
        deployPom = canDeployPom();
        deployPomCheckbox = new CheckBox("deployPom", new PropertyModel(this, "deployPom"));
        deployPomCheckbox.setOutputMarkupId(true);
        add(deployPomCheckbox);
        //Add the dropdown choice for the targetRepo
        CentralConfig cc = parent.getCc();
        final PropertyModel targetRepoModel = new PropertyModel(ArtifactForm.this, "targetRepo");
        final List<LocalRepo> localRepos = cc.getLocalRepositories();
        final LocalRepo defaultTarget = localRepos.get(0);
        DropDownChoice targetRepo =
                new DropDownChoice("targetRepo", targetRepoModel, localRepos) {
                    @Override
                    protected CharSequence getDefaultChoice(final Object selected) {
                        return defaultTarget.toString();
                    }
                };
        targetRepo.add(new ValidateArtifactFormBehavior("onChange"));
        //Needed because get getDefaultChoice does not update the actual selection object
        targetRepo.setModelObject(defaultTarget);
        targetRepo.setPersistent(true);
        add(targetRepo);
        //Add the submit link
        artifactSubmit = new SubmitLink("artifactSubmit");
        artifactSubmit.setOutputMarkupId(true);
        add(artifactSubmit);
        Link cancel = new AjaxFallbackLink("cancel") {
            public void onClick(final AjaxRequestTarget target) {
                ArtifactForm.this.parent.removeUploadedFile();
                ArtifactForm.this.parent.setResponsePage(HomePage.class);
            }
        };
        cancel.setOutputMarkupId(false);
        add(cancel);
    }

    void enable(boolean b) {
        setVisible(b);
        deployPom = canDeployPom();
        deployPomCheckbox.setEnabled(deployPom);
        artifactSubmit.setEnabled(b);
    }

    public LocalRepo getTargetRepo() {
        return targetRepo;
    }

    public void setTargetRepo(LocalRepo targetRepo) {
        this.targetRepo = targetRepo;
    }

    @Override
    protected void onSubmit() {
        final DeployableArtifact deployableArtifact = parent.getDeployableArtifact();
        if (!deployableArtifact.isValid()) {
            error("Artifact deployment submission attempt ignored - form not valid.");
            return;
        }
        File uploadedFile = parent.getUploadedFile();
        Artifact artifact = null;
        //Add the deployer component and friends
        ArtifactoryContext context = parent.getContext();
        //Sanity check
        if (targetRepo == null) {
            throw new RuntimeException("No local file store found for deployment.");
        }
        //Check acceptance according to include/exclude patterns
        String groupId = deployableArtifact.getGroup();
        String artifactId = deployableArtifact.getArtifact();
        String version = deployableArtifact.getVersion();
        String classifier = deployableArtifact.getClassifier();
        PackagingType packaging = deployableArtifact.getPackaging();
        String relativePath = ArtifactResource.getRelativePath(
                groupId, artifactId, version, classifier, packaging.toString());
        if (!targetRepo.accept(relativePath)) {
            error("Failed: Repository + '" + targetRepo.getKey()
                    + "' include/exclude patterns rejected this artifact. "
                    + " Try to select a different repository.");
            return;
        }
        try {
            Maven maven = context.getMaven();
            artifact = createArtifact(maven, packaging);
            SecurityHelper security = parent.getContext().getSecurity();
            if (!security.canDeploy(artifact)) {
                error("Not enough permissions to deploy artifact '" + artifact + "'.");
                return;
            }
            maven.deploy(uploadedFile, artifact, targetRepo);
            parent.getUploadForm().info("Successfully deployed: '" + artifact + "'.");
            Model model = null;
            //Try to create a model
            if (deployPom) {
                //If the artifact has a pom attached, deploy it as well
                model = deployableArtifact.getModel();
                if (model == null) {
                    //Generate a default pom
                    model = new Model();
                    model.setModelVersion("4.0.0");
                    model.setGroupId(groupId);
                    model.setArtifactId(artifactId);
                    model.setVersion(version);
                    model.setPackaging(packaging.toString());
                    model.setDescription("Auto generated POM");
                }
            }
            if (model != null && !packaging.equals(PackagingType.pom)) {
                Artifact pomArtifact = createArtifact(maven, PackagingType.pom);
                //Create the uploaded file
                String path = uploadedFile.getAbsolutePath();
                String pomFileName = path.substring(
                        path.lastIndexOf(File.separator) + 1, path.length() - 3) + "pom";
                File pomFile = new File(ArtifactoryApp.UPLOAD_FOLDER, pomFileName);
                //Write the pom to the file
                MavenXpp3Writer writer = new MavenXpp3Writer();
                OutputStreamWriter osw = new OutputStreamWriter(
                        new FileOutputStream(pomFile), "utf-8");
                writer.write(osw, model);
                IOUtils.closeQuietly(osw);
                maven.deploy(pomFile, pomArtifact, targetRepo);
                parent.getUploadForm().info("Successfully deployed: '" + pomArtifact + "'.");
                //Delete the file
                pomFile.delete();
            }
            enable(false);
        } catch (Exception e) {
            error("Failed to deploy artifact '" + artifact + "'. Cause: " + e.getMessage());
            LOGGER.warn("Failed to deploy artifact '" + artifact + "'.", e);
        } finally {
            deployableArtifact.invalidate();
            parent.removeUploadedFile();
        }
    }

    void update(DeployableArtifact deployableArtifact) {
        setModelObject(deployableArtifact);
        enable(true);
    }

    private class ValidateArtifactFormBehavior extends AjaxFormComponentUpdatingBehavior {
        private static final long serialVersionUID = 1L;

        public ValidateArtifactFormBehavior(final String event) {
            super(event);
        }

        protected void onUpdate(AjaxRequestTarget target) {
            DeployableArtifact artifact = parent.getDeployableArtifact();
            boolean valid = artifact.isValid() && targetRepo != null;
            artifactSubmit.setEnabled(valid);
            target.appendJavascript(
                    "dojo.widget.byId('artifactSubmit').setDisabled(" + !valid + ");");
            //TODO: [by yl] There is a mess here between dojo and normal checkbox that causes no enable when
            //switching from pom to jar. Also, take out the from markup to another container and instantiate
            //the form from sratch rather then update.
            boolean canDeployPom = canDeployPom();
            deployPomCheckbox.setEnabled(canDeployPom);
            if (!canDeployPom) {
                deployPom = false;
                target.appendJavascript("dojo.widget.byId('deployPom').setValue(false);");
                target.appendJavascript("dojo.widget.byId('deployPom').disable();");
            } else {
                target.appendJavascript("dojo.widget.byId('deployPom').enable();");
            }
            target.addComponent(deployPomCheckbox);
            target.addComponent(artifactSubmit);
        }

    }

    /**
     * Check if can deploy default pom for packaging other than pom
     *
     * @return
     */
    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private boolean canDeployPom() {
        DeployableArtifact deployableArtifact = parent.getDeployableArtifact();
        PackagingType packagingType = deployableArtifact.getPackaging();
        boolean canDeployPom = !PackagingType.pom.equals(packagingType);
        return canDeployPom;
    }

    private Artifact createArtifact(Maven maven, PackagingType packaging) {
        DeployableArtifact deployableArtifact = parent.getDeployableArtifact();
        return maven.createArtifact(
                deployableArtifact.getGroup().trim(),
                deployableArtifact.getArtifact().trim(),
                deployableArtifact.getVersion().trim(),
                deployableArtifact.getClassifier() != null ?
                        deployableArtifact.getClassifier().trim() : null,
                packaging.name());
    }
}
