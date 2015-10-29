package org.artifactory.api.bintray;


import org.artifactory.repo.RepoPath;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;

/**
 * Bintray single item info.
 */
public class BintrayItemInfo implements Serializable {
    @JsonProperty(value = "name")
    private String name;
    @JsonProperty(value = "package")
    private String packageName;
    @JsonProperty(value = "version")
    private String version;
    @JsonProperty(value = "repo")
    private String repo;
    @JsonProperty(value = "created")
    private String created;
    @JsonProperty(value = "path")
    private String path;
    @JsonProperty(value = "owner")
    private String owner;

    // Local information
    private boolean cached;
    private RepoPath localRepoPath;


    public String getName() {
        return name;
    }

    public String getPackage() {
        return packageName;
    }

    public String getVersion() {
        return version;
    }

    public String getRepo() {
        return repo;
    }

    public String getCreated() {
        return created;
    }

    public String getPath() {
        return path;
    }

    public boolean getCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public String getOwner() {
        return owner;
    }

    public boolean isCached() {
        return cached;
    }

    public RepoPath getLocalRepoPath() {
        return localRepoPath;
    }

    public void setLocalRepoPath(RepoPath localRepoPath) {
        this.localRepoPath = localRepoPath;
    }

    public void setCreated(String created) {
        this.created = created;
    }

}


