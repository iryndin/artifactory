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

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.deploy.DeployableArtifact;
import org.artifactory.maven.Maven;
import org.artifactory.repo.CentralConfig;
import org.artifactory.repo.LocalRepo;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.PackagingType;
import org.artifactory.resource.RepoResource;
import org.artifactory.security.RepoPath;
import org.artifactory.security.SecurityHelper;
import org.artifactory.spring.ContextUtils;
import org.artifactory.utils.ExceptionUtils;
import org.artifactory.webapp.wicket.ArtifactoryApp;
import org.artifactory.webapp.wicket.home.HomePage;

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

    private static final long serialVersionUID = 1L;

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
        //Add the dropdown choice for the targetRepo
        CentralConfig cc = CentralConfig.get();
        final PropertyModel targetRepoModel = new PropertyModel(ArtifactForm.this, "targetRepo");
        final List<LocalRepo> localRepos = cc.getLocalRepositories();
        final LocalRepo defaultTarget = localRepos.get(0);
        DropDownChoice targetRepo =
                new DropDownChoice("targetRepo", targetRepoModel, localRepos) {
                    private static final long serialVersionUID = 1L;

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
        //Add the deploy pom checkbox
        deployPomCheckbox = new CheckBox("deployPom", new PropertyModel(this, "deployPom"));
        deployPomCheckbox.setOutputMarkupId(true);
        add(deployPomCheckbox);
        //Add the submit link
        artifactSubmit = new SubmitLink("artifactSubmit");
        artifactSubmit.setOutputMarkupId(true);
        add(artifactSubmit);
        //Cancel link
        Link cancel = new AjaxFallbackLink("cancel") {
            private static final long serialVersionUID = 1L;

            public void onClick(final AjaxRequestTarget target) {
                cleanupResources();
                ArtifactForm.this.parent.setResponsePage(HomePage.class);
            }
        };
        cancel.setOutputMarkupId(false);
        add(cancel);
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        //Cleanup resources if we are not staying on the same page
        Page targetPage = RequestCycle.get().getResponsePage();
        Page page = getPage();
        //Target page can be null on ajax requests - in this case we do not wish to clean up
        if (targetPage != null && !page.equals(targetPage)) {
            cleanupResources();
        }
    }

    protected void cleanupResources() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Cleaning up deployment resources.");
        }
        DeployableArtifact deployableArtifact = parent.getDeployableArtifact();
        if (deployableArtifact != null) {
            deployableArtifact.invalidate();
        }
        this.parent.removeUploadedFile();
    }

    void enable(boolean enabled) {
        setVisible(enabled);
        boolean nonPomDeployableArtifact = nonPomDeployableArtifact();
        deployPom = nonPomDeployableArtifact && !pomExists();
        deployPomCheckbox.setEnabled(nonPomDeployableArtifact);
        artifactSubmit.setEnabled(enabled);
    }

    public LocalRepo getTargetRepo() {
        return targetRepo;
    }

    public void setTargetRepo(LocalRepo targetRepo) {
        this.targetRepo = targetRepo;
    }

    
    @Override
    @SuppressWarnings({"unchecked"})
    protected void onSubmit() {
        final DeployableArtifact deployableArtifact = parent.getDeployableArtifact();
        if (!deployableArtifact.isValid()) {
            error("Artifact deployment submission attempt ignored - form not valid.");
            cleanupResources();
            return;
        }
        //Sanity check
        if (targetRepo == null) {
            cleanupResources();
            throw new RuntimeException("No target repository found for deployment.");
        }
        //Check acceptance according to include/exclude patterns
        String groupId = deployableArtifact.getGroup();
        String artifactId = deployableArtifact.getArtifact();
        String version = deployableArtifact.getVersion();
        String classifier = deployableArtifact.getClassifier();
        PackagingType packaging = deployableArtifact.getPackaging();
        String path = ArtifactResource.getPath(
                groupId, artifactId, version, classifier, packaging.toString());
        if (!targetRepo.accepts(path)) {
            error("Failed: Repository + '" + targetRepo.getKey()
                    + "' include/exclude patterns rejected this artifact. "
                    + " Try to select a different repository.");
            cleanupResources();
            return;
        }
        ArtifactResource ar = new ArtifactResource(targetRepo, path);
        if (!targetRepo.handles(ar)) {
            error("Repository '" + targetRepo.getKey() + "' does handle artifact '" + ar + "'.");
            cleanupResources();
            return;
        }
        Artifact artifact = null;
        File pomFile = null;
        try {
            Maven maven = ContextUtils.getContext().getMaven();
            artifact = maven.createArtifact(
                    groupId, artifactId, version, classifier, packaging.name());
            SecurityHelper security = ContextUtils.getContext().getSecurity();
            RepoPath repoPath = new RepoPath(targetRepo.getKey(), path);
            if (!security.canDeploy(repoPath)) {
                error("Not enough permissions to deploy artifact '" + artifact + "'.");
                return;
            }
            File uploadedFile = parent.getUploadedFile();
            Model model = deployableArtifact.getModel();
            //Handle pom deployment
            if (deployPom && !packaging.equals(PackagingType.pom)) {
                if (model == null) {
                    model = new Model();
                    model.setModelVersion("4.0.0");
                    model.setGroupId(groupId);
                    model.setArtifactId(artifactId);
                    model.setVersion(version);
                    model.setPackaging(packaging.name());
                    model.setDescription("Auto generated POM");
                }
                //Create the pom file in the uploads dir
                Artifact pomArtifact = maven.createArtifact(
                        groupId, artifactId, version, classifier, PackagingType.pom.name());
                String pomFileName = uploadedFile.getName() + ".pom";
                pomFile = new File(ArtifactoryApp.UPLOAD_FOLDER, pomFileName);
                //Write the pom to the file
                MavenXpp3Writer writer = new MavenXpp3Writer();
                OutputStreamWriter osw = null;
                try {
                    osw = new OutputStreamWriter(new FileOutputStream(pomFile), "utf-8");
                    writer.write(osw, model);
                } finally {
                    IOUtils.closeQuietly(osw);
                }
                //Add project metadata that will trigger additional deployment of the pom file
                ArtifactMetadata metadata = new ProjectArtifactMetadata(artifact, pomFile);
                artifact.addMetadata(metadata);
                parent.getUploadForm().info("Successfully deployed: '" + pomArtifact + "'.");
            }
            //Add the latest version metadata for plugins.
            //With regular maven deploy this is handled automatically by the
            //AddPluginArtifactMetadataMojo, as part of the "maven-plugin" packaging lifecycle.
            if (model != null && "maven-plugin".equals(model.getPackaging())) {
                //Set the current deployed version as the latest
                Versioning versioning = new Versioning();
                versioning.setLatest(version);
                versioning.updateTimestamp();
                ArtifactRepositoryMetadata metadata =
                        new ArtifactRepositoryMetadata(artifact, versioning);
                artifact.addMetadata(metadata);
            }
            maven.deploy(uploadedFile, artifact, targetRepo);
            parent.getUploadForm().info("Successfully deployed: '" + artifact + "'.");
            enable(false);
        } catch (Exception e) {
            Throwable cause = ExceptionUtils.unwrapThrowable(
                    e, ArtifactDeploymentException.class, RuntimeException.class);
            error("Failed to deploy artifact '" + artifact + "'. Cause: " + cause.getMessage());
            LOGGER.warn("Failed to deploy artifact '" + artifact + "'.", e);
        } finally {
            if (pomFile != null) {
                pomFile.delete();
            }
            cleanupResources();
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
            //TODO: [by yl] There is a mess here between dojo and normal checkbox that causes
            //disable when switching from pom to jar. Also, take out the form markup to another
            //container and instantiate the form from sratch rather then update.
            boolean canDeployPom = nonPomDeployableArtifact();
            deployPomCheckbox.setEnabled(canDeployPom);
            if (!canDeployPom) {
                deployPom = false;
                target.appendJavascript("dojo.widget.byId('deployPom').setValue(false);");
                target.appendJavascript("dojo.widget.byId('deployPom').disable();");
            } else {
                target.appendJavascript("dojo.widget.byId('deployPom').enable();");
                boolean pomExists = pomExists();
                deployPom = !pomExists;
                target.appendJavascript(
                        "dojo.widget.byId('deployPom').setValue(" + !pomExists + ");");
            }
            target.addComponent(deployPomCheckbox);
            target.addComponent(artifactSubmit);
        }
    }

    /**
     * Checks if a matching pom for the deployable artifact exists in the target repo
     *
     * @return
     */
    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private boolean pomExists() {
        final DeployableArtifact deployableArtifact = parent.getDeployableArtifact();
        if (!deployableArtifact.isValid()) {
            return false;
        }
        String groupId = deployableArtifact.getGroup();
        String artifactId = deployableArtifact.getArtifact();
        String version = deployableArtifact.getVersion();
        PackagingType packaging = PackagingType.pom;
        String path = ArtifactResource.getPath(
                groupId, artifactId, version, null, packaging.toString());
        //Sanity check
        if (targetRepo == null) {
            cleanupResources();
            throw new RuntimeException("No target repository found for deployment.");
        } else {
            //If a pom is already deployed, default value should be not to override it
            RepoResource repoResource = targetRepo.getInfo(path);
            boolean result = repoResource.isFound();
            return result;
        }
    }

    /**
     * Check if can deploy default pom for packaging other than pom
     *
     * @return
     */
    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private boolean nonPomDeployableArtifact() {
        DeployableArtifact deployableArtifact = parent.getDeployableArtifact();
        PackagingType packagingType = deployableArtifact.getPackaging();
        boolean nonPomDeployableArtifact = !PackagingType.pom.equals(packagingType);
        return nonPomDeployableArtifact;
    }
}
