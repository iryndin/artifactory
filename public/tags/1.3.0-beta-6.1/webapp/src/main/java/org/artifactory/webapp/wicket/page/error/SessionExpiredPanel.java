package org.artifactory.webapp.wicket.page.error;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledPanel;
import org.artifactory.webapp.wicket.page.security.login.LoginPage;

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
