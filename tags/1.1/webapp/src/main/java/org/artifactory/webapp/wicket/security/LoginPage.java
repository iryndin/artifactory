package org.artifactory.webapp.wicket.security;

import org.apache.log4j.Logger;
import org.artifactory.webapp.wicket.BasePage;
import wicket.PageParameters;
import wicket.authentication.panel.SignInPanel;
import wicket.markup.html.form.FormComponent;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class LoginPage extends BasePage {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(LoginPage.class);

    private static final long serialVersionUID = 1L;

    /**
     * Construct
     */
    public LoginPage() {
        this(null);
    }

    /**
     * Constructor
     *
     * @param parameters The page parameters
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public LoginPage(final PageParameters parameters) {
        SignInPanel panel = new SignInPanel("signInPanel");
        panel.setPersistent(true);
        //Enable empty password
        ((FormComponent) panel.get("signInForm:password")).setRequired(false);
        add(panel);
    }
}
