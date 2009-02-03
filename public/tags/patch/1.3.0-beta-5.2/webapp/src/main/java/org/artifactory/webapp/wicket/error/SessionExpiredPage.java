package org.artifactory.webapp.wicket.error;

import org.apache.log4j.Logger;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.BasePage;
import org.artifactory.webapp.wicket.home.HomePage;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SessionExpiredPage extends BasePage {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private static final Logger LOGGER = Logger.getLogger(SessionExpiredPage.class);

    @SpringBean
    private AuthorizationService authService;

    public SessionExpiredPage() {
        if (authService.isAnonymous()) {
            setResponsePage(HomePage.class);
        } else {
            add(new SessionExpiredPanel("sessionExpiredPanel"));
        }
    }
}
