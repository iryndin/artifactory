package org.artifactory.webapp.wicket.page.security.login.forgot;

import org.artifactory.common.wicket.component.links.TitledLink;

/**
 * Forgot password link button.
 *
 * @author Tomer Cohen
 */
public class ForgotPasswordLink extends TitledLink {
    public ForgotPasswordLink(String id) {
        super(id, "Forgot Password");
    }

    @Override
    public void onClick() {
        setResponsePage(ForgotPasswordPage.class);
    }
}
