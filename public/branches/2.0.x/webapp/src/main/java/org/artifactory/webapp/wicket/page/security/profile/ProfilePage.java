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
    protected String getPageName() {
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
