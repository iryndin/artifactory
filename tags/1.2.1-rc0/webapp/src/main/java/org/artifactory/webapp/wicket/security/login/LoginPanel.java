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

import org.artifactory.webapp.wicket.panel.WindowPanel;
import wicket.authentication.panel.SignInPanel;
import wicket.behavior.AttributeAppender;
import wicket.markup.html.form.FormComponent;
import wicket.model.Model;

/**
 * @author Yoav Aharoni
 */
public class LoginPanel extends WindowPanel {

    public LoginPanel(String string) {
        super(string);
        SignInPanel panel = new SignInPanel("signInPanel");
        panel.setPersistent(true);
        //Enable empty password
        final FormComponent passwordInput = ((FormComponent) panel.get("signInForm:password"));
        passwordInput.setRequired(false);

        // set css styles
        passwordInput.add(new AttributeAppender("class", new Model("textfield"), " "));
        panel.get("signInForm:username").add(new AttributeAppender("class", new Model("textfield"), " "));
        panel.get("signInForm:rememberMeRow:rememberMe").add(new AttributeAppender("class", new Model("chekbox"), " "));

        add(panel);
    }
}
