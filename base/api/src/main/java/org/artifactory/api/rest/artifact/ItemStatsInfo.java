package org.artifactory.api.rest.artifact;

import org.artifactory.fs.StatsInfo;
import org.codehaus.jackson.annotate.JsonIgnore;

import java.io.Serializable;

/**
 * @author Yoav Luft
 */
public class ItemStatsInfo implements StatsInfo, Serializable {

    private String uri;
    private long downloadCount;
    private long lastDownloaded;
    private String lastDownloadedBy;

    private long remoteDownloadCount;
    private long remoteLastDownloaded;
    private String remoteLastDownloadedBy;
    private String origin;

    public ItemStatsInfo() {
    }

    public ItemStatsInfo(String uri, long downloadCount, long lastDownloaded,
            String lastDownloadedBy) {
        this.uri = uri;
        this.downloadCount = downloadCount;
        this.lastDownloaded = lastDownloaded;
        this.lastDownloadedBy = lastDownloadedBy;
    }

    public ItemStatsInfo(String uri, long downloadCount, long lastDownloaded,
            String lastDownloadedBy, String origin) {
        this.uri = uri;
        this.downloadCount = downloadCount;
        this.lastDownloaded = lastDownloaded;
        this.lastDownloadedBy = lastDownloadedBy;
        this.origin = origin;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setDownloadCount(long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public void setLastDownloaded(long lastDownloaded) {
        this.lastDownloaded = lastDownloaded;
    }

    public void setLastDownloadedBy(String lastDownloadedBy) {
        this.lastDownloadedBy = lastDownloadedBy;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public long getDownloadCount() {
        return downloadCount;
    }

    @Override
    public long getLastDownloaded() {
        return lastDownloaded;
    }

    @Override
    public String getLastDownloadedBy() {
        return lastDownloadedBy;
    }

    @Override
    public long getRemoteDownloadCount() {
        return remoteDownloadCount;
    }

    @Override
    public long getRemoteLastDownloaded() {
        return remoteLastDownloaded;
    }

    @Override
    public String getRemoteLastDownloadedBy() {
        return remoteLastDownloadedBy;
    }

    @JsonIgnore
    @Override
    public String getPath() {
        return null;
    }

    @Override
    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }
}
