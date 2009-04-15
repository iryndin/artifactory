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
import org.artifactory.api.version.ArtifactoryVersioning;
import org.artifactory.api.version.VersionHolder;
import org.artifactory.api.version.VersionInfoService;
import org.artifactory.common.ConstantsValue;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.HttpClientUtils;
import org.artifactory.util.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
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
    static final String CACHE_KEY = "versioning";
    /**
     * An instance of the cache service
     */
    @Autowired
    private CacheService cacheService;

    @Autowired
    private TaskService taskService;

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
     * {@inheritDoc}
     */
    public String getLatestRevision(Map<String, String> headersMap, boolean release) {
        ArtifactoryVersioning versioning = getVersioning(headersMap);
        if (release) {
            return versioning.getRelease().getRevision();
        }
        return versioning.getLatest().getRevision();
    }

    /**
     * {@inheritDoc}
     */
    public String getLatestVersionFromCache(boolean release) {
        ArtifactoryVersioning cachedversioning = getVersioningFromCache();
        if (cachedversioning != null) {
            return release ? cachedversioning.getRelease().getVersion() : cachedversioning.getLatest().getVersion();
        } else {
            return SERVICE_UNAVAILABLE;
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
            synchronized (this) {
                if (getVersioningFromCache() == null && !taskService.hasTaskOfType(VersioningRetrieverJob.class)) {
                    // get the version asynchronouosly from the remote server
                    QuartzTask versioningRetriever = new QuartzTask(VersioningRetrieverJob.class, 0);
                    versioningRetriever.addAttribute("headers", headersMap);
                    versioningRetriever.setSingleton(true);
                    taskService.startTask(versioningRetriever);
                }
            }
            versioning = createServiceUnavailableVersioning();
        }
        return versioning;
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
    public ArtifactoryVersioning getRemoteVersioning(Map<String, String> headersMap) {
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
        // Set the connection timeout
        clientParams.setConnectionManagerTimeout(1500);

        // Don't retry
        clientParams.setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));

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
        if ("Referer".equalsIgnoreCase(headerKey)) {
            headerVal = adjustRefererValue(headersMap, headerVal);
        }
        if (headerVal != null) {
            getMethod.setRequestHeader(headerKey, headerVal);
        }
    }

    private String adjustRefererValue(Map<String, String> headersMap, String headerVal) {
        //Append the artifactory uagent to the referer
        if (headerVal == null) {
            //Fallback to host
            headerVal = headersMap.get("HOST");
            if (headerVal == null) {
                //Fallback to unknown
                headerVal = "UNKNOWN";
            }
        }
        if (!headerVal.startsWith("http")) {
            headerVal = "http://" + headerVal;
        }
        try {
            java.net.URL uri = new java.net.URL(headerVal);
            //Extract only the host part
            headerVal = uri.getHost();
        } catch (MalformedURLException e) {
            //Nothing
        }
        headerVal += "/" + HttpUtils.getArtifactoryUserAgent();
        return headerVal;
    }

    private Map<Object, ArtifactoryVersioning> getCache() {
        return cacheService.getCache(ArtifactoryCache.versioning);
    }

    private ArtifactoryVersioning createServiceUnavailableVersioning() {
        return new ArtifactoryVersioning(VersionHolder.VERSION_UNAVAILABLE, VersionHolder.VERSION_UNAVAILABLE);
    }
}
