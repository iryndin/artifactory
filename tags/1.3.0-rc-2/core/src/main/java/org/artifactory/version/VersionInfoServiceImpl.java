package org.artifactory.version;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.version.VersionInfoService;
import org.artifactory.common.ConstantsValue;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.HttpClientUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * Main implementation of the Version Info Service. Can be used to etrieve the latest version and revision numbers.
 *
 * @author Noam Tenne
 */
@Service
public class VersionInfoServiceImpl implements VersionInfoService {
    /**
     * URL of remote version info
     */
    private static String URL = "http://service.jfrog.org/api/version";
    /**
     * Key to use in version information cache
     */
    private final String CACHE_KEY = "versioning";
    /**
     * An instance of the cache service
     */
    @Autowired
    private CacheService cacheService;

    private final String PARAM_VM_VERSION = "java.vm.version";
    private final String PARAM_OS_ARCH = "os.arch";
    private final String PARAM_OS_NAME = "os.name";

    /**
     * {@inheritDoc}
     */
    public String getLatestVersion(Map<String, String> headersMap, boolean release) {
        ArtifactoryVersioning versioning = getVersioning(headersMap);
        if (release) {
            return versioning.getRelease().getVersion();
        }
        return versioning.getLatest().getVersion();
    }

    /**
     * Get latest revision number
     *
     * @param release True - to get the latest stable revision, False - to get the latest revision of any kind
     * @return String Latest revision number
     */
    public String getLatestRevision(Map<String, String> headersMap, boolean release) {
        ArtifactoryVersioning versioning = getVersioning(headersMap);
        if (release) {
            return versioning.getRelease().getRevision();
        }
        return versioning.getLatest().getRevision();
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
            try {
                versioning = getRemote(headersMap);
            } catch (Exception e) {
                VersionHolder versionHolder = new VersionHolder(SERVICE_UNAVAILABLE,
                        SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
                versioning = new ArtifactoryVersioning(versionHolder, versionHolder);
            }
            getCache().put(CACHE_KEY, versioning);
        }
        return versioning;
    }

    public String getLatestVersionFromCache(boolean release) {
        ArtifactoryVersioning cachedversioning = getVersioningFromCache();
        if (cachedversioning != null) {
            return release ? cachedversioning.getRelease().getVersion() : cachedversioning.getLatest().getVersion();
        } else {
            return SERVICE_UNAVAILABLE;
        }
    }

    private ArtifactoryVersioning getVersioningFromCache() {
        return getCache().get(CACHE_KEY);
    }

    /**
     * Retrieves the remote version info
     *
     * @param headersMap A map of the needed headers
     * @return ArtifactoryVersioning - Latest version info
     */
    private ArtifactoryVersioning getRemote(Map<String, String> headersMap) {
        GetMethod getMethod = new GetMethod(URL);
        NameValuePair[] httpMethodParams = new NameValuePair[]{
                new NameValuePair(ConstantsValue.artifactoryVersion.getPropertyName(),
                        ConstantsValue.artifactoryVersion.getString()),
                new NameValuePair(PARAM_VM_VERSION, System.getProperty(PARAM_VM_VERSION)),
                new NameValuePair(PARAM_OS_ARCH, System.getProperty(PARAM_OS_ARCH)),
                new NameValuePair(PARAM_OS_NAME, System.getProperty(PARAM_OS_NAME))
        };
        getMethod.setQueryString(httpMethodParams);
        //Append headers
        setHeader(getMethod, headersMap, "User-Agent");
        setHeader(getMethod, headersMap, "Referer");

        HttpClient client = new HttpClient();
        HttpClientParams clientParams = client.getParams();
        // Set the socket data timeout
        clientParams.setSoTimeout(15000);
        // Limit the retries to a signle retry
        clientParams.setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(1, false));

        //Update the proxy settings
        ProxyDescriptor proxy = InternalContextHelper.get().getCentralConfig().getDescriptor().getDefaultProxy();
        HttpClientUtils.configureProxy(client, proxy);

        String returnedInfo = null;
        try {
            client.executeMethod(getMethod);
            if (getMethod.getStatusCode() == HttpStatus.SC_OK) {
                returnedInfo = getMethod.getResponseBodyAsString();
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        if (("".equals(returnedInfo)) || (returnedInfo == null)) {
            throw new RuntimeException("Requested field was not found.");
        }
        return VersionParser.parse(returnedInfo);
    }

    private void setHeader(GetMethod getMethod, Map<String, String> headersMap, String headerKey) {
        String headerVal = headersMap.get(headerKey.toUpperCase());
        if (headerVal != null) {
            getMethod.setRequestHeader(headerKey, headerVal);
        }
    }

    private Map<Object, ArtifactoryVersioning> getCache() {
        return cacheService.getCache(ArtifactoryCache.versioning);
    }
}
