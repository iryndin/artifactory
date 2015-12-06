package org.artifactory.ui.rest.resource.admin.advanced.storagesummary;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.ui.rest.service.admin.advanced.AdvancedServiceFactory;
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
@Path("storagesummary")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StorageSummaryResource extends BaseResource {

    @Autowired
    protected AdvancedServiceFactory advanceFactory;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStorageSummary()
            throws Exception {
        return runService(advanceFactory.getStorageSummaryService());
    }
}
