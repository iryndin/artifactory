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

package org.artifactory.rest.resource.repositories;

import com.google.common.collect.Lists;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.common.StatusEntry;
import org.artifactory.api.common.StatusEntryLevel;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.constant.RepositoriesRestConstants;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoDetails;
import org.artifactory.repo.RepoDetailsType;
import org.artifactory.rest.resource.system.StreamStatusHolder;
import org.artifactory.rest.util.RestUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.artifactory.repo.RepoDetailsType.*;

/**
 * A resource to manage all repository related operations
 *
 * @author Noam Tenne
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path(RepositoriesRestConstants.PATH_ROOT)
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
public class RepositoriesResource {

    private static final Logger log = LoggerFactory.getLogger(RepositoriesResource.class);


    @Context
    HttpServletRequest httpRequest;

    @Context
    HttpServletResponse httpResponse;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    SearchService searchService;

    /**
     * Repository configuration resource delegator
     *
     * @return RepoConfigurationResource
     */
    @Path("{repoKey}/" + RepositoriesRestConstants.PATH_CONFIGURATION)
    public RepoConfigurationResource getRepoConfigResource(@PathParam("repoKey") String repoKey) {
        return new RepoConfigurationResource(repositoryService, repoKey);
    }

    /**
     * Returns a JSON list of repository details.
     * <p/>
     * NOTE: Used by CI integration to get a list of deployment repository targets.
     *
     * @param repoType Name of repository type, as defined in {@link org.artifactory.repo.RepoDetailsType}. Can be null
     * @return JSON repository details list. Will return details of defined type, if given. And will return details of
     *         all types if not
     * @throws Exception
     */
    @GET
    @Produces(RepositoriesRestConstants.MT_REPOSITORY_DETAILS_LIST)
    public List<RepoDetails> getAllRepoDetails(@QueryParam(RepositoriesRestConstants.PARAM_REPO_TYPE)
    String repoType) throws Exception {
        return getRepoDetailsList(repoType);
    }

    @POST
    @Consumes("application/x-www-form-urlencoded")
    public void importRepositories(
            //The base path to import from (may contain a single repo or multiple repos with named sub folders
            @QueryParam(RepositoriesRestConstants.PATH) String path,
            //Empty/null repo -> all
            @QueryParam(RepositoriesRestConstants.TARGET_REPO) String targetRepo,
            //Include metadata - default 1
            @QueryParam(RepositoriesRestConstants.INCLUDE_METADATA) String includeMetadata,
            //Verbose - default 0
            @QueryParam(RepositoriesRestConstants.VERBOSE) String verbose) throws IOException {
        StreamStatusHolder statusHolder = new StreamStatusHolder(httpResponse);
        String repoNameToImport = targetRepo;
        if (StringUtils.isBlank(repoNameToImport)) {
            repoNameToImport = "All repositories";
        }
        statusHolder.setStatus("Starting Repositories Import of " + repoNameToImport + " from " + path, log);
        if (!authorizationService.isAdmin()) {
            statusHolder.setError(
                    "User " + authorizationService.currentUsername() + " is not permitted to import repositories",
                    HttpStatus.SC_UNAUTHORIZED, log);
        }
        if (StringUtils.isEmpty(path)) {
            statusHolder.setError("Source directory path may not be empty.", HttpStatus.SC_BAD_REQUEST, log);
        }
        File baseDir = new File(path);
        if (!baseDir.exists()) {
            statusHolder.setError("Directory " + path + " does not exist.", HttpStatus.SC_BAD_REQUEST, log);
        }
        ImportSettings importSettings = new ImportSettings(baseDir, statusHolder);
        if (StringUtils.isNotBlank(includeMetadata)) {
            importSettings.setIncludeMetadata(Integer.parseInt(includeMetadata) == 1);
        }
        if (StringUtils.isNotBlank(verbose)) {
            importSettings.setVerbose(Integer.parseInt(verbose) == 1);
        }
        try {
            if (StringUtils.isBlank(targetRepo)) {
                repositoryService.importAll(importSettings);
            } else {
                importSettings.setIndexMarkedArchives(true);
                repositoryService.importRepo(targetRepo, importSettings);
            }
        } catch (Exception e) {
            statusHolder.setError("Unable to import repository", e, log);
        } finally {
            if (!importSettings.isIndexMarkedArchives()) {
                searchService.indexMarkedArchives();
            }
        }
        StringBuilder builder = new StringBuilder();
        List<StatusEntry> statusEntries = statusHolder.getEntries(StatusEntryLevel.INFO);
        for (StatusEntry entry : statusEntries) {
            String message = entry.getMessage();
            builder.append(message).append("\n");
        }
    }


    /**
     * Returns a list of repository details
     *
     * @param repoType Name of repository type, as defined in {@link org.artifactory.repo.RepoDetailsType}. Can be null
     * @return List of repository details
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

            detailsList.add(new RepoDetails(key, remoteRepo.getDescription(), REMOTE, remoteRepo.getUrl(),
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
        return new StringBuilder().append(RestUtils.getServletContextUrl(httpRequest)).append("/").append(repoKey)
                .toString();
    }

    /**
     * Returns the repository configuration URL
     *
     * @param repoKey Key of repository to assemble URL for
     * @return Repository configuration URL
     */
    private String getRepoConfigUrl(String repoKey) {
        return new StringBuilder().append(RestUtils.getRestApiUrl(httpRequest)).append("/")
                .append(RepositoriesRestConstants.PATH_ROOT).append("/").append(repoKey).append("/").
                        append(RepositoriesRestConstants.PATH_CONFIGURATION).toString();
    }
}