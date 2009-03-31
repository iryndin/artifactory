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
package org.artifactory.webapp.wicket.page.security.acl;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AclService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.util.AlreadyExistsException;
import org.artifactory.webapp.wicket.common.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.webapp.wicket.common.component.CreateUpdateAction;
import org.artifactory.webapp.wicket.common.component.CreateUpdatePanel;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.SimpleLink;
import org.artifactory.webapp.wicket.common.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.common.component.help.HelpBubble;
import org.artifactory.webapp.wicket.common.component.modal.ModalHandler;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Yoav Landman
 * @author Yoav Aharoni
 */
public class PermissionTargetCreateUpdatePanel extends CreateUpdatePanel<PermissionTargetInfo> {
    private static final Logger log =
            LoggerFactory.getLogger(PermissionTargetCreateUpdatePanel.class);

    @SpringBean
    private RepositoryService repositoryService;

    @SpringBean
    private AclService aclService;

    @SpringBean
    private AuthorizationService authService;

    private PermissionTargetRecipientsPanel recipientsPanel;

    public PermissionTargetCreateUpdatePanel(
            CreateUpdateAction action, PermissionTargetInfo target,
            final Component targetsTable) {
        super(action, target);
        setWidth(740);

        form.setOutputMarkupId(true);
        add(form);

        TitledBorder border = new TitledBorder("border");
        form.add(border);

        // add the recipients table or empty container if create action
        if (isCreate()) {
            border.add(new WebMarkupContainer("recipients"));
        } else {
            recipientsPanel = new PermissionTargetRecipientsPanel("recipients", target);
            border.add(recipientsPanel);
        }

        addPermissionTargetNameField(border);
        addRepositoriesDropDownChoice(border);

        StringResourceModel helpMessage = new StringResourceModel("help.patterns", this, null);
        List<CommonPathPattern> includesExcludesSuggestions = Arrays.asList(CommonPathPattern.values());

        addIncludesPatternFields(helpMessage, includesExcludesSuggestions, border);
        addExcludesPatternFields(helpMessage, includesExcludesSuggestions, border);

        addCancelButton();
        addSubmitButton(targetsTable);

    }

    private void addPermissionTargetNameField(TitledBorder border) {
        RequiredTextField nameTf = new RequiredTextField("name");
        border.add(nameTf);
        if (!isSystemAdmin() || !isCreate()) {
            nameTf.setEnabled(false);
        }
    }

    private void addRepositoriesDropDownChoice(TitledBorder border) {
        //Drop-down choice of target repositories
        List<String> repoKeys = getRepositoriesKeys();
        DropDownChoice repoDdc = new DropDownChoice("repoKey", repoKeys);
        repoDdc.setOutputMarkupId(true);
        repoDdc.setRequired(true);
        repoDdc.setEnabled(isSystemAdmin());
        border.add(repoDdc);
    }

    private void addIncludesPatternFields(StringResourceModel helpMessage,
            final List<CommonPathPattern> includesExcludesSuggestions, TitledBorder border) {
        TextArea includesTa = new TextArea("includesPattern");
        includesTa.setEnabled(isSystemAdmin());
        includesTa.setOutputMarkupId(true);
        border.add(includesTa);

        border.add(new HelpBubble("includesHelp", helpMessage));

        Model include = new Model();
        DropDownChoice includesSuggest =
                new DropDownChoice("includesSuggest", include, includesExcludesSuggestions) {
                    @Override
                    protected CharSequence getDefaultChoice(Object selected) {
                        return includesExcludesSuggestions.get(0).getDisplayName();
                    }
                };
        includesSuggest.add(new UpdatePatternsBehavior(include, includesTa));
        if (isCreate()) {
            includesSuggest.setModelObject(CommonPathPattern.ANY);
        }
        includesSuggest.setEnabled(isSystemAdmin());
        border.add(includesSuggest);
    }

    private void addExcludesPatternFields(StringResourceModel helpMessage,
            final List<CommonPathPattern> includesExcludesSuggestions, TitledBorder border) {
        TextArea excludesTa = new TextArea("excludesPattern");
        excludesTa.setEnabled(isSystemAdmin());
        excludesTa.setOutputMarkupId(true);
        border.add(excludesTa);

        border.add(new HelpBubble("excludesHelp", helpMessage));

        //Excludes suggestions
        Model exclude = new Model();
        DropDownChoice excludesSuggest =
                new DropDownChoice("excludesSuggest", exclude, includesExcludesSuggestions) {
                    @Override
                    protected CharSequence getDefaultChoice(Object selected) {
                        return includesExcludesSuggestions.get(0).getDisplayName();
                    }
                };
        excludesSuggest.add(new UpdatePatternsBehavior(exclude, excludesTa));
        excludesSuggest.setEnabled(isSystemAdmin());
        border.add(excludesSuggest);
    }

    private void addCancelButton() {
        SimpleLink cancel = new SimpleLink("cancel", "Cancel") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (recipientsPanel != null) {
                    recipientsPanel.cancel();
                }
                ModalHandler.closeCurrent(target);
            }
        };
        form.add(cancel);
    }

    private void addSubmitButton(final Component targetsTable) {
        String submitCaption = isCreate() ? "Create" : "Save";
        SimpleButton submit = new SimpleButton("submit", form, submitCaption) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                final String name = entity.getName();
                if (isCreate()) {
                    try {
                        aclService.createAcl(entity);
                    } catch (Exception e) {
                        String msg;
                        if (e instanceof AlreadyExistsException) {
                            //Workaround acegi's annoyances
                            msg = "Permission target '" + name + "' already exists.";
                        } else {
                            msg = "Failed to create permissions target: " + e.getMessage();
                            log.error(msg, e);
                        }
                        getPage().error(msg);
                        FeedbackUtils.refreshFeedback(target);
                        return;
                    }
                    getPage().info("Permission target '" + name + "' created successfully.");
                } else {
                    try {
                        recipientsPanel.getAclInfo().setPermissionTarget(entity);
                        recipientsPanel.save();
                        getPage().info("Permission target '" + name + "' updated successfully.");
                        target.addComponent(PermissionTargetCreateUpdatePanel.this);
                    } catch (Exception e) {
                        String msg = "Failed to update permissions target: " + e.getMessage();
                        log.error(msg, e);
                        getPage().error(msg);
                        FeedbackUtils.refreshFeedback(target);
                        return;
                    }
                }
                //Close the modal window and re-render the table
                targetsTable.modelChanged();
                target.addComponent(targetsTable);
                FeedbackUtils.refreshFeedback(target);
                ModalHandler.closeCurrent(target);
            }
        };
        form.add(submit);
        form.add(new DefaultButtonBehavior(submit));
    }

    private List<String> getRepositoriesKeys() {
        ArrayList<String> reposKeys = new ArrayList<String>();
        // add the logical "ANY" repository
        reposKeys.add(0, PermissionTargetInfo.ANY_REPO);

        // add the keys of all the configured repositories
        List<LocalRepoDescriptor> repos = repositoryService.getLocalAndCachedRepoDescriptors();
        for (RepoDescriptor repo : repos) {
            reposKeys.add(repo.getKey());
        }
        return reposKeys;
    }

    void enableFields() {
        throw new UnsupportedOperationException("stop being lazy and implement me");
    }

    private static class UpdatePatternsBehavior extends AjaxFormComponentUpdatingBehavior {
        private final Model comboBoxModel;
        private final Component textArea;

        public UpdatePatternsBehavior(Model comboBoxModel, TextArea textArea) {
            super("onChange");
            this.comboBoxModel = comboBoxModel;
            this.textArea = textArea;
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
            CommonPathPattern commonPathPattern = (CommonPathPattern) comboBoxModel.getObject();
            String pattern = commonPathPattern.getPattern();
            String existingPattern = textArea.getModelObjectAsString();
            if (CommonPathPattern.NONE.equals(commonPathPattern) ||
                    !StringUtils.hasText(existingPattern)) {
                textArea.setModelObject(pattern);
            } else {
                textArea.setModelObject(existingPattern + ", " + pattern);
            }
            target.addComponent(textArea);
        }
    }

    /**
     * @return True if the current user is a system admin (not just the current permission target admin). Non system
     *         admins can only change the receipients table.
     */
    private boolean isSystemAdmin() {
        return authService.isAdmin();
    }

    @Override
    public String getCookieName() {
        return null;
    }
}
