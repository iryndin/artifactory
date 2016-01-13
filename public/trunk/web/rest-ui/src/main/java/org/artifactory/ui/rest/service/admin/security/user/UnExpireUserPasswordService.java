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
 * Service expiring user password
 *
 * Created by Michael Pasternak
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UnExpireUserPasswordService<T extends String> implements RestService<T> {

    @Autowired
    protected SecurityService securityService;

    @Override
    public void execute(ArtifactoryRestRequest<T> request, RestResponse response) {
        String userName = request.getImodel();
        try {
            securityService.unExpireUserCredentials(userName);
            response.responseCode(Response.Status.OK.getStatusCode());
            response.info("User credentials were successfully revalidated");
        } catch (PasswordExpireException e) {
            response.responseCode(Response.Status.BAD_REQUEST.getStatusCode());
            response.error(e.getMessage());
        }
    }
}
