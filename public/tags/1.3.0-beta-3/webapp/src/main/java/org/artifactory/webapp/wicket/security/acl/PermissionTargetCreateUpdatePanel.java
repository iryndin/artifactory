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
package org.artifactory.webapp.wicket.security.acl;

import org.apache.log4j.Logger;
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
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.webapp.wicket.component.CreateUpdatePanel;
import org.artifactory.webapp.wicket.component.SimpleButton;
import org.artifactory.webapp.wicket.help.HelpBubble;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Yoav Landman
 * @author Yoav Aharoni
 */
public class PermissionTargetCreateUpdatePanel extends CreateUpdatePanel<PermissionTargetInfo> {
    private final static Logger LOGGER = Logger.getLogger(PermissionTargetCreateUpdatePanel.class);

    @SpringBean
    private RepositoryService repositoryService;

    @SpringBean
    private AclService aclService;

    @SpringBean
    private AuthorizationService authService;

    public PermissionTargetCreateUpdatePanel(
            CreateUpdateAction action, PermissionTargetInfo target,
            final Component targetsTable) {
        super(action, target);
        form.setOutputMarkupId(true);

        //Add the new permission target function
        if (action.equals(CreateUpdateAction.CREATE)) {
            form.add(new WebMarkupContainer("recipients"));
        } else {
            form.add(new PermissionTargetRecipientsPanel("recipients", target));
        }

        addPermissionTargetNameField();
        addRepositoriesDropDownChoice();

        StringResourceModel helpMessage = new StringResourceModel("help.patterns", this, null);
        List<CommonPathPattern> includesExcludesSuggestions =
                Arrays.asList(CommonPathPattern.values());

        addIncludesPatternFields(helpMessage, includesExcludesSuggestions);
        addExcludesPatternFields(helpMessage, includesExcludesSuggestions);

        addCancelButton();
        addSubmitButton(targetsTable);

        add(form);
    }

    private void addPermissionTargetNameField() {
        RequiredTextField nameTf = new RequiredTextField("name");
        form.add(nameTf);
        if (!isSystemAdmin() || !isCreate()) {
            nameTf.setEnabled(false);
        }
    }

    private void addRepositoriesDropDownChoice() {
        //Drop-down choice of target repositories
        List<String> repoKeys = getRepositoriesKeys();
        DropDownChoice repoDdc = new DropDownChoice("repoKey", repoKeys);
        repoDdc.setOutputMarkupId(true);
        repoDdc.setRequired(true);
        repoDdc.setEnabled(isSystemAdmin());
        form.add(repoDdc);
    }

    private void addIncludesPatternFields(StringResourceModel helpMessage,
            final List<CommonPathPattern> includesExcludesSuggestions) {
        TextArea includesTa = new TextArea("includesPattern");
        includesTa.setEnabled(isSystemAdmin());
        includesTa.setOutputMarkupId(true);
        form.add(includesTa);

        form.add(new HelpBubble("includesHelp", helpMessage));


        Model include = new Model();
        DropDownChoice includesSuggest =
                new DropDownChoice("includesSuggest", include, includesExcludesSuggestions) {
                    @Override
                    protected CharSequence getDefaultChoice(Object selected) {
                        return includesExcludesSuggestions.get(0).getDisplayName();
                    }
                };
        includesSuggest.add(new UpdatePatternsBehavior(include, includesTa));
        includesSuggest.setEnabled(isSystemAdmin());
        form.add(includesSuggest);
    }

    private void addExcludesPatternFields(StringResourceModel helpMessage,
            final List<CommonPathPattern> includesExcludesSuggestions) {
        TextArea excludesTa = new TextArea("excludesPattern");
        excludesTa.setEnabled(isSystemAdmin());
        excludesTa.setOutputMarkupId(true);
        form.add(excludesTa);

        form.add(new HelpBubble("excludesHelp", helpMessage));

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
        form.add(excludesSuggest);
    }

    private void addCancelButton() {
        SimpleButton cancel = new SimpleButton("cancel", form, "Cancel") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                PermissionTargetRecipientsPanel panel = getPermissionTargetRecipientsPanel();
                if (panel != null) {
                    panel.cancel();
                }

                //Back to create
                PermissionTargetCreateUpdatePanel.this.replaceWith(
                        target, (getAclPage().newCreatePanel()));
            }
        };
        cancel.setDefaultFormProcessing(false);
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
                        String msg =
                                "Failed to create permissions target: " + e.getMessage();
                        error(msg);
                        LOGGER.error(msg, e);
                        return;
                    }
                    info("Permission target '" + name + "' created successfully.");
                    PermissionTargetCreateUpdatePanel.this.replaceWith(
                            target, (getAclPage().newCreatePanel()));
                } else {
                    try {
                        PermissionTargetRecipientsPanel panel =
                                getPermissionTargetRecipientsPanel();
                        if (panel != null) {
                            panel.getAclInfo().setPermissionTarget(entity);
                            panel.save();
                        } else {
                            aclService.updateAcl(entity);
                        }
                        info("Permission target '" + name + "' updsated successfully.");
                        target.addComponent(PermissionTargetCreateUpdatePanel.this);
                    } catch (Exception e) {
                        String msg =
                                "Failed to update permissions target: " + e.getMessage();
                        error(msg);
                        LOGGER.error(msg, e);
                        return;
                    }

                }
                //Rerender the table
                targetsTable.modelChanged();
                target.addComponent(targetsTable);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(getFeedback());
            }
        };
        form.add(submit);
    }

    private List<String> getRepositoriesKeys() {
        ArrayList<String> reposKeys = new ArrayList<String>();
        // add the logical "ANY" repository
        reposKeys.add(0, PermissionTargetInfo.ANY_REPO);

        // add the keys of all the configured repositories
        List<RepoDescriptor> repos = repositoryService.getLocalAndRemoteRepoDescriptors();
        for (RepoDescriptor repo : repos) {
            reposKeys.add(repo.getKey());
        }
        return reposKeys;
    }

    public PermissionTargetRecipientsPanel getPermissionTargetRecipientsPanel() {
        Component component = form.get("recipients");
        if (component instanceof PermissionTargetRecipientsPanel) {
            return (PermissionTargetRecipientsPanel) component;
        }
        return null;
    }

    private AclsPage getAclPage() {
        return (AclsPage) getPage();
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
     * @return True if the current user is a system admin (not just the current permission target
     *         admin). Non system admins can only change the receipients table.
     */
    private boolean isSystemAdmin() {
        return authService.isAdmin();
    }
}
