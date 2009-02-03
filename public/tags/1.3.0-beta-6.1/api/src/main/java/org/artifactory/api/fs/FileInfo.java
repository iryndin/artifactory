package org.artifactory.api.fs;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.utils.PathUtils;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
@XStreamAlias(FileInfo.ROOT)
public class FileInfo extends ItemInfo {
    public static final String ROOT = "artifactory-file";

    private long size;
    private String mimeType;
    private FileExtraInfo extension;

    public FileInfo(RepoPath repoPath) {
        super(repoPath);
        this.size = 0;
        this.mimeType = null;
        this.extension = new FileExtraInfo();
    }

    public FileInfo(FileInfo info) {
        super(info);
        this.size = info.getSize();
        this.mimeType = info.getMimeType();
        this.extension = new FileExtraInfo(info.getExtension());
    }

    /**
     * Create with a different repoPath
     *
     * @param info
     * @param repoPath
     */
    public FileInfo(FileInfo info, RepoPath repoPath) {
        super(info, repoPath);
        this.size = info.getSize();
        this.mimeType = info.getMimeType();
        this.extension = new FileExtraInfo(info.getExtension());
    }

    @Override
    public boolean isFolder() {
        return false;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getAge() {
        return getLastUpdated() != 0 ?
                System.currentTimeMillis() - getLastUpdated() : -1;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public FileExtraInfo getExtension() {
        return extension;
    }

    public void setExtension(FileExtraInfo extension) {
        this.extension = extension;
    }

    public String getSha1() {
        return extension.getSha1();
    }

    public void setSha1(String sha1) {
        extension.setSha1(sha1);
    }

    public String getMd5() {
        return extension.getMd5();
    }

    public void setMd5(String md5) {
        extension.setMd5(md5);
    }

    public long getLastUpdated() {
        return extension.getLastUpdated();
    }

    public void setLastUpdated(long lastUpdated) {
        extension.setLastUpdated(lastUpdated);
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                super.toString() +
                ", size=" + size +
                ", mimeType='" + mimeType + '\'' +
                ", extension=" + extension +
                '}';
    }

    @Override
    public boolean isIdentical(ItemInfo info) {
        if (!(info instanceof FileInfo)) {
            return false;
        }
        FileInfo fileInfo = (FileInfo) info;
        return this.size == fileInfo.size &&
                PathUtils.safeStringEquals(this.mimeType, fileInfo.mimeType) &&
                this.extension.isIdentical(fileInfo.extension) &&
                super.isIdentical(info);
    }
}
