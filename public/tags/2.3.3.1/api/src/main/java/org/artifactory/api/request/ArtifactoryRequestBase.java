/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.api.request;

import org.artifactory.api.md.PropertiesImpl;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

public abstract class ArtifactoryRequestBase implements ArtifactoryRequest {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryRequestBase.class);

    private RepoPath repoPath;

    /**
     * A set of matrix parameters found on the request path in the form of:
     * <p/>
     * /pathseg1/pathseg2;param1=v1;param2=v2;param3=v3
     */
    private Properties properties = new PropertiesImpl();

    private long modificationTime = -1;

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public String getRepoKey() {
        return repoPath.getRepoKey();
    }

    public String getPath() {
        return repoPath.getPath();
    }

    public Properties getProperties() {
        return properties;
    }

    public boolean hasProperties() {
        return !properties.isEmpty();
    }

    public boolean isMetadata() {
        return NamingUtils.isMetadata(getPath());
    }

    public boolean isChecksum() {
        return NamingUtils.isChecksum(getPath());
    }

    public String getName() {
        return PathUtils.getName(getPath());
    }

    public boolean isNewerThan(long resourceLastModified) {
        long modificationTime = getModificationTime();
        //Check that the resource has a modification time and that it is older than the request's one.
        //Since HTTP dates do not carry millisecond-level data compare with the value rounded-down to the nearest sec.
        return resourceLastModified >= 0 && roundMillis(resourceLastModified) <= modificationTime;
    }

    public long getModificationTime() {
        //If not calculated yet
        if (modificationTime < 0) {
            //These headers are not filled by mvn lw-http wagon (doesn't call "getIfNewer")
            long lastModified = getLastModified();
            long ifModifiedSince = getIfModifiedSince();
            if (lastModified < 0 && ifModifiedSince < 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Neither If-Modified-Since nor Last-Modified are set");
                }
                return -1;
            }
            if (lastModified >= 0 && ifModifiedSince >= 0 && lastModified != ifModifiedSince) {
                if (log.isDebugEnabled()) {
                    log.warn(
                            "If-Modified-Since (" + ifModifiedSince + ") AND Last-Modified (" + lastModified +
                                    ") both set and unequal");
                }

            }
            modificationTime = Math.max(lastModified, ifModifiedSince);
        }
        return modificationTime;
    }

    protected void setRepoPath(RepoPath repoPath) {
        this.repoPath = repoPath;
    }

    public static long roundMillis(long time) {
        if (time != -1) {
            return time / 1000 * 1000;
        }
        return time;
    }

    public String getParameter(String name) {
        return null;
    }

    public String[] getParameterValues(String name) {
        return new String[0];
    }

    /**
     * Calculates a repoPath based on the given servlet path (path after the context root, including the repo prefix).
     */
    @SuppressWarnings({"deprecation"})
    protected RepoPath calculateRepoPath(String requestPath) {
        String repoKey = PathUtils.getFirstPathElement(requestPath);
        // index where the path to the file or directory starts (i.e., the request path after the repository key)
        int pathStartIndex;
        if (NamingUtils.isMetadata(repoKey)) {
            //Support repository-level metadata requests
            repoKey = NamingUtils.stripMetadataFromPath(repoKey);
            pathStartIndex = repoKey.length() + NamingUtils.METADATA_PREFIX.length();
        } else if (LIST_BROWSING_PATH.equals(repoKey)) {
            int repoKeyStartIndex = requestPath.indexOf(LIST_BROWSING_PATH) + LIST_BROWSING_PATH.length() + 1;
            repoKey = PathUtils.getFirstPathElement(requestPath.substring(repoKeyStartIndex));
            pathStartIndex = repoKeyStartIndex + repoKey.length() + 1;
        } else if (ArtifactoryRequest.SIMPLE_BROWSING_PATH.equals(repoKey)) {
            int repoKeyStartIndex = requestPath.indexOf(SIMPLE_BROWSING_PATH) + SIMPLE_BROWSING_PATH.length() + 1;
            repoKey = PathUtils.getFirstPathElement(requestPath.substring(repoKeyStartIndex));
            pathStartIndex = repoKeyStartIndex + repoKey.length() + 1;
        } else {
            pathStartIndex = requestPath.startsWith("/") ? repoKey.length() + 2 : repoKey.length() + 1;
        }

        //REPO HANDLING

        //Look for the deprecated legacy format of repo-key@repo
        int legacyRepoSeparatorIndex = repoKey.indexOf(ArtifactoryRequest.LEGACY_REPO_SEP);
        repoKey = legacyRepoSeparatorIndex > 0 ? repoKey.substring(0, legacyRepoSeparatorIndex) : repoKey;

        //Calculate matrix params on the repo
        repoKey = processMatrixParamsIfExist(repoKey);

        //Test if we need to substitute the targetRepo due to system prop existence
        String substTargetRepo = ArtifactoryHome.get().getArtifactoryProperties().getSubstituteRepoKeys().get(repoKey);
        if (substTargetRepo != null) {
            repoKey = substTargetRepo;
        }

        //PATH HANDLING

        //Strip any trailing '/'
        int pathEndIndex = requestPath.endsWith("/") ? requestPath.length() - 1 : requestPath.length();
        String path = pathStartIndex < pathEndIndex ? requestPath.substring(pathStartIndex, pathEndIndex) : "";
        //Calculate matrix params on the path
        path = processMatrixParamsIfExist(path);
        return new RepoPathImpl(repoKey, path);
    }

    private String processMatrixParamsIfExist(String fragment) {
        int matrixParamStart = fragment.indexOf(Properties.MATRIX_PARAMS_SEP);
        if (matrixParamStart > 0) {
            PropertiesImpl.processMatrixParams(this.properties, fragment.substring(matrixParamStart));
            //Return the clean fragment
            return fragment.substring(0, matrixParamStart);
        } else {
            return fragment;
        }
    }

    @Override
    public String toString() {
        return "source=" + getClientAddress()
                + ", path=" + getPath() + ", lastModified=" + getLastModified()
                + ", ifModifiedSince=" + getIfModifiedSince();
    }
}