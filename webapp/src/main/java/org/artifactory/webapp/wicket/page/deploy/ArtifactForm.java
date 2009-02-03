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

import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.repo.DeployableArtifact;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.utils.ExceptionUtils;
import org.artifactory.webapp.wicket.WicketProperty;
import org.artifactory.webapp.wicket.common.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.checkbox.styled.StyledCheckbox;
import org.artifactory.webapp.wicket.common.component.links.TitledAjaxLink;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactForm extends Form {
    private static final Logger log = LoggerFactory.getLogger(ArtifactForm.class);

    @SpringBean
    private RepositoryService repoService;

    private final DeployArtifactPanel parent;

    private SimpleButton artifactSubmitButton;

    private LocalRepoDescriptor targetRepo;

    private StyledCheckbox deployPomCheckbox;

    @WicketProperty
    private boolean deployPom;// value of the checkbox

    public ArtifactForm(final String id, DeployArtifactPanel parent) {
        super(id, new CompoundPropertyModel(parent.getDeployableArtifact()));
        this.parent = parent;
        setOutputMarkupId(true);

        addInputTextFields();
        addDeployPomCheckBox();
        addTargetRepoDropDown();
        addSubmitFormButton();
        addCancelButton();
    }

    private void addInputTextFields() {
        addTextField("group");
        addTextField("artifact");
        addTextField("version");
        addTextField("classifier");
        addTextField("packaging");
    }

    private void addDeployPomCheckBox() {
        deployPomCheckbox = new StyledCheckbox("deployPom", new PropertyModel(this, "deployPom"));
        deployPomCheckbox.setLabel(new Model("Use Jar POM/Generate default POM"));
        deployPomCheckbox.setOutputMarkupId(true);
        add(deployPomCheckbox);
    }

    private void addSubmitFormButton() {
        artifactSubmitButton = new SimpleButton("artifactSubmit", this, "Deploy Artifact") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                DeployableArtifact deployableArtifact = parent.getDeployableArtifact();
                try {
                    repoService.deploy(targetRepo, deployableArtifact, deployPom, parent.getUploadedFile());
                    parent.getUploadForm().info("Successfully deployed: '" + deployableArtifact + "'.");
                    FeedbackUtils.refreshFeedback(target);
                } catch (Exception e) {
                    log.warn("Failed to deploy artifact", e);
                    Throwable cause = ExceptionUtils.unwrapThrowablesOfTypes(
                            e, RepoAccessException.class,
                            IllegalArgumentException.class);

                    String msg = "Failed to deploy artifact '" + deployableArtifact + "'. Cause: " +
                            cause.getMessage();
                    error(msg);
                    FeedbackUtils.refreshFeedback(target);
                } finally {
                    enable(false);
                    cleanupResources();
                    target.addComponent(parent);
                }
            }
        };
        artifactSubmitButton.setOutputMarkupId(true);
        add(artifactSubmitButton);
        add(new DefaultButtonBehavior(artifactSubmitButton));
    }

    private void addTargetRepoDropDown() {
        PropertyModel targetRepoModel = new PropertyModel(this, "targetRepo");
        List<LocalRepoDescriptor> deployableRepos = getDeployableRepos();
        final RepoDescriptor defaultTarget = deployableRepos.get(0);
        DropDownChoice targetRepo =
                new DropDownChoice("targetRepo", targetRepoModel, deployableRepos) {
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
    }

    private List<LocalRepoDescriptor> getDeployableRepos() {
        return repoService.getLocalRepoDescriptors();
    }

    private void addCancelButton() {
        TitledAjaxLink cancel = new TitledAjaxLink("cancel", "Cancel") {
            public void onClick(AjaxRequestTarget target) {
                cleanupResources();
                enable(false);
                target.addComponent(parent);
            }
        };
        add(cancel);
    }

    private TextField addTextField(String wicketId) {
        TextField textField = new TextField(wicketId);
        textField.add(new ValidateArtifactFormBehavior("onKeyup"));
        add(textField);
        return textField;
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
        log.debug("Cleaning up deployment resources.");
        DeployableArtifact deployableArtifact = parent.getDeployableArtifact();
        if (deployableArtifact != null) {
            deployableArtifact.invalidate();
        }
        parent.removeUploadedFile();
    }

    public void enable(boolean enabled) {
        setVisible(enabled);
        parent.getUploadForm().setVisible(!enabled);
        boolean nonPomDeployableArtifact = nonPomDeployableArtifact();
        deployPom = nonPomDeployableArtifact && !pomExists();
        deployPomCheckbox.setEnabled(nonPomDeployableArtifact);
        artifactSubmitButton.setEnabled(enabled);
    }

    public LocalRepoDescriptor getTargetRepo() {
        return targetRepo;
    }

    public void setTargetRepo(LocalRepoDescriptor targetRepo) {
        this.targetRepo = targetRepo;
    }

    void update(DeployableArtifact deployableArtifact) {
        setModelObject(deployableArtifact);
        enable(true);
    }

    private class ValidateArtifactFormBehavior extends AjaxFormComponentUpdatingBehavior {
        private static final long serialVersionUID = 1L;

        private ValidateArtifactFormBehavior(final String event) {
            super(event);
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
            DeployableArtifact artifact = parent.getDeployableArtifact();
            boolean valid = artifact.isValid() && targetRepo != null;
            artifactSubmitButton.setEnabled(valid);
            boolean canDeployPom = nonPomDeployableArtifact();
            deployPomCheckbox.setEnabled(canDeployPom);
            if (canDeployPom) {
                boolean pomExists = pomExists();
                deployPom = !pomExists;
            } else {
                deployPom = false;
            }
            target.addComponent(deployPomCheckbox);
            target.addComponent(artifactSubmitButton);
        }
    }

    /**
     * Checks if a matching pom for the deployable artifact exists in the target repo
     */
    private boolean pomExists() {
        try {
            return repoService.pomExists(targetRepo, parent.getDeployableArtifact());
        } catch (RuntimeException e) {
            cleanupResources();
            throw e;
        }
    }

    /**
     * Check if can deploy default pom for packaging other than pom
     */
    private boolean nonPomDeployableArtifact() {
        DeployableArtifact deployableArtifact = parent.getDeployableArtifact();
        String packagingType = deployableArtifact.getPackaging();
        return !ContentType.mavenPom.getDefaultExtension().equalsIgnoreCase(packagingType);
    }
}
