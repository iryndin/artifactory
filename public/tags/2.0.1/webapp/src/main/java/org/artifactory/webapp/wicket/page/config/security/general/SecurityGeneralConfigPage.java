package org.artifactory.webapp.wicket.page.config.security.general;

import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;

/**
 * Security general configuration page.
 *
 * @author Yossi Shaul
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class SecurityGeneralConfigPage extends AuthenticatedPage {

    public SecurityGeneralConfigPage() {
        add(new SecurityGeneralConfigPanel("generalConfigPanel"));
    }

    @Override
    protected String getPageName() {
        return "Security General Configuration";
    }

}