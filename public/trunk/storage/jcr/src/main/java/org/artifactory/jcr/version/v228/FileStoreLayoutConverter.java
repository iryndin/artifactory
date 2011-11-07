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

package org.artifactory.jcr.version.v228;

import org.apache.commons.io.FileUtils;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.jcr.JcrConfResourceLoader;
import org.artifactory.jcr.version.RepoConfigConverterBase;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.XmlUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

/**
 * Converts from the old binaries layout to the new one. Binaries are stored by sha1 checksum. The old layout saved
 * files in a three levels deep folders each consisting of the next 2 characters of the checksum. The new layout has
 * only one level with the first two characters of the sha1 checksum.
 *
 * @author Yossi Shaul
 * @author Fred Simon
 */
public class FileStoreLayoutConverter extends RepoConfigConverterBase {
    private static final Logger log = LoggerFactory.getLogger(FileStoreLayoutConverter.class);
    private static final String REP_HOME = "${rep.home}";
    private static final String FILESTORE = "filestore";
    private static final String CACHE = "cache";

    @SuppressWarnings({"unchecked"})
    @Override
    public void convert(ArtifactoryHome config) {
        ConverterMovedCounter counter = new ConverterMovedCounter();
        File filestoreFolder = null;
        File cacheFolder = null;

        File definedConfigFile = JcrConfResourceLoader.getDefinedConfigFile();
        if (definedConfigFile.exists()) {
            Document doc;
            try {
                doc = XmlUtils.parse(FileUtils.readFileToString(definedConfigFile));
            } catch (IOException e) {
                throw new RuntimeException("Could not read file " + definedConfigFile.getAbsolutePath(), e);
            }
            Element dataStore = doc.getRootElement().getChild("DataStore");
            List<Element> params = dataStore.getChildren("param");
            for (Element param : params) {
                String value = param.getAttribute("value").getValue();
                if ("blobsCacheDir".equals(param.getAttribute("name").getValue())) {
                    if (!(REP_HOME + "/" + CACHE).equals(value)) {
                        if (value.startsWith(REP_HOME)) {
                            cacheFolder = new File(config.getDataDir(), value.substring(REP_HOME.length() + 1));
                        } else {
                            cacheFolder = new File(value);
                        }
                        if (!cacheFolder.exists()) {
                            throw new RuntimeException(
                                    "Found blobsCacheDir value " + value
                                            + " in " + definedConfigFile.getAbsolutePath()
                                            + " which points to no existing folder " + cacheFolder.getAbsolutePath() + "!\n" +
                                            "If this is correct, create the folder before executing Artifactory!");
                        }
                    }
                }
                if ("fileStoreDir".equals(param.getAttribute("name").getValue())) {
                    if (!(REP_HOME + "/" + FILESTORE).equals(value)) {
                        if (value.startsWith(REP_HOME)) {
                            filestoreFolder = new File(config.getDataDir(), value.substring(REP_HOME.length() + 1));
                        } else {
                            filestoreFolder = new File(value);
                        }
                        if (!filestoreFolder.exists()) {
                            throw new RuntimeException(
                                    "Found fileStoreDir value " + value
                                            + " in " + definedConfigFile.getAbsolutePath()
                                            + " which points to no existing folder " + filestoreFolder.getAbsolutePath() + "!\n" +
                                            "If this is correct, create the folder before executing Artifactory!");
                        }
                    }
                }
            }
        }
        if (cacheFolder == null) {
            cacheFolder = new File(config.getDataDir(), CACHE);
        }
        if (filestoreFolder == null) {
            filestoreFolder = new File(config.getDataDir(), FILESTORE);
        }
        convertBinariesFolder(filestoreFolder, counter);
        convertBinariesCache(cacheFolder);
    }

    private void convertBinariesFolder(File binariesFolder, ConverterMovedCounter counter) {
        if (!binariesFolder.exists()) {
            return;
        }
        MultiStatusHolder statusHolder = new MultiStatusHolder();
        statusHolder.setStatus("Starting conversion of binaries layout in: " + binariesFolder.getAbsolutePath(), log);
        long start = System.currentTimeMillis();

        File[] firstLevel = binariesFolder.listFiles();
        // In case the binaries folder does not contain files, it returns null
        if (firstLevel == null) {
            statusHolder.setWarning("No files found in folder: " + binariesFolder.getAbsolutePath(), log);
        } else {
            int totalFirstLevel = firstLevel.length;
            int movingCount = 0;
            for (File first : firstLevel) {
                statusHolder.setStatus("Moving old files " + (++movingCount) + "/" + totalFirstLevel, log);
                moveFromOldToNewLayout(first, statusHolder, counter);
                pruneIfNeeded(statusHolder, first, counter);
            }
        }
        long tt = (System.currentTimeMillis() - start);
        statusHolder.setStatus("Removed " + counter.foldersRemoved
                + " empty folders, and moved " + counter.filesMoved + " files in " + tt + "ms", log);
        if (statusHolder.isError()) {
            throw new RuntimeException(
                    "Converting binaries layout failed with " + statusHolder.getLastError().getMessage());
        }
    }

    private void convertBinariesCache(File cacheFolder) {
        // delete the content of the caches directory if exists
        if (cacheFolder.exists()) {
            FileUtils.deleteQuietly(cacheFolder);
            cacheFolder.mkdirs();
        }
    }

    private void moveFromOldToNewLayout(File first, MultiStatusHolder statusHolder, ConverterMovedCounter counter) {
        File[] oldSecondDirectories = first.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        for (File oldSecondDir : oldSecondDirectories) {
            // If there are directories on second level => old layout
            boolean toDeleteSecond = true;
            File[] thirdDirectories = oldSecondDir.listFiles();
            for (File thirdDirectory : thirdDirectories) {
                boolean toDeleteThird = true;
                File[] oldCsFiles = thirdDirectory.listFiles();
                for (File oldCsFile : oldCsFiles) {
                    File newCsFile = new File(first, oldCsFile.getName());
                    if (!oldCsFile.renameTo(newCsFile)) {
                        toDeleteSecond = false;
                        toDeleteThird = false;
                        statusHolder.setError(
                                "Could not move file from " + oldCsFile.getAbsolutePath()
                                        + " to new " + newCsFile.getAbsolutePath(), log);
                    } else {
                        counter.filesMoved++;
                    }
                }
                if (toDeleteThird) {
                    if (!thirdDirectory.delete()) {
                        statusHolder.setWarning(
                                "Could not remove empty filestore directory " + thirdDirectory.getAbsolutePath(), log);
                        toDeleteSecond = false;
                    } else {
                        counter.foldersRemoved++;
                    }
                }
            }
            if (toDeleteSecond) {
                if (!oldSecondDir.delete()) {
                    statusHolder.setWarning(
                            "Could not remove empty filestore directory " + oldSecondDir.getAbsolutePath(), log);
                } else {
                    counter.foldersRemoved++;
                }
            }
        }
    }

    private void pruneIfNeeded(MultiStatusHolder statusHolder, File first, ConverterMovedCounter counter) {
        File[] files = first.listFiles();
        if (files == null || files.length == 0) {
            if (!first.delete()) {
                statusHolder.setWarning(
                        "Could not remove empty filestore directory " + first.getAbsolutePath(), log);
            } else {
                counter.foldersRemoved++;
            }
        }
    }

    static class ConverterMovedCounter {
        long foldersRemoved = 0;
        long filesMoved = 0;
    }

}
