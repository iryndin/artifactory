/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

import org.artifactory.fs.ZipEntryInfo;
import org.artifactory.util.PathUtils;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.zip.ZipEntry;

/**
 * Simple serializable object with zip entry information.
 *
 * @author Yossi Shaul
 */
public class ZipEntryImpl implements Serializable, ZipEntryInfo {
    final private String path;    // full path of the entry
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
     * @param name      The full path of the entry
     * @param directory
     */
    public ZipEntryImpl(@Nonnull String path, boolean directory) {
        this.path = path;
        this.name = PathUtils.getFileName(path);
        this.time = 0;
        this.size = 0;
        this.compressedSize = 0;
        this.comment = null;
        this.crc = 0;
        this.directory = directory;
    }

    public ZipEntryImpl(@Nonnull ZipEntry entry) {
        this.path = entry.getName();
        this.name = PathUtils.getFileName(path);
        this.time = entry.getTime();
        this.size = entry.getSize();
        this.compressedSize = entry.getCompressedSize();
        this.comment = entry.getComment();
        this.crc = entry.getCrc();
        this.directory = entry.isDirectory();
    }

    public String getPath() {
        return path;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ZipEntryImpl)) {
            return false;
        }

        ZipEntryImpl zipEntry = (ZipEntryImpl) o;

        if (directory != zipEntry.directory) {
            return false;
        }
        if (!name.equals(zipEntry.name)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (directory ? 1 : 0);
        return result;
    }
}
