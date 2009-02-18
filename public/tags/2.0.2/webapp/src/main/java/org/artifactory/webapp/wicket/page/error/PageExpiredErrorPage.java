package org.artifactory.webapp.wicket.page.error;

import org.apache.wicket.Session;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class PageExpiredErrorPage extends BaseMessagePage {
    public PageExpiredErrorPage() {
        Session.get().warn("Page Expired: The page you requested has expired.");
    }
}