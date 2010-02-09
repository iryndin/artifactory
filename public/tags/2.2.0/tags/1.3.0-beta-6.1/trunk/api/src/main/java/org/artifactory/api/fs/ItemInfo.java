package org.artifactory.api.fs;

import org.artifactory.api.common.Info;
import org.artifactory.api.repo.RepoPath;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public abstract class ItemInfo implements Info {

    public static final String METADATA_FOLDER = ".artifactory-metadata";

    private final RepoPath repoPath;
    private final String name;
    private long created;
    protected long lastModified;

    protected ItemInfo(RepoPath repoPath) {
        if (repoPath == null) {
            throw new IllegalArgumentException("RepoPath cannot be null");
        }
        this.repoPath = repoPath;
        this.name = repoPath.getName();
        this.created = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
    }

    protected ItemInfo(ItemInfo info) {
        this(info.getRepoPath());
        this.created = info.getCreated();
        this.lastModified = info.getLastModified();
    }

    protected ItemInfo(ItemInfo info, RepoPath repoPath) {
        this(repoPath);
        this.created = info.getCreated();
        this.lastModified = info.getLastModified();
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public abstract boolean isFolder();

    public String getName() {
        return name;
    }

    public String getRepoKey() {
        return repoPath.getRepoKey();
    }

    public String getRelPath() {
        return repoPath.getPath();
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public abstract ItemExtraInfo getExtension();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ItemInfo info = (ItemInfo) o;

        return repoPath.equals(info.repoPath);
    }

    @Override
    public int hashCode() {
        return repoPath.hashCode();
    }

    @Override
    public String toString() {
        return "ItemInfo{" +
                "repoPath=" + repoPath +
                ", created=" + created +
                ", lastModified=" + lastModified +
                '}';
    }

    public boolean isIdentical(ItemInfo info) {
        return this.lastModified == info.lastModified &&
                this.name.equals(info.name) &&
                this.repoPath.equals(info.repoPath) &&
                this.created == info.created;
    }
}
