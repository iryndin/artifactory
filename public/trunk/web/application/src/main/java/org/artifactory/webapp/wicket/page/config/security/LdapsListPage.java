/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.webapp.wicket.page.config.security;

import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.LdapGroupWebAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;

/**
 * @author Kobi Berkovich
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class LdapsListPage extends AuthenticatedPage {

    @SpringBean
    private CentralConfigService centralConfigService;

    @SpringBean
    private AddonsManager addonsManager;

    public LdapsListPage() {
        add(new LdapsListPanel("ldapListPanel"));
        add(addonsManager.addonByType(LdapGroupWebAddon.class).getLdapGroupConfigurationPanel("ldapGroupListPanel"));
    }

    @Override
    public String getPageName() {
        return "LDAP Settings";
    }
}
