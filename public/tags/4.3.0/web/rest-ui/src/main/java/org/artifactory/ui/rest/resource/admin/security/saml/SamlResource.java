package org.artifactory.ui.rest.resource.admin.security.saml;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.ui.rest.model.admin.security.saml.Saml;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.ui.rest.service.admin.security.SecurityServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Chen Keinan
 */
@RolesAllowed({AuthorizationService.ROLE_ADMIN})
@Component
@Path("saml/config")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SamlResource extends BaseResource {

    @Autowired
    private SecurityServiceFactory securityFactory;

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSaml(Saml saml)
            throws Exception {
        return runService(securityFactory.updateSaml(), saml);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSaml()
            throws Exception {
        return runService(securityFactory.getSaml());
    }
}
