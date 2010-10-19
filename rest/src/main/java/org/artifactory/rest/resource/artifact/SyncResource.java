/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.rest.resource.artifact;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.rest.RestAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.AuthorizationService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.artifactory.api.rest.constant.ArtifactRestConstants.*;

/**
 * A REST api resource for remote folder replication
 *
 * @author Noam Y. Tenne
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path(PATH_SYNC)
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
public class SyncResource {

    @Context
    HttpServletResponse httpResponse;

    public enum Overwrite {
        never, force
    }

    /**
     * Locally replicates the given remote path
     *
     * @param path      The path of the remote folder to replicate
     * @param progress  One to show transfer progress, Zero or other to show normal transfer completion message
     * @param mark      Every how many bytes to print a progress mark (when using progress tracking policy)
     * @param delete    One to delete existing files which do not exist in the remote source, Zero to keep
     * @param overwrite Never for never replacing an existing file and force for replacing existing files
     *                  (only if the local file is older than the target) and Null for default of force.
     * @return Response
     */
    @GET
    @Path("{path: .+}")
    @Produces({MediaType.TEXT_PLAIN})
    public Response sync(
            @PathParam("path") String path,
            @QueryParam(PARAM_PROGRESS) @DefaultValue("1") int progress,
            @QueryParam(PARAM_MARK) int mark,
            @QueryParam(PARAM_DELETE) int delete,
            @QueryParam(PARAM_OVERWRITE) Overwrite overwrite) throws Exception {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        return addonsManager.addonByType(RestAddon.class).replicate(path, progress, mark, delete, overwrite,
                httpResponse);
    }
}
