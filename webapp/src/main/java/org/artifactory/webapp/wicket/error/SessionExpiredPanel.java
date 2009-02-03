package org.artifactory.webapp.wicket.error;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.artifactory.webapp.wicket.component.panel.titled.TitledPanel;
import org.artifactory.webapp.wicket.security.login.LoginPage;

/**
 * @author Yoav Aharoni
 */
public class SessionExpiredPanel extends TitledPanel {

    public SessionExpiredPanel(String string) {
        super(string);
        BookmarkablePageLink homeLink = new BookmarkablePageLink("home", LoginPage.class);
        add(homeLink);
    }
}
