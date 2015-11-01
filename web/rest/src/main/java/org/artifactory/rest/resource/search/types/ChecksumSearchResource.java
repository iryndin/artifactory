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

package org.artifactory.rest.resource.search.types;

import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.properties.ArtifactPropertiesAddon;
import org.artifactory.addon.rest.AuthorizationRestException;
import org.artifactory.addon.rest.MissingRestAddonException;
import org.artifactory.addon.rest.RestAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryBrowsingService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.constant.SearchRestConstants;
import org.artifactory.api.rest.search.result.InfoRestSearchResult;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.search.property.PropertySearchControls;
import org.artifactory.api.search.property.PropertySearchResult;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.fs.FileInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.common.exception.BadRequestException;
import org.artifactory.rest.common.exception.RestException;
import org.artifactory.rest.common.list.StringList;
import org.artifactory.rest.util.StorageInfoHelper;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Set;

/**
 * Exposes the checksum searcher to REST via the REST addon
 *
 * @author Noam Y. Tenne
 */
public class ChecksumSearchResource {
    private static final Logger log = LoggerFactory.getLogger(ChecksumSearchResource.class);

    private AuthorizationService authorizationService;

    private RestAddon restAddon;
    private RepositoryService repositoryService;
    private RepositoryBrowsingService repoBrowsingService;
    private HttpServletRequest request;

    public ChecksumSearchResource(AuthorizationService authorizationService, RestAddon restAddon,
            RepositoryService repositoryService, RepositoryBrowsingService repoBrowsingService,
            HttpServletRequest request) {
        this.authorizationService = authorizationService;
        this.restAddon = restAddon;
        this.repositoryService = repositoryService;
        this.repoBrowsingService = repoBrowsingService;
        this.request = request;
    }

    /**
     * Searches for artifacts by checksum
     *
     * @param md5Checksum   MD5 checksum value
     * @param sha1Checksum  SHA1 checksum value
     * @param reposToSearch Specific repositories to search within
     * @return Search results object
     */
    @GET
    @Produces({SearchRestConstants.MT_CHECKSUM_SEARCH_RESULT, MediaType.APPLICATION_JSON})
    public InfoRestSearchResult get(@QueryParam(SearchRestConstants.PARAM_MD5_CHECKSUM) String md5Checksum,
                                    @QueryParam(SearchRestConstants.PARAM_SHA1_CHECKSUM) String sha1Checksum,
                                    @QueryParam(SearchRestConstants.PARAM_SHA256_CHECKSUM) String sha256Checksum,
                                    @QueryParam(SearchRestConstants.PARAM_REPO_TO_SEARCH) StringList reposToSearch)
            throws IOException, AuthorizationRestException {
        if (!authorizationService.isAuthenticated()) {
            throw new AuthorizationRestException();
        }

        try {
            return search(md5Checksum, sha1Checksum, sha256Checksum, reposToSearch);
        } catch (MissingRestAddonException mrae) {
            throw mrae;
        } catch (IllegalArgumentException iae) {
            throw new BadRequestException(iae.getMessage());
        } catch (Exception e) {
            String errorMessage =
                    String.format("Error occurred while searching for artifacts by checksum: %s", e.getMessage());
            log.error(errorMessage, e);
            throw new RestException(errorMessage);
        }
    }

    private InfoRestSearchResult search(String md5Checksum, String sha1Checksum, String sha256, StringList reposToSearch) {
        InfoRestSearchResult resultToReturn = new InfoRestSearchResult();
        if (matchToSha256Length(sha256)) {
            findArtifactBySha256Checksum(sha256, reposToSearch, resultToReturn);
            return resultToReturn;
        }
        Set<RepoPath> matchingArtifacts = restAddon.searchArtifactsByChecksum(md5Checksum, sha1Checksum, reposToSearch);
        for (RepoPath matchingArtifact : matchingArtifacts) {
            FileInfo fileInfo = repositoryService.getFileInfo(matchingArtifact);
            StorageInfoHelper storageInfoHelper = new StorageInfoHelper(request, repositoryService, repoBrowsingService,
                    fileInfo);
            resultToReturn.results.add(storageInfoHelper.createStorageInfo());
        }
        return resultToReturn;
    }

    /**
     * find artifact by sha 256 checksum property
     *
     * @param sha256         - sha256
     * @param reposToSearch  - list of repositories
     * @param resultToReturn - artifact
     */
    private void findArtifactBySha256Checksum(String sha256, StringList reposToSearch, InfoRestSearchResult resultToReturn) {
        SearchService searchService = InternalContextHelper.get().beanForType(SearchService.class);
        ArtifactPropertiesAddon artifactPropertiesAddon = ContextHelper.get().beanForType(AddonsManager.class)
                .addonByType(ArtifactPropertiesAddon.class);
        PropertySearchControls sha256PropertyControlSearch = artifactPropertiesAddon.getSha256PropertyControlSearch(sha256, reposToSearch);
        // search property
        if (sha256PropertyControlSearch != null) {
            ItemSearchResults<PropertySearchResult> propertyResults = searchService.searchPropertyAql(sha256PropertyControlSearch);
            propertyResults.getResults().forEach(result -> {
                FileInfo fileInfo = (FileInfo) result.getItemInfo();
                if(authorizationService.canRead(fileInfo.getRepoPath())) {
                    StorageInfoHelper storageInfoHelper = new StorageInfoHelper(request, repositoryService,
                            repoBrowsingService,fileInfo);
                    resultToReturn.results.add(storageInfoHelper.createStorageInfo());
                }
            });
        }
    }

    /**
     * check if checksum length match sha 256 length
     *
     * @return - true if match sha256
     */
    private boolean matchToSha256Length(String sha256) {
        return StringUtils.length(sha256) == ChecksumType.sha256.length();
    }
}
