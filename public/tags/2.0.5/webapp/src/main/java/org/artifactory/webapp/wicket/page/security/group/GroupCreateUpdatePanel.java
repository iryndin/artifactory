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
package org.artifactory.webapp.wicket.page.security.group;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.webapp.wicket.common.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.webapp.wicket.common.component.CreateUpdateAction;
import org.artifactory.webapp.wicket.common.component.CreateUpdatePanel;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.common.component.checkbox.styled.StyledCheckbox;
import org.artifactory.webapp.wicket.common.component.modal.ModalHandler;
import org.artifactory.webapp.wicket.common.component.modal.links.ModalCloseLink;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.utils.validation.JcrNameValidator;

/**
 * @author Yossi Shaul
 */
public class GroupCreateUpdatePanel extends CreateUpdatePanel<GroupInfo> {

    @SpringBean
    private UserGroupService groupService;

    public GroupCreateUpdatePanel(CreateUpdateAction action, GroupInfo groupInfo,
            GroupsListPanel groupsListPanel) {
        super(action, groupInfo);
        setWidth(440);

        add(form);

        TitledBorder border = new TitledBorder("border");
        add(border);
        form.add(border);

        // Group name
        RequiredTextField groupNameTf = new RequiredTextField("groupName");
        groupNameTf.setEnabled(isCreate());// don't allow groupname update
        groupNameTf.add(new JcrNameValidator("Invalid group name '%s'"));
        border.add(groupNameTf);

        // Group description
        TextArea groupDescriptionTextArea = new TextArea("description");
        border.add(groupDescriptionTextArea);

        // If default for newly created users
        border.add(new StyledCheckbox("newUserDefault"));

        // Cancel button
        form.add(new ModalCloseLink("cancel"));

        // Submit button
        SimpleButton submit = createSubmitButton(groupsListPanel);
        form.add(submit);
        form.add(new DefaultButtonBehavior(submit));
    }

    private SimpleButton createSubmitButton(final GroupsListPanel groupsListPanel) {
        String submitCaption = isCreate() ? "Create" : "Save";
        SimpleButton submit = new SimpleButton("submit", form, submitCaption) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                boolean hasError = false;
                if (isCreate()) {
                    hasError = onCreate();
                } else {
                    onUpdate();
                }
                FeedbackUtils.refreshFeedback(target);
                if (!hasError) {
                    // close panel and refresh
                    target.addComponent(groupsListPanel);
                    ModalHandler.closeCurrent(target);
                }
            }
        };
        return submit;
    }

    private boolean onCreate() {
        GroupInfo group = getGroupInfo();
        boolean created = groupService.createGroup(group);
        if (!created) {
            error("Group '" + group.getGroupName() + "' already exists.");
            return true;    // has error
        } else {
            getPage().info("Group '" + group.getGroupName() + "' successfully created.");
            return false;   // no error
        }
    }

    private void onUpdate() {
        groupService.updateGroup(getGroupInfo());
        getPage().info("Group '" + getGroupInfo().getGroupName() + "' successfully updated.");
    }

    private GroupInfo getGroupInfo() {
        return (GroupInfo) form.getModelObject();
    }
}