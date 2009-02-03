package org.artifactory.webapp.wicket.page.config;

import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.page.admin.AdminPage;

/**
 * @author Yoav Aharoni
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class ConfigHomePage extends AdminPage {
}
