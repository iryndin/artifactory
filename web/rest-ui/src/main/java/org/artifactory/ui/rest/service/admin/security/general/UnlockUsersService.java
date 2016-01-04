package org.artifactory.ui.rest.service.admin.security.general;

import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Michael Pasternak
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UnlockUsersService<T extends List> implements RestService<T> {

    @Autowired
    private SecurityService securityService;

    @Override
    public void execute(ArtifactoryRestRequest<T> request, RestResponse response) {
        List<String> users = request.getImodel();

        if(users != null && users.size() > 0) {
            users.parallelStream().forEach(u -> {
                ((UserGroupService)securityService).unlockUser(u);
            });
            response.info(
                users.size() > 1 ?
                    String.format("All users were successfully unlocked", users)
                    :
                    String.format("User '%s' was successfully unlocked", users.get(0))
            );
        }
    }
}
