package org.artifactory.webapp.wicket.page.config.general;

import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.form.Form;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;

/**
 * Security configuration page.
 *
 * @author Yossi Shaul
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class GeneralConfigPage extends AuthenticatedPage {

    public GeneralConfigPage() {
        Form form = new Form("form");
        add(form);
        form.add(new GeneralSettingsPanel("generalConfigPanel", form));
    }

    @Override
    protected String getPageName() {
        return "General Configuration";
    }

}