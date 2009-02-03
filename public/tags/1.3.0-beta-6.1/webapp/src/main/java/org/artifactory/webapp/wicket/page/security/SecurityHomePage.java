package org.artifactory.webapp.wicket.page.security;

import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.page.admin.AdminPage;
import org.artifactory.webapp.wicket.page.security.user.UsersPage;

/**
 * @author Yoav Aharoni
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class SecurityHomePage extends AdminPage {
    public SecurityHomePage() {
        setResponsePage(UsersPage.class);
    }
}
