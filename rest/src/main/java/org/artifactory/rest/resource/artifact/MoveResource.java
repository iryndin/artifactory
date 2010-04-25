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
import org.artifactory.addon.RestAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.rest.artifact.MoveCopyResult;
import org.artifactory.api.rest.constant.ArtifactRestConstants;
import org.artifactory.api.security.AuthorizationService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import static org.artifactory.api.rest.constant.ArtifactRestConstants.PATH_MOVE;

/**
 * REST API used to move an artifact from one path to another.
 *
 * @author Tomer Cohen
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path(PATH_MOVE)
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
public class MoveResource {

    /**
     * Move an item (file or folder) from one path to another.
     *
     * @param path   The source path.
     * @param target The target path.
     * @param dryRun Flag whether to perform a dry run before executing the actual move.
     * @return The operation result
     * @throws Exception
     */
    @POST
    @Path("{path: .+}")
    @Produces({ArtifactRestConstants.MT_COPY_MOVE_RESULT})
    public MoveCopyResult move(
            // The path of the source item to be moved/copied
            @PathParam("path") String path,
            // The target path to to move/copy the item.
            @FormParam(ArtifactRestConstants.PARAM_TARGET) String target,
            // Flag to indicate whether to perform a dry run first. default false
            @FormParam(ArtifactRestConstants.PARAM_DRY_RUN) int dryRun) throws Exception {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        return addonsManager.addonByType(RestAddon.class).move(path, target, dryRun);
    }
}
