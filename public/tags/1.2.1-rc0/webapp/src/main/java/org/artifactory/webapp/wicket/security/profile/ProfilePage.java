package org.artifactory.webapp.wicket.security.profile;

import org.apache.log4j.Logger;
import org.artifactory.webapp.wicket.ArtifactoryPage;

public class ProfilePage extends ArtifactoryPage {

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
