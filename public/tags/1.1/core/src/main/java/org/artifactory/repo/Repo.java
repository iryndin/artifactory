package org.artifactory.repo;

import org.artifactory.engine.ResourceStreamHandle;
import org.artifactory.resource.RepoResource;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface Repo extends Serializable {
    String getUrl();

    String getKey();

    String getDescription();
    
    RepoResource getInfo(String path);

    boolean isLocal();

    boolean isHandleReleases();

    boolean isHandleSnapshots();

    String getIncludesPattern();

    String getExcludesPattern();

    void init(CentralConfig cc);

    boolean isBlackedOut();

    ResourceStreamHandle getResourceStreamHandle(RepoResource res) throws IOException;

    boolean accept(String path);

    void undeploy(String relPath);
}
