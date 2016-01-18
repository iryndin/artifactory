package org.artifactory.ui.rest.resource.admin.security.general;

import com.google.common.base.Strings;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.descriptor.security.UserLockPolicy;
import org.artifactory.ui.rest.model.admin.security.general.SecurityConfig;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.ui.rest.service.admin.security.SecurityServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Chen Keinan
 */
@RolesAllowed({AuthorizationService.ROLE_ADMIN})
@Component
@Path("securityconfig")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GeneralSecurityConfigResource extends BaseResource {

    @Autowired
    private SecurityServiceFactory securityFactory;

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateConfig(SecurityConfig securityConfig)
            throws Exception {
        return runService(securityFactory.updateSecurityConfig(),securityConfig);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig()
            throws Exception {
        return runService(securityFactory.getSecurityConfig());
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("userLockPolicy")
    public Response updateUserLockPolicy(UserLockPolicy userLockPolicy) {
        return runService(securityFactory.updateUserLockPolicy(), userLockPolicy);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("userLockPolicy")
    public Response getUserLockPolicy() {
        return runService(securityFactory.getUserLockPolicy());
    }

    @POST
    @Path("unlockUsers/{userName}")
    public Response unlockUser(@PathParam("userName") String userName) {
        return runService(securityFactory.unlockUser(), userName);
    }

    @POST
    @Path("unlockAllUsers")
    public Response unlockAllUsers() {
        return runService(securityFactory.unlockAllUsers());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("unlockUsers")
    public Response unlockUsers(List<String> users) {
        return runService(securityFactory.unlockUsers(), users);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("lockedUsers")
    public Response getAllLockedUsers() {
        return runService(securityFactory.getAllLockedUsers());
    }
}
