package org.artifactory.webapp.wicket.security.profile;

import org.apache.log4j.Logger;
import org.artifactory.webapp.wicket.AuthenticatedPage;

public class ProfilePage extends AuthenticatedPage {

    @SuppressWarnings({"UnusedDeclaration", "UNUSED_SYMBOL"})
    private final static Logger LOGGER = Logger.getLogger(ProfilePage.class);

    public ProfilePage() {
        ProfilePanel updatePanel = new ProfilePanel("updatePanel");
        add(updatePanel);
    }

    protected String getPageName() {
        return "Users Profile";
    }

}
