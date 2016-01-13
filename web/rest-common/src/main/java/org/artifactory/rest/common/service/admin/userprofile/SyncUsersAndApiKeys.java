package org.artifactory.rest.common.service.admin.userprofile;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.rest.common.model.userprofile.UserProfileModel;
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

import java.sql.SQLException;
import java.util.List;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SyncUsersAndApiKeys implements RestService {
    private static final Logger log = LoggerFactory.getLogger(SyncUsersAndApiKeys.class);

    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    UserGroupService userGroupService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        if (authorizationService.isAdmin()) {
            List<UserProfileModel> userProfileModels = request.getModels();
            StringBuilder syncErrorBuilder = new StringBuilder();
            userProfileModels.forEach(userProfileModel -> {
                try {
                    userGroupService.updatePropsToken(userProfileModel.getUserName(), ApiKeyManager.API_KEY, userProfileModel.getApiKey());
                } catch (SQLException e) {
                    log.debug("Error while syncing api key '{}' for user '{}'", userProfileModel.getApiKey(),
                            userProfileModel.getUserName());
                    syncErrorBuilder.append("Error while syncing api key for user: " + userProfileModel.getUserName() + "\n");

                }
            });
            if (StringUtils.isEmpty(syncErrorBuilder.toString())) {
                response.error(syncErrorBuilder.toString());
            }
        }
    }
}
