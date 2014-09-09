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

import org.apache.commons.io.IOUtils;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.binstore.BinaryInfo;
import org.artifactory.io.checksum.Sha1Md5ChecksumInputStream;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.StorageProperties;
import org.artifactory.storage.binstore.service.BinaryNotFoundException;
import org.artifactory.storage.binstore.service.FileBinaryProvider;
import org.artifactory.storage.db.binstore.model.BinaryInfoImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A binary provider that manage low level checksum files on filesystem.
 *
 * @author Fred Simon
 */
class DoubleFileBinaryProviderImpl extends BinaryProviderBase implements FileBinaryProvider {
    private static final Logger log = LoggerFactory.getLogger(DoubleFileBinaryProviderImpl.class);
    private final DynamicFileBinaryProviderImpl[] providers;

    public DoubleFileBinaryProviderImpl(File rootDataDir, StorageProperties storageProperties) {
        long checkPeriod = storageProperties.getLongProperty(
                StorageProperties.Key.binaryProviderFilesystemSecondCheckPeriod.key(), 60000L);
        providers = new DynamicFileBinaryProviderImpl[2];
        providers[0] = new DynamicFileBinaryProviderImpl(
                FileBinaryProviderBase.getDataFolder(rootDataDir, storageProperties,
                        StorageProperties.Key.binaryProviderFilesystemDir, "filestore"), checkPeriod);
        providers[1] = new DynamicFileBinaryProviderImpl(
                FileBinaryProviderBase.getDataFolder(rootDataDir, storageProperties,
                        StorageProperties.Key.binaryProviderFilesystemSecondDir, "second-filestore"), checkPeriod);
    }

    @Override
    public void setContext(@Nonnull BinaryProviderContext ctx) {
        super.setContext(ctx);
        for (DynamicFileBinaryProviderImpl provider : providers) {
            provider.setContext(ctx);
            provider.verifyState(provider.binariesDir);
        }
    }

    private DynamicFileBinaryProviderImpl getFirst() {
        DynamicFileBinaryProviderImpl result = findActiveBinaryProvider();

        if (result == null) {
            throw new StorageException("Could not find any file binary provider active!\n");
        }
        return result;
    }

    private DynamicFileBinaryProviderImpl findActiveBinaryProvider() {
        // TODO: Load balance or weigh based read
        DynamicFileBinaryProviderImpl result = null;
        for (DynamicFileBinaryProviderImpl provider : providers) {
            if (provider.isActive()) {
                result = provider;
                break;
            }
        }
        return result;
    }

    @Nonnull
    @Override
    public File getBinariesDir() {
        DynamicFileBinaryProviderImpl provider = findActiveBinaryProvider();
        if (provider == null) {
            // Don't throw exception here always return the first one
            return providers[0].getBinariesDir();
        }
        return provider.getBinariesDir();
    }

    @Nonnull
    @Override
    public File getFile(String sha1) {
        return getFirst().getFile(sha1);
    }

    @Override
    public void prune(MultiStatusHolder statusHolder) {
        for (DynamicFileBinaryProviderImpl provider : providers) {
            if (provider.isActive()) {
                provider.prune(statusHolder);
            }
        }
    }

    @Override
    public boolean exists(String sha1, long length) {
        for (DynamicFileBinaryProviderImpl provider : providers) {
            if (provider.isActive() && provider.exists(sha1, length)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public InputStream getStream(String sha1) throws BinaryNotFoundException {
        BinaryNotFoundException eNotFound = null;
        // TODO: Load balance or weigh based read
        for (DynamicFileBinaryProviderImpl provider : providers) {
            if (provider.isActive()) {
                File file = provider.getFile(sha1);
                if (file.exists()) {
                    log.trace("File found: {}", file.getAbsolutePath());
                    try {
                        return new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        log.info("Failed accessing existing file due to " + e.getMessage() + ".\n" +
                                "Will mark provider inactive!");
                        provider.markInactive(e);
                        eNotFound = new BinaryNotFoundException("Couldn't access file '" + file.getAbsolutePath() + "'",
                                e);
                    }
                }
            }
        }
        if (eNotFound != null) {
            throw eNotFound;
        }
        return next().getStream(sha1);
    }

    @Override
    public boolean delete(String sha1) {
        // We are blocking GC from running if not ALL providers are up.
        // So, should return true only if all deletion worked
        boolean result = true;
        for (DynamicFileBinaryProviderImpl provider : providers) {
            if (provider.isActive()) {
                result &= provider.delete(sha1);
            } else {
                result = false;
            }
        }
        return result;
    }

    @Override
    @Nonnull
    public BinaryInfo addStream(InputStream in) throws IOException {
        ProviderAndTempFile[] providerAndTempFiles = null;
        Sha1Md5ChecksumInputStream checksumStream = null;
        try {
            // first save to a temp file and calculate checksums while saving
            if (in instanceof Sha1Md5ChecksumInputStream) {
                checksumStream = (Sha1Md5ChecksumInputStream) in;
            } else {
                checksumStream = new Sha1Md5ChecksumInputStream(in);
            }
            providerAndTempFiles = writeToTempFile(checksumStream);
            BinaryInfo bd = new BinaryInfoImpl(checksumStream);
            log.trace("Inserting {} in file binary provider", bd);

            String sha1 = bd.getSha1();
            for (ProviderAndTempFile providerAndTempFile : providerAndTempFiles) {
                File tempFile = providerAndTempFile.tempFile;
                if (tempFile != null && providerAndTempFile.somethingWrong == null) {
                    long fileLength = tempFile.length();
                    if (fileLength != checksumStream.getTotalBytesRead()) {
                        throw new IOException("File length is " + fileLength + " while total bytes read on" +
                                " stream is " + checksumStream.getTotalBytesRead());
                    }
                    File file = providerAndTempFile.provider.getFile(sha1);
                    Path target = file.toPath();
                    if (!java.nio.file.Files.exists(target)) {
                        // move the file from the pre-filestore to the filestore
                        java.nio.file.Files.createDirectories(target.getParent());
                        try {
                            log.trace("Moving {} to {}", tempFile.getAbsolutePath(), target);
                            java.nio.file.Files.move(tempFile.toPath(), target, StandardCopyOption.ATOMIC_MOVE);
                            log.trace("Moved  {} to {}", tempFile.getAbsolutePath(), target);
                        } catch (FileAlreadyExistsException ignore) {
                            // May happen in heavy concurrency cases
                            log.trace("Failed moving {} to {}. File already exist", tempFile.getAbsolutePath(),
                                    target);
                        }
                        providerAndTempFile.tempFile = null;
                    } else {
                        log.trace("File {} already exist in the file store. Deleting temp file: {}",
                                target, tempFile.getAbsolutePath());
                    }
                }
            }
            return bd;
        } finally {
            IOUtils.closeQuietly(checksumStream);
            if (providerAndTempFiles != null) {
                for (ProviderAndTempFile providerAndTempFile : providerAndTempFiles) {
                    File file = providerAndTempFile.tempFile;
                    if (file != null && file.exists()) {
                        if (!file.delete()) {
                            log.error("Could not delete temp file {}", file.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }

    static class ProviderAndTempFile {
        final DynamicFileBinaryProviderImpl provider;
        File tempFile = null;
        IOException somethingWrong = null;

        ProviderAndTempFile(DynamicFileBinaryProviderImpl provider) throws IOException {
            this.provider = provider;
            if (provider.isActive()) {
                tempFile = provider.createTempBinFile();
            }
        }
    }

    /**
     * Creates a temp file for each active provider and copies the data there.
     * The input stream is closed afterwards.
     *
     * @param in the input stream
     * @return the collection of provider and temp file
     * @throws java.io.IOException On failure writing to all temp file
     */
    private ProviderAndTempFile[] writeToTempFile(InputStream in) throws IOException {
        ProviderAndTempFile[] result = new ProviderAndTempFile[providers.length];
        int i = 0;
        for (DynamicFileBinaryProviderImpl provider : providers) {
            result[i++] = new ProviderAndTempFile(provider);
        }

        //log.trace("Saving temp files: {} {}", 0, 0);
        try (OutputStream os = new MultipleFilesOutputStream(result)) {
            byte[] buffer = new byte[4 * 1024];
            int n;
            while (-1 != (n = in.read(buffer))) {
                os.write(buffer, 0, n);
            }
            os.close();
            in.close();
        }
        //log.trace("Saved  temp file: {} {}", temp[0].getAbsolutePath(), temp[1].getAbsolutePath());
        return result;
    }

    /*
    @Override
    protected void pruneFiles(MultiStatusHolder statusHolder, MovedCounter movedCounter, File first) {
        statusHolder.status("Starting checking if files in " + first.getAbsolutePath() + " are in DB!", log);
        //Set<DataIdentifier> identifiersSet = getIdentifiersSet();
        File[] files = first.listFiles();
        if (files == null) {
            statusHolder.status("Nothing to do in " + first.getAbsolutePath() + " " + Files.readFailReason(first), log);
            return;
        }
        Set<String> filesInFolder = new HashSet<>(files.length);
        for (File file : files) {
            filesInFolder.add(file.getName());
        }
        Set<String> existingSha1 = getContext().isInStore(filesInFolder);
        for (File file : files) {
            String sha1 = file.getName();
            if (!existingSha1.contains(sha1)) {
                if (getContext().isActivelyUsed(sha1)) {
                    statusHolder.status("Skipping deletion for in-use artifact record: " + sha1, log);
                } else {
                    long size = file.length();
                    Files.removeFile(file);
                    if (file.exists()) {
                        statusHolder.error("Could not delete file " + file.getAbsolutePath(), log);
                    } else {
                        movedCounter.filesMoved++;
                        movedCounter.totalSize += size;
                    }
                }
            }
        }
    }
    */

    interface Do {
        void call(FileOutputStream os) throws IOException;
    }

    static class DoWrite implements Do {
        final byte[] b;
        final int off;
        final int len;

        DoWrite(byte[] b, int off, int len) {
            this.b = b.clone();
            this.off = off;
            this.len = len;
        }

        @Override
        public void call(FileOutputStream os) throws IOException {
            os.write(b, off, len);
        }
    }

    static class DoWriteByte implements Do {
        final int b;

        DoWriteByte(int b) {
            this.b = b;
        }

        @Override
        public void call(FileOutputStream os) throws IOException {
            os.write(b);
        }
    }

    static class DoFlush implements Do {
        @Override
        public void call(FileOutputStream os) throws IOException {
            os.flush();
        }
    }

    static class DoClose implements Do {
        @Override
        public void call(FileOutputStream os) throws IOException {
            os.close();
        }
    }

    static class FileOutputStreamWithQueue {
        private final ProviderAndTempFile providerAndTempFile;
        private final BlockingQueue<Do> queue;
        private final Thread consumer;
        private boolean closed;

        FileOutputStreamWithQueue(ProviderAndTempFile pAndTemp) {
            this.closed = false;
            this.providerAndTempFile = pAndTemp;
            this.queue = new ArrayBlockingQueue<>(4);
            this.consumer = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        try (FileOutputStream os = new FileOutputStream(providerAndTempFile.tempFile)) {
                            while (!closed || !queue.isEmpty()) {
                                try {
                                    Do take = queue.take();
                                    take.call(os);
                                } catch (InterruptedException e) {
                                    throw new IOException("Got interrupted writing", e);
                                }
                            }
                        }
                    } catch (IOException e) {
                        providerAndTempFile.somethingWrong = e;
                    }
                }
            }, "par-file-writer-" + providerAndTempFile.tempFile.getName());
        }

        void add(Do call) throws IOException {
            try {
                queue.offer(call, 5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new IOException("Got interrupted sending " + call.getClass().getSimpleName()
                        + " to the Output Stream", e);
            }
        }

        void close() throws IOException {
            try {
                add(new DoFlush());
                this.closed = true;
                add(new DoClose());
                this.consumer.join(5000L);
            } catch (InterruptedException e) {
                throw new IOException("Got interrupted closing Output Stream", e);
            }
        }
    }

    static class MultipleFilesOutputStream extends OutputStream {
        private final ProviderAndTempFile[] providerAndTempFiles;
        private final FileOutputStreamWithQueue[] outputStreams;

        MultipleFilesOutputStream(ProviderAndTempFile[] providerAndTempFiles) throws FileNotFoundException {
            if (providerAndTempFiles == null || providerAndTempFiles.length == 0) {
                throw new IllegalArgumentException("Cannot create File Output Stream from empty file!");
            }
            this.providerAndTempFiles = providerAndTempFiles;
            this.outputStreams = new FileOutputStreamWithQueue[providerAndTempFiles.length];
            int i = 0;
            for (ProviderAndTempFile providerAndTempFile : providerAndTempFiles) {
                if (providerAndTempFile.tempFile != null) {
                    outputStreams[i++] = new FileOutputStreamWithQueue(providerAndTempFile);
                }
            }
            for (FileOutputStreamWithQueue outputStream : outputStreams) {
                if (outputStream != null) {
                    outputStream.consumer.start();
                }
            }
        }


        private void execute(Do d) throws IOException {
            for (int i = 0; i < outputStreams.length; i++) {
                // If something failed before, don't try again
                if (outputStreams[i] != null && providerAndTempFiles[i].somethingWrong == null) {
                    try {
                        outputStreams[i].add(d);
                    } catch (IOException e) {
                        providerAndTempFiles[i].somethingWrong = e;
                        // If all providers are failing => throw back the exception
                        for (int j = 0; j < providerAndTempFiles.length; j++) {
                            if (outputStreams[j] != null
                                    && providerAndTempFiles[j].somethingWrong == null) {
                                // We are good still one provider on
                                return;
                            }
                        }
                        // No more valid providers
                        throw e;
                    }
                }
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            execute(new DoWrite(b, off, len));
        }

        @Override
        public void write(int b) throws IOException {
            execute(new DoWriteByte(b));
        }

        @Override
        public void flush() throws IOException {
            execute(new DoFlush());
        }

        @Override
        public void close() throws IOException {
            for (FileOutputStreamWithQueue outputStream : outputStreams) {
                outputStream.close();
            }
        }
    }
}
