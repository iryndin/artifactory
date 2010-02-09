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
package org.artifactory.webapp.wicket.security.login;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.ValueMap;
import org.artifactory.webapp.wicket.behavior.SubmitWithButtonBehavior;
import org.artifactory.webapp.wicket.component.NotifingFeedbackPanel;
import org.artifactory.webapp.wicket.component.SimpleButton;
import org.artifactory.webapp.wicket.component.panel.titled.TitledPanel;
import org.artifactory.webapp.wicket.home.HomePage;

/**
 * @author Yoav Aharoni
 */
public class LoginPanel extends TitledPanel {

    private static final long serialVersionUID = 1L;

    /**
     * True if the panel should display a remember-me checkbox
     */
    private boolean includeRememberMe = true;

    /**
     * Field for password.
     */
    private PasswordTextField password;

    /**
     * True if the user should be remembered via form persistence (cookies)
     */
    private boolean rememberMe = true;

    /**
     * Field for user name.
     */
    private TextField username;

    public LoginPanel(String string) {
        super(string);
        add(new NotifingFeedbackPanel("feedback"));

        //Add sign-in form to page, passing feedback panel as validation error handler
        add(new LoginForm("loginForm"));
    }

    /**
     * Sign in form.
     */
    public final class LoginForm extends Form {
        private static final long serialVersionUID = 1L;

        /**
         * Model for form
         */
        private final ValueMap properties = new ValueMap();

        /**
         * Constructor.
         *
         * @param id id of the form component
         */
        public LoginForm(String id) {
            super(id);

            //Attach textfield components that edit properties map
            //in lieu of a formal beans model
            username = new TextField("username", new PropertyModel(properties, "username"));
            username.setRequired(true);
            add(username);

            password = new PasswordTextField("password", new PropertyModel(properties, "password"));
            add(password);

            //MarkupContainer row for remember me checkbox
            WebMarkupContainer rememberMeRow = new WebMarkupContainer("rememberMeRow");
            add(rememberMeRow);
            //Add rememberMe checkbox
            rememberMeRow.add(new CheckBox("rememberMe",
                    new PropertyModel(LoginPanel.this, "rememberMe")));
            //Make form values persistent
            username.setPersistent(rememberMe);
            //Enable empty password
            password.setRequired(false);
            //Show remember me checkbox?
            rememberMeRow.setVisible(includeRememberMe);
            SimpleButton cancel = new SimpleButton("cancel", this, "Cancel") {
                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    setResponsePage(HomePage.class);
                }
            };
            add(cancel);
            SimpleButton submit = new LoginButton("submit");
            add(submit);

            add(new SubmitWithButtonBehavior(submit));

        }

        private class LoginButton extends SimpleButton {
            private LoginButton(String id) {
                super(id, LoginForm.this, "Sign In");
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                String username = getUsername();
                boolean signedIn =
                        AuthenticatedWebSession.get().signIn(username, getPassword());
                if (signedIn) {
                    //If login has been called because the user was not yet
                    //logged in, than continue to the original destination,
                    //otherwise to the Home page
                    if (!continueToOriginalDestination()) {
                        setResponsePage(
                                getApplication().getSessionSettings().getPageFactory().newPage(
                                        getApplication().getHomePage(), null));
                    }
                } else {
                    //Try the component based localizer first. If not found try the
                    //application localizer. Else use the default
                    error(getLocalizer().getString("signInFailed", this, "Sign in failed"));
                    if (target != null) {
                        target.addComponent(LoginPanel.this);
                    }
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                super.onError(target, form);
                target.addComponent(LoginPanel.this);
            }
        }
    }

    /**
     * IndexerManagerImpl
     * Convenience method to access the password.
     *
     * @return The password
     */
    public String getPassword() {
        return password.getInput();
    }

    /**
     * Get model object of the rememberMe checkbox
     *
     * @return True if user should be remembered in the future
     */
    public boolean getRememberMe() {
        return rememberMe;
    }

    /**
     * Convenience method to access the username.
     *
     * @return The user name
     */
    public String getUsername() {
        return username.getModelObjectAsString();
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
}
