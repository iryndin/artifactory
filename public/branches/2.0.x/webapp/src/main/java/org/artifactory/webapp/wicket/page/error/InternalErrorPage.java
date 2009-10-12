package org.artifactory.webapp.wicket.page.error;

import org.apache.wicket.Session;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class InternalErrorPage extends BaseMessagePage {
    public InternalErrorPage() {
        Session.get().error("Internal error occurred");
    }
}