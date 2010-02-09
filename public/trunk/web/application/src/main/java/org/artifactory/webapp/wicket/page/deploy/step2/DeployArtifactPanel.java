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

package org.artifactory.webapp.wicket.page.deploy.step2;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.repo.DeployService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.repo.exception.maven.BadPomException;
import org.artifactory.common.wicket.ajax.NoAjaxIndicatorDecorator;
import org.artifactory.common.wicket.behavior.collapsible.CollapsibleBehavior;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxLink;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.panel.titled.TitledActionPanel;
import org.artifactory.common.wicket.panel.editor.TextEditorPanel;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.common.wicket.util.CookieUtils;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.MavenModelUtils;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.FileUtils;
import org.artifactory.util.StringInputStream;
import org.artifactory.webapp.wicket.page.deploy.DeployArtifactPage;
import org.artifactory.webapp.wicket.page.deploy.step1.UploadArtifactPanel;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class DeployArtifactPanel extends TitledActionPanel {
    private static final Logger log = LoggerFactory.getLogger(DeployArtifactPanel.class);

    public DeployArtifactPanel(String id, File file) {
        super(id);

        add(new DeployArtifactForm(file));
    }

    private static class DeployModel implements Serializable {
        private List<LocalRepoDescriptor> repos;
        private File file;
        private LocalRepoDescriptor targetRepo;
        private boolean deployPom;
        private boolean pomChanged;
        private String pomXml;
        private MavenArtifactInfo artifactInfo;
    }

    private class DeployArtifactForm extends Form {
        @SpringBean
        private RepositoryService repoService;

        @SpringBean
        private DeployService deployService;

        private DeployModel model;
        private static final String TARGET_REPO = "targetRepo";

        private DeployArtifactForm(File file) {
            super("form");
            model = new DeployModel();
            model.file = file;
            model.artifactInfo = guessArtifactInfo();
            model.repos = getRepos();
            model.targetRepo = getPersistentTargetRepo();
            model.deployPom = isPomArtifact() || !isPomExists();
            model.pomChanged = false;
            model.pomXml = deployService.getModelString(model.artifactInfo);

            setModel(new CompoundPropertyModel(model));

            add(new Label("file.name"));
            addPathField();
            addTargetRepoDropDown();
            addDeployMavenCheckbox();

            WebMarkupContainer artifactInfo = newMavenArtifactContainer();
            add(artifactInfo);

            artifactInfo.add(newPomEditContainer());

            addDefaultButton(new DeployLink("deploy"));
            addButton(new CancelLink("cancel"));
        }

        //will be first initialize from cookie value, fall back to default

        private LocalRepoDescriptor getPersistentTargetRepo() {
            String cookieName = buildCookieName();
            String cookie = CookieUtils.getCookie(cookieName);
            int value = 0;
            if (cookie != null) {
                try {
                    value = Integer.parseInt(cookie);
                } catch (NumberFormatException e) {
                    log.debug("no cookie found for upload target repo, will use default repo");
                }
            }
            return getRepos().get(value);
        }

        private String buildCookieName() {
            StringBuilder name = new StringBuilder(DeployArtifactPanel.this.getId());
            name.append(".").append(this.getId()).append(".").append(TARGET_REPO);
            return name.toString();
        }

        private List<LocalRepoDescriptor> getRepos() {
            List<LocalRepoDescriptor> repos = repoService.getDeployableRepoDescriptors();
            if (repos.isEmpty()) {
                throw new UnauthorizedInstantiationException(DeployArtifactPage.class);
            }
            return repos;
        }

        private Component newPomEditContainer() {
            MarkupContainer pomEditContainer = new WebMarkupContainer("pomEditContainer");
            pomEditContainer.setOutputMarkupPlaceholderTag(true);
            pomEditContainer.add(newGeneratePomCheckBox());
            pomEditContainer.add(newPomEditorPanel());
            return pomEditContainer;
        }

        private Component newPomEditorPanel() {
            final String helpMessage =
                    "View the resulting POM and handle possible discrepancies: fix bad coordinates, remove " +
                            "unwanted repository references, etc. Use with caution!";

            TextEditorPanel pomEditPanel = new TextEditorPanel("pomEditPanel", "POM Editor", helpMessage) {
                @Override
                protected IModel newTextModel() {
                    return new PropertyModel(model, "pomXml");
                }

                @Override
                public boolean isVisible() {
                    return model.deployPom;
                }
            };
            pomEditPanel.add(new CollapsibleBehavior().setUseAjax(true));
            pomEditPanel.addTextAreaBehavior(new OnPomXmlChangeBehavior());
            return pomEditPanel;
        }

        private Component newGeneratePomCheckBox() {
            FormComponent checkbox = new StyledCheckbox("deployPom");
            checkbox.setVisible(!isPomArtifact());
            checkbox.setLabel(new Model("Also Deploy Jar's Internal POM/Generate Default POM"));
            checkbox.add(new OnGeneratePomChangeBehavior());
            return checkbox;
        }

        private WebMarkupContainer newMavenArtifactContainer() {
            WebMarkupContainer artifactInfo = new WebMarkupContainer("artifactInfo") {
                @Override
                public boolean isVisible() {
                    return model.artifactInfo.isAutoCalculatePath();
                }
            };
            artifactInfo.setOutputMarkupPlaceholderTag(true);
            artifactInfo.add(newGavcField("artifactInfo.groupId", true, new OnGavcChangeBehavior()));
            artifactInfo.add(newGavcField("artifactInfo.artifactId", true, new OnGavcChangeBehavior()));
            artifactInfo.add(newGavcField("artifactInfo.version", true, new OnGavcChangeBehavior()));
            artifactInfo.add(newGavcField("artifactInfo.classifier", false, new OnGavcChangeBehavior()));
            artifactInfo.add(newGavcField("artifactInfo.type", true, new OnPackTypeChangeBehavior()));
            return artifactInfo;
        }

        private Component newGavcField(String id, boolean required, IBehavior behavior) {
            FormComponent textField = new TextField(id);
            textField.setRequired(required);
            textField.setOutputMarkupId(true);
            textField.add(behavior);
            return textField;
        }

        private void addPathField() {
            FormComponent path = new TextField("artifactInfo.path") {
                @Override
                public boolean isEnabled() {
                    return !model.artifactInfo.isAutoCalculatePath();
                }
            };
            path.setRequired(true);
            path.setOutputMarkupId(true);
            add(path);

            add(new HelpBubble("path.help", new ResourceModel("path.help")));
        }

        private void addDeployMavenCheckbox() {
            Component autoCalculatePath = new StyledCheckbox("artifactInfo.autoCalculatePath").setPersistent(true);
            autoCalculatePath.add(new OnDeployTypeChangeBehavior());
            add(autoCalculatePath);
            add(new HelpBubble("autoCalculatePath.help", new ResourceModel("autoCalculatePath.help")));
        }

        private void addTargetRepoDropDown() {
            FormComponent targetRepo = new DropDownChoice(TARGET_REPO, model.repos);
            targetRepo.setPersistent(true);
            targetRepo.setRequired(true);
            targetRepo.add(new OnGavcChangeBehavior());
            add(targetRepo);

            add(new HelpBubble("targetRepo.help", new ResourceModel("targetRepo.help")));
        }

        private MarkupContainer getPomEditorContainer() {
            return (MarkupContainer) get("artifactInfo:pomEditContainer");
        }

        /**
         * Try to guess the properties from pom/jar content.
         *
         * @return artifact info
         */
        private MavenArtifactInfo guessArtifactInfo() {
            try {
                return deployService.getArtifactInfo(model.file);
            } catch (Exception e) {
                String msg = "Unable to analyze uploaded file content. Cause: " + e.getMessage();
                log.debug(msg, e);
                error(msg);
            }
            return new MavenArtifactInfo();
        }

        private boolean isPomArtifact() {
            String packagingType = model.artifactInfo.getType();
            return ContentType.mavenPom.getDefaultExtension().equalsIgnoreCase(packagingType);
        }

        private boolean isPomExists() {
            try {
                return repoService.pomExists(model.targetRepo, model.artifactInfo);
            } catch (RuntimeException e) {
                cleanupResources();
                throw e;
            }
        }

        private void cleanupResources() {
            log.debug("Cleaning up deployment resources.");
            if (model.artifactInfo != null) {
                model.artifactInfo.invalidate();
            }
            FileUtils.removeFile(model.file);
            model.pomXml = "";
        }

        private class OnGavcChangeBehavior extends AjaxFormComponentUpdatingBehavior {

            private OnGavcChangeBehavior() {
                super("onchange");
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new NoAjaxIndicatorDecorator();
            }

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                boolean isPomExists = isPomExists();
                model.artifactInfo.setBuiltFromPomInfo(isPomExists);
                getPomEditorContainer().get("deployPom").setModel(new Model(!isPomExists));
                if (model.deployPom) {
                    model.pomChanged = false;
                    model.pomXml = deployService.getModelString(model.artifactInfo);
                }
                target.addComponent(getPomEditorContainer());
            }

        }

        private class OnDeployTypeChangeBehavior extends AjaxFormComponentUpdatingBehavior {

            private OnDeployTypeChangeBehavior() {
                super("onclick");
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new NoAjaxIndicatorDecorator();
            }

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.addComponent(get("artifactInfo"));
                target.addComponent(get("artifactInfo.path"));
            }

        }

        private class OnPomXmlChangeBehavior extends AjaxFormComponentUpdatingBehavior {

            private OnPomXmlChangeBehavior() {
                super("onchange");
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new NoAjaxIndicatorDecorator();
            }

            @SuppressWarnings({"EmptyCatchBlock"})
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                model.pomChanged = true;
                if (StringUtils.isNotEmpty(model.pomXml)) {
                    try {
                        org.apache.maven.model.Model mavenModel = MavenModelUtils.stringToMavenModel(model.pomXml);
                        if (mavenModel != null) {
                            model.artifactInfo.setGroupId(mavenModel.getGroupId());
                            model.artifactInfo.setArtifactId(mavenModel.getArtifactId());
                            model.artifactInfo.setVersion(mavenModel.getVersion());

                            target.addComponent(get("artifactInfo:artifactInfo.groupId"));
                            target.addComponent(get("artifactInfo:artifactInfo.artifactId"));
                            target.addComponent(get("artifactInfo:artifactInfo.version"));
                        }
                    } catch (RepositoryRuntimeException e) {
                        // NOP
                    }
                }
            }

        }

        private class OnGeneratePomChangeBehavior extends AjaxFormComponentUpdatingBehavior {
            private OnGeneratePomChangeBehavior() {
                super("onclick");
            }

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (model.deployPom) {
                    model.pomChanged = false;
                    model.pomXml = deployService.getModelString(model.artifactInfo);
                }

                target.addComponent(getPomEditorContainer());
            }
        }

        private class OnPackTypeChangeBehavior extends OnGavcChangeBehavior {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                Component deployPomCheckbox = getPomEditorContainer().get("deployPom");
                boolean pomArtifact = isPomArtifact();
                deployPomCheckbox.setVisible(!pomArtifact);
                if (pomArtifact) {
                    model.deployPom = true;
                }
                super.onUpdate(target);
            }
        }

        private void finish(AjaxRequestTarget target) {
            cleanupResources();
            Component uploadPanel = new UploadArtifactPanel();
            DeployArtifactPanel.this.replaceWith(uploadPanel);
            target.addComponent(uploadPanel);
        }

        private class DeployLink extends TitledAjaxSubmitLink {
            private DeployLink(String id) {
                super(id, "Deploy Artifact", DeployArtifactForm.this);
                setOutputMarkupId(true);
            }

            @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                try {
                    //Make sure not to override a good pom.
                    boolean deployPom = !isPomExists() && model.deployPom && model.artifactInfo.isAutoCalculatePath();

                    if (deployPom) {
                        if (isPomArtifact()) {
                            deployPom();
                        } else {
                            deployFileAndPom();
                        }
                    } else {
                        deployFile();
                    }

                    info(String.format("Successfully deployed: %s into %s.", model.artifactInfo.getPath(),
                            model.targetRepo.getKey()));
                    AjaxUtils.refreshFeedback(target);
                    finish(target);

                } catch (Exception e) {
                    Throwable cause = ExceptionUtils.getRootCause(e);
                    if (cause instanceof BadPomException) {
                        log.warn("Failed to deploy artifact: {}", e.getMessage());
                    } else {
                        log.warn("Failed to deploy artifact.", e);
                    }
                    error(e.getMessage());
                    AjaxUtils.refreshFeedback(target);
                }
            }

            private void deployPom() throws Exception {
                if (model.pomChanged) {
                    savePomXml();
                }
                deployFile();
            }

            private void savePomXml() throws Exception {
                StringInputStream input = null;
                FileOutputStream output = null;
                try {
                    input = new StringInputStream(model.pomXml);
                    output = new FileOutputStream(model.file);
                    IOUtils.copy(input, output);
                } finally {
                    IOUtils.closeQuietly(input);
                    IOUtils.closeQuietly(output);
                }
            }

            private void deployFileAndPom() throws IOException, RepoAccessException {
                deployService.validatePom(model.pomXml, model.artifactInfo.getPath(),
                        model.targetRepo.isSuppressPomConsistencyChecks());
                deployService.deploy(model.targetRepo, model.artifactInfo, model.file, model.pomXml, true, false);
            }

            private void deployFile() throws RepoAccessException {
                deployService.deploy(model.targetRepo, model.artifactInfo, model.file, false, false);
            }
        }

        private class CancelLink extends TitledAjaxLink {
            private CancelLink(String id) {
                super(id, "Cancel");
            }

            public void onClick(AjaxRequestTarget target) {
                finish(target);
            }
        }
    }
}
