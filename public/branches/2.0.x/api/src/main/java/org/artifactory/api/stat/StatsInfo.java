package org.artifactory.api.stat;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.common.Info;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
@XStreamAlias(StatsInfo.ROOT)
public class StatsInfo implements Info {
    public static final String ROOT = "artifactory.stats";

    private long downloadCount;

    public StatsInfo() {
    }

    public StatsInfo(long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public StatsInfo(StatsInfo statsInfo) {
        this.downloadCount = statsInfo.getDownloadCount();
    }

    public long getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(long downloadCount) {
        this.downloadCount = downloadCount;
    }
}