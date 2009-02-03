package org.artifactory.api.fs;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.repo.RepoPath;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
@XStreamAlias(FolderInfo.ROOT)
public class FolderInfo extends ItemInfo {
    public static final String ROOT = "artifactory-folder";

    private FolderExtraInfo extension;

    public FolderInfo(RepoPath repoPath) {
        super(repoPath);
        extension = new FolderExtraInfo();
    }

    public FolderInfo(FolderInfo info) {
        super(info);
        extension = new FolderExtraInfo(info.getExtension());
    }

    @Override
    public FolderExtraInfo getExtension() {
        return extension;
    }

    public void setExtension(FolderExtraInfo extension) {
        this.extension = extension;
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    @Override
    public String toString() {
        return "FolderInfo{" +
                super.toString() +
                ", extension=" + extension +
                '}';
    }

    @Override
    public boolean isIdentical(ItemInfo info) {
        if (!(info instanceof FolderInfo)) {
            return false;
        }
        FolderInfo folderInfo = (FolderInfo) info;
        return this.extension.isIdentical(folderInfo.extension) &&
                super.isIdentical(info);
    }
}