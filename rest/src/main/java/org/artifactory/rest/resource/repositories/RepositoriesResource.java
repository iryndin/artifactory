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

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.RepositoriesRestConstants;
import org.artifactory.api.rest.RestConstants;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.repo.RepoDetails;
import org.artifactory.repo.RepoDetailsType;
import static org.artifactory.repo.RepoDetailsType.*;
import org.artifactory.util.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import java.util.List;

/**
 * A resource to manage all repository related operations
 *
 * @author Noam Tenne
 */
@Path("/" + RepositoriesRestConstants.REPOSITORIES_PATH_ROOT)
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
public class RepositoriesResource {

    @Context
    private HttpServletRequest httpRequest;

    @Context
    private HttpServletResponse httpResponse;

    @Autowired
    RepositoryService repositoryService;

    /**
     * Remote repository configuration resource delegator
     *
     * @return RepoConfigurationResource
     */
    @Path("{remoteRepoKey}/" + RepositoriesRestConstants.REPOSITORIES_PATH_CONFIGURATION)
    public RepoConfigurationResource getRepoConfigResource(@PathParam("remoteRepoKey") String remoteRepoKey) {
        return new RepoConfigurationResource(httpResponse, repositoryService, remoteRepoKey);
    }

    /**
     * Returns a JSON list of repository details
     *
     * @param repoType Name of repository type, as defined in {@link org.artifactory.repo.RepoDetailsType}. Can be null
     * @return JSON repository details list. Will return details of defined type, if given. And will return details of
     *         all types if not
     * @throws Exception
     */
    @GET
    @Produces("application/vnd.org.jfrog.artifactory+json")
    public List<RepoDetails> getAllRepoDetails(@QueryParam(RepositoriesRestConstants.REPOSITORIES_PARAM_REPO_TYPE)
    String repoType) throws Exception {
        return getRepoDetailsList(repoType);
    }

    /**
     * Returns a list of repository details
     *
     * @param repoType Name of repository type, as defined in {@link org.artifactory.repo.RepoDetailsType}. Can be null
     * @return List of repository details
     * @throws Exception
     */
    private List<RepoDetails> getRepoDetailsList(String repoType) throws Exception {
        List<RepoDetails> detailsList = Lists.newArrayList();

        boolean noTypeSelected = StringUtils.isBlank(repoType);
        RepoDetailsType selectedType = null;
        if (!noTypeSelected) {
            try {
                selectedType = valueOf(repoType.toUpperCase());
            } catch (IllegalArgumentException e) {
                //On an unfound type, return empty list
                return detailsList;
            }
        }

        if (noTypeSelected || LOCAL.equals(selectedType)) {
            addLocalOrVirtualRepoDetails(detailsList, repositoryService.getLocalRepoDescriptors(), LOCAL);
        }

        if (noTypeSelected || REMOTE.equals(selectedType)) {
            addRemoteRepoDetails(detailsList);
        }

        if (noTypeSelected || VIRTUAL.equals(selectedType)) {
            addLocalOrVirtualRepoDetails(detailsList, repositoryService.getVirtualRepoDescriptors(), VIRTUAL);
        }
        return detailsList;
    }

    /**
     * Adds a list of local or virtual repositories to the repository details list
     *
     * @param detailsList List that details should be appended to
     * @param reposToAdd  List of repositories to add details of
     * @param type        Type of repository which is being added
     */
    private void addLocalOrVirtualRepoDetails(List<RepoDetails> detailsList, List<? extends RepoDescriptor> reposToAdd,
            RepoDetailsType type) {
        for (RepoDescriptor repoToAdd : reposToAdd) {
            String key = repoToAdd.getKey();
            detailsList.add(new RepoDetails(key, repoToAdd.getDescription(), type, getRepoUrl(key)));
        }
    }

    /**
     * Adds a list of remote repositories to the repo details list
     *
     * @param detailsList List that details should be appended to
     */
    private void addRemoteRepoDetails(List<RepoDetails> detailsList) {
        List<RemoteRepoDescriptor> remoteRepos = repositoryService.getRemoteRepoDescriptors();

        for (RemoteRepoDescriptor remoteRepo : remoteRepos) {
            String key = remoteRepo.getKey();

            String configUrl = null;
            if (remoteRepo.isShareConfiguration()) {
                configUrl = getRepoConfigUrl(key);
            }

            detailsList.add(new RepoDetails(key, remoteRepo.getDescription(), REMOTE, getRepoUrl(key),
                    configUrl));
        }
    }

    /**
     * Returns the repository browse URL
     *
     * @param repoKey Key of repository to assemble URL for
     * @return Repository URL
     */
    private String getRepoUrl(String repoKey) {
        return new StringBuilder().append(getServletContextUrl()).append("/").append(repoKey).toString();
    }

    /**
     * Returns the repository configuration URL
     *
     * @param repoKey Key of repository to assemble URL for
     * @return Repository configuration URL
     */
    private String getRepoConfigUrl(String repoKey) {
        return new StringBuilder().append(getServletContextUrl()).append("/").append(RestConstants.PATH_API).append("/")
                .append(RepositoriesRestConstants.REPOSITORIES_PATH_ROOT).append("/").append(repoKey).append("/").
                        append(RepositoriesRestConstants.REPOSITORIES_PATH_CONFIGURATION).toString();
    }

    /**
     * Returns the servlet context URL based on the HTTP request
     *
     * @return Servlet context URL
     */
    private String getServletContextUrl() {
        return HttpUtils.getServletContextUrl(httpRequest);
    }
}