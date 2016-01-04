package org.artifactory.ui.rest.resource.admin.security.apikey;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.ui.rest.service.admin.security.SecurityServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Chen Keinan
 */
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
@Component
@Path("userApiKey{id:(/[^/]+?)?}")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UserApiKeyResource extends BaseResource {

    @Autowired
    protected SecurityServiceFactory securityFactory;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiKey()
            throws Exception {
        return runService(securityFactory.getApiKey());
    }


    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response revokeApiKey()
            throws Exception {
        return runService(securityFactory.revokeApiKey());
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response regenerateApiKey()
            throws Exception {
        return runService(securityFactory.regenerateApiKey());
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response createApiKey()
            throws Exception {
        return runService(securityFactory.createApiKey());
    }
}
