package org.artifactory.webapp.wicket.security.login;

import org.artifactory.webapp.wicket.window.WindowPanel;
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
