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

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.storage.StorageUnit;
import org.artifactory.storage.StorageProperties;
import org.artifactory.storage.binstore.service.FileBinaryProvider;
import org.artifactory.util.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Date: 12/13/12
 * Time: 7:56 AM
 *
 * @author freds
 */
public abstract class FileBinaryProviderBase extends FileBinaryProviderReadOnlyBase {
    private static final Logger log = LoggerFactory.getLogger(FileBinaryProviderBase.class);

    public FileBinaryProviderBase(File binariesDir) {
        super(binariesDir);
    }

    protected static File getDataFolder(File rootDataDir, StorageProperties storageProperties,
            StorageProperties.Key keyProperty, String defaultName) {
        String name = storageProperties.getProperty(keyProperty);
        if (StringUtils.isBlank(name)) {
            return new File(rootDataDir, defaultName);
        }
        File currentFile = new File(name);
        if (currentFile.isAbsolute()) {
            return currentFile;
        }
        return new File(rootDataDir, name);
    }

    @Override
    public boolean delete(String sha1) {
        if (deleteNoChain(sha1)) {
            return next().delete(sha1);
        }
        return false;
    }

    protected boolean deleteNoChain(String sha1) {
        check();
        if (getContext().isActivelyUsed(sha1)) {
            log.info("File {} is read. Deletion is skipped", sha1);
            return false;
        }
        File file = getFile(sha1);
        log.debug("Deleting file {}", file.getAbsolutePath());
        Files.removeFile(file);
        if (file.exists()) {
            log.error("Could not delete file " + file.getAbsolutePath());
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void prune(MultiStatusHolder statusHolder) {
        File binariesFolder = getBinariesDir();
        statusHolder.status("Starting cleaning folder " + binariesFolder.getAbsolutePath(), log);
        long start = System.currentTimeMillis();
        MovedCounter movedCounter = new MovedCounter();

        File[] firstLevel = binariesFolder.listFiles();
        // In case the binaries folder does not contain files, it returns null
        if (firstLevel == null) {
            statusHolder.warn("No files found in folder: " + binariesFolder.getAbsolutePath() + ": "
                    + Files.readFailReason(binariesFolder), log);
        } else {
            // Then prune empty dirs
            statusHolder.status("Starting removing empty folders in " + binariesFolder.getAbsolutePath(), log);
            for (File first : firstLevel) {
                if (first.getName().equals(tempBinariesDir.getName())) {
                    // Never prune temp folder
                    continue;
                }
                // Do the pruning
                pruneFiles(statusHolder, movedCounter, first);
                pruneIfNeeded(statusHolder, movedCounter, first);
            }
        }
        long tt = (System.currentTimeMillis() - start);
        statusHolder.status("Removed " + movedCounter.foldersRemoved
                + " empty folders and " + movedCounter.filesMoved
                + " files in total size of " + StorageUnit.toReadableString(movedCounter.totalSize)
                + " (" + tt + "ms).", log);
        BinaryProviderBase next = next();
        if (next instanceof FileBinaryProviderBase) {
            ((FileBinaryProvider) next()).prune(statusHolder);
        }
    }

    protected void pruneIfNeeded(MultiStatusHolder statusHolder, MovedCounter movedCounter, File first) {
        File[] files = first.listFiles();
        if (files == null || files.length == 0) {
            if (!first.delete()) {
                statusHolder.warn(
                        "Could not remove empty directory " + first.getAbsolutePath(), log);
            } else {
                movedCounter.foldersRemoved++;
            }
        }
    }

    protected abstract void pruneFiles(MultiStatusHolder statusHolder, MovedCounter movedCounter, File first);

    static class MovedCounter {
        long foldersRemoved = 0;
        long filesMoved = 0;
        long totalSize = 0;
    }
}
