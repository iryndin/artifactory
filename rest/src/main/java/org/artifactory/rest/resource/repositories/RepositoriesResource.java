/*
 * This file is part of Artifactory.
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

package org.artifactory.rest.resource.repositories;

import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.RepositoriesRestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

/**
 * A resource to manage all repository related operations
 *
 * @author Noam Tenne
 */
@Path("/" + RepositoriesRestConstants.REPOSITORIES_PATH_ROOT)
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RepositoriesResource {

    @Context
    private HttpServletResponse httpResponse;

    @Autowired
    RepositoryService repositoryService;

    /**
     * Remote repository resource delegator
     *
     * @return RemoteRepoResource
     */
    @Path(RepositoriesRestConstants.REPOSITORIES_PATH_REMOTE)
    public RemoteRepoResource getRemoteResource() {
        return new RemoteRepoResource(httpResponse, repositoryService);
    }
}
