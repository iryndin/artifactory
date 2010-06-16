/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.md.Properties;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public abstract class ArtifactoryRequestBase implements ArtifactoryRequest {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryRequestBase.class);

    private RepoPath repoPath;

    /**
     * A set of matrix parameters found on the request path in the form of:
     * <p/>
     * /pathseg1/pathseg2;param1=v1;param2=v2;param3=v3
     */
    private Properties properties = new Properties();

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

    public boolean hasMatrixProperties() {
        return properties.size() > 0;
    }

    public boolean isSnapshot() {
        return MavenNaming.isSnapshot(getPath());
    }

    public boolean isMetadata() {
        return NamingUtils.isMetadata(getPath());
    }

    public boolean isChecksum() {
        return NamingUtils.isChecksum(getPath());
    }

    public String getName() {
        String path = getPath();
        return PathUtils.getName(path);
    }

    public String getDir() {
        String path = getPath();
        int dirEndIdx = path.lastIndexOf('/');
        if (dirEndIdx == -1) {
            return null;
        }

        return path.substring(0, dirEndIdx);
    }

    public boolean isNewerThanResource(long resourceLastModified) {
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

    /**
     * Calculates a repoPath based on the given servlet path (path after the context root, including the repo prefix).
     */
    @SuppressWarnings({"deprecation"})
    protected RepoPath calculateRepoPath(String requestPath) {
        String prefix = PathUtils.getPathFirstPart(requestPath);
        //Support repository-level metadata requests
        int startIdx;
        if (NamingUtils.isMetadata(prefix)) {
            prefix = NamingUtils.stripMetadataFromPath(prefix);
            startIdx = prefix.length() + NamingUtils.METADATA_PREFIX.length();
        } else {
            startIdx = requestPath.startsWith("/") ? prefix.length() + 2 : prefix.length() + 1;
        }

        //REPO HANDLING

        //Look for the deprecated legacy format of repo-key@repo
        int idx = prefix.indexOf(ArtifactoryRequest.LEGACY_REPO_SEP);
        String targetRepo = idx > 0 ? prefix.substring(0, idx) : prefix;
        //Calculate matrix params on the repo
        targetRepo = processMatrixParamsIfExist(targetRepo);
        //Test if we need to substitute the targetRepo due to system prop existence
        String substTargetRepo = ArtifactoryHome.get().getArtifactoryProperties().getSubstituteRepoKeys().get(
                targetRepo);
        if (substTargetRepo != null) {
            targetRepo = substTargetRepo;
        }

        //PATH HANDLING

        //Strip any trailing '/'
        int endIdx = (requestPath.endsWith("/") ? requestPath.length() - 1 : requestPath.length());
        String path = startIdx < endIdx ? requestPath.substring(startIdx, endIdx) : "";
        //Calculate matrix params on the path
        path = processMatrixParamsIfExist(path);
        RepoPath repoPath = new RepoPath(targetRepo, path);
        return repoPath;
    }

    private String processMatrixParamsIfExist(String fragment) {
        int matrixParamStart = fragment.indexOf(MATRIX_PARAMS_SEP);
        if (matrixParamStart > 0) {
            processMatrixParams(fragment.substring(matrixParamStart));
            //Return the clean fragment
            return fragment.substring(0, matrixParamStart);
        } else {
            return fragment;
        }
    }

    private void processMatrixParams(String matrixParams) {
        int matrixParamStart = 0;
        do {
            int matrixParamEnd = matrixParams.indexOf(MATRIX_PARAMS_SEP, matrixParamStart + 1);
            if (matrixParamEnd < 0) {
                matrixParamEnd = matrixParams.length();
            }
            String param = matrixParams.substring(matrixParamStart + 1, matrixParamEnd);
            int equals = param.indexOf('=');
            if (equals > 0) {
                String key = param.substring(0, equals);
                String value = param.substring(equals + 1);
                // url-decode the value
                try {
                    value = URLDecoder.decode(value, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    log.warn("Encoding not supported: {}. Using original value", e.getMessage());
                }
                this.properties.put(key, value);
            } else if (equals == 0) {
                //No key declared, ignore
            } else if (param.length() > 0) {
                this.properties.put(param, "");
            }
            matrixParamStart = matrixParamEnd;
        } while (matrixParamStart > 0 && matrixParamStart < matrixParams.length());
    }

    @Override
    public String toString() {
        return "source=" + getSourceDescription()
                + ", path=" + getPath() + ", lastModified=" + getLastModified()
                + ", ifModifiedSince=" + getIfModifiedSince();
    }
}