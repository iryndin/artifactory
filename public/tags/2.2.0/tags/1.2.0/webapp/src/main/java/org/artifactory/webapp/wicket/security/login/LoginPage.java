package org.artifactory.webapp.wicket.security.login;

import org.apache.log4j.Logger;
import org.artifactory.webapp.wicket.BasePage;
import wicket.PageParameters;

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
        add(new LoginPanel("loginPanel"));
    }
}
