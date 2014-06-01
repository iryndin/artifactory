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

package org.artifactory.storage.db.binstore.service;

import com.google.common.collect.MapMaker;
import org.artifactory.binstore.BinaryInfo;
import org.artifactory.storage.binstore.BinaryStoreInputStream;
import org.artifactory.storage.binstore.service.BinaryNotFoundException;
import org.artifactory.storage.binstore.service.BinaryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date: 12/11/12
 * Time: 6:31 PM
 *
 * @author freds
 */
class ReadTrackingBinaryProvider extends BinaryProviderBase implements BinaryProvider {
    private static final Logger log = LoggerFactory.getLogger(ReadTrackingBinaryProvider.class);

    private ConcurrentMap<String, AtomicInteger> readersCounter;

    public ReadTrackingBinaryProvider() {
        readersCounter = new MapMaker().makeMap();
    }

    public boolean isUsedByReader(String sha1) {
        AtomicInteger readers = readersCounter.get(sha1);
        return readers != null && readers.get() > 0;
    }

    @Nonnull
    @Override
    public InputStream getStream(String sha1) throws BinaryNotFoundException {
        readersCounter.putIfAbsent(sha1, new AtomicInteger(0));
        AtomicInteger readersCount = readersCounter.get(sha1);
        if (readersCount.get() < 0) {
            throw new BinaryNotFoundException("File " + sha1 + " is currently being deleted!");
        }
        return new ReaderTrackingStream(next().getStream(sha1), sha1, readersCount);
    }


    @Override
    public boolean exists(String sha1, long length) {
        return next().exists(sha1, length);
    }

    @Nonnull
    @Override
    public BinaryInfo addStream(InputStream is) throws IOException {
        return next().addStream(is);
    }

    @Override
    public boolean delete(String sha1) {
        AtomicInteger reader = readersCounter.get(sha1);
        if (reader != null) {
            if (reader.compareAndSet(0, -30)) {
                // OK, marked for deletion with neg value on readers => can remove entry
                readersCounter.remove(sha1);
            } else {
                log.info("Deletion of file '" + sha1 + "', blocked since it is still being read!");
                return false;
            }
        }
        return next().delete(sha1);
    }

    static class ReaderTrackingStream extends BufferedInputStream implements BinaryStoreInputStream {
        private final String sha1;
        private final AtomicInteger readersCount;
        private boolean closed = false;

        public ReaderTrackingStream(InputStream is, String sha1, AtomicInteger readersCount) {
            super(is);
            this.sha1 = sha1;
            this.readersCount = readersCount;
            int newReadersCount = readersCount.incrementAndGet();
            if (newReadersCount < 0) {
                try {
                    // File being deleted...
                    super.close();
                } catch (IOException ignore) {
                    log.debug("IO on close when deletion", ignore);
                }
                throw new BinaryNotFoundException("File " + sha1 + " is currently being deleted!");
            }
        }

        @Override
        public String getSha1() {
            return sha1;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                if (!closed) {
                    closed = true;
                    readersCount.decrementAndGet();
                }
            }
        }
    }

}


