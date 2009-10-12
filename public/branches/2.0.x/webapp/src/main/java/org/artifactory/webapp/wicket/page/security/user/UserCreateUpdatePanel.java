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
package org.artifactory.webapp.wicket.page.security.user;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.webapp.wicket.common.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.webapp.wicket.common.component.CreateUpdateAction;
import org.artifactory.webapp.wicket.common.component.CreateUpdatePanel;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.common.component.checkbox.styled.StyledCheckbox;
import org.artifactory.webapp.wicket.common.component.deletable.listview.DeletableLabelGroup;
import org.artifactory.webapp.wicket.common.component.help.HelpBubble;
import org.artifactory.webapp.wicket.common.component.modal.ModalHandler;
import org.artifactory.webapp.wicket.common.component.modal.links.ModalCloseLink;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.utils.validation.JcrNameValidator;
import org.artifactory.webapp.wicket.utils.validation.PasswordStreangthValidator;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class UserCreateUpdatePanel extends CreateUpdatePanel<UserModel> {

    @SpringBean
    private UserGroupService userGroupService;

    PasswordTextField passwordField;
    PasswordTextField retypedPasswordField;

    public UserCreateUpdatePanel(CreateUpdateAction action, UserModel user, final UsersTable usersListTable) {
        super(action, user);
        setWidth(380);

        form.setOutputMarkupId(true);
        add(form);

        TitledBorder border = new TitledBorder("border");
        form.add(border);

        final boolean create = isCreate();

        //Username
        RequiredTextField usernameTf = new RequiredTextField("username");
        usernameTf.setEnabled(create);
        usernameTf.add(new JcrNameValidator("Invalid username '%s'"));
        border.add(usernameTf);

        //Password
        passwordField = new PasswordTextField("password");
        passwordField.setRequired(create);
        passwordField.add(PasswordStreangthValidator.getInstance());
        border.add(passwordField);
        retypedPasswordField = new PasswordTextField("retypedPassword");
        retypedPasswordField.setRequired(create);
        border.add(retypedPasswordField);

        // validate password and retyped password
        form.add(new EqualPasswordInputValidator(passwordField, retypedPasswordField) {
            @Override
            public void validate(Form form) {
                if (entity.isDisableInternalPassword()) {
                    // no need to validate passwords if internal passwords are disabled
                    return;
                }
                if (!create && StringUtils.hasText(passwordField.getModelObjectAsString())) {
                    return;
                }
                super.validate(form);
            }
        });

        //Email
        RequiredTextField emailTf = new RequiredTextField("email");
        emailTf.add(EmailAddressValidator.getInstance());
        border.add(emailTf);

        //Admin
        border.add(new StyledCheckbox("admin").setLabel(new Model("Admin")));

        //Can update profile
        border.add(new StyledCheckbox("updatableProfile"));

        // Internal password
        final StyledCheckbox disableInternalPassword = new StyledCheckbox("disableInternalPassword");
        disableInternalPassword.setEnabled(!create);    // disable if creating new user
        disableInternalPassword.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (disableInternalPassword.isChecked()) {
                    disablePasswordFields();
                } else {
                    enablePasswordFields();
                }
                target.addComponent(form);
            }
        });
        border.add(disableInternalPassword);
        StringResourceModel helpMessage = new StringResourceModel("disableInternalPasswordHelp", this, null);
        border.add(new HelpBubble("disableInternalPasswordHelp", helpMessage));

        if (!create && user.isDisableInternalPassword()) {
            disablePasswordFields();
        }

        // groups
        Set<String> userGroups = user.getGroups();
        final DeletableLabelGroup<String> groupsListView = new DeletableLabelGroup<String>("groups", userGroups);
        groupsListView.setLabelClickable(false);
        groupsListView.setVisible(!create);
        border.add(groupsListView);
        String groupsLabelText = "Groups";
        if ((userGroups == null) || (userGroups.isEmpty())) {
            groupsLabelText = "User has no group memberships";
        }
        Label groupsLabel = new Label("groupsLabel", groupsLabelText);
        groupsLabel.setVisible(!create);
        border.add(groupsLabel);

        //Cancel
        form.add(new ModalCloseLink("cancel"));

        //Submit
        String submitCaption = create ? "Create" : "Save";
        SimpleButton submit = new SimpleButton("submit", form, submitCaption) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                String username = entity.getUsername();
                boolean successful = true;
                if (create) {
                    successful = createNewUser(username);
                } else {
                    updateUser(username);
                }
                if (successful) {
                    usersListTable.refreshUsersList(target);
                    FeedbackUtils.refreshFeedback(target);
                    ModalHandler.closeCurrent(target);
                }
            }

            private boolean createNewUser(String username) {
                UserInfo newUser = new UserInfo(
                        username, DigestUtils.md5Hex(entity.getPassword()), entity.getEmail(),
                        entity.isAdmin(), true, entity.isUpdatableProfile(), true, true, true);
                newUser.setGroups(new HashSet<String>(groupsListView.getData()));
                boolean created = userGroupService.createUser(newUser);
                if (!created) {
                    error("User '" + username + "' already exists.");
                } else {
                    getPage().info("User '" + username + "' successfully created.");
                }
                return created;
            }

            private void updateUser(String username) {
                // get the user info from the database and update it from the model
                UserInfo userInfo = userGroupService.findUser(username);
                userInfo.setEmail(entity.getEmail());
                userInfo.setAdmin(entity.isAdmin());
                userInfo.setUpdatableProfile(entity.isUpdatableProfile());
                userInfo.setGroups(new HashSet<String>(groupsListView.getData()));
                if (entity.isDisableInternalPassword()) {
                    // user should authentiate externally - set password to invalid
                    userInfo.setPassword(UserInfo.INVALID_PASSWORD);
                } else if (StringUtils.hasText(entity.getPassword())) {
                    userInfo.setPassword(DigestUtils.md5Hex(entity.getPassword()));
                }
                userGroupService.updateUser(userInfo);
                getPage().info("User '" + username + "' successfully updated.");
            }
        };
        form.add(submit);
        form.add(new DefaultButtonBehavior(submit));
    }

    private void enablePasswordFields() {
        passwordField.setEnabled(true);
        retypedPasswordField.setEnabled(true);
    }

    private void disablePasswordFields() {
        passwordField.setEnabled(false);
        retypedPasswordField.setEnabled(false);
    }

}
