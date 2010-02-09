package org.artifactory.webapp.wicket.widget;

import org.apache.log4j.Logger;
import wicket.Page;
import wicket.authorization.IAuthorizationStrategy;
import wicket.markup.html.link.IPageLink;
import wicket.markup.html.link.PageLink;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SecuredPageLink extends PageLink {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SecuredPageLink.class);

    private final Class<? extends Page> pageClass;

    @SuppressWarnings({"unchecked"})
    public SecuredPageLink(final String id, final Class c) {
        super(id, c);
        this.pageClass = c;
    }

    public SecuredPageLink(final String id, final Page page) {
        super(id, page);
        this.pageClass = page.getClass();
    }

    @SuppressWarnings({"unchecked"})
    public SecuredPageLink(final String id, final IPageLink pageLink) {
        super(id, pageLink);
        this.pageClass = pageLink.getPageIdentity();
    }

    public boolean isEnabled() {
        IAuthorizationStrategy authorizationStrategy = getSession().getAuthorizationStrategy();
        boolean authorized = authorizationStrategy.isInstantiationAuthorized(pageClass);
        return authorized && super.isEnabled() && isEnableAllowed();
    }
}
