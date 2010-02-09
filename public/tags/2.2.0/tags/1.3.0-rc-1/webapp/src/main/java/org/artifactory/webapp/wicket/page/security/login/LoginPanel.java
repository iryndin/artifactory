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
package org.artifactory.webapp.wicket.page.security.login;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.webapp.wicket.WicketProperty;
import org.artifactory.webapp.wicket.application.ArtifactoryApplication;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.checkbox.styled.StyledCheckbox;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledActionPanel;

/**
 * @author Yoav Aharoni
 */
public class LoginPanel extends TitledActionPanel {

    private static final long serialVersionUID = 1L;

    /**
     * True if the panel should display a remember-me checkbox
     */
    @SuppressWarnings({"FieldCanBeLocal"})
    private boolean includeRememberMe = true;

    /**
     * True if the user should be remembered via form persistence (cookies)
     */
    @SuppressWarnings({"FieldCanBeLocal"})
    private boolean rememberMe = true;

    public LoginPanel(String string) {
        super(string);

        //Add sign-in form to page, passing feedback panel as validation error handler
        add(new LoginForm("loginForm"));
    }

    /**
     * Sign in form.
     */
    public final class LoginForm extends Form {
        private static final long serialVersionUID = 1L;

        @WicketProperty
        private String username;

        @WicketProperty
        private String password;

        public LoginForm(String id) {
            super(id);

            //Attach textfield components that edit properties map
            //in lieu of a formal beans model
            TextField username = new TextField("username", new PropertyModel(this, "username"));
            username.setRequired(true);
            //Make form values persistent
            username.setPersistent(rememberMe);
            add(username);

            PasswordTextField password = new PasswordTextField("password", new PropertyModel(this, "password"));
            //Enable empty password
            password.setRequired(false);
            add(password);

            //MarkupContainer row for remember me checkbox
            WebMarkupContainer rememberMeRow = new WebMarkupContainer("rememberMeRow");
            add(rememberMeRow);
            //Add rememberMe checkbox
            rememberMeRow.add(new StyledCheckbox("rememberMe",
                    new PropertyModel(LoginPanel.this, "rememberMe")));

            //Show remember me checkbox?
            rememberMeRow.setVisible(includeRememberMe);

            addDefaultButton(new LoginButton("submit"));
            addButton(new SimpleButton("cancel", LoginForm.this, "Cancel") {
                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    setResponsePage(ArtifactoryApplication.get().getHomePage());
                }
            });
        }

        private class LoginButton extends SimpleButton {
            private LoginButton(String id) {
                super(id, LoginForm.this, "Log In");
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                boolean signedIn = AuthenticatedWebSession.get().signIn(username, password);
                if (signedIn) {
                    //If login has been called because the user was not yet
                    //logged in, than continue to the original destination,
                    //otherwise to the Home page
                    if (!continueToOriginalDestination()) {
                        setResponsePage(ArtifactoryApplication.get().getHomePage());
                    }
                } else {
                    //Try the component based localizer first. If not found try the
                    //application localizer. Else use the default
                    error("User name or password are incorrect. Login failed.");
                    if (target != null) {
                        target.addComponent(LoginPanel.this);
                        FeedbackUtils.refreshFeedback(target);
                    }
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                super.onError(target, form);
                target.addComponent(LoginPanel.this);
                FeedbackUtils.refreshFeedback(target);
            }
        }
    }

}
