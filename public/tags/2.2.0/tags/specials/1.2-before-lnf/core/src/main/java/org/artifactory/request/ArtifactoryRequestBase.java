package org.artifactory.request;

import org.apache.log4j.Logger;
import org.artifactory.maven.MavenUtils;
import org.artifactory.resource.RepoResource;

import java.rmi.dgc.VMID;

public abstract class ArtifactoryRequestBase implements ArtifactoryRequest {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger LOGGER = Logger.getLogger(ArtifactoryRequestBase.class);

    public static final String ORIGIN_ARTIFACTORY = "Origin-Artifactory";
    public static final String HOST_ID = getHostId();

    private long modificationTime = -1;

    public boolean isSnapshot() {
        return MavenUtils.isSnapshot(getPath());
    }

    public boolean isMetaData() {
        return MavenUtils.isMetaData(getPath());
    }

    public boolean isPom() {
        return MavenUtils.isPom(getPath());
    }

    public String getDir() {
        String path = getPath();
        int dirEndIdx = path.lastIndexOf('/');
        if (dirEndIdx != -1) {
            return path.substring(0, dirEndIdx);
        } else {
            return null;
        }
    }

    public boolean isNewerThanResource(RepoResource res) {
        long modificationTime = getModificationTime();
        long resLastModifiedTime = res.getLastModifiedTime();
        return resLastModifiedTime <= modificationTime;
    }

    public long getModificationTime() {
        //If not calculated yet
        if (modificationTime < 0) {
            //These headers are not filled by mvn lw-http wagon (doesn't call "getIfNewer")
            if (getLastModified() < 0 && getIfModifiedSince() < 0) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Neither If-Modified-Since nor Last-Modified are set");
                }
                return -1;
            }
            if (getLastModified() >= 0 && getIfModifiedSince() >= 0
                    && getLastModified() != getIfModifiedSince()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.warn("If-Modified-Since (" + getIfModifiedSince()
                            + ") AND Last-Modified ("
                            + getLastModified() + ") both set and unequal");
                }

            }
            modificationTime = Math.max(getLastModified(), getIfModifiedSince());
        }
        return modificationTime;
    }

    /**
     * Caculate a unique id for the VM to support Artifactories with the same ip (e.g. accross
     * NATs)
     */
    private static String getHostId() {
        VMID vmid = new VMID();
        return vmid.toString();
    }

    public static long round(long time) {
        if (time != -1) {
            return time / 1000 * 1000;
        }
        return time;
    }
}