package org.artifactory.webapp.wicket.page.config.security;

import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;

/**
 * @author Kobi Berkovich
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class LdapKeyListPage extends AuthenticatedPage {

    @SpringBean
    private CentralConfigService centralConfigService;

    public LdapKeyListPage() {
        add(new LdapsListPanel("ldapListPanel"));
    }

    protected String getPageName() {
        return "LDAP Settings";
    }
}
