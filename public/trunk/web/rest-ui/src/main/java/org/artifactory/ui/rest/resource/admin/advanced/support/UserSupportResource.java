package org.artifactory.ui.rest.resource.admin.advanced.support;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.support.config.bundle.BundleConfigurationImpl;
import org.artifactory.ui.rest.service.admin.advanced.AdvancedServiceFactory;
import org.artifactory.ui.rest.service.admin.advanced.support.BundleConfigurationWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Michael Pasternak
 */
@Path("userSupport")
@RolesAllowed({AuthorizationService.ROLE_ADMIN})
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UserSupportResource extends BaseResource {

    @Autowired
    private AdvancedServiceFactory advancedServiceFactory;

    @Autowired
    private AuthorizationService authorizationService;

    @Context
    private HttpServletRequest httpServletRequest;

    @Path("generateBundle")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateBundle(BundleConfigurationImpl bundleConfiguration) throws Exception {
        if (authorizationService.isAnonymous() || !authorizationService.isAdmin()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return runService(advancedServiceFactory.getSupportServiceGenerateBundle(),
                new BundleConfigurationWrapper(bundleConfiguration, httpServletRequest)
        );
    }

    @Path("downloadBundle/{archive: .+}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadBundle(@PathParam("archive") String archive) throws Exception {
        if (authorizationService.isAnonymous() || !authorizationService.isAdmin()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return runService(advancedServiceFactory.getSupportServiceDownloadBundle(), archive);
    }

    @Path("listBundles")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listBundles() throws Exception {
        if (authorizationService.isAnonymous() || !authorizationService.isAdmin()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return runService(advancedServiceFactory.getSupportServiceListBundles());
    }

    @Path("deleteBundle/{archive: .+}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteBundle(@PathParam("archive") String archive) throws Exception {
        if (authorizationService.isAnonymous() || !authorizationService.isAdmin()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return runService(advancedServiceFactory.getSupportServiceDeleteBundle(), archive);
    }
}