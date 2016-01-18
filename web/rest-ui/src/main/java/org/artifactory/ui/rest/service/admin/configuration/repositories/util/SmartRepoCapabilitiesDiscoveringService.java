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

package org.artifactory.ui.rest.service.admin.configuration.repositories.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.artifactory.api.jackson.JacksonFactory;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.ResearchService;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.admin.configuration.repository.remote.RemoteAdvancedRepositoryConfigModel;
import org.artifactory.ui.rest.model.admin.configuration.repository.remote.RemoteBasicRepositoryConfigModel;
import org.artifactory.ui.rest.model.admin.configuration.repository.remote.RemoteNetworkRepositoryConfigModel;
import org.artifactory.ui.rest.model.admin.configuration.repository.remote.RemoteRepositoryConfigModel;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Checks whether remoteRepo is an Artifactory instance and if
 * so, retrieves its capabilities for the given version
 *
 * @author Michael Pasternak
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SmartRepoCapabilitiesDiscoveringService<T extends RemoteRepositoryConfigModel> implements RestService<T> {
    protected static final Logger log = LoggerFactory.getLogger(SmartRepoCapabilitiesDiscoveringService.class);

    @Autowired
    ResearchService researchService;

    @Override
    public void execute(ArtifactoryRestRequest<T> artifactoryRequest, RestResponse artifactoryResponse) {

        RemoteRepositoryConfigModel repositoryModel = artifactoryRequest.getImodel();
        RemoteAdvancedRepositoryConfigModel remoteRepoAdvancedModel = repositoryModel.getAdvanced();
        if (remoteRepoAdvancedModel == null) {
            artifactoryResponse.error("Network details was not sent.")
                    .responseCode(HttpStatus.SC_BAD_REQUEST);
            return;
        }
        RemoteBasicRepositoryConfigModel basicModel = repositoryModel.getBasic();
        if (basicModel == null || StringUtils.isEmpty(basicModel.getUrl())) {
            artifactoryResponse.error("Remote Url was not sent.")
                    .responseCode(HttpStatus.SC_BAD_REQUEST);
            return;
        }
        String remoteRepoUrl = PathUtils.addTrailingSlash(basicModel.getUrl());
        if (remoteRepoUrl == null) {
            artifactoryResponse.error("Remote repo url was not sent.")
                    .responseCode(HttpStatus.SC_BAD_REQUEST);
            return;
        }
        RemoteNetworkRepositoryConfigModel networkModel = remoteRepoAdvancedModel.getNetwork();
        CloseableHttpClient client = null;
        try {
            client = RemoteRepositoryProvider.getRemoteRepoHttpClient(remoteRepoUrl, networkModel);
            artifactoryResponse
                    .iModel(JacksonFactory.createObjectMapper().writeValueAsString(
                            researchService.getSmartRepoCapabilities(remoteRepoUrl, client)))
                    .responseCode(HttpStatus.SC_OK);
        } catch (IOException e) {
            log.error("Cannot serialize SmartRepoCapabilities", e);
            artifactoryResponse.responseCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } finally {
            IOUtils.closeQuietly(client);
        }
    }
}
