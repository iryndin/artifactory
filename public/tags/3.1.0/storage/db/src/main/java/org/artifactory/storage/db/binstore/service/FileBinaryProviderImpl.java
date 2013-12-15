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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.binstore.BinaryInfo;
import org.artifactory.io.checksum.Sha1Md5ChecksumInputStream;
import org.artifactory.storage.StorageProperties;
import org.artifactory.storage.binstore.service.FileBinaryProvider;
import org.artifactory.storage.db.binstore.model.BinaryInfoImpl;
import org.artifactory.util.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

/**
 * A binary provider that manage low level checksum files on filesystem.
 *
 * @author Fred Simon
 */
class FileBinaryProviderImpl extends FileBinaryProviderBase implements FileBinaryProvider {
    private static final Logger log = LoggerFactory.getLogger(FileBinaryProviderImpl.class);

    public FileBinaryProviderImpl(File rootDataDir, StorageProperties storageProperties) {
        super(getDataFolder(rootDataDir, storageProperties,
                StorageProperties.Key.binaryProviderFilesystemDir, "filestore"));
    }

    @Override
    @Nonnull
    public BinaryInfo addStream(InputStream in) throws IOException {
        check();
        // TODO: How to know if next() should be called?

        File preFileStoreFile = null;
        Sha1Md5ChecksumInputStream checksumStream = null;
        try {
            // first save to a temp file and calculate checksums while saving
            if (in instanceof Sha1Md5ChecksumInputStream) {
                checksumStream = (Sha1Md5ChecksumInputStream) in;
            } else {
                checksumStream = new Sha1Md5ChecksumInputStream(in);
            }
            preFileStoreFile = writeToTempFile(checksumStream);
            BinaryInfo bd = new BinaryInfoImpl(checksumStream);
            log.trace("Inserting {} in file binary provider", bd);

            String sha1 = bd.getSha1();
            long fileLength = preFileStoreFile.length();
            if (fileLength != checksumStream.getTotalBytesRead()) {
                //TODO: [by FSI] Should it be a real exception?
                log.error("File length is " + fileLength + " while total bytes read on" +
                        " stream is " + checksumStream.getTotalBytesRead());
            }

            Path target = getFile(sha1).toPath();
            if (!java.nio.file.Files.exists(target)) {
                // move the file from the pre-filestore to the filestore
                java.nio.file.Files.createDirectories(target.getParent());
                try {
                    java.nio.file.Files.move(preFileStoreFile.toPath(), target, StandardCopyOption.ATOMIC_MOVE);
                } catch (FileAlreadyExistsException ignore) {
                    // May happen in heavy concurrency cases
                }
                preFileStoreFile = null;
            }
            return bd;
        } finally {
            IOUtils.closeQuietly(checksumStream);
            if (preFileStoreFile != null && preFileStoreFile.exists()) {
                if (!preFileStoreFile.delete()) {
                    log.error("Could not delete temp file " + preFileStoreFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Creates a temp file and copies the data there. The input stream is closed afterwards.
     *
     * @param in the input stream
     * @return the file
     * @throws IOException On failure writing to the temp file
     */
    private File writeToTempFile(InputStream in) throws IOException {
        File temp = getTempBinFile();
        FileUtils.copyInputStreamToFile(in, temp);
        return temp;
    }

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
                if (getContext().isUsedByReader(sha1)) {
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
}
