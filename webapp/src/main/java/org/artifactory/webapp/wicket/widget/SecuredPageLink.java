package org.artifactory.webapp.wicket.widget;

import org.apache.log4j.Logger;
import wicket.Page;
import wicket.markup.html.link.IPageLink;
import wicket.markup.html.link.PageLink;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SecuredPageLink extends PageLink {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SecuredPageLink.class);

    public SecuredPageLink(final String id, final Class c) {
        super(id, c);
    }

    public SecuredPageLink(final String id, final Page page) {
        super(id, page);
    }

    public SecuredPageLink(final String id, final IPageLink pageLink) {
        super(id, pageLink);
    }

    public boolean isEnabled() {
        return super.isEnabled() && isEnableAllowed();
    }
}
