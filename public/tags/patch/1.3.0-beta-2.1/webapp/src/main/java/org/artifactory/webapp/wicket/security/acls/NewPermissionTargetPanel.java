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

import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.config.CentralConfig;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.security.ArtifactorySecurityManager;
import org.artifactory.security.ExtendedAclService;
import org.artifactory.security.SecuredRepoPath;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.artifactory.utils.MutableBoolean;
import org.artifactory.webapp.wicket.components.SimpleButton;
import org.artifactory.webapp.wicket.components.panel.FeedbackEnabledPanel;
import org.artifactory.webapp.wicket.help.HelpBubble;
import org.springframework.security.acls.AlreadyExistsException;

import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class NewPermissionTargetPanel extends FeedbackEnabledPanel {

    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(NewPermissionTargetPanel.class);

    private SecuredRepoPath permissionTarget;

    public NewPermissionTargetPanel(String string, final Component targetsTable,
                                    final WebMarkupContainer recipientsPanel) {
        super(string);

        //Add the new permission target function
        Form form = new Form("newTargetForm", newPermissionTargetModel());
        form.setOutputMarkupId(true);

        //Repository
        CentralConfig cc = CentralConfig.get();
        VirtualRepo virtualRepo = cc.getGlobalVirtualRepo();
        final List<LocalRepo> repos = virtualRepo.getLocalAndCachedRepositories();
        IChoiceRenderer choiceRenderer = new IChoiceRenderer() {
            public Object getDisplayValue(Object object) {
                LocalRepo repo = (LocalRepo) object;
                //For cache repos display the short name of the remote
                if (repo.isCache()) {
                    LocalCacheRepo cacheRepo = (LocalCacheRepo) repo;
                    return cacheRepo.getRemoteRepo().getKey();
                } else {
                    return repo.getKey();
                }
            }

            public String getIdValue(Object object, int index) {
                if (object instanceof String) {
                    return object.toString();
                } else {
                    RealRepo repo = (RealRepo) object;
                    return repo.getKey();
                }
            }
        };
        //Drop-down choce of target repositories
        final DropDownChoice repoDdc = new DropDownChoice("repoKey", repos, choiceRenderer) {
            @Override
            protected CharSequence getDefaultChoice(final Object selected) {
                return super.getDefaultChoice(repos.get(0));
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
                            permissionTarget.setRepoKey(SecuredRepoPath.ANY);
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
                            permissionTarget.setPath(SecuredRepoPath.ANY);
                        }
                        pathTf.setEnabled(!value);
                        target.addComponent(pathTf);
                    }
                };
        form.add(anyPathCheckbox);

        //Cancel
        SimpleButton cancel = new SimpleButton("cancel", form, "Cancel") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                form.setModel(newPermissionTargetModel());
                target.addComponent(form);
                target.addComponent(getFeedback());
                recipientsPanel.setVisible(false);
                target.addComponent(recipientsPanel);
            }
        };
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);

        //Create
        SimpleButton submit = new SimpleButton("submit", form, "Create") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                ArtifactoryContext context = ContextHelper.get();
                ArtifactorySecurityManager security = context.getSecurity();
                ExtendedAclService aclService = security.getAclService();
                target.addComponent(getFeedback());
                try {
                    aclService.createAcl(permissionTarget);
                } catch (AlreadyExistsException e) {
                    String msg = "Permission target '" + permissionTarget.getIdentifier() +
                            "' already exists.";
                    error(msg);
                    LOGGER.error(msg, e);
                    return;
                } catch (Exception e) {
                    String msg = "Failed to create permissions target: " + e.getMessage();
                    error(msg);
                    LOGGER.error(msg, e);
                    return;
                }
                //Do this before we reset the model
                info("Permission target '" + permissionTarget.getIdentifier() +
                        "' created successfully.");
                //Rerender the table
                targetsTable.modelChanged();
                target.addComponent(targetsTable);
                form.setModel(newPermissionTargetModel());
                target.addComponent(form);
                recipientsPanel.setVisible(false);
                target.addComponent(recipientsPanel);
            }

            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(getFeedback());
            }
        };
        form.add(submit);
        add(form);
    }

    private CompoundPropertyModel newPermissionTargetModel() {
        permissionTarget = new SecuredRepoPath();
        return new CompoundPropertyModel(new Model(permissionTarget));
    }
}
