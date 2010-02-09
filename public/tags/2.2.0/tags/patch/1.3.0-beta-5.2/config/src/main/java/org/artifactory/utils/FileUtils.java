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
package org.artifactory.utils;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Random;

/**
 * User: freds Date: Jun 25, 2008 Time: 12:11:46 AM
 */
public class FileUtils {

    private final static Logger LOGGER = Logger.getLogger(FileUtils.class);

    public static final String ORIGINAL = ".original.";

    private static final Random intGenerator = new Random(System.currentTimeMillis());

    /**
     * Rename the oldFile to oldFileName.original.XX.extension, where XX is a rolling number for
     * files with identical names in the oldFile.getParent() directory. Then Rename the newFile to
     * oldFile.
     *
     * @param oldFile
     * @param newFile
     */
    public static void switchFiles(File oldFile, File newFile) {
        if (!oldFile.isFile()) {
            throw new RuntimeException("Cannot switch files " + oldFile + " to " +
                    newFile + " that are not files!");
        }
        File dir = oldFile.getParentFile();
        String fileName = oldFile.getName();
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

        final String fileNameNoNumber = fileNameNoExt + ORIGINAL;
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
                    LOGGER.warn("Minor issue in file name '" + origFile + "':" + e.getMessage());
                }
            }
            backupOrigFileName = new File(dir, fileNameNoNumber + maxNb + ".xml");
        }
        oldFile.renameTo(backupOrigFileName);
        newFile.renameTo(new File(dir, fileName));
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

    public static boolean createSymLink(File source, File dest) {
        String cmd = "ln " + "-s " + source + " " + dest.getAbsolutePath();
        return ExecUtils.execute(cmd);
    }
}