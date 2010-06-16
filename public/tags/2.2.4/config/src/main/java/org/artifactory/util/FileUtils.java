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

package org.artifactory.util;

import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Random;

/**
 * User: freds Date: Jun 25, 2008 Time: 12:11:46 AM
 */
public class FileUtils {
    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);
    private static final Random intGenerator = new Random(System.currentTimeMillis());

    public static String getDecodedFileUrl(File file) {
        return "file:" + file.toURI().getPath();
    }

    /**
     * Rename the oldFile to oldFileName.original.XX.extension, where XX is a rolling number for files with identical
     * names in the oldFile.getParent() directory. Then Rename the newFile to oldFile.
     *
     * @param oldFile
     * @param newdFile
     */
    public static void switchFiles(File oldFile, File newdFile) {
        String exceptionMsg = "Cannot switch files '" + oldFile.getAbsolutePath() + "' to '" +
                newdFile.getAbsolutePath() + "'. '";
        if (!oldFile.exists() || !oldFile.isFile()) {
            throw new IllegalArgumentException(exceptionMsg + oldFile.getAbsolutePath() + "' is not a file.");
        }
        if (!newdFile.exists()) {
            // Just rename the new file to the old name
            oldFile.renameTo(newdFile);
        }
        if (newdFile.exists() && !newdFile.isFile()) {
            //renameTo() of open files does not work on windows (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6213298)
            throw new IllegalArgumentException(exceptionMsg + newdFile.getAbsolutePath() + "' is not a file.");
        }
        //Else is
        File dir = newdFile.getParentFile();
        String fileName = newdFile.getName();
        int lastDot = fileName.lastIndexOf('.');
        final String extension;
        final String fileNameNoExt;
        if (lastDot <= 0) {
            extension = "";
            fileNameNoExt = fileName;
        } else {
            extension = fileName.substring(lastDot + 1);
            fileNameNoExt = fileName.substring(0, lastDot);
        }

        final String fileNameNoNumber = fileNameNoExt + ".";
        File backupOrigFileName = new File(dir, fileNameNoNumber + extension);
        if (backupOrigFileName.exists()) {
            // Rolling files
            String[] allOrigFiles = dir.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith(fileNameNoExt) && name.endsWith(extension) &&
                            name.length() > fileNameNoNumber.length() + extension.length();
                }
            });
            int maxNb = 1;
            for (String origFile : allOrigFiles) {
                try {
                    String middle = origFile.substring(fileNameNoNumber.length(),
                            origFile.length() - extension.length() - 1);
                    int value = Integer.parseInt(middle);
                    if (value >= maxNb) {
                        maxNb = value + 1;
                    }
                } catch (Exception e) {
                    log.warn("Minor issue in file name '" + origFile + "':" + e.getMessage());
                }
            }
            backupOrigFileName = new File(dir, fileNameNoNumber + maxNb + ".xml");
        }
        newdFile.renameTo(backupOrigFileName);
        oldFile.renameTo(new File(dir, fileName));
    }

    public static File createRandomDir(File parentDir, String prefix) {
        File dir = new File(parentDir, prefix + intGenerator.nextInt());
        if (dir.exists()) {
            //Either we did not clean up or are VERY unlucky
            throw new RuntimeException("Directory " + dir.getAbsolutePath() + " already exists!");
        }
        if (!dir.mkdirs()) {
            throw new RuntimeException("Failed to create directory '" + dir.getPath() + "'.");
        }
        return dir;
    }

    /**
     * Deletes all empty directories (directories that don't contain any files) under the input directory. The input
     * directory will not be deleted.
     *
     * @param directory The directory to cleanup
     */
    public static void cleanupEmptyDirectories(File directory) {
        cleanupEmptyDirectories(directory, false);
    }

    /**
     * Deletes all empty directories (directories that don't contain any files) under the input directory.
     *
     * @param directory            The directory to cleanup
     * @param includeBaseDirectory if true the input directory will also be deleted if it is empty
     */
    private static void cleanupEmptyDirectories(File directory, boolean includeBaseDirectory) {
        if (!directory.exists()) {
            log.warn("{} does not exist", directory);
            return;
        }

        if (!directory.isDirectory()) {
            log.warn("{} is not a directory", directory);
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {  // null if security restricted
            log.warn("Failed to list contents of {}", directory);
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                cleanupEmptyDirectories(file, true);  // recursive call always include new base directory
            }
        }

        // all other directories scanned - now check if this direcotry should be deleted
        if (includeBaseDirectory && directory.listFiles().length == 0) {
            // empty directory - delete and return
            boolean deleted = directory.delete();
            if (!deleted) {
                log.warn("Failed to delete empty directory {}", directory);
            }
        }

    }

    public static boolean removeFile(File file) {
        if (file != null && file.exists()) {
            //Try to delete the file
            if (!remove(file)) {
                log.warn("Unable to remove " + file.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    private static boolean remove(final File file) {
        if (!file.delete()) {
            // NOTE: fix for java/win bug. see:
            // http://forum.java.sun.com/thread.jsp?forum=4&thread=158689&tstart=
            // 0&trange=15
            System.gc();
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                // nop
            }

            // Try one more time to delete the file
            return file.delete();
        }
        return true;
    }

    public static String readFileToString(File file) {
        try {
            return org.apache.commons.io.FileUtils.readFileToString(file, "utf-8");
        } catch (IOException e) {
            throw new RuntimeException("Couldn't read text file from " + file.getAbsolutePath(), e);
        }
    }
}