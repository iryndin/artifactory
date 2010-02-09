package org.artifactory.webapp.wicket.application.sitemap;

import org.apache.wicket.Page;
import org.apache.wicket.injection.web.InjectorHolder;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.AuthorizationService;

/**
 * @author Yoav Aharoni
 */
public abstract class SecuredPageNode extends MenuNode {
    @SpringBean
    private AuthorizationService authorizationService;

    protected SecuredPageNode(Class<? extends Page> pageClass, String name) {
        super(name, pageClass);
    }

    {
        InjectorHolder.getInjector().inject(this);
    }

    public AuthorizationService getAuthorizationService() {
        return authorizationService;
    }

    @Override
    public abstract boolean isEnabled();
}
