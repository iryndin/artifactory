/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

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
    private long lastDownloaded;
    private String lastDownloadedBy;

    public StatsInfo() {
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

    public long getLastDownloaded() {
        return lastDownloaded;
    }

    public void setLastDownloaded(long lastDownloaded) {
        this.lastDownloaded = lastDownloaded;
    }

    public String getLastDownloadedBy() {
        return lastDownloadedBy;
    }

    public void setLastDownloadedBy(String lastDownloadedBy) {
        this.lastDownloadedBy = lastDownloadedBy;
    }
}