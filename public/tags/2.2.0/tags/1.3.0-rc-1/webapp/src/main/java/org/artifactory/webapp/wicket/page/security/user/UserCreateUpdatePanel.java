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
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.Model;
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
import org.artifactory.webapp.wicket.common.component.modal.ModalHandler;
import org.artifactory.webapp.wicket.common.component.modal.links.ModalCloseLink;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.utils.validation.JcrNameValidator;
import org.artifactory.webapp.wicket.utils.validation.PasswordStreangthValidator;
import org.springframework.util.StringUtils;

import java.util.HashSet;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class UserCreateUpdatePanel extends CreateUpdatePanel<UserModel> {

    @SpringBean
    private UserGroupService userGroupService;

    public UserCreateUpdatePanel(CreateUpdateAction action, UserModel user, final UsersTable usersListTable) {
        super(action, user);
        setWidth(380);

        final boolean create = isCreate();

        add(form);

        TitledBorder border = new TitledBorder("border");
        form.add(border);

        //Username
        RequiredTextField usernameTf = new RequiredTextField("username");
        usernameTf.setEnabled(create);
        usernameTf.add(new JcrNameValidator("Invalid username '%s'"));
        border.add(usernameTf);

        //Password
        final PasswordTextField password = new PasswordTextField("password");
        password.setRequired(create);
        password.add(PasswordStreangthValidator.getInstance());
        border.add(password);
        PasswordTextField retypedPassword = new PasswordTextField("retypedPassword");
        retypedPassword.setRequired(create);
        border.add(retypedPassword);

        // validate password and retyped password
        form.add(new EqualPasswordInputValidator(password, retypedPassword) {
            @Override
            public void validate(Form form) {
                //For updates do not validate empty passwords
                if (!create && StringUtils.hasText(password.getModelObjectAsString())) {
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
        border.add(
                new StyledCheckbox("updatableProfile").setLabel(new Model("Can Update Profile")));

        // groups
        final DeletableLabelGroup<String> groupsListView =
                new DeletableLabelGroup<String>("groups", user.getGroups());
        groupsListView.setLabelClickable(false);
        border.add(groupsListView);

        //Cancel
        form.add(new ModalCloseLink("cancel"));

        //Submit
        String submitCaption = create ? "Create" : "Save";
        SimpleButton submit = new SimpleButton("submit", form, submitCaption) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                //Do this in a separate tx so that we see our change in index search results
                String username = entity.getUsername();
                if (create) {
                    UserInfo newUser = new UserInfo(
                            username, DigestUtils.md5Hex(entity.getPassword()), entity.getEmail(),
                            entity.isAdmin(), true, entity.isUpdatableProfile(), true, true, true);
                    newUser.setGroups(new HashSet<String>(groupsListView.getData()));
                    boolean created = userGroupService.createUser(newUser);
                    if (!created) {
                        error("User '" + username + "' already exists.");
                        return;
                    } else {
                        info("User '" + username + "' successfully created.");
                    }
                } else {// upadte user info
                    // get the user info from the database and update it from the model
                    UserInfo userInfo = userGroupService.findUser(username);
                    userInfo.setEmail(entity.getEmail());
                    userInfo.setAdmin(entity.isAdmin());
                    userInfo.setUpdatableProfile(entity.isUpdatableProfile());
                    userInfo.setGroups(new HashSet<String>(groupsListView.getData()));
                    if (StringUtils.hasText(entity.getPassword())) {
                        userInfo.setPassword(DigestUtils.md5Hex(entity.getPassword()));
                    }
                    userGroupService.updateUser(userInfo);
                    info("User '" + username + "' successfully updated.");
                }
                usersListTable.refreshUsersList(target);
                FeedbackUtils.refreshFeedback(target);
                ModalHandler.closeCurrent(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                super.onError(target, form);
                FeedbackUtils.refreshFeedback(target);
            }
        };
        form.add(submit);
        form.add(new DefaultButtonBehavior(submit));
    }
}
