package org.artifactory.ui.rest.service.admin.configuration.repositories.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.crypto.CryptoHelper;
import org.artifactory.ui.rest.model.admin.configuration.repository.remote.RemoteAdvancedRepositoryConfigModel;
import org.artifactory.ui.rest.model.admin.configuration.repository.remote.RemoteBasicRepositoryConfigModel;
import org.artifactory.ui.rest.model.admin.configuration.repository.remote.RemoteNetworkRepositoryConfigModel;
import org.artifactory.ui.rest.model.admin.configuration.repository.remote.RemoteRepositoryConfigModel;
import org.artifactory.ui.rest.model.admin.configuration.repository.typespecific.TypeSpecificConfigModel;
import org.artifactory.util.HttpClientConfigurator;
import org.artifactory.util.HttpClientUtils;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author Aviad Shikloshi
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RemoteRepositoryTestUrl<T extends RemoteRepositoryConfigModel> implements RestService<T> {
    protected static final Logger log = LoggerFactory.getLogger(RemoteRepositoryTestUrl.class);
    protected static final int RETRY_COUNT = 1;
    protected static final int DEFAULT_TIMEOUT = 15000;

    @Autowired
    private CentralConfigService configService;

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
        TypeSpecificConfigModel repoTypeModel = repositoryModel.getTypeSpecific();
        if (repoTypeModel == null) {
            artifactoryResponse.error("Package type was not sent.")
                    .responseCode(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        RemoteNetworkRepositoryConfigModel networkModel = remoteRepoAdvancedModel.getNetwork();
        CloseableHttpClient client = getRemoteRepoTestHttpClient(remoteRepoUrl, networkModel);
        CloseableHttpResponse response = null;

        try {
            HttpRequestBase request = TestMethodFactory.createTestMethod(remoteRepoUrl, remoteRepoAdvancedModel,
                    repoTypeModel);
            response = client.execute(request);
            boolean success = testSucceeded(response.getStatusLine());
            if (!success) {
                IOUtils.closeQuietly(response);
                // S3 hosted repositories are not hierarchical and does not have a notion of "collection" (folder, directory)
                // Therefore we should not add the trailing slash when testing them
                final Header serverHeader = response.getFirstHeader("Server");
                if (serverHeader != null && "AmazonS3".equals(serverHeader.getValue())) {
                    log.debug("Remote repository is hosted on Amazon S3, trying without a trailing slash");
                    remoteRepoUrl = networkModel.getUrl();
                    request = TestMethodFactory.createTestMethod(remoteRepoUrl, remoteRepoAdvancedModel, repoTypeModel);
                    response = client.execute(request);
                    success = testSucceeded(response.getStatusLine());
                }
            }
            if (!success) {
                artifactoryResponse.error(
                        "Connection failed: Error " + response.getStatusLine().getStatusCode() + ": " +
                                response.getStatusLine().getReasonPhrase());
            } else {
                artifactoryResponse.info("Successfully connected to server");
            }
        } catch (IOException e) {
            artifactoryResponse.error("Connection failed with exception: " + HttpClientUtils.getErrorMessage(e));
            log.debug("Test connection to '" + remoteRepoUrl + "' failed with exception", e);
        } finally {
            IOUtils.closeQuietly(response);
            IOUtils.closeQuietly(client);
        }
    }

    private boolean testSucceeded(StatusLine status) {
        return status != null && (status.getStatusCode() == HttpStatus.SC_OK || status.getStatusCode() == HttpStatus.SC_NO_CONTENT);
    }

    private CloseableHttpClient getRemoteRepoTestHttpClient(String remoteUrl,
            RemoteNetworkRepositoryConfigModel networkConfig) {
        // In case network model was not sent in the request we are using the default values
        if (networkConfig == null) {
            networkConfig = new RemoteNetworkRepositoryConfigModel();
        }
        ProxyDescriptor proxyDescriptor = configService.getDescriptor().getProxy(networkConfig.getProxy());
        int socketTimeout =
                networkConfig.getSocketTimeout() == null ? DEFAULT_TIMEOUT : networkConfig.getSocketTimeout();
        return new HttpClientConfigurator()
                .hostFromUrl(remoteUrl)
                .connectionTimeout(socketTimeout)
                .soTimeout(socketTimeout)
                .staleCheckingEnabled(true)
                .retry(RETRY_COUNT, false)
                .localAddress(networkConfig.getLocalAddress())
                .proxy(proxyDescriptor)
                .authentication(networkConfig.getUsername(), CryptoHelper.decryptIfNeeded(networkConfig.getPassword()),
                        networkConfig.getLenientHostAuth() != null)
                .enableCookieManagement(networkConfig.getCookieManagement() != null)
                .getClient();
    }
}
