/*
 * This file is part of Artifactory.
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

package org.artifactory.webapp.wicket.page.security.group;

import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonFactory;
import org.artifactory.addon.AddonsManager;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;

/**
 * @author Yossi Shaul
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class GroupsPage extends AuthenticatedPage {
    @SpringBean
    private AddonsManager addonsManager;

    public GroupsPage() {

        add(new GroupsListPanel("groupsList"));
        Label warningLabel = new Label("warning",
                "LDAP groups authorization is active - not showing user-specific permissions implied by the user's effective LDAP groups.");
        add(warningLabel);
        warningLabel.setVisible(addonsManager.<AddonFactory>isAddonActivated("ldap"));
    }

    @Override
    public String getPageName() {
        return "Groups Management";
    }
}