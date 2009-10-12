package org.artifactory.webapp.wicket.page.base;

import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.application.ArtifactoryWebSession;
import org.artifactory.webapp.wicket.common.component.SimplePageLink;
import org.artifactory.webapp.wicket.page.security.login.LoginPage;

/**
 * @author Yoav Aharoni
 */
public class LoginLink extends SimplePageLink {
    @SpringBean
    private AuthorizationService authorizationService;

    public LoginLink(String id, String caption) {
        super(id, caption, LoginPage.class);
    }

    @Override
    public boolean isVisible() {
        return !ArtifactoryWebSession.get().isSignedIn() || authorizationService.isAnonymous();
    }
}
