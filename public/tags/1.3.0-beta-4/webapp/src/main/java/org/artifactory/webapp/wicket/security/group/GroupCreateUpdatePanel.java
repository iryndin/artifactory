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
package org.artifactory.webapp.wicket.security.group;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.webapp.wicket.component.CancelButton;
import org.artifactory.webapp.wicket.component.CreateUpdatePanel;
import org.artifactory.webapp.wicket.component.SimpleButton;
import org.artifactory.webapp.wicket.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.utils.validation.JcrNameValidator;

/**
 * @author Yossi Shaul
 */
public class GroupCreateUpdatePanel extends CreateUpdatePanel<GroupInfo> {

    @SpringBean
    private UserGroupService groupService;

    public GroupCreateUpdatePanel(CreateUpdateAction action, GroupInfo groupInfo,
            final GroupsListPanel groupsListPanel) {
        super(action, groupInfo);

        // Group name
        RequiredTextField groupNameTf = new RequiredTextField("groupName");
        groupNameTf.setEnabled(isCreate());// don't allow groupname update
        groupNameTf.add(JcrNameValidator.getInstance());
        form.add(groupNameTf);

        // Group description
        TextArea groupDescriptionTextArea = new TextArea("description");
        form.add(groupDescriptionTextArea);

        // Cancel button
        form.add(new CancelButton(form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                super.onSubmit(target, form);
                //Back to create
                GroupCreateUpdatePanel.this.replaceWith(target, getGroupsPage().newCreatePanel());
            }
        });

        // Submit button
        form.add(createSubmitButton(groupsListPanel));

        add(form);
    }

    private SimpleButton createSubmitButton(final GroupsListPanel groupsListPanel) {
        String submitCaption = isCreate() ? "Create" : "Save";
        SimpleButton submit = new SimpleButton("submit", form, submitCaption) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                if (isCreate()) {
                    onCreate();
                } else {
                    onUpdate();
                }
                GroupCreateUpdatePanel.this.replaceWith(target, getGroupsPage().newCreatePanel());
                groupsListPanel.refreshGroups();

                FeedbackUtils.refreshFeedback(target);
                target.addComponent(groupsListPanel);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                FeedbackUtils.refreshFeedback(target);
            }
        };
        return submit;
    }

    private void onCreate() {
        GroupInfo group = getGroupInfo();
        boolean created = groupService.createGroup(group);
        if (!created) {
            error("Group '" + group.getGroupName() + "' already exists.");
        } else {
            info("Group '" + group.getGroupName() + "' successfully created.");
        }
    }

    private void onUpdate() {
        groupService.updateGroup(getGroupInfo());
        info("Group '" + getGroupInfo().getGroupName() + "' successfully updated.");
    }

    private GroupsPage getGroupsPage() {
        return (GroupsPage) getPage();
    }

    private GroupInfo getGroupInfo() {
        return (GroupInfo) form.getModelObject();
    }
}