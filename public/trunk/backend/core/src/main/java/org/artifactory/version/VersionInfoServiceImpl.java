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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.CoreAddons;
import org.artifactory.addon.OssAddonsManager;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.jackson.JacksonFactory;
import org.artifactory.api.version.ArtifactoryVersioning;
import org.artifactory.api.version.CallHomeRequest;
import org.artifactory.api.version.VersionHolder;
import org.artifactory.api.version.VersionInfoService;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.HttpClientConfigurator;
import org.artifactory.util.HttpUtils;
import org.codehaus.jackson.JsonGenerator;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.artifactory.common.ConstantValues.artifactoryVersion;

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
    private static final String URL = "http://service.jfrog.org/api/version";
    /**
     * Key to use in version information cache
     */
    static final String CACHE_KEY = "versioning";

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private CentralConfigService configService;

    private Map<String, ArtifactoryVersioning> cache =
            CacheBuilder.newBuilder().initialCapacity(3).expireAfterWrite(
                    ConstantValues.versioningQueryIntervalSecs.getLong(),
                    TimeUnit.SECONDS).<String, ArtifactoryVersioning>build().asMap();

    private static final String PARAM_JAVA_VERSION = "java.version";
    private static final String PARAM_OS_ARCH = "os.arch";
    private static final String PARAM_OS_NAME = "os.name";
    private static final String PARAM_OEM = "oem";
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
     * Retrieves the remote version info asynchronously.
     *
     * @param headersMap A map of the needed headers
     * @return ArtifactoryVersioning - Versioning info from the server
     */
    @Override
    public synchronized Future<ArtifactoryVersioning> getRemoteVersioningAsync(Map<String, String> headersMap) {

        ArtifactoryVersioning result;
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            URIBuilder urlBuilder = new URIBuilder(URL)
                    .addParameter(artifactoryVersion.getPropertyName(), artifactoryVersion.getString())
                    .addParameter(PARAM_JAVA_VERSION, System.getProperty(PARAM_JAVA_VERSION))
                    .addParameter(PARAM_OS_ARCH, System.getProperty(PARAM_OS_ARCH))
                    .addParameter(PARAM_OS_NAME, System.getProperty(PARAM_OS_NAME))
                    .addParameter(PARAM_HASH, addonsManager.getLicenseKeyHash());

            if (addonsManager.isPartnerLicense()) {
                urlBuilder.addParameter(PARAM_OEM, "VMware");
            }
            HttpGet getMethod = new HttpGet(urlBuilder.build());
            //Append headers
            setHeader(getMethod, headersMap, HttpHeaders.USER_AGENT);
            setHeader(getMethod, headersMap, HttpHeaders.REFERER);

            client = createHttpClient();

            log.debug("Retrieving Artifactory versioning from remote server");
            response = client.execute(getMethod);
            String returnedInfo = null;
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                returnedInfo = EntityUtils.toString(response.getEntity());
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
        } finally {
            IOUtils.closeQuietly(client);
            IOUtils.closeQuietly(response);
        }

        cache.put(VersionInfoServiceImpl.CACHE_KEY, result);
        return new AsyncResult<>(result);
    }

    @Override
    public synchronized void callHome() {
        if (ConstantValues.versionQueryEnabled.getBoolean() && !configService.getDescriptor().isOfflineMode()) {
            try (CloseableHttpClient client = createHttpClient()) {
                String url = ConstantValues.bintrayApiUrl.getString() + "/products/jfrog/artifactory/stats/usage";
                HttpPost postMethod = new HttpPost(url);
                postMethod.setEntity(callHomeEntity());
                log.debug("Calling home...");
                client.execute(postMethod);
            } catch (Exception e) {
                log.debug("Failed calling home: " + e.getMessage(), e);
            }
        }
    }

    private HttpEntity callHomeEntity() throws IOException {
        CallHomeRequest request = new CallHomeRequest();
        request.version = artifactoryVersion.getString();
        request.licenseType = getLicenseType();
        request.licenseOEM = addonsManager.isPartnerLicense() ? "VMware" : null;
        Date licenseValidUntil = addonsManager.getLicenseValidUntil();
        if (licenseValidUntil != null) {
            request.licenseExpiration = ISODateTimeFormat.dateTime().print(new DateTime(licenseValidUntil));
        }
        request.setDist(System.getProperty("artdist"));
        request.environment.hostId = addonsManager.addonByType(HaCommonAddon.class).getHostId();
        request.environment.licenseHash = addonsManager.getLicenseKeyHash();
        request.environment.attributes.osName = System.getProperty(PARAM_OS_NAME);
        request.environment.attributes.osArch = System.getProperty(PARAM_OS_ARCH);
        request.environment.attributes.javaVersion = System.getProperty(PARAM_JAVA_VERSION);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jsonGenerator = JacksonFactory.createJsonGenerator(out);
        jsonGenerator.writeObject(request);
        ByteArrayEntity entity = new ByteArrayEntity(out.toByteArray());
        entity.setContentType("application/json");
        return entity;
    }

    private String getLicenseType() {
        if (addonsManager instanceof OssAddonsManager) {
            return "oss";
        }
        if (addonsManager.addonByType(CoreAddons.class).isAol()) {
            return "aol";
        } else if (addonsManager.getLicenseDetails()[2].equals("Trial")) {
            return "trial";
        } else if (addonsManager.getLicenseDetails()[2].equals("Commercial")) {
            return "pro";
        } else if (addonsManager.isHaLicensed()) {
            return "ent";
        }
        return null;
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

    private CloseableHttpClient createHttpClient() {
        ProxyDescriptor proxy = InternalContextHelper.get().getCentralConfig().getDescriptor().getDefaultProxy();
        return new HttpClientConfigurator()
                .soTimeout(15000)
                .connectionTimeout(1500)
                .retry(0, false)
                .proxy(proxy)
                .getClient();
    }

    private void setHeader(HttpGet getMethod, Map<String, String> headersMap, String headerKey) {
        String headerVal = headersMap.get(headerKey.toUpperCase());
        if ("Referer".equalsIgnoreCase(headerKey)) {
            headerVal = HttpUtils.adjustRefererValue(headersMap, headerVal);
        }
        if (headerVal != null) {
            getMethod.setHeader(headerKey, headerVal);
        }
    }

    private ArtifactoryVersioning createServiceUnavailableVersioning() {
        return new ArtifactoryVersioning(VersionHolder.VERSION_UNAVAILABLE, VersionHolder.VERSION_UNAVAILABLE);
    }

    private VersionInfoService getTransactionalMe() {
        return ContextHelper.get().beanForType(VersionInfoService.class);
    }
}
