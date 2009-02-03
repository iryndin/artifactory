/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.jcr.fs;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A wrapper on a zip resource under a jcr file. This class is non tread safe.
 */
public class JcrZipFile {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrZipFile.class);

    private JcrFile jcrFile;
    private List<ZipEntry> entries;
    private List<InputStream> streams = new ArrayList<InputStream>();

    public JcrZipFile(String absPath) {
        this(new JcrFile(absPath));
    }

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
            if (currentEntry.equals(entry)) {
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
                throw new RuntimeException(
                        "Failed to read zip entries from '" + getName() + "'.", e);
            }
        }
        return entries;
    }

    public int size() {
        return entries().size();
    }

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