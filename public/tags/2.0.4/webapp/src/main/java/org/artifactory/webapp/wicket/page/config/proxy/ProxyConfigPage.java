package org.artifactory.webapp.wicket.page.config.proxy;

import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;
import org.artifactory.api.security.AuthorizationService;
import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;

/**
 * @author Yoav Aharoni
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class ProxyConfigPage extends AuthenticatedPage {
    public ProxyConfigPage() {
        add(new ProxiesListPanel("proxiesListPanel"));
    }

    @Override
    protected String getPageName() {
        return "Configure Proxy";
    }
}
