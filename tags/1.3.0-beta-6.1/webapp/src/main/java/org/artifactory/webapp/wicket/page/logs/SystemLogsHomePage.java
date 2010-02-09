package org.artifactory.webapp.wicket.page.logs;

import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.page.admin.AdminPage;
import org.artifactory.webapp.wicket.page.importexport.repos.ImportExportReposPage;

/**
 * @author Yoav Aharoni
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class SystemLogsHomePage extends AdminPage {
    public SystemLogsHomePage() {
        setResponsePage(SystemLogsPage.class);
    }
}