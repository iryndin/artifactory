package org.artifactory.rest.common.service.admin.userprofile;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.props.auth.ApiKeyManager;
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
public class RevokeApiKeyService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(RevokeApiKeyService.class);

    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    ApiKeyManager apiKeyManager;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        if (authorizationService.isAnonymous()) {
            return;
        }
        String userName = request.getPathParamByKey("id");
        boolean deleteAll = request.getQueryParamByKey("deleteAll").equals("1") ? true : false;
        String authUserName = authorizationService.currentUsername();
        if (StringUtils.isEmpty(userName) && !deleteAll) {
            userName = authUserName;
        }
        boolean isAdmin = authorizationService.isAdmin();
        if (((!StringUtils.isEmpty(userName) && isAdmin) || (userName.equals(authUserName)) && !deleteAll)) {
            // revoke apiKey
            revokeApiKey(response, userName);
        } else {
            if (StringUtils.isEmpty(userName) && isAdmin && deleteAll) {
                // revoke all api keys
                revokeAllApiKeys(response);
            } else {
                response.responseCode(HttpServletResponse.SC_FORBIDDEN);
            }
        }
    }

    /**
     * revoke all api keys
     *
     * @param response - encapsulate data related tto response
     */
    private void revokeAllApiKeys(RestResponse response) {
        boolean revokeSucceeded = apiKeyManager.revokeAllToken();
        if (revokeSucceeded) {
            response.info("All api keys have been successfully revoked");
            log.debug("All api keys have been successfully revoked by: '{}'", authorizationService.currentUsername());
        } else {
            log.error("Error revoking all api keys");
            response.error("Error revoking all api keys");
        }
    }

    /**
     * revoke api key for specific user
     *
     * @param response     - artifactory rest response
     * @param userName     - user name to revoke api
     */
    private void revokeApiKey(RestResponse response, String userName) {
        if (apiKeyManager.revokeToken(userName)) {
                response.info("Api key for user: '" + userName + "' has been successfully revoked");
                log.debug("Api key for user: '" + userName + "' has been successfully revoked by user : '{}'", authorizationService.currentUsername());
        } else {
            log.error("Error revoking api key for user '{}'", userName);
            response.error("Error revoking api key for user: " + userName);
        }
    }
}
