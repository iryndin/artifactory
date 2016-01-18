package org.artifactory.ui.rest.service.admin.configuration.repositories.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.admin.configuration.repository.remote.RemoteAdvancedRepositoryConfigModel;
import org.artifactory.ui.rest.model.admin.configuration.repository.remote.RemoteBasicRepositoryConfigModel;
import org.artifactory.ui.rest.model.admin.configuration.repository.remote.RemoteNetworkRepositoryConfigModel;
import org.artifactory.ui.rest.model.admin.configuration.repository.remote.RemoteRepositoryConfigModel;
import org.artifactory.ui.rest.model.admin.configuration.repository.typespecific.TypeSpecificConfigModel;
import org.artifactory.util.HttpClientUtils;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.apache.http.HttpStatus.*;
import static org.artifactory.ui.rest.service.admin.configuration.repositories.util.RemoteRepositoryProvider.getRemoteRepoHttpClient;

/**
 * @author Aviad Shikloshi
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RemoteRepositoryTestUrl<T extends RemoteRepositoryConfigModel> implements RestService<T> {
    protected static final Logger log = LoggerFactory.getLogger(RemoteRepositoryTestUrl.class);

    @Override
    public void execute(ArtifactoryRestRequest<T> artifactoryRequest, RestResponse artifactoryResponse) {
        RemoteRepositoryConfigModel repositoryModel = artifactoryRequest.getImodel();
        testConnection(artifactoryResponse, repositoryModel);
    }

    public void testConnection(RestResponse artifactoryResponse, RemoteRepositoryConfigModel repositoryModel) {
        RemoteAdvancedRepositoryConfigModel remoteRepoAdvancedModel = repositoryModel.getAdvanced();
        RemoteBasicRepositoryConfigModel basicModel = repositoryModel.getBasic();
        TypeSpecificConfigModel repoTypeModel = repositoryModel.getTypeSpecific();
        if (!validateModels(artifactoryResponse, basicModel, remoteRepoAdvancedModel, repoTypeModel)) {
            return;
        }
        String remoteRepoUrl = PathUtils.addTrailingSlash(basicModel.getUrl());
        RemoteNetworkRepositoryConfigModel networkModel = remoteRepoAdvancedModel.getNetwork();
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;
        try {
            client = getRemoteRepoHttpClient(remoteRepoUrl, networkModel);
            HttpRequestBase request = TestMethodFactory.createTestMethod(remoteRepoUrl, repoTypeModel.getRepoType(),
                    remoteRepoAdvancedModel.getQueryParams());
            response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            boolean success = testSucceeded(statusCode);
            if (!success) {
                IOUtils.closeQuietly(response);
                success = handleSpecialCases(repositoryModel, client, response);
            }
            if (!success) {
                artifactoryResponse.error("Connection failed: Error " + statusCode + ": "
                        + response.getStatusLine().getReasonPhrase()).responseCode(SC_BAD_REQUEST);
            } else {
                artifactoryResponse.info("Successfully connected to server");
            }
        } catch (IOException e) {
            artifactoryResponse.error("Connection failed with exception: " + HttpClientUtils.getErrorMessage(e))
                    .responseCode(SC_BAD_REQUEST);
            log.debug("Test connection to '" + remoteRepoUrl + "' failed with exception", e);
        } finally {
            IOUtils.closeQuietly(response);
            IOUtils.closeQuietly(client);
        }
    }

    private boolean handleSpecialCases(RemoteRepositoryConfigModel repositoryModel, CloseableHttpClient client,
            CloseableHttpResponse response) throws IOException {
        boolean success;
        success = tryS3(client, repositoryModel, response.getFirstHeader("Server"));
        if (!success) {
            success = testDockerHub(response);
        }
        return success;
    }

    public boolean validateModels(RestResponse artifactoryResponse, RemoteBasicRepositoryConfigModel basicModel,
            RemoteAdvancedRepositoryConfigModel remoteRepoAdvancedModel, TypeSpecificConfigModel repoTypeModel) {
        if (remoteRepoAdvancedModel == null) {
            artifactoryResponse.error("Network details was not sent.").responseCode(SC_BAD_REQUEST);
            return false;
        }
        if (basicModel == null || StringUtils.isEmpty(basicModel.getUrl())) {
            artifactoryResponse.error("Remote Url was not sent.").responseCode(SC_BAD_REQUEST);
            return false;
        }
        if (repoTypeModel == null) {
            artifactoryResponse.error("Package type was not sent.").responseCode(SC_BAD_REQUEST);
            return false;
        }
        return true;
    }

    public boolean tryS3(CloseableHttpClient client, RemoteRepositoryConfigModel repoModel, final Header serverHeader)
            throws IOException {
        // S3 hosted repositories are not hierarchical and does not have a notion of "collection" (folder, directory)
        // Therefore we should not add the trailing slash when testing them
        if (serverHeader != null && "AmazonS3".equals(serverHeader.getValue())) {
            log.debug("Remote repository is hosted on Amazon S3, trying without a trailing slash");
            String remoteRepoUrl = repoModel.getAdvanced().getNetwork().getUrl();
            HttpRequestBase request = TestMethodFactory.createTestMethod(remoteRepoUrl,
                    repoModel.getTypeSpecific().getRepoType(), repoModel.getAdvanced().getQueryParams());
            try (CloseableHttpResponse response = client.execute(request)) {
                return testSucceeded(response.getStatusLine().getStatusCode());
            }
        }
        return false;
    }

    private boolean testDockerHub(CloseableHttpResponse response) {
        // Docker Hub is stupid
        Header dockerHeader = response.getFirstHeader("Docker-Distribution-Api-Version");
        return dockerHeader != null && dockerHeader.getValue().contains("registry");
    }

    private boolean testSucceeded(int statusCode) {
        return (statusCode == SC_OK || statusCode == SC_NO_CONTENT || statusCode == SC_MOVED_TEMPORARILY);
    }
}
