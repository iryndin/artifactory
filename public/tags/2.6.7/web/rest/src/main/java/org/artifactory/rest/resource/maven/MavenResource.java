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

package org.artifactory.rest.resource.maven;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.rest.RestAddon;
import org.artifactory.api.rest.constant.MavenRestConstants;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.list.StringList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A resource for manually running maven indexer
 *
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Path(MavenRestConstants.PATH_ROOT)
@RolesAllowed({AuthorizationService.ROLE_ADMIN})
public class MavenResource {

    @Autowired
    private AddonsManager addonsManager;

    @POST
    @Produces({MediaType.TEXT_PLAIN})
    public Response runIndexer(@QueryParam(MavenRestConstants.PARAM_REPOS_TO_INDEX) StringList reposToIndex,
            @QueryParam(MavenRestConstants.PARAM_FORCE) int force) {
        RestAddon restAddon = addonsManager.addonByType(RestAddon.class);
        return restAddon.runMavenIndexer(reposToIndex, force);
    }
}
