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
package org.artifactory.webapp.wicket.security.user;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.webapp.wicket.component.CancelButton;
import org.artifactory.webapp.wicket.component.CreateUpdatePanel;
import org.artifactory.webapp.wicket.component.SimpleButton;
import org.artifactory.webapp.wicket.component.deletable.listview.DeletableLabelGroup;
import org.artifactory.webapp.wicket.utils.validation.JcrNameValidator;
import org.springframework.util.StringUtils;

import java.util.HashSet;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class UserCreateUpdatePanel extends CreateUpdatePanel<UserModel> {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(UserCreateUpdatePanel.class);

    @SpringBean
    private UserGroupService userGroupService;

    public UserCreateUpdatePanel(
            CreateUpdateAction action, final UserModel user, final UsersListPanel usersListPanel) {
        super(action, user);
        final boolean create = isCreate();

        //Username
        RequiredTextField usernameTf = new RequiredTextField("username");
        usernameTf.setEnabled(create);
        usernameTf.add(JcrNameValidator.getInstance());
        form.add(usernameTf);

        //Password
        final PasswordTextField password = new PasswordTextField("password");
        password.setRequired(create);
        form.add(password);
        PasswordTextField retypedPassword = new PasswordTextField("retypedPassword");
        retypedPassword.setRequired(create);
        form.add(retypedPassword);
        form.add(new EqualPasswordInputValidator(password, retypedPassword) {
            private static final long serialVersionUID = 1L;

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
        form.add(emailTf);

        //Admin
        form.add(new CheckBox("admin"));

        //Can update profile
        form.add(new CheckBox("updatableProfile"));

        // groups
        final DeletableLabelGroup<String> groupsListView =
                new DeletableLabelGroup<String>("groups", user.getGroups());
        groupsListView.setLabelClickable(false);
        form.add(groupsListView);

        //Cancel
        form.add(new CancelButton(form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                super.onSubmit(target, form);
                //Back to create
                UserCreateUpdatePanel.this.replaceWith(target, getUsersPage().newCreatePanel());
            }
        });

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
                            entity.isAdmin(), entity.isUpdatableProfile(),
                            true, true, true, true);
                    boolean created = userGroupService.createUser(newUser);
                    if (!created) {
                        error("User '" + username + "' already exists.");
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
                UserCreateUpdatePanel.this.replaceWith(
                        target, (getUsersPage().newCreatePanel()));
                usersListPanel.refreshUsersList(target);
                target.addComponent(getFeedback());
                target.addComponent(usersListPanel);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(getFeedback());
            }
        };
        form.add(submit);
        add(form);
    }

    private UsersPage getUsersPage() {
        return (UsersPage) getPage();
    }
}
