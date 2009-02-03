package org.artifactory.webapp.wicket.page.security.profile;

import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;

public class ProfilePage extends AuthenticatedPage {

    public ProfilePage() {
        ProfilePanel updatePanel = new ProfilePanel("updatePanel");
        add(updatePanel);
    }

    protected String getPageName() {
        return "Edit User Profile";
    }

}
