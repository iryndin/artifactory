package org.artifactory.rest.resource.userprofile;

import org.artifactory.api.security.AuthorizationService;
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

/**
 * @author Chen Keinan
 */
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
@Component
@Path("security/apiKey{id:(/[^/]+?)?}")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ApiKeyResource extends BaseResource {

    @Autowired
    protected ConfigServiceFactory configServiceFactory;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiKey()
            throws Exception {
        return runService(configServiceFactory.getApiKey());
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response revokeApiKey()
            throws Exception {
        return runService(configServiceFactory.revokeApiKey());
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response regenerateApiKey()
            throws Exception {
        return runService(configServiceFactory.regenerateApiKey());
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response createApiKey()
            throws Exception {
        return runService(configServiceFactory.createApiKey());
    }
}
