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

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.rest.constant.SecurityRestConstants;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.descriptor.security.PasswordExpirationPolicy;
import org.artifactory.descriptor.security.PasswordSettings;
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
 * @author Michael Pasternak
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path(SecurityRestConstants.PATH_ROOT + "/" + "configuration")
@RolesAllowed({AuthorizationService.ROLE_ADMIN})
public class SecurityConfigurationResource {

    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    SecurityService securityService;

    @Autowired
    private CentralConfigService centralConfigService;

    @PUT
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("passwordExpirationPolicy")
    public Response updatePasswordPolicy(PasswordExpirationPolicy newPasswordExpirationPolicy) {
        if (newPasswordExpirationPolicy.getExpiresIn() > 999 || newPasswordExpirationPolicy.getExpiresIn() < 1)
            return Response.status(Response.Status.BAD_REQUEST).entity("Password expiration must be between 1 - 999").build();

        PasswordSettings originalPasswordSettings =
                centralConfigService.getDescriptor().getSecurity().getPasswordSettings();

        if (originalPasswordSettings == null) {
            PasswordSettings defaultPasswordSettings = new PasswordSettings();
            defaultPasswordSettings.setExpirationPolicy(newPasswordExpirationPolicy);
            centralConfigService.getDescriptor().getSecurity().setPasswordSettings(defaultPasswordSettings);
        } else {
            if(originalPasswordSettings.getExpirationPolicy() == null) {
                originalPasswordSettings.setExpirationPolicy(newPasswordExpirationPolicy);
            } else {
                if(newPasswordExpirationPolicy.isEnabled() != null)
                    originalPasswordSettings.getExpirationPolicy().setEnabled(newPasswordExpirationPolicy.isEnabled());
                if(newPasswordExpirationPolicy.getExpiresIn() != null)
                    originalPasswordSettings.getExpirationPolicy().setExpiresIn(newPasswordExpirationPolicy.getExpiresIn());
                if(newPasswordExpirationPolicy.isNotifyByEmail() != null)
                    originalPasswordSettings.getExpirationPolicy().setNotifyByEmail(newPasswordExpirationPolicy.isNotifyByEmail());
            }
        }
        centralConfigService.saveEditedDescriptorAndReload(centralConfigService.getDescriptor());
        return Response.ok().entity("Successfully updated password expiration policy").build();
    }

    @GET
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("passwordExpirationPolicy")
    public PasswordExpirationPolicy getPasswordPolicy() {
        return centralConfigService.getDescriptor().getSecurity().getPasswordSettings().getExpirationPolicy();
    }
}
