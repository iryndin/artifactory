package org.artifactory.security;

import org.apache.log4j.Logger;
import org.springframework.util.Assert;

/**
 * An object identity that represents a repository and a groupId
 * <p/>
 * Created by IntelliJ IDEA. User: yoavl
 */
public class RepoPath implements SecuredResource {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(RepoPath.class);

    public static final char REPO_PATH_SEP = ':';
    public static final String ANY = "ANY";
    public static final RepoPath ANY_REPO_AND_PATH = new RepoPath();

    private String repoKey;
    private String path;

    public static RepoPath forRepo(String repoKey) {
        return new RepoPath(repoKey, ANY);
    }

    public static RepoPath forPath(String path) {
        return new RepoPath(ANY, path);
    }

    public RepoPath() {
        this(ANY, ANY);
    }

    public RepoPath(String repoKey, String path) {
        this.repoKey = repoKey;
        this.path = path;
    }

    public RepoPath(String id) {
        Assert.notNull(id, "RepoAndGroupIdIdentity cannot have a null id");
        int idx = id.indexOf(REPO_PATH_SEP);
        Assert.state(idx > 0, "Could not determine both repository key and groupId from '" +
                id + "'.");
        repoKey = id.substring(0, idx);
        path = id.substring(idx + 1);
        //Handle migration from 1.2.0.x
        /*if (repoKey.equals("java.lang.String")) {
            LOGGER.warn("Assuming older 1.2.0.x settings for permission target with id '" +
                    id + "'. Migrating...");
            repoKey = ANY;
            path = path.replace('.', '/');
        }*/
    }

    public String getRepoKey() {
        return repoKey;
    }

    public void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getId() {
        return repoKey + REPO_PATH_SEP + path;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RepoPath identity = (RepoPath) o;
        return path.equals(identity.path) && repoKey.equals(identity.repoKey);
    }

    public int hashCode() {
        int result;
        result = repoKey.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }

    public String toString() {
        return getId();
    }
}
