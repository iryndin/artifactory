package org.artifactory.webapp.wicket.error;

import org.artifactory.webapp.wicket.panel.WindowPanel;
import org.artifactory.webapp.wicket.security.login.LoginPage;
import wicket.markup.html.link.BookmarkablePageLink;

/**
 * @author Yoav Aharoni
 */
public class SessionExpiredPanel extends WindowPanel {

    public SessionExpiredPanel(String string) {
        super(string);
        BookmarkablePageLink homeLink = new BookmarkablePageLink("home", LoginPage.class);
        add(homeLink);
    }
}
