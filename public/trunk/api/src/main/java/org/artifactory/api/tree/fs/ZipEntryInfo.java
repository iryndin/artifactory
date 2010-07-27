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

package org.artifactory.api.tree.fs;

import java.io.Serializable;
import java.util.zip.ZipEntry;

/**
 * Simple serializable object with zip entry information.
 *
 * @author Yossi Shaul
 */
public class ZipEntryInfo implements Serializable {
    final private String name;    // entry name
    final private long time;    // modification time (in DOS time)
    final private long crc;        // crc-32 of entry data
    final private long size;    // uncompressed size of entry data
    final private long compressedSize;   // compressed size of entry data
    final private String comment;        // optional comment string for entry
    final private boolean directory;    // is this entry a directory

    /**
     * Builds a directory entry with just a name (some jar files doesn't contain ZipEntry for directories).
     *
     * @param name The full path of the entry
     */
    public ZipEntryInfo(String name) {
        this.name = name;
        this.time = 0;
        this.size = 0;
        this.compressedSize = 0;
        this.comment = null;
        this.crc = 0;
        this.directory = true;
    }

    public ZipEntryInfo(ZipEntry entry) {
        this.name = entry.getName();
        this.time = entry.getTime();
        this.size = entry.getSize();
        this.compressedSize = entry.getCompressedSize();
        this.comment = entry.getComment();
        this.crc = entry.getCrc();
        this.directory = entry.isDirectory();
    }

    public String getName() {
        return name;
    }

    public long getTime() {
        return time;
    }

    public long getCrc() {
        return crc;
    }

    public long getSize() {
        return size;
    }

    public long getCompressedSize() {
        return compressedSize;
    }

    public String getComment() {
        return comment;
    }

    public boolean isDirectory() {
        return directory;
    }
}
