/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2014 JFrog Ltd.
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

package org.artifactory.rest.resource.ssh;

import org.apache.http.HttpStatus;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SshAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.file.Files;


/**
 * @author Noam Y. Tenne
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path(SshResource.PATH_ROOT)
public class SshResource {
    private static final Logger log = LoggerFactory.getLogger(SshResource.class);

    public static final String PATH_ROOT = "ssh";

    @Autowired
    private SshAuthService sshAuthService;

    @Context
    private HttpServletRequest request;

    @GET
    @Path("key/public")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPublicKey() {
        java.nio.file.Path key = sshAuthService.getPublicKeyFile();
        if (Files.notExists(key)) {
            return Response.status(HttpStatus.SC_NOT_FOUND).entity(
                    "No public SSH server key exists in Artifactory").build();
        }

        return Response.status(HttpStatus.SC_OK)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + key.getFileName().toString())
                .entity(key.toFile()).build();
    }

    @PUT
    @Path("key/public")
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    public Response putPublicKey(String publicKey) {
        try {
            sshAuthService.savePublicKey(publicKey);
        } catch (Exception e) {
            String message = "Failed to install public SSH server key";
            log.error(message, e);
            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(message).build();
        }
        return Response.status(HttpStatus.SC_OK).entity("Successfully installed the public SSH server key").build();

    }

    @PUT
    @Path("key/private")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    public Response putPrivateKey(String privateKey) {
        try {
            sshAuthService.savePrivateKey(privateKey);
        } catch (Exception e) {
            String message = "Failed to install private SSH server key";
            log.error(message, e);
            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(message).build();
        }
        return Response.status(HttpStatus.SC_OK).entity("Successfully installed the private SSH server key").build();
    }
}