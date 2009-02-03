package org.artifactory.cache;

import org.artifactory.Startable;
import org.artifactory.resource.RepoResource;

import java.io.Serializable;

public interface RetrievalCache extends Startable, Serializable {
    RepoResource getResource(String path);

    void setResource(RepoResource res);

    RepoResource removeResource(String path);

    long getResourceAge(String path);
}