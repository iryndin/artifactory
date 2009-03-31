package org.artifactory.webapp.wicket.page.error;

import org.apache.wicket.Session;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SessionExpiredPage extends BaseMessagePage {
    public SessionExpiredPage() {
        Session.get().warn("Session has expired");
    }
}
