package org.artifactory.api.fs;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
@XStreamAlias(FileInfo.ROOT)
public class FileInfo extends ItemInfo {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(FileInfo.class);

    public static final String ROOT = "artifactory.file";

    /**
     * The last time the (cached) resource has been updated from it's remote location.
     */
    private long lastUpdated;
    private long lastModified;
    private long size;
    private String mimeType;
    private String sha1;
    private String md5;

    public FileInfo() {
    }

    public FileInfo(FileInfo info) {
        update(info);
    }

    @Override
    public void update(ItemInfo info) {
        super.update(info);
        FileInfo fileInfo = (FileInfo) info;
        this.lastUpdated = fileInfo.getLastUpdated();
        this.lastModified = fileInfo.getLastModified();
        this.size = fileInfo.getSize();
        this.mimeType = fileInfo.getMimeType();
        this.sha1 = fileInfo.getSha1();
        this.md5 = fileInfo.getMd5();
    }

    @Override
    public boolean isFolder() {
        return false;
    }

    @Override
    public String getRootName() {
        return ROOT;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getAge() {
        return lastUpdated != 0 ? System.currentTimeMillis() - lastUpdated : -1;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String toString() {
        return "FileInfo{" +
                "super=" + super.toString() +
                ", lastUpdated=" + lastUpdated +
                ", lastModified=" + lastModified +
                ", size=" + size +
                ", mimeType='" + mimeType + '\'' +
                ", sha1='" + sha1 + '\'' +
                ", md5='" + md5 + '\'' +
                '}';
    }
}
