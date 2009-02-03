package org.artifactory.webapp.wicket.error;

import org.apache.log4j.Logger;
import org.artifactory.webapp.wicket.BasePage;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SessionExpiredPage extends BasePage {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SessionExpiredPage.class);

    public SessionExpiredPage() {
        add(new SessionExpiredPanel("sessionExpiredPanel"));
    }
}
