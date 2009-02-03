package org.artifactory.webapp.wicket.page.error;

import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.page.base.BasePage;
import org.artifactory.webapp.wicket.page.home.HomePage;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SessionExpiredPage extends BasePage {

    @SpringBean
    private AuthorizationService authService;

    public SessionExpiredPage() {
        if (authService.isAnonymous()) {
            setResponsePage(HomePage.class);
        } else {
            add(new SessionExpiredPanel("sessionExpiredPanel"));
        }
    }

    @Override
    protected String getPageName() {
        return "Session Expired";
    }
}
