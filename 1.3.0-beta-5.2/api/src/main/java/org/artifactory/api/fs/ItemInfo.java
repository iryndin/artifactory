package org.artifactory.api.fs;

import org.apache.log4j.Logger;
import org.artifactory.api.common.Info;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.utils.PathUtils;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public abstract class ItemInfo implements Info {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ItemInfo.class);

    public static final String METADATA_FOLDER = ".artifactory-metadata";

    private String repoKey;
    private String relPath;
    private long created;
    private String modifiedBy;

    public ItemInfo() {
    }

    protected ItemInfo(ItemInfo info) {
        update(info);
    }

    public void update(ItemInfo info) {
        this.repoKey = info.getRepoKey();
        this.relPath = info.getRelPath();
        this.created = info.getCreated();
        this.modifiedBy = info.getModifiedBy();
    }

    public RepoPath getRepoPath() {
        return new RepoPath(repoKey, relPath);
    }

    public abstract boolean isFolder();

    public abstract String getRootName();

    public String getName() {
        return PathUtils.getName(relPath);
    }

    public String getRepoKey() {
        return repoKey;
    }

    public void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
    }

    public String getRelPath() {
        return relPath;
    }

    public void setRelPath(String relPath) {
        this.relPath = relPath;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemInfo)) {
            return false;
        }
        ItemInfo item = (ItemInfo) o;
        return relPath.equals(item.relPath) && repoKey.equals(item.repoKey);

    }

    @Override
    public int hashCode() {
        int result;
        result = repoKey.hashCode();
        result = 31 * result + relPath.hashCode();
        return result;
    }

    public String toString() {
        return "ItemInfo{" +
                "repoKey='" + repoKey + '\'' +
                ", relPath='" + relPath + '\'' +
                ", created=" + created +
                ", modifiedBy='" + modifiedBy + '\'' +
                '}';
    }
}
