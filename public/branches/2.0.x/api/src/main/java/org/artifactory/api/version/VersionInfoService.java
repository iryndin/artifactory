package org.artifactory.api.version;

import java.util.Map;

/**
 * Main interface for the Version Info Service
 *
 * @author Noam Tenne
 */
public interface VersionInfoService {

    /**
     * Indicates that the remote versioning service is unavailable It might be down, blocked or not connected yet
     */
    static final String SERVICE_UNAVAILABLE = "NA";

    /**
     * Get latest version number. If not yet retrieved from the remote server return SERVICE_UNAVAILABLE and retrieve
     * the versioning in a background task.
     *
     * @param headersMap a map of the original http headers
     * @param release    True to get the latest stable version, False to get the latest version of any kind @return
     *                   String Latest version number
     */
    public String getLatestVersion(Map<String, String> headersMap, boolean release);

    /**
     * Get latest revision number. If not yet retrieved from the remote server return SERVICE_UNAVAILABLE and retrieve
     * the versioning in a background task.
     *
     * @param headersMap a map of the original http headers
     * @param release    True - to get the latest stable revision, False - to get the latest revision of any kind
     * @return String Latest revision number
     */
    public String getLatestRevision(Map<String, String> headersMap, boolean release);

    /**
     * Get latest version number from the cache. If doesn't exist will return NA.
     *
     * @param release True to get the latest stable version, False to get the latest version of any kind
     * @return String Latest version number
     */
    public String getLatestVersionFromCache(boolean release);

    /**
     * @param headersMap Client http header params
     * @return Artifactory versioning info from the remove jfrog service.
     */
    ArtifactoryVersioning getRemoteVersioning(Map<String, String> headersMap);
}
