package org.artifactory.rest.resource.storagesummary;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.rest.services.ConfigServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Chen Keinan
 */
@RolesAllowed({AuthorizationService.ROLE_ADMIN})
@Component
@Path("storageinfo")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StorageSummaryInfoResource extends BaseResource {

    @Autowired
    protected ConfigServiceFactory configServiceFactory;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStorageSummary()
            throws Exception {
        return runService(configServiceFactory.getStorageSummaryService());
    }
}
