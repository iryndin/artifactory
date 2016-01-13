package org.artifactory.ui.rest.service.admin.security.user;

import org.artifactory.api.security.SecurityService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.exceptions.PasswordExpireException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;

/**
 * Service expiring all users password
 *
 * Created by Michael Pasternak
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ExpirePasswordForAllUsersService implements RestService {

    @Autowired
    protected SecurityService securityService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        try {
            securityService.expireCredentialsForAllUsers();
            response.responseCode(Response.Status.OK.getStatusCode());
            response.info("All users credentials were successfully expired");
        } catch (PasswordExpireException e) {
            response.responseCode(Response.Status.BAD_REQUEST.getStatusCode());
            response.error(e.getMessage());
        }
    }
}
