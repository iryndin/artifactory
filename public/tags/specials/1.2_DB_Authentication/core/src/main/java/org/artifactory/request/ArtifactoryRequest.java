package org.artifactory.request;

import org.artifactory.resource.RepoResource;

import java.io.IOException;
import java.io.InputStream;

public interface ArtifactoryRequest {

    char REPO_SEP = '@';
    
    String getTargetRepoGroup();

    long getLastModified();

    long getIfModifiedSince();

    String getPath();

    String getSourceDescription();

    InputStream getInputStream() throws IOException;

    boolean isSnapshot();

    boolean isMetaData();

    boolean isPom();

    /**
     * This feels a bit dirty, but it represents a request where we don't want the actual file, just
     * the meta information about last update etc.
     *
     * @return
     */
    boolean isHeadOnly();

    /**
     * Indicates whether the request is coming back to the same proxy as a result of reverse
     * mirroring
     *
     * @return
     */
    boolean isRecursive();

    /**
     * Checks if the request originated from another artifactory
     *
     * @return
     */
    boolean isFromAnotherArtifactory();

    long getModificationTime();

    /**
     * Returns true if the request specification is newer than the resource.
     * This will occur if the client has a newer version of the artifact than we can provide.
     *
     * @param res
     * @return
     */
    boolean isNewerThanResource(RepoResource res);
}
