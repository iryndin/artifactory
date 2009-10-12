package org.artifactory.webapp.wicket.page.logs;

import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;

/**
 * This page which contains a panel that displays information and content of the system log file
 *
 * @author Noam Tenne
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class SystemLogsPage extends AuthenticatedPage {
    public SystemLogsPage() {
        add(new SystemLogsViewPanel("systemLogsViewPanel"));
    }

    @Override
    protected String getPageName() {
        return "System Logs";
    }
}
