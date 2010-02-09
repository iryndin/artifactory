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
package org.artifactory.webapp.wicket.security.acls;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.repo.CentralConfig;
import org.artifactory.repo.Repo;
import org.artifactory.security.ExtendedJdbcAclDao;
import org.artifactory.security.RepoPath;
import org.artifactory.security.SecurityHelper;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextUtils;
import org.artifactory.utils.MutableBoolean;
import org.artifactory.webapp.wicket.components.DojoButton;
import org.artifactory.webapp.wicket.help.HelpBubble;
import org.artifactory.webapp.wicket.panel.TitlePanel;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class NewPermissionTargetPanel extends TitlePanel {

    private RepoPath permissionTarget;

    public NewPermissionTargetPanel(String string, final Component targetsTable,
                                    final WebMarkupContainer recipientsPanel) {
        super(string);
        //Add a feedback panel
        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        //Add the new permission target function
        Form form = new Form("newTargetForm", newPermissionTargetModel());
        form.setOutputMarkupId(true);

        //Repository
        CentralConfig cc = CentralConfig.get();
        List<Repo> repos = cc.getLocalAndRemoteRepositoriesList();
        List<String> repoKeys = new ArrayList<String>(repos.size());
        for (Repo repo : repos) {
            String repoKey = repo.getKey();
            repoKeys.add(repoKey);
        }
        final String defaultRepoKey = repoKeys.get(0);
        final DropDownChoice repoDdc = new DropDownChoice("repoKey", repoKeys) {
            @Override
            protected CharSequence getDefaultChoice(final Object selected) {
                return super.getDefaultChoice(defaultRepoKey);
            }
        };
        repoDdc.setPersistent(true);
        repoDdc.setOutputMarkupId(true);
        form.add(repoDdc);
        //Any-repo checkbox
        final MutableBoolean anyRepo = new MutableBoolean();
        AjaxCheckBox anyRepoCheckbox =
                new AjaxCheckBox("anyRepo", new PropertyModel(anyRepo, "value")) {
                    protected void onUpdate(AjaxRequestTarget target) {
                        boolean value = anyRepo.value();
                        if (value) {
                            permissionTarget.setRepoKey(RepoPath.ANY);
                        }
                        repoDdc.setEnabled(!value);
                        target.addComponent(repoDdc);
                    }
                };
        form.add(anyRepoCheckbox);

        //Path
        final RequiredTextField pathTf = new RequiredTextField("path");
        pathTf.setOutputMarkupId(true);
        form.add(pathTf);
        HelpBubble bubble = new HelpBubble("pathPrefixHelp",
                "Prefix for the path of the artifact in the repository " +
                        "(without a leading slash).<br/>For example: \"org/apache\"");
        form.add(bubble);

        //Any-path checkbox
        final MutableBoolean anyPath = new MutableBoolean();
        AjaxCheckBox anyPathCheckbox =
                new AjaxCheckBox("anyPath", new PropertyModel(anyPath, "value")) {
                    protected void onUpdate(AjaxRequestTarget target) {
                        boolean value = anyPath.value();
                        if (value) {
                            permissionTarget.setPath(RepoPath.ANY);
                        }
                        pathTf.setEnabled(!value);
                        target.addComponent(pathTf);
                    }
                };
        form.add(anyPathCheckbox);

        //Cancel
        DojoButton cancel = new DojoButton("cancel", form, "Cancel") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                form.setModel(newPermissionTargetModel());
                target.addComponent(form);
                target.addComponent(feedback);
                recipientsPanel.setVisible(false);
                target.addComponent(recipientsPanel);
            }
        };
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);
        //Submit
        DojoButton submit = new DojoButton("submit", form, "Create") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                super.onSubmit(target, form);
                ArtifactoryContext context = ContextUtils.getContext();
                SecurityHelper security = context.getSecurity();
                ExtendedJdbcAclDao aclDao = security.getAclDao();
                target.addComponent(feedback);
                //TODO: [by yl] Do we want to compute a parent path?
                try {
                    aclDao.createAclObjectIdentity(permissionTarget, null);
                } catch (DataIntegrityViolationException e) {
                    error("Permission target '" + permissionTarget.getId() + "' already exists.");
                    return;
                } catch (Exception e) {
                    error("Failed to create permissions target: " + e.getMessage());
                    return;
                }
                //Do this before we reset the model
                info("Permission target '" + permissionTarget.getId() + "' created successfully.");
                //Rerender the table
                targetsTable.modelChanged();
                target.addComponent(targetsTable);
                form.setModel(newPermissionTargetModel());
                target.addComponent(form);
                recipientsPanel.setVisible(false);
                target.addComponent(recipientsPanel);
            }

            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(feedback);
            }
        };
        form.add(submit);
        add(form);
    }

    private CompoundPropertyModel newPermissionTargetModel() {
        permissionTarget = new RepoPath();
        return new CompoundPropertyModel(new Model(permissionTarget));
    }
}
