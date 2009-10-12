package org.artifactory.webapp.wicket.page.base;

import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.webapp.wicket.application.ArtifactoryWebSession;
import org.artifactory.webapp.wicket.common.component.SimplePageLink;
import org.artifactory.webapp.wicket.page.security.profile.ProfilePage;

/**
 * @author Yoav Aharoni
 */
public class EditProfileLink extends SimplePageLink {
    @SpringBean
    private AuthorizationService authorizationService;

    @SpringBean
    private SecurityService securityService;

    public EditProfileLink(String id) {
        super(id, null, ProfilePage.class);
        setModelObject(authorizationService.currentUsername());
    }

    @Override
    public boolean isVisible() {
        return ArtifactoryWebSession.get().isSignedIn() && !authorizationService.isAnonymous();
    }

    @Override
    public boolean isEnabled() {
        return authorizationService.isUpdatableProfile() || securityService.isPasswordEncryptionEnabled();
    }

}
