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
package org.artifactory.webapp.wicket.page.config.repos;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.webapp.wicket.common.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.webapp.wicket.common.component.CreateUpdateAction;
import org.artifactory.webapp.wicket.common.component.CreateUpdatePanel;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.common.component.modal.links.ModalCloseLink;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.utils.validation.JcrNameValidator;
import org.artifactory.webapp.wicket.utils.validation.UniqueXmlIdValidator;
import org.artifactory.webapp.wicket.utils.validation.XsdNCNameValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Base panel for repositories configuration.
 *
 * @author Yossi Shaul
 */
public abstract class RepoConfigCreateUpdatePanel<E extends RepoDescriptor>
        extends CreateUpdatePanel<E> {

    @SpringBean
    private CentralConfigService centralConfigService;

    protected RepoConfigCreateUpdatePanel(CreateUpdateAction action, E repoDescriptor) {
        super(action, repoDescriptor);

        setWidth(550);

        TitledBorder commonFields = new TitledBorder("commonFields");
        form.add(commonFields);

        // Repository name
        RequiredTextField repoKeyField = new RequiredTextField("key");
        repoKeyField.setEnabled(isCreate());// don't allow key update
        if (isCreate()) {
            repoKeyField.add(JcrNameValidator.getInstance());
            repoKeyField.add(XsdNCNameValidator.getInstance());
            repoKeyField.add(new UniqueXmlIdValidator(getEditingDescriptor()));
        }

        commonFields.add(repoKeyField);
        commonFields.add(new SchemaHelpBubble("key.help"));

        // Repository description
        commonFields.add(new TextArea("description"));
        commonFields.add(new SchemaHelpBubble("description.help"));

        // Cancel button
        form.add(new ModalCloseLink("cancel"));

        // Submit button
        SimpleButton submit = createSubmitButton();
        form.add(submit);
        form.add(new DefaultButtonBehavior(submit));

        add(form);
    }

    public abstract void handleCreate(CentralConfigDescriptor descriptor);

    private SimpleButton createSubmitButton() {
        String submitCaption = isCreate() ? "Create" : "Save";
        return new SimpleButton("submit", form, submitCaption) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                if (isCreate()) {
                    handleCreate(getEditingDescriptor());
                    centralConfigService.saveEditedDescriptorAndReload();
                    getPage().info("Repository '" + getRepoDescriptor().getKey() + "' successfully created.");
                } else {
                    handleUpdate();
                }

                ((RepositoryConfigPage) getPage()).refresh(target);
                FeedbackUtils.refreshFeedback(target);
                close(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                super.onError(target, form);
                FeedbackUtils.refreshFeedback(target);
            }
        };
    }

    private void handleUpdate() {
        centralConfigService.saveEditedDescriptorAndReload();
        getPage().info("Repository '" + getRepoDescriptor().getKey() + "' successfully updated.");
    }

    @SuppressWarnings({"unchecked"})
    protected E getRepoDescriptor() {
        return (E) form.getModelObject();
    }

    protected List<RepoDescriptor> getRepos() {
        MutableCentralConfigDescriptor mutableDescriptor = getEditingDescriptor();
        List<RepoDescriptor> result = new ArrayList<RepoDescriptor>();
        result.addAll(mutableDescriptor.getLocalRepositoriesMap().values());
        result.addAll(mutableDescriptor.getRemoteRepositoriesMap().values());
        result.addAll(mutableDescriptor.getVirtualRepositoriesMap().values());
        return result;
    }

    protected MutableCentralConfigDescriptor getEditingDescriptor() {
        return centralConfigService.getDescriptorForEditing();
    }

}