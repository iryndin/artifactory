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

package org.artifactory.version;

import com.google.common.cache.CacheBuilder;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.version.ArtifactoryVersioning;
import org.artifactory.api.version.VersionHolder;
import org.artifactory.api.version.VersionInfoService;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.HttpClientConfigurator;
import org.artifactory.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Main implementation of the Version Info Service. Can be used to retrieve the latest version and revision numbers.
 *
 * @author Noam Tenne
 */
@Service
public class VersionInfoServiceImpl implements VersionInfoService {
    private static final Logger log = LoggerFactory.getLogger(VersionInfoServiceImpl.class);

    /**
     * URL of remote version info
     */
    private static String URL = "http://service.jfrog.org/api/version";
    /**
     * Key to use in version information cache
     */
    static final String CACHE_KEY = "versioning";

    @Autowired
    private AddonsManager addonsManager;

    private Map<String, ArtifactoryVersioning> cache =
            CacheBuilder.newBuilder().initialCapacity(3).expireAfterWrite(
                    ConstantValues.versioningQueryIntervalSecs.getLong(),
                    TimeUnit.SECONDS).<String, ArtifactoryVersioning>build().asMap();

    private static final String PARAM_JAVA_VERSION = "java.version";
    private static final String PARAM_OS_ARCH = "os.arch";
    private static final String PARAM_OS_NAME = "os.name";
    private static final String PARAM_HASH = "artifactory.hash";

    /**
     * {@inheritDoc}
     */
    @Override
    public VersionHolder getLatestVersion(Map<String, String> headersMap, boolean release) {
        ArtifactoryVersioning versioning = getVersioning(headersMap);
        if (release) {
            return versioning.getRelease();
        }
        return versioning.getLatest();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VersionHolder getLatestVersionFromCache(boolean release) {
        ArtifactoryVersioning cachedVersioning = getVersioningFromCache();
        if (cachedVersioning != null) {
            return release ? cachedVersioning.getRelease() : cachedVersioning.getLatest();
        } else {
            return createServiceUnavailableVersioning().getRelease();
        }
    }

    /**
     * Retrieves the versioning info (either cached, or remote if needed)
     *
     * @param headersMap A map of the needed headers
     * @return ArtifactoryVersioning - Latest version info
     */
    private ArtifactoryVersioning getVersioning(Map<String, String> headersMap) {
        ArtifactoryVersioning versioning = getVersioningFromCache();
        if (versioning == null) {
            // get the version asynchronously from the remote server
            getTransactionalMe().getRemoteVersioningAsync(headersMap);
            // return service unavailable
            versioning = createServiceUnavailableVersioning();
        }
        return versioning;
    }

    private ArtifactoryVersioning getVersioningFromCache() {
        return cache.get(CACHE_KEY);
    }

    /**
     * Retrieves the remote version info asynchronously.
     *
     * @param headersMap A map of the needed headers
     * @return ArtifactoryVersioning - Versioning info from the server
     */
    @Override
    public synchronized Future<ArtifactoryVersioning> getRemoteVersioningAsync(Map<String, String> headersMap) {
        GetMethod getMethod = new GetMethod(URL);
        NameValuePair[] httpMethodParams = new NameValuePair[]{
                new NameValuePair(ConstantValues.artifactoryVersion.getPropertyName(),
                        ConstantValues.artifactoryVersion.getString()),
                new NameValuePair(PARAM_JAVA_VERSION, System.getProperty(PARAM_JAVA_VERSION)),
                new NameValuePair(PARAM_OS_ARCH, System.getProperty(PARAM_OS_ARCH)),
                new NameValuePair(PARAM_OS_NAME, System.getProperty(PARAM_OS_NAME)),
                new NameValuePair(PARAM_HASH, addonsManager.getLicenseKeyHash())
        };
        getMethod.setQueryString(httpMethodParams);
        //Append headers
        setHeader(getMethod, headersMap, "User-Agent");
        setHeader(getMethod, headersMap, "Referer");

        ProxyDescriptor proxy = InternalContextHelper.get().getCentralConfig().getDescriptor().getDefaultProxy();
        HttpClient client = new HttpClientConfigurator()
                .soTimeout(15000)
                .connectionTimeout(1500)
                .retry(0, false)
                .proxy(proxy)
                .getClient();

        ArtifactoryVersioning result;
        try {
            log.debug("Retrieving Artifactory versioning from remote server");
            client.executeMethod(getMethod);
            String returnedInfo = null;
            if (getMethod.getStatusCode() == HttpStatus.SC_OK) {
                returnedInfo = getMethod.getResponseBodyAsString();
            }
            if (StringUtils.isBlank(returnedInfo)) {
                log.debug("Versioning response contains no data");
                result = createServiceUnavailableVersioning();
            } else {
                result = VersionParser.parse(returnedInfo);
            }
        } catch (Exception e) {
            log.debug("Failed to retrieve Artifactory versioning from remote server {}", e.getMessage());
            result = createServiceUnavailableVersioning();
        }

        cache.put(VersionInfoServiceImpl.CACHE_KEY, result);
        return new AsyncResult<>(result);
    }

    private void setHeader(GetMethod getMethod, Map<String, String> headersMap, String headerKey) {
        String headerVal = headersMap.get(headerKey.toUpperCase());
        if ("Referer".equalsIgnoreCase(headerKey)) {
            headerVal = HttpUtils.adjustRefererValue(headersMap, headerVal);
        }
        if (headerVal != null) {
            getMethod.setRequestHeader(headerKey, headerVal);
        }
    }

    private ArtifactoryVersioning createServiceUnavailableVersioning() {
        return new ArtifactoryVersioning(VersionHolder.VERSION_UNAVAILABLE, VersionHolder.VERSION_UNAVAILABLE);
    }

    private VersionInfoService getTransactionalMe() {
        return ContextHelper.get().beanForType(VersionInfoService.class);
    }
}
