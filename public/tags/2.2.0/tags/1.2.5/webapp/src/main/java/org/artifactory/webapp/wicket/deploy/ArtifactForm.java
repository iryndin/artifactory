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
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.config.CentralConfig;
import org.artifactory.deploy.DeployableArtifact;
import org.artifactory.maven.Maven;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.PackagingType;
import org.artifactory.security.AccessLogger;
import org.artifactory.security.RepoPath;
import org.artifactory.security.SecurityHelper;
import org.artifactory.spring.ContextHelper;
import org.artifactory.utils.ExceptionUtils;
import org.artifactory.webapp.wicket.ArtifactoryApp;
import org.artifactory.webapp.wicket.components.SimpleAjaxSubmitLink;
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

    private SimpleAjaxSubmitLink artifactSubmit;

    private LocalRepo targetRepo;

    private CheckBox deployPomCheckbox;

    private boolean deployPom;

    public ArtifactForm(final String id, DeployArtifactPanel parent) {
        super(id, new CompoundPropertyModel(parent.getDeployableArtifact()));
        this.parent = parent;
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
        TextField packagingTf = new TextField("packaging");
        packagingTf.add(new ValidateArtifactFormBehavior("onKeyup"));
        add(packagingTf);
        //Add the dropdown choice for the targetRepo
        CentralConfig cc = CentralConfig.get();
        final PropertyModel targetRepoModel = new PropertyModel(ArtifactForm.this, "targetRepo");
        VirtualRepo virtualRepo = cc.getGlobalVirtualRepo();
        final List<LocalRepo> localRepos = virtualRepo.getLocalRepositories();
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
        artifactSubmit = new SimpleAjaxSubmitLink("artifactSubmit", this);
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
        setOutputMarkupId(true);
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
        String packaging = deployableArtifact.getPackaging();
        String path = ArtifactResource.getPath(groupId, artifactId, version, classifier, packaging);
        if (!targetRepo.accepts(path)) {
            error("The repository + '" + targetRepo.getKey()
                    + "' include/exclude patterns rejected the artifact '" + path + "'."
                    + " Try to select a different repository.");
            cleanupResources();
            return;
        }
        if (!targetRepo.handles(path)) {
            error("The snapshot/release handling policy of the repository '" + targetRepo.getKey()
                    + "' rejected the artifact '" + path
                    + "'. Try to select a different repository.");
            cleanupResources();
            return;
        }
        Artifact artifact = null;
        File pomFile = null;
        try {
            Maven maven = ContextHelper.get().getMaven();
            artifact = maven.createArtifact(groupId, artifactId, version, classifier, packaging);
            SecurityHelper security = ContextHelper.get().getSecurity();
            RepoPath repoPath = new RepoPath(targetRepo.getKey(), path);
            if (!security.canDeploy(repoPath)) {
                error("Not enough permissions to deploy artifact '" + artifact + "'.");
                AccessLogger.deployDenied(repoPath);
                return;
            }
            File uploadedFile = parent.getUploadedFile();
            Model model = deployableArtifact.getModel();
            //Handle pom deployment
            if (deployPom && !packaging.equalsIgnoreCase(PackagingType.pom.name())) {
                if (model == null) {
                    model = new Model();
                    model.setModelVersion("4.0.0");
                    model.setGroupId(groupId);
                    model.setArtifactId(artifactId);
                    model.setVersion(version);
                    model.setPackaging(packaging);
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
            Throwable cause = ExceptionUtils.unwrapThrowablesOfTypes(
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
            //container and instantiate the form from scratch rather than update.
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
        String packaging = PackagingType.pom.name();
        String path = ArtifactResource.getPath(groupId, artifactId, version, null, packaging);
        //Sanity check
        if (targetRepo == null) {
            cleanupResources();
            throw new RuntimeException("No target repository found for deployment.");
        } else {
            //If a pom is already deployed (or a folder by the same name exists), default value
            //should be not to override it
            boolean exists = targetRepo.itemExists(path);
            return exists;
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
        String packagingType = deployableArtifact.getPackaging();
        boolean nonPomDeployableArtifact =
                !PackagingType.pom.name().equalsIgnoreCase(packagingType);
        return nonPomDeployableArtifact;
    }
}
