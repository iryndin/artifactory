/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.webapp.wicket.page.config.services;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.component.form.SecureForm;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;

/**
 * A page to display the indexer job configuration
 *
 * @author Noam Tenne
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class IndexerConfigPage extends AuthenticatedPage {
    private IndexerForm form;

    public IndexerConfigPage() {
        form = new IndexerForm();
        add(form);
    }

    void refresh(AjaxRequestTarget target) {
        // must refresh all the fileds in the page
        target.add(form);
    }

    @Override
    public String getPageName() {
        return "Maven Indexer Support";
    }

    private static class IndexerForm extends SecureForm {
        private IndexerForm() {
            super("form");
            setOutputMarkupId(true);

            add(new IndexerConfigPanel("indexerConfigPanel", this));
        }
    }
}
