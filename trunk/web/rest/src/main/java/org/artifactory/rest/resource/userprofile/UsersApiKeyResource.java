package org.artifactory.rest.resource.userprofile;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.model.userprofile.UserProfileModel;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.rest.services.ConfigServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Chen Keinan
 */
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
@Component
@Path("security/users/apiKey")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UsersApiKeyResource extends BaseResource {

    @Autowired
    protected ConfigServiceFactory configServiceFactory;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllUsersApiKey()
            throws Exception {
        return runService(configServiceFactory.getUsersAndApiKeys());
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response syncAllUsersApiKey(List<UserProfileModel> userProfileModels)
            throws Exception {
        return runService(configServiceFactory.syncUsersAndApiKeys(), userProfileModels);
    }
}
