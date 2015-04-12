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

package org.artifactory.storage.db.fs.entity;

/**
 * Represents a record in the stats table.
 *
 * @author Yossi Shaul
 */
public class Stat {
    private final long nodeId;
    private final long downloadCount;
    private final long lastDownloaded;
    private final String lastDownloadedBy;

    public Stat(long nodeId, long downloadCount, long lastDownloaded, String lastDownloadedBy) {
        this.nodeId = nodeId;
        this.downloadCount = downloadCount;
        this.lastDownloadedBy = lastDownloadedBy;
        this.lastDownloaded = lastDownloaded;
    }

    public long getNodeId() {
        return nodeId;
    }

    public long getDownloadCount() {
        return downloadCount;
    }

    public String getLastDownloadedBy() {
        return lastDownloadedBy;
    }

    public long getLastDownloaded() {
        return lastDownloaded;
    }
}
