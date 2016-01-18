package org.artifactory.rest.resource.reverseproxies;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.ReverseProxyDescriptor;
import org.artifactory.rest.common.model.reverseproxy.ReverseProxyDescriptorModel;
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
 * @author Shay Yaakov
 */
@Path("system/configuration/webServer{id:(/[^/]+?)?}")
@RolesAllowed({AuthorizationService.ROLE_ADMIN})
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ReverseProxiesResource extends BaseResource {

    @Autowired
    protected ConfigServiceFactory configServiceFactory;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createReverseProxy(ReverseProxyDescriptorModel reverseProxy) throws Exception {
        return runService(configServiceFactory.createReverseProxy(), reverseProxy);
    }


    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateReverseProxy(ReverseProxyDescriptor reverseProxy) throws Exception {
        return runService(configServiceFactory.updateReverseProxy(), reverseProxy);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReverseProxies() throws Exception {
        return runService(configServiceFactory.getReverseProxies());
    }
}
