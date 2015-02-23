/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2013 JFrog Ltd.
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

package org.artifactory.webapp.wicket.page.config;

import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;

/**
 * @author Yoav Luft
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class HaNotConfiguredPage extends AuthenticatedPage {

    public HaNotConfiguredPage() {
        add(new NotConfiguredBorder("border"));
    }

    @Override
    public String getPageName() {
        return "Configure High Availability";
    }

    private static class NotConfiguredBorder extends TitledBorder {

        public NotConfiguredBorder(String id) {
            super(id);
        }
    }
}
