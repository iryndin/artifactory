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

package org.artifactory.jcr.fs;

import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A wrapper on a zip resource under a jcr file. This class is non tread safe.
 */
public class JcrZipFile implements Closeable {

    private JcrFile jcrFile;
    private List<ZipEntry> entries;
    private List<InputStream> streams = new ArrayList<InputStream>();

    public JcrZipFile(JcrFile jcrFile) {
        this.jcrFile = jcrFile;
    }

    public ZipEntry getEntry(String name) {
        List<? extends ZipEntry> entries = entries();
        for (ZipEntry entry : entries) {
            if (name.equals(entry.getName())) {
                return entry;
            }
        }
        return null;
    }

    public InputStream getInputStream(ZipEntry entry) throws IOException {
        ZipInputStream zis = getZipInputStream();
        ZipEntry currentEntry;
        while ((currentEntry = zis.getNextEntry()) != null) {
            if (currentEntry.getName().equals(entry.getName())) {
                return zis;
            }
        }
        throw new IOException(
                "Failed to read zip entry '" + entry.getName() + "' from '" + getName() + "'.");
    }

    public String getName() {
        return jcrFile.getName();
    }

    public List<? extends ZipEntry> entries() {
        if (entries == null) {
            entries = new ArrayList<ZipEntry>();
            ZipInputStream zis = getZipInputStream();
            ZipEntry entry;
            try {
                while ((entry = zis.getNextEntry()) != null) {
                    entries.add(entry);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read zip entries from '" + getName() + "'.", e);
            }
        }
        return entries;
    }

    public int size() {
        return entries().size();
    }

    @Override
    public void close() throws IOException {
        for (InputStream stream : streams) {
            IOUtils.closeQuietly(stream);
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JcrZipFile)) {
            return false;
        }
        JcrZipFile file = (JcrZipFile) o;
        return jcrFile.equals(file.jcrFile);

    }

    public int hashCode() {
        return jcrFile.hashCode();
    }

    private ZipInputStream getZipInputStream() {
        InputStream stream = jcrFile.getStream();
        ZipInputStream zipInputStream = new ZipInputStream(stream);
        streams.add(zipInputStream);
        return zipInputStream;
    }
}