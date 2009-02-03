package org.artifactory.webapp.wicket.error;

import org.apache.log4j.Logger;
import org.artifactory.security.ArtifactorySecurityManager;
import org.artifactory.webapp.wicket.BasePage;
import org.artifactory.webapp.wicket.home.HomePage;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SessionExpiredPage extends BasePage {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SessionExpiredPage.class);

    public SessionExpiredPage() {
        if (ArtifactorySecurityManager.isAnonymous()) {
            setResponsePage(HomePage.class);
        } else {
            add(new SessionExpiredPanel("sessionExpiredPanel"));
        }
    }
}
