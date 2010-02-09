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

package org.artifactory.webapp.wicket.page.security.profile;

import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.webapp.wicket.application.ArtifactoryApplication;
import org.artifactory.webapp.wicket.application.ArtifactoryWebSession;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;
import org.artifactory.webapp.wicket.page.base.BasePage;

public class ProfilePage extends AuthenticatedPage {

    @SpringBean
    private AuthorizationService authorizationService;

    @SpringBean
    private SecurityService securityService;

    public ProfilePage() {
        if (!isEnabled()) {
            Class accessDeniedPage = getApplication().getApplicationSettings().getAccessDeniedPage();
            setResponsePage(accessDeniedPage);
        }
        add(new ProfilePanel("updatePanel"));
    }

    @Override
    public String getPageName() {
        return "User Profile: " + authorizationService.currentUsername();
    }

    @Override
    protected Class<? extends BasePage> getMenuPageClass() {
        return ArtifactoryApplication.get().getHomePage();
    }


    @Override
    public boolean isEnabled() {
        return super.isEnabled()
                && ArtifactoryWebSession.get().isSignedIn()
                && !authorizationService.isAnonymous()
                && (authorizationService.isUpdatableProfile()
                || securityService.isPasswordEncryptionEnabled());
    }
}
