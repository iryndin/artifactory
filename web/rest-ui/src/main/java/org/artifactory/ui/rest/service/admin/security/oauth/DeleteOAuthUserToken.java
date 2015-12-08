package org.artifactory.ui.rest.service.admin.security.oauth;

import org.artifactory.api.security.UserGroupService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.admin.security.oauth.OAuthUserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Gidi Shabat
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteOAuthUserToken <T extends OAuthUserToken>implements RestService<T> {
    private static final Logger log = LoggerFactory.getLogger(DeleteOAuthProviderSettings.class);

    @Autowired
    UserGroupService userGroupService;

    @Override
    public void execute(ArtifactoryRestRequest<T> request, RestResponse response) {
        T userToken = request.getImodel();
        log.debug("Deleting OAuth token for provider: '{}' for user: '{}'", userToken.getProviderName(),
                userToken.getUserName());
        userGroupService.deleteProperty(userToken.getUserName(), "authinfo."+userToken.getProviderName());
        log.debug("Successfully deleted token for provider: '{}' for user: '{}'",userToken.getProviderName(),
                userToken.getUserName());
    }
}
