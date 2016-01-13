package org.artifactory.rest.common.service.admin.userprofile;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.model.userprofile.UserProfileModel;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.props.auth.ApiKeyManager;
import org.artifactory.security.props.auth.model.TokenKeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateApiKeyService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(UpdateApiKeyService.class);

    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    ApiKeyManager apiKeyManager;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        if (authorizationService.isAnonymous()) {
            return;
        }
        String userName = authorizationService.currentUsername();
        updateApiKeys(userName, response);
    }

    /**
     * revoke all api keys
     *
     * @param response - encapsulate data related tto response
     */
    private void updateApiKeys(String userName, RestResponse response) {
        TokenKeyValue token = apiKeyManager.refreshToken(userName);
        if (token != null) {
            UserProfileModel userProfileModel = new UserProfileModel(token.getToken());
                response.iModel(userProfileModel);
                log.debug("user: {} successfuly updated it apiKey", userName);
        } else {
            log.error("Error updating api key for user '{}'", userName);
            response.error("Error updating api key for user " + userName);
        }
    }
}
