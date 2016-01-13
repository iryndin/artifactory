/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.rest.resource.security;

import org.artifactory.api.rest.constant.SecurityRestConstants;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.rest.common.dataholder.PasswordContainer;
import org.artifactory.security.exceptions.PasswordChangeException;
import org.artifactory.security.exceptions.PasswordExpireException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Michael Pasternak
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path(SecurityRestConstants.PATH_ROOT + "/users/authorization")
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
public class SecurityUserResource {

    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    SecurityService securityService;

    @POST
    @RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("changePassword")
    public Response changePassword(PasswordContainer passwordContainer) {
        try {
            securityService.changePassword(
                    passwordContainer.getUserName(),
                    passwordContainer.getOldPassword(),
                    passwordContainer.getNewPassword1(),
                    passwordContainer.getNewPassword2()
            );
            return Response.status(Response.Status.OK).entity("Password has been successfully changed").build();
        } catch (PasswordChangeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("expirePassword")
    public Response expirePassword(List<String> users) {
        if(users!=null) {
            try {
                users.parallelStream().forEach(u -> {
                    securityService.expireUserCredentials(u);
                });
            } catch (PasswordExpireException e) {
                return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
            }
            return Response.status(Response.Status.OK).entity("Users credentials have been successfully expired").build();
        }
        return Response.status(Response.Status.BAD_REQUEST).entity("Bad request, please verify username/s").build();
    }

    @POST
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    @Produces(MediaType.TEXT_PLAIN)
    @Path("expirePassword/{userName}")
    public Response expirePassword(@PathParam("userName") String userName) {
        try {
            securityService.expireUserCredentials(userName);
        } catch (PasswordExpireException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        return Response.status(Response.Status.OK).entity(userName + "'s credentials have been expired").build();
    }

    @POST
    @Path("unExpirePassword/{userName}")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    public Response unExpireUserPassword(@PathParam("userName") String userName) {
        try {
            securityService.unExpireUserCredentials(userName);
        } catch (PasswordExpireException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        return Response.status(Response.Status.OK).entity(userName + "'s credentials have been expired").build();
    }

    @POST
    @Path("expirePasswordForAllUsers")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    public Response expirePasswordForAllUsers() {
        try {
            securityService.expireCredentialsForAllUsers();
        } catch (PasswordExpireException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        return Response.status(Response.Status.OK).entity("Credentials have been successfully expired for all users").build();
    }

    @POST
    @Path("unExpirePasswordForAllUsers")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    public Response unExpirePasswordForAllUsers() {
        try {
            securityService.unExpireCredentialsForAllUsers();
        } catch (PasswordExpireException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        return Response.status(Response.Status.OK).entity("Credentials have been successfully un-expired for all users").build();
    }
}
