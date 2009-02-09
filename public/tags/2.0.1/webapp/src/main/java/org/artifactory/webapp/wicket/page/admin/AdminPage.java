package org.artifactory.webapp.wicket.page.admin;

import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;
import org.artifactory.webapp.wicket.page.config.general.GeneralConfigPage;
import org.artifactory.webapp.wicket.page.security.acl.AclsPage;

/**
 * @author Yoav Aharoni
 */
public class AdminPage extends AuthenticatedPage {
    @SpringBean
    private AuthorizationService authService;

    public AdminPage() {
        if (authService.isAdmin()) {
            // for now redirect all valid admin requests to the general configuration tab
            setResponsePage(GeneralConfigPage.class);
        } else if (authService.canAdminPermissionTarget()) {
            setResponsePage(AclsPage.class);
        } else {
            throw new UnauthorizedInstantiationException(getClass());
        }
    }

    @Override
    protected String getPageName() {
        return "Administration";
    }
}
