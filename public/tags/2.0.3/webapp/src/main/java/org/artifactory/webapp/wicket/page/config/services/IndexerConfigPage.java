package org.artifactory.webapp.wicket.page.config.services;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.form.Form;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;

/**
 * A page to display the indexer job configuration
 *
 * @author Noam Tenne
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class IndexerConfigPage extends AuthenticatedPage {
    private IndexerConfigPage.GeneralSettingsForm form;

    public IndexerConfigPage() {
        form = new GeneralSettingsForm();
        add(form);
    }

    void refresh(AjaxRequestTarget target) {
        // must refresh all the fileds in the page
        target.addComponent(form);
    }

    @Override
    protected String getPageName() {
        return "Indexer";
    }

    private class GeneralSettingsForm extends Form {
        private GeneralSettingsForm() {
            super("form");
            setOutputMarkupId(true);

            add(new IndexerConfigPanel("indexerConfigPanel", this));
        }
    }
}
