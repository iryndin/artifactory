package org.artifactory.fs;

import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class FsItemMetadata {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(FsItemMetadata.class);

    public static final String SUFFIX = ".artifactory-metadata";

    private String repoKey;
    private String relPath;
    private String modifiedBy;

    public FsItemMetadata() {
    }

    public FsItemMetadata(String repoKey, String relPath, String modifiedBy) {
        this.repoKey = repoKey;
        this.relPath = relPath;
        this.modifiedBy = modifiedBy;
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

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FsItemMetadata)) {
            return false;
        }
        FsItemMetadata item = (FsItemMetadata) o;
        return relPath.equals(item.relPath) && repoKey.equals(item.repoKey);

    }

    public int hashCode() {
        int result;
        result = repoKey.hashCode();
        result = 31 * result + relPath.hashCode();
        return result;
    }
}
