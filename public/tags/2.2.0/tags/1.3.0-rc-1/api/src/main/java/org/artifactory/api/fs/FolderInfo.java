package org.artifactory.api.fs;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.repo.RepoPath;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
@XStreamAlias(FolderInfo.ROOT)
public class FolderInfo extends ItemInfo {
    public static final String ROOT = "artifactory-folder";

    private FolderAdditionaInfo additionalInfo;

    public FolderInfo(RepoPath repoPath) {
        super(repoPath);
        additionalInfo = new FolderAdditionaInfo();
    }

    public FolderInfo(FolderInfo info) {
        super(info);
        additionalInfo = new FolderAdditionaInfo(info.additionalInfo);
    }


    /**
     * Should not be called by clients - for internal use
     *
     * @return
     */
    public void setAdditionalInfo(FolderAdditionaInfo additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    @Override
    public String toString() {
        return "FolderInfo{" + super.toString() + ", extension=" + additionalInfo + '}';
    }

    @Override
    public boolean isIdentical(ItemInfo info) {
        if (!(info instanceof FolderInfo)) {
            return false;
        }
        FolderInfo folderInfo = (FolderInfo) info;
        return this.additionalInfo.isIdentical(folderInfo.additionalInfo) &&
                super.isIdentical(info);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    @Deprecated
    public FolderAdditionaInfo getInernalXmlInfo() {
        return additionalInfo;
    }

    public String getCreatedBy() {
        return additionalInfo.getCreatedBy();
    }

    public String getModifiedBy() {
        return additionalInfo.getModifiedBy();
    }

    public long getLastUpdated() {
        return additionalInfo.getLastUpdated();
    }

    FolderAdditionaInfo getAdditionalInfo() {
        return additionalInfo;
    }
}