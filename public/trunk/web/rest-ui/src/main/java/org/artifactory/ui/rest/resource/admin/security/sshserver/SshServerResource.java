package org.artifactory.ui.rest.resource.admin.security.sshserver;

import com.sun.jersey.multipart.FormDataMultiPart;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.ui.rest.model.admin.security.sshserver.SshServer;
import org.artifactory.ui.rest.model.utils.FileUpload;
import org.artifactory.ui.rest.service.admin.security.SecurityServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Noam Y. Tenne
 */
@RolesAllowed({AuthorizationService.ROLE_ADMIN})
@Component
@Path("sshserver")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SshServerResource extends BaseResource {
    @Autowired
    private SecurityServiceFactory securityFactory;

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateSshServer(SshServer sshServer)
            throws Exception {
        return runService(securityFactory.updateSshServer(), sshServer);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSshServer()
            throws Exception {
        return runService(securityFactory.getSshServer());
    }

    @POST
    @Path("install")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadSshKey(FormDataMultiPart formParams) throws Exception {
        FileUpload fileUpload = new FileUpload(formParams);
        return runService(securityFactory.uploadSshServerKey(), fileUpload);
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeSigningKey()
            throws Exception {
        return runService(securityFactory.removeSshServerKeyService());
    }
}
