package org.artifactory.ui.rest.service.admin.security.user;

import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.exceptions.PasswordChangeException;
import org.artifactory.rest.common.dataholder.PasswordContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;

/**
 * Service changing user password
 *
 * Created by Michael Pasternak
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ChangePasswordService<T extends PasswordContainer> implements RestService<T> {

    @Autowired
    protected SecurityService securityService;
    @Autowired
    protected UserGroupService userGroupService;

    @Override
    public void execute(ArtifactoryRestRequest<T> request, RestResponse response) {
        PasswordContainer passwordContainer = request.getImodel();
        try {
            securityService.changePassword(
                    passwordContainer.getUserName(),
                    passwordContainer.getOldPassword(),
                    passwordContainer.getNewPassword1(),
                    passwordContainer.getNewPassword2()
            );
            response.responseCode(Response.Status.OK.getStatusCode());
            response.info("Password has been successfully changed");
        } catch (PasswordChangeException e) {
            response.responseCode(Response.Status.BAD_REQUEST.getStatusCode());
            response.error(e.getMessage());
        }
    }
}
