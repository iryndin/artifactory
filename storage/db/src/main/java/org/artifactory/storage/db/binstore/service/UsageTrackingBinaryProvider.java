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

import org.artifactory.binstore.BinaryInfo;
import org.artifactory.storage.binstore.BinaryStoreInputStream;
import org.artifactory.storage.binstore.service.BinaryNotFoundException;
import org.artifactory.storage.binstore.service.BinaryProvider;
import org.artifactory.storage.binstore.service.SkippableInputStream;
import org.artifactory.storage.binstore.service.base.BinaryProviderBase;
import org.artifactory.storage.binstore.service.BinaryStoreServices;
import org.artifactory.storage.binstore.service.annotation.BinaryProviderClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This binary provider wraps binary streams to protect the underlying binary from deletion while the streams is still open.
 *
 * @author freds
 */
@BinaryProviderClassInfo(nativeName = "tracking")
public class UsageTrackingBinaryProvider extends BinaryProviderBase implements BinaryProvider {
    private static final Logger log = LoggerFactory.getLogger(UsageTrackingBinaryProvider.class);

    @Nonnull
    @Override
    public InputStream getStream(String sha1) throws BinaryNotFoundException {
        return new ReaderTrackingStream(next().getStream(sha1), sha1, getBinaryStoreServices());
    }


    @Override
    public boolean exists(String sha1) {
        return next().exists(sha1);
    }

    @Nonnull
    @Override
    public BinaryInfo addStream(InputStream is) throws IOException {
        return next().addStream(is);
    }

    @Override
    public boolean delete(String sha1) {
        return next().delete(sha1);
    }

    static class ReaderTrackingStream extends BufferedInputStream implements BinaryStoreInputStream ,SkippableInputStream{
        private final String sha1;
        private final InputStream inputStream;
        private BinaryStoreServices binaryStoreServices;
        private boolean closed = false;

        public ReaderTrackingStream(InputStream is, String sha1, BinaryStoreServices binaryStoreServices) {
            super(is);
            inputStream=is;
            this.sha1 = sha1;
            this.binaryStoreServices = binaryStoreServices;
            int newReadersCount = binaryStoreServices.incrementNoDeleteLock(sha1);
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
                    binaryStoreServices.decrementNoDeleteLock(sha1);
                }
            }
        }

        @Override
        public boolean isSkippable() {
            if(inputStream instanceof SkippableInputStream){
                return ((SkippableInputStream)inputStream).isSkippable();
            }
            return true;
        }
    }

}


