package org.artifactory.api.version;

import java.util.Map;

/**
 * Main interface for the Version Info Service
 *
 * @author Noam Tenne
 */
public interface VersionInfoService {
    /**
     * Get latest version number
     *
     * @param headersMap a map of the original http headers
     * @param release    True to get the latest stable version, False to get the latest version of any kind @return
     *                   String Latest version number
     */
    public String getLatestVersion(Map<String, String> headersMap, boolean release);

    /**
     * Get latest revision number
     *
     * @param headersMap a map of the original http headers
     * @param release    True - to get the latest stable revision, False - to get the latest revision of any kind
     * @return String Latest revision number
     */
    public String getLatestRevision(Map<String, String> headersMap, boolean release);
}
