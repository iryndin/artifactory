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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.repo.exception.maven.BadPomException;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.ajax.NoAjaxIndicatorDecorator;
import org.artifactory.common.wicket.behavior.collapsible.CollapsibleBehavior;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.links.TitledAjaxLink;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.panel.editor.TextEditorPanel;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.MavenModelUtils;
import org.artifactory.util.ExceptionUtils;
import org.slf4j.Logger;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactForm extends Form {
    private static final Logger log = LoggerFactory.getLogger(ArtifactForm.class);

    @SpringBean
    private RepositoryService repoService;

    private final DeployArtifactPanel parent;

    private TitledAjaxSubmitLink artifactSubmitButton;

    private LocalRepoDescriptor targetRepo;

    private StyledCheckbox deployPomCheckbox;

    private TextEditorPanel pomEditPanel;

    @WicketProperty
    private boolean deployPom;// value of the checkbox

    private MavenArtifactInfo artifactInfo;

    private TextField groupIdTextField;
    private TextField artifactIdTextField;
    private TextField versionTextField;
    private TextField classiferTextField;
    private WebMarkupContainer pomEditContainer;

    public ArtifactForm(final String id, DeployArtifactPanel parent) {
        super(id, new CompoundPropertyModel(parent.getDeployableArtifact()));
        this.parent = parent;
        setOutputMarkupId(true);

        pomEditContainer = new WebMarkupContainer("pomEditContainer");
        pomEditContainer.setOutputMarkupId(true);
        add(pomEditContainer);

        addInputTextFields();
        addPomCheckBoxes();
        addTargetRepoDropDown();
        addSubmitFormButton();
        addCancelButton();
        addPomEditor();
    }

    private void addInputTextFields() {
        groupIdTextField = addTextField("groupId", true);
        artifactIdTextField = addTextField("artifactId", true);
        versionTextField = addTextField("version", true);
        classiferTextField = addTextField("classifier", false);
        addTextField("type", true);
    }

    private void addPomCheckBoxes() {
        deployPomCheckbox = new StyledCheckbox("deployPom", new PropertyModel(this, "deployPom")) {
            @Override
            public boolean isEnabled() {
                boolean isEmpty = StringUtils.isEmpty(classiferTextField.getValue());
                if (!isEmpty) {
                    setModelObject(Boolean.FALSE);
                }
                return isEmpty;
            }
        };
        deployPomCheckbox.setLabel(new Model("Use Jar's Internal POM/Generate Default POM"));
        deployPomCheckbox.setOutputMarkupId(true);
        deployPomCheckbox.setOutputMarkupPlaceholderTag(true);
        deployPomCheckbox.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (deployPom) {
                    artifactInfo = parent.getDeployableArtifact();
                    String pomString = repoService.getModelString(artifactInfo);
                    pomEditPanel.setEditorValue(pomString);
                }
                target.addComponent(ArtifactForm.this);
            }
        });
        pomEditContainer.add(deployPomCheckbox);
    }

    private void addSubmitFormButton() {
        artifactSubmitButton = new TitledAjaxSubmitLink("artifactSubmit", "Deploy Artifact", this) {
            @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                artifactInfo = parent.getDeployableArtifact();
                try {
                    if (deployPom) {
                        String pomString = pomEditPanel.getEditorValue();
                        repoService.validatePom(pomString, artifactInfo.getPath(),
                                targetRepo.isSuppressPomConsistencyChecks());
                        repoService.deploy(targetRepo, artifactInfo, parent.getUploadedFile(), pomString, deployPom,
                                false);
                    } else {
                        repoService.deploy(targetRepo, artifactInfo, parent.getUploadedFile(), deployPom, false);
                    }
                    parent.getUploadForm().info("Successfully deployed: '" + artifactInfo + "'.");
                    AjaxUtils.refreshFeedback(target);
                    enable(false);
                    cleanupResources();
                } catch (Exception e) {
                    Throwable cause = ExceptionUtils.getRootCause(e);
                    if (cause instanceof BadPomException) {
                        log.warn("Failed to deploy artifact: {}", e.getMessage());
                    } else {
                        log.warn("Failed to deploy artifact.", e);
                        cause = ExceptionUtils.unwrapThrowablesOfTypes(e,
                                RepoAccessException.class, IllegalArgumentException.class);
                    }
                    error(cause.getMessage());
                    AjaxUtils.refreshFeedback(target);
                } finally {
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
        if (deployableRepos.isEmpty()) {
            throw new UnauthorizedInstantiationException(DeployArtifactPage.class);
        }
        final RepoDescriptor defaultTarget = deployableRepos.get(0);
        DropDownChoice targetRepo = new DropDownChoice("targetRepo", targetRepoModel, deployableRepos);
        targetRepo.add(new ValidateArtifactFormBehavior("onChange"));
        //Needed because get getDefaultChoice does not update the actual selection object
        targetRepo.setModelObject(defaultTarget);
        targetRepo.setPersistent(true);
        targetRepo.setRequired(true);
        add(targetRepo);
    }

    private void addPomEditor() {
        String helpMessage = "View the resulting POM and handle possible discrepancies: fix bad coordinates, remove " +
                "unwanted repository references, etc. Use with caution!";
        pomEditPanel = new TextEditorPanel("pomEditPanel", "POM Editor", helpMessage) {
            @Override
            public boolean isVisible() {
                if (deployPom && (pomEditPanel.getEditorValue() != null) &&
                        ("".equals(pomEditPanel.getEditorValue()))) {
                    artifactInfo = parent.getDeployableArtifact();
                    String pomString = repoService.getModelString(artifactInfo);
                    pomEditPanel.setEditorValue(pomString);
                }
                return deployPom && deployPomCheckbox.isEnabled();
            }
        };
        pomEditPanel.add(new CollapsibleBehavior());
        pomEditPanel.addTextAreaBehavior(new ValidatePomEditorBehavior("onchange"));
        pomEditContainer.add(pomEditPanel);
    }

    private List<LocalRepoDescriptor> getDeployableRepos() {
        return repoService.getDeployableRepoDescriptors();
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

    private TextField addTextField(String wicketId, boolean required) {
        TextField textField = required ? new RequiredTextField(wicketId) : new TextField(wicketId);
        textField.add(new ValidateArtifactFormBehavior("onchange"));
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
        MavenArtifactInfo artifactInfo = parent.getDeployableArtifact();
        if (artifactInfo != null) {
            artifactInfo.invalidate();
        }
        parent.removeUploadedFile();
        pomEditPanel.setEditorValue("");
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

    void update(MavenArtifactInfo artifactInfo) {
        setModelObject(artifactInfo);
        enable(true);
        String pomString = repoService.getModelString(artifactInfo);
        pomEditPanel.setEditorValue(pomString);
    }

    private class ValidateArtifactFormBehavior extends AjaxFormComponentUpdatingBehavior {
        private static final long serialVersionUID = 1L;

        private ValidateArtifactFormBehavior(final String event) {
            super(event);
        }

        @Override
        protected IAjaxCallDecorator getAjaxCallDecorator() {
            return new NoAjaxIndicatorDecorator();
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
            boolean canDeployPom = nonPomDeployableArtifact();
            deployPomCheckbox.setEnabled(canDeployPom);
            if (canDeployPom) {
                boolean pomExists = pomExists();
                deployPom = !pomExists;
            } else {
                deployPom = false;
            }

            if (deployPom && deployPomCheckbox.isEnabled()) {
                updatePomEditor();
            }
            target.addComponent(pomEditContainer);
        }
    }

    /**
     * Checks if a matching pom for the deployable artifact exists in the target repo
     *
     * @return boolean - True if pom exists
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
     *
     * @return boolean - True if artifact is deployed with pom
     */
    private boolean nonPomDeployableArtifact() {
        MavenArtifactInfo artifactInfo = parent.getDeployableArtifact();
        String packagingType = artifactInfo.getType();
        return !ContentType.mavenPom.getDefaultExtension().equalsIgnoreCase(packagingType);
    }

    private void updatePomEditor() {
        MavenArtifactInfo artifactInfo = parent.getDeployableArtifact();
        String pomString = repoService.getModelString(artifactInfo);
        pomEditPanel.setEditorValue(pomString);
    }

    private class ValidatePomEditorBehavior extends AjaxFormComponentUpdatingBehavior {
        private static final long serialVersionUID = 1L;

        /**
         * Construct.
         *
         * @param event event to trigger this behavior
         */
        public ValidatePomEditorBehavior(String event) {
            super(event);
        }

        @Override
        protected IAjaxCallDecorator getAjaxCallDecorator() {
            return new NoAjaxIndicatorDecorator();
        }

        @SuppressWarnings({"EmptyCatchBlock"})
        @Override
        protected void onUpdate(AjaxRequestTarget target) {
            String pomValue = pomEditPanel.getEditorValue();
            if (pomValue != null) {
                org.apache.maven.model.Model model = null;
                try {
                    model = MavenModelUtils.stringToMavenModel(pomValue);
                } catch (RepositoryRuntimeException rre) {
                }
                if (model != null) {
                    groupIdTextField.setModelObject(model.getGroupId());
                    artifactIdTextField.setModelObject(model.getArtifactId());
                    versionTextField.setModelObject(model.getVersion());
                    target.addComponent(groupIdTextField);
                    target.addComponent(artifactIdTextField);
                    target.addComponent(versionTextField);
                }
            }
        }
    }
}