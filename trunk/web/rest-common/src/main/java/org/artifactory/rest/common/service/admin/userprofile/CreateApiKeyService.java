package org.artifactory.rest.common.service.admin.userprofile;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.UserGroupService;
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

import javax.servlet.http.HttpServletResponse;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateApiKeyService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(CreateApiKeyService.class);
    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    UserGroupService userGroupService;

    @Autowired
    ApiKeyManager apiKeyManager;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {

        if (authorizationService.isAnonymous()) {
            response.responseCode(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String userName = authorizationService.currentUsername();
        String apiKeyByUser = userGroupService.getPropsToken(userName, ApiKeyManager.API_KEY);
        if (!StringUtils.isEmpty(apiKeyByUser)) {
            log.error("Api key already exists for user: '{}'", userName);
            response.error("Api key already exists for user: " + userName);
            return;
        }
        TokenKeyValue token = apiKeyManager.createToken(userName);
        if (token == null) {
            log.error("Error while generating api key for user '{}'", userName);
            response.error("Failed to create api key for user: " + userName);
            return;
        }
        log.debug("User '{}' successfully created api key", userName);
        response.responseCode(HttpServletResponse.SC_CREATED);
        if (token != null) {
            UserProfileModel userProfileModel = new UserProfileModel(token.getToken());
            response.iModel(userProfileModel);

        }
    }
}
