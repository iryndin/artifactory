package org.artifactory.webapp.wicket.page.error;

import org.apache.wicket.Session;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class AccessDeniedPage extends BaseMessagePage {
    public AccessDeniedPage() {
        Session.get().error("Access Denied: You do not have access to the requested page.");
    }
}