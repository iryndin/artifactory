package org.artifactory.webapp.wicket.security.login;

import org.apache.log4j.Logger;
import org.artifactory.webapp.wicket.ArtifactorySession;
import org.artifactory.webapp.wicket.BasePage;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class LogoutPage extends BasePage {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(LogoutPage.class);

    @Override
    protected void onBeforeRender() {
        //Signout
        ArtifactorySession session = ArtifactorySession.get();
        session.signOut();
    }
}
