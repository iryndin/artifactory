package org.artifactory.rest.common.service.admin.userprofile;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.rest.common.model.userprofile.UserProfileModel;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.UserInfo;
import org.artifactory.security.props.auth.ApiKeyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetUsersAndApiKeys implements RestService {
    private static final Logger log = LoggerFactory.getLogger(GetUsersAndApiKeys.class);

    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    UserGroupService userGroupService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        List<UserProfileModel> userProfileModels = new ArrayList<>();
        if (authorizationService.isAdmin()) {
            List<UserInfo> allUsers = userGroupService.getAllUsers(true);
            allUsers.forEach(userInfo -> {
                String username = userInfo.getUsername();
                userInfo.getUserProperties().forEach(userPropertyInfo -> {
                    if (userPropertyInfo.getPropKey().equals(ApiKeyManager.API_KEY)) {
                        userProfileModels.add(new UserProfileModel(userPropertyInfo.getPropValue(), username));
                    }
                });
            });
        }
        response.iModelList(userProfileModels);
    }
}
