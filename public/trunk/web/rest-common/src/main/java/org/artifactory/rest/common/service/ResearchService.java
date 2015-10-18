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

package org.artifactory.rest.common.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.CoreAddons;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.VersionInfo;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.jackson.JacksonReader;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.rest.search.result.VersionRestResult;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.features.VersionFeature;
import org.artifactory.features.matrix.SmartRepoVersionFeatures;
import org.artifactory.repo.HttpRepositoryConfiguration;
import org.artifactory.repo.HttpRepositoryConfigurationImpl;
import org.artifactory.security.crypto.CryptoHelper;
import org.artifactory.util.HttpClientConfigurator;
import org.artifactory.util.PathUtils;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.List;

/**
 * Service used to discover capabilities of another artifactory
 *
 * @author michaelp
 */
@Component
public class ResearchService extends AbstractResearchService {

    private static final Logger log = LoggerFactory.getLogger(ResearchService.class);
    private static final String ARTIFACTORY_SYSTEM_VERSION_PATH = "/api/system/version";
    private static final String ARTIFACTORY_REPOSITORIES_PATH = "/api/repositories";
    private static final String ARTIFACTORY_APP_PATH = "/artifactory";

    @Lazy(true)
    @Autowired
    private SmartRepoVersionFeatures smartRepoVersionFeatures;

    @Autowired
    private CentralConfigService configService;

    public ResearchResponse getSmartRepoCapabilities(HttpRepositoryConfigurationImpl configuration) {
        if(!Strings.isNullOrEmpty(configuration.getUrl())) {
            URI uri = URI.create(configuration.getUrl());
            if (uri != null) {
                if (uri.getPath().startsWith(ARTIFACTORY_APP_PATH)) {
                    return getArtifactoryCapabilities(getHttpClient(configuration), uri, true);
                } else {
                    return getArtifactoryCapabilities(getHttpClient(configuration), uri, false);
                }
            } else {
                log.debug("Url is malformed.");
            }
        } else {
            log.debug("Url is a mandatory (query) parameter.");
        }
        return ResearchResponse.notArtifactory();
    }

    /**
     * Checks whether given target is another artifactory instance
     * and if true, retrieves SmartRepo capabilities based on artifactory
     * version
     *
     * @param httpRepoDescriptor {@link HttpRepoDescriptor}
     *
     * @return {@link ResearchResponse}
     */
    public ResearchResponse getSmartRepoCapabilities(HttpRepoDescriptor httpRepoDescriptor) {
        if(!Strings.isNullOrEmpty(httpRepoDescriptor.getUrl())) {
            URI uri = URI.create(httpRepoDescriptor.getUrl());
            if (uri != null) {
                if (uri.getPath().startsWith(ARTIFACTORY_APP_PATH)) {
                    return getArtifactoryCapabilities(getHttpClient(httpRepoDescriptor), uri, true);
                } else {
                    return getArtifactoryCapabilities(getHttpClient(httpRepoDescriptor), uri, false);
                }
            } else {
                log.debug("Url is malformed.");
            }
        } else {
            log.debug("Url is a mandatory (query) parameter.");
        }
        return ResearchResponse.notArtifactory();
    }

    /**
     * Checks whether given target is another artifactory instance
     * and if true, retrieves SmartRepo capabilities based on artifactory
     * version
     *
     * @param url url to test against
     * @param client http client to be used
     *
     * @return {@link ResearchResponse}
     *
     * @return boolean
     */
    public ResearchResponse getSmartRepoCapabilities(String url, CloseableHttpClient client) {
        assert client != null : "HttpClient cannot be empty";

        if(!Strings.isNullOrEmpty(url)) {
            URI uri = URI.create(url);
            VersionInfo versionInfo;
            if (uri != null) {
                if (uri.getPath().startsWith(ARTIFACTORY_APP_PATH)){
                    return getArtifactoryCapabilities(client, uri, true);
                } else {
                    return getArtifactoryCapabilities(client, uri, false);
                }
            } else {
                log.debug("Url is malformed.");
            }
        } else {
            log.debug("Url is a mandatory (query) parameter.");
        }
        return ResearchResponse.notArtifactory();
    }

    /**
     * Fetches artifactory capabilities (if target is artifactory)
     *
     * @param client http client to use
     * @param uri remote uri to test against
     * @param inArtifactoryContext whether given app deployed
     *                             in /artifactory context or not
     *
     * @return {@link VersionInfo} if target is artifactory or null
     */
    private ResearchResponse getArtifactoryCapabilities(CloseableHttpClient client, URI uri,
            boolean inArtifactoryContext) {
        assert client != null : "HttpClient cannot be empty";

        CloseableHttpResponse response;
        String requestUrl = produceVersionUrl(uri, inArtifactoryContext);
        HttpGet getMethod = new HttpGet(requestUrl);

        try {
            response = client.execute(getMethod);
            String returnedInfo = null;
            if (response != null ) {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    returnedInfo = EntityUtils.toString(response.getEntity());
                    if (!Strings.isNullOrEmpty(returnedInfo)) {
                        VersionRestResult vrr = parseVersionRestResult(returnedInfo);
                        if (vrr != null && !Strings.isNullOrEmpty(vrr.version) &&
                                validateLicense(vrr.license, getArtifactoryId(response))) {

                            Boolean isRealRepo = isRealRepo(client, uri, // make sure it not a virt repo
                                    PathUtils.getLastPathElement(uri.getPath()),
                                    inArtifactoryContext
                            );

                            if (isRealRepo == null) {
                                                    // we were unable to check repoType
                                                    // what may happen if config doesn't
                                                    // have credentials or has insufficient permissions
                                                    // (api requires it even if anonymous login is on)
                                log.debug("We were unable to check repoType, it may happen if config doesn't have " +
                                        "credentials or user permissions insufficient");
                            }

                            if (isRealRepo == null || isRealRepo.booleanValue()) {
                                log.debug("Repo '{}' is artifactory repository (not virtual) and has supported version for SmartRepo");
                                VersionInfo versionInfo =  vrr.toVersionInfo();
                                return ResearchResponse.artifactoryMeta(
                                        true,
                                        versionInfo,
                                        smartRepoVersionFeatures.getFeaturesByVersion(versionInfo)
                                );
                            } else {
                                log.debug("Virtual repository is not supported in this version of SmartRepo");
                            }
                        } else {
                            log.debug("Unsupported version: {}", vrr);
                        }
                    }
                } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                    return ResearchResponse.artifactory();
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            log.debug("Checking remote artifactory version has failed: {}.", e);
        }
        return ResearchResponse.notArtifactory();
    }

    /**
     * Validates that remote Artifactory uses different license and it is PRO license
     *
     * @param license remote license
     * @param artifactoryId remote artifactory id
     *
     * @return true if both constraints are true otherwise false
     */
    private boolean validateLicense(String license, String artifactoryId) {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        CoreAddons coreAddons = addonsManager.addonByType(CoreAddons.class);

        boolean isDifferentLicense = coreAddons.validateTargetHasDifferentLicense(license, artifactoryId);
        boolean isProLicensed = addonsManager.isProLicensed(license);

        boolean result = isDifferentLicense && isProLicensed;
        if(!result) {
            if (!isDifferentLicense)
                log.warn("License uniqueness validation against target repository " +
                        "has failed, SmartRepo capabilities won't be enabled");
            if (!isProLicensed)
                log.warn("License PRO validation against target repository has failed, " +
                        "SmartRepo capabilities won't be enabled");
        }
        return result;
    }

    /**
     * Fetches ARTIFACTORY_ID header from {@link CloseableHttpResponse}
     *
     * @param response
     *
     * @return ArtifactoryId if present or null
     */
    @Nullable
    private String getArtifactoryId(CloseableHttpResponse response) {
        assert response != null : "HttpResponse cannot be empty";
        Header artifactoryIdHeader = response.getFirstHeader(ArtifactoryResponse.ARTIFACTORY_ID);
        if (artifactoryIdHeader != null && !Strings.isNullOrEmpty(artifactoryIdHeader.getValue())) {
            return artifactoryIdHeader.getValue();
        }
        return null;
    }

    /**
     * Produces url to be used against target host
     * @param uri original URI
     * @param inArtifactoryContext if application resides under
     *                             /artifactory path
     *
     * @return url to be used
     */
    private String produceVersionUrl(URI uri, boolean inArtifactoryContext) {
        return new StringBuilder()
                .append(uri.getScheme())
                .append("://")
                .append(uri.getHost())
                .append(uri.getPort() != -1 ?
                        ":" + uri.getPort()
                        :
                        ""
                )
                .append(inArtifactoryContext ? ARTIFACTORY_APP_PATH : "")
                .append(ARTIFACTORY_SYSTEM_VERSION_PATH)
                .toString();
    }

    /**
     * Produces CloseableHttpClient
     *
     * @param configuration {@link HttpRepositoryConfigurationImpl}
     *
     * @return CloseableHttpClient
     */
    private CloseableHttpClient getHttpClient(HttpRepositoryConfigurationImpl configuration) {
        ProxyDescriptor proxyDescriptor = configService.getDescriptor().getProxy(configuration.getProxy());
        return new HttpClientConfigurator()
                .hostFromUrl(configuration.getUrl())
                .connectionTimeout(configuration.getSocketTimeoutMillis())
                .soTimeout(configuration.getSocketTimeoutMillis())
                .staleCheckingEnabled(true)
                .retry(0, false)
                .localAddress(configuration.getLocalAddress())
                .proxy(proxyDescriptor)
                .authentication(
                        configuration.getUsername(),
                        CryptoHelper.decryptIfNeeded(configuration.getPassword()),
                        configuration.isAllowAnyHostAuth())
                .enableCookieManagement(configuration.isEnableCookieManagement())
                .getClient();
    }

    /**
     * Produces CloseableHttpClient
     *
     * @param httpRepoDescriptor {@link HttpRepoDescriptor}
     *
     * @return CloseableHttpClient
     */
    private CloseableHttpClient getHttpClient(HttpRepoDescriptor httpRepoDescriptor) {
        return new HttpClientConfigurator()
                .hostFromUrl(httpRepoDescriptor.getUrl())
                .connectionTimeout(httpRepoDescriptor.getSocketTimeoutMillis())
                .soTimeout(httpRepoDescriptor.getSocketTimeoutMillis())
                .staleCheckingEnabled(true)
                .retry(0, false)
                .localAddress(httpRepoDescriptor.getLocalAddress())
                .proxy(httpRepoDescriptor.getProxy())
                .authentication(
                        httpRepoDescriptor.getUsername(),
                        CryptoHelper.decryptIfNeeded(httpRepoDescriptor.getPassword()),
                        httpRepoDescriptor.isAllowAnyHostAuth())
                .enableCookieManagement(httpRepoDescriptor.isEnableCookieManagement())
                .getClient();
    }

    /**
     * Unmarshals VersionRestResult from string response
     *
     * @param versionRestResult
     * @return {@link VersionRestResult}
     *
     * @throws IOException if conversion fails
     */
    private VersionRestResult parseVersionRestResult(String versionRestResult) throws IOException {
        return getObjectMapper().readValue(
                getJsonParser(versionRestResult.getBytes()),
                new TypeReference<VersionRestResult>() {}
        );
    }

    /**
     * Checks if target repository is Virtual
     *
     * @param client
     * @param repoKey
     * @param inArtifactoryContext
     *
     * @return true if not virtual, otherwise false
     */
    private final Boolean isRealRepo(CloseableHttpClient client, URI uri, String repoKey,
            boolean inArtifactoryContext) {

        assert client != null : "HttpClient cannot be empty";

        CloseableHttpResponse response;
        String requestUrl = produceRepoInfoUrl(uri, repoKey, inArtifactoryContext);
        HttpGet getMethod = new HttpGet(requestUrl);

        try {
            response = client.execute(getMethod);
            if (response != null ) {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    HttpRepositoryConfiguration httpRepositoryConfiguration =
                            JacksonReader.streamAsClass(
                                    response.getEntity().getContent(),
                                    HttpRepositoryConfigurationImpl.class
                            );
                    if (httpRepositoryConfiguration != null) {
                        if(httpRepositoryConfiguration.getType().equals("virtual")) {
                            log.debug("Found virtual repository '{}'", repoKey);
                            return false;
                        } else {
                            log.debug("Found real repository '{}'", repoKey);
                            return true;
                        }
                    } else {
                        log.warn("Cannot fetch \"" + repoKey + "\" metadata, no response received");
                    }
                } else {
                    log.warn("Cannot fetch \"" + repoKey + "\" metadata, cause: " + response);
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            log.debug(
                    "Checking remote artifactory type has failed: {}.",
                    e.getMessage()
            );
        }
        return null;
    }

    /**
     * Generates repository info URL
     *
     * @param uri default target URI
     * @param repoKey
     * @param inArtifactoryContext
     *
     * @return url
     */
    private String produceRepoInfoUrl(URI uri, String repoKey, boolean inArtifactoryContext) {
        return new StringBuilder()
                .append(uri.getScheme())
                .append("://")
                .append(uri.getHost())
                .append(uri.getPort() != -1 ?
                                ":" + uri.getPort()
                                :
                                ""
                )
                .append(inArtifactoryContext ? ARTIFACTORY_APP_PATH : "")
                .append(ARTIFACTORY_REPOSITORIES_PATH)
                .append("/")
                .append(repoKey)
                .toString();
    }

    /**
     * ResearchResponse
     */
    @XStreamAlias("researchResponse")
    public static class ResearchResponse implements Serializable {

        private final boolean isArtifactory;
        private final VersionInfo versionInfo;
        private final List<VersionFeature> features;

        /**
         * Produces response with artifactory=false
         *
         * @return {@link ResearchResponse}
         */
        private ResearchResponse() {
            this.isArtifactory = false;
            this.versionInfo = null;
            this.features = Lists.newLinkedList();
        }

        /**
         * Produces response with metadata describing remote
         * artifactory instance
         *
         * @param isArtifactory
         * @param versionInfo
         * @param features
         *
         * @return {@link ResearchResponse}
         */
        private ResearchResponse(boolean isArtifactory, VersionInfo versionInfo,
                List<VersionFeature> features) {
            this.isArtifactory = isArtifactory;
            this.versionInfo = versionInfo;
            this.features = features;
        }

        /**
         * @return definition of remote host being artifactory or not
         */
        public boolean isArtifactory() {
            return isArtifactory;
        }

        /**
         * @return remote artifactory version
         */
        public VersionInfo getVersion() {
            return versionInfo;
        }

        /**
         * @return available features for remote artifactory
         *         (based on its version)
         */
        public List<VersionFeature> getFeatures() {
            return features;
        }

        /**
         * Produces response with artifactory=false
         *
         * @return {@link ResearchResponse}
         */
        public static ResearchResponse notArtifactory () {
            return new ResearchResponse();
        }

        /**
         * Produces response with metadata describing remote
         * artifactory instance
         *
         * @param isArtifactory
         * @param versionInfo
         * @param features
         *
         * @return {@link ResearchResponse}
         */
        public static ResearchResponse artifactoryMeta (boolean isArtifactory, VersionInfo versionInfo,
                List<VersionFeature> features) {
            return new ResearchResponse(isArtifactory, versionInfo, features);
        }

        /**
         * Produces response without any metadata, but implies artifactory=true
         *
         * @return {@link ResearchResponse}
         */
        public static ResearchResponse artifactory () {
            return new ResearchResponse(true, null, Lists.newLinkedList());
        }
    }
}
