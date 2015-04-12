/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.model.xstream.fs;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.fs.MutableStatsInfo;
import org.artifactory.fs.StatsInfo;

/**
 * @author Yoav Landman
 */
@XStreamAlias(StatsImpl.ROOT)
public class StatsImpl implements MutableStatsInfo {

    private long downloadCount;
    private long lastDownloaded;
    private String lastDownloadedBy;

    public StatsImpl() {
    }

    public StatsImpl(StatsInfo statsInfo) {
        this.downloadCount = statsInfo.getDownloadCount();
    }

    @Override
    public long getDownloadCount() {
        return downloadCount;
    }

    @Override
    public void setDownloadCount(long downloadCount) {
        this.downloadCount = downloadCount;
    }

    @Override
    public long getLastDownloaded() {
        return lastDownloaded;
    }

    @Override
    public void setLastDownloaded(long lastDownloaded) {
        this.lastDownloaded = lastDownloaded;
    }

    @Override
    public String getLastDownloadedBy() {
        return lastDownloadedBy;
    }

    @Override
    public void setLastDownloadedBy(String lastDownloadedBy) {
        this.lastDownloadedBy = lastDownloadedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StatsImpl stats = (StatsImpl) o;

        if (downloadCount != stats.downloadCount) {
            return false;
        }
        if (lastDownloaded != stats.lastDownloaded) {
            return false;
        }
        if (lastDownloadedBy != null ? !lastDownloadedBy.equals(stats.lastDownloadedBy) :
                stats.lastDownloadedBy != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (downloadCount ^ (downloadCount >>> 32));
        result = 31 * result + (int) (lastDownloaded ^ (lastDownloaded >>> 32));
        result = 31 * result + (lastDownloadedBy != null ? lastDownloadedBy.hashCode() : 0);
        return result;
    }
}