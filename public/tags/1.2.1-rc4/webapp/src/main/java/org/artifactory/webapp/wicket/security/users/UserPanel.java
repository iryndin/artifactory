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
package org.artifactory.webapp.wicket.security.users;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitButton;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.CompoundPropertyModel;
import org.artifactory.security.ExtendedUserDetailsService;
import org.artifactory.security.SecurityHelper;
import org.artifactory.security.SimpleUser;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextUtils;
import org.artifactory.webapp.wicket.components.CreateUpdatePanel;
import org.artifactory.webapp.wicket.components.DojoButton;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class UserPanel extends CreateUpdatePanel<User> {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(UserPanel.class);

    private Form form;
    protected String header;

    public UserPanel(String id, final CreateUpdateAction action, final User user) {
        super(id, action, user);
        CompoundPropertyModel model = new CompoundPropertyModel(entity);
        //Form
        form = new Form("userForm", model);
        form.setOutputMarkupId(true);
        final boolean create = action.equals(CreateUpdateAction.CREATE);
        header = create ? "New User" : "User Details";

        //Username
        RequiredTextField usernameTf = new RequiredTextField("username");
        form.add(usernameTf);
        usernameTf.setEnabled(create);
        //Password
        PasswordTextField password = new PasswordTextField("password");
        form.add(password);
        PasswordTextField retypedPassword = new PasswordTextField("retypedPassword");
        form.add(retypedPassword);
        form.add(new EqualPasswordInputValidator(password, retypedPassword));
        //Admin
        form.add(new CheckBox("admin"));
        /*//Can update profile
        form.add(new CheckBox("updatableProfile"));*/
        //Cancel
        DojoButton cancel = new DojoButton("cancel", form, "Cancel") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                if (create) {
                    form.clearInput();
                    clearFeedback(target);
                    target.addComponent(form);
                } else {
                    flip(target);
                }
            }
        };
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);
        //Submit
        String submitCaption = create ? "Create" : "Save";
        AjaxSubmitButton submit = new DojoButton("submit", form, submitCaption) {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                super.onSubmit(target, form);
                ArtifactoryContext context = ContextUtils.getContext();
                SecurityHelper security = context.getSecurity();
                ExtendedUserDetailsService userDetailsService = security.getUserDetailsService();
                SimpleUser details = entity.getUser();
                if (create) {
                    boolean created = userDetailsService.createUser(details);
                    if (!created) {
                        error("User '" + entity.getUsername() + "' already exists.");
                    } else {
                        info("User '" + entity.getUsername() + "' successfully created.");
                        if (changeListener != null) {
                            target.addComponent(changeListener);
                        }
                        form.clearInput();
                        target.addComponent(form);
                    }
                    target.addComponent(feedback);
                } else {
                    userDetailsService.updateUser(details);
                    info("User '" + entity.getUsername() + "' successfully updated.");
                    flip(target);
                }
            }

            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(feedback);
            }
        };
        form.add(submit);
        add(form);
    }

    @Override
    protected void onModelChanged() {
        //Notify the form
        CompoundPropertyModel model = new CompoundPropertyModel(entity);
        form.setModel(model);
    }


    public String getTitle() {
        return header;
    }
}
