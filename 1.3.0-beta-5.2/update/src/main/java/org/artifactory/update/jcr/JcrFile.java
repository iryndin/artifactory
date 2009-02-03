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
package org.artifactory.update.jcr;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;
import static org.apache.jackrabbit.JcrConstants.JCR_DATA;
import org.apache.log4j.Logger;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.stat.StatsInfo;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Yoav Landman
 * @author Yossi Shaul
 */
public class JcrFile extends JcrFsItem {
    private final static Logger LOGGER = Logger.getLogger(JcrFile.class);

    //File specific properties
    public static final String NT_ARTIFACTORY_FILE = "artifactory:file";
    public static final String PROP_ARTIFACTORY_DOWNLOAD_COUNT = "artifactory:downloadCount";
    public static final String PROP_ARTIFACTORY_LAST_UPDATED = "artifactory:lastUpdated";
    public static final String PROP_ARTIFACTORY_MD5 = "artifactory:MD5";
    public static final String PROP_ARTIFACTORY_SHA1 = "artifactory:SHA1";

    public JcrFile(Node node, String repoKey) {
        super(node, repoKey);
    }

    public void exportTo(File exportDir, StatusHolder status) throws Exception {
        try {
            if (isChecksumPath(getNode().getPath())) {
                //Don't bother exporting checksum files - will be recalculated during import
                return;
            }
            File targetFile = new File(exportDir, getRelativePath());
            export(targetFile, true);
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to export jcr file: %s. Skipping file",
                    e.getMessage()));
            LOGGER.debug("Stack trace: ", e);
        }
    }

    public void export(File targetFile, boolean includeMetadata) throws Exception {
        LOGGER.debug("Exporting file '" + getRelativePath() + "'...");
        exportFileContent(targetFile);
        if (includeMetadata) {
            exportMetadata(targetFile, false);
        }
    }

    public void exportMetadata(File targetFile, boolean abortOnError) throws Exception {
        try {
            FileInfo fileInfo = createFileInfo();
            StatsInfo statsInfo = createStatsInfo();
            writeMetadataToFiles(targetFile, fileInfo, statsInfo);
        } catch (Exception e) {
            String msg = "Failed to export metadata of '" + getRelativePath() + "'.";
            if (abortOnError) {
                throw new RuntimeException(msg, e);
            } else {
                LOGGER.warn(msg, e);
            }
        }
    }

    private FileInfo createFileInfo() throws RepositoryException {
        FileInfo fileInfo = new FileInfo();
        fillWithGeneralMetadata(fileInfo);
        fileInfo.setLastUpdated(getLastUpdated());
        fileInfo.setLastModified(getLastModified());
        fileInfo.setSize(getSize());
        fileInfo.setMimeType(getMimeType());
        fileInfo.setSha1(getSha1());
        fileInfo.setMd5(getMd5());
        return fileInfo;
    }

    private StatsInfo createStatsInfo() throws RepositoryException {
        StatsInfo statsInfo = new StatsInfo();
        statsInfo.setDownloadCount(getDownloadCount());
        return statsInfo;
    }

    public void exportFileContent(File targetFile) throws Exception {
        //Do not bother exporting if the target file exists with the same modification date
        long modified = getLastModified();
        if (targetFile.exists() && targetFile.lastModified() == modified) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Skipping export of unmodified file '" + targetFile.getAbsolutePath() +
                        "'.");
            }
            return;
        }
        OutputStream os = null;
        InputStream is = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(targetFile));
            is = getStream();
            IOUtils.copy(is, os);
            if (modified >= 0) {
                targetFile.setLastModified(modified);
            }
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }

    public InputStream getStream() throws RepositoryException {
        Node resNode = getResourceNode();
        Value attachedDataValue = resNode.getProperty(JCR_DATA).getValue();
        InputStream is = attachedDataValue.getStream();
        return is;
    }

    private Node getResourceNode() throws RepositoryException {
        return getNode().getNode(JCR_CONTENT);
    }

    /**
     * When was a cacheable file last updated
     *
     * @return the last update time as UTC milliseconds
     */
    public long getLastUpdated() throws RepositoryException {
        if (getNode().hasProperty(PROP_ARTIFACTORY_LAST_UPDATED)) {
            return getPropValue(PROP_ARTIFACTORY_LAST_UPDATED).getDate().getTimeInMillis();
        }
        return 0;
    }

    public long getLastModified() throws RepositoryException {
        Node resourceNode = getResourceNode();
        if (resourceNode.hasProperty(JcrConstants.JCR_LASTMODIFIED)) {
            Property prop = resourceNode.getProperty(JcrConstants.JCR_LASTMODIFIED);
            return prop.getDate().getTimeInMillis();
        }
        return 0;
    }

    public String getMimeType() throws RepositoryException {
        Node resourceNode = getResourceNode();
        if (getNode().hasProperty(JcrConstants.JCR_MIMETYPE)) {
            Property prop = resourceNode.getProperty(JcrConstants.JCR_MIMETYPE);
            return prop.getString();
        }
        return null;
    }

    public long getDownloadCount() throws RepositoryException {
        if (getNode().hasProperty(PROP_ARTIFACTORY_DOWNLOAD_COUNT)) {
            return getPropValue(PROP_ARTIFACTORY_DOWNLOAD_COUNT).getLong();
        }
        return 0;
    }

    public String getMd5() throws RepositoryException {
        if (getNode().hasProperty(PROP_ARTIFACTORY_MD5)) {
            return getPropValue(PROP_ARTIFACTORY_MD5).getString();
        }
        return null;
    }

    public String getSha1() throws RepositoryException {
        if (getNode().hasProperty(PROP_ARTIFACTORY_SHA1)) {
            return getPropValue(PROP_ARTIFACTORY_SHA1).getString();
        }
        return null;
    }

    public long getSize() throws RepositoryException {
        Node resourceNode = getResourceNode();
        if (resourceNode.hasProperty(JCR_DATA)) {
            return resourceNode.getProperty(JCR_DATA).getLength();
        }
        return 0;
    }

    private static void writeMetadataToFiles(
            File targetFile, FileInfo fileInfo, StatsInfo statsInfo)
            throws FileNotFoundException {
        File parentFolder = targetFile.getParentFile();
        File metadataFolder = new File(
                parentFolder, targetFile.getName() + FileInfo.METADATA_FOLDER);
        metadataFolder.mkdir();
        File fileInfoMetadataFile = new File(metadataFolder, FileInfo.ROOT + ".xml");
        File statsInfoMetadataFile = new File(metadataFolder, StatsInfo.ROOT + ".xml");
        FileOutputStream fileInfoOutputStream = null;
        FileOutputStream statsInfoOutputStream = null;
        try {
            XStream xstream = getXStream();
            fileInfoOutputStream = new FileOutputStream(fileInfoMetadataFile);
            xstream.toXML(fileInfo, fileInfoOutputStream);
            statsInfoOutputStream = new FileOutputStream(statsInfoMetadataFile);
            xstream.toXML(statsInfo, statsInfoOutputStream);
        } finally {
            IOUtils.closeQuietly(fileInfoOutputStream);
            IOUtils.closeQuietly(statsInfoOutputStream);
        }
    }

    private static boolean isChecksumPath(String path) {
        path = path.toLowerCase();
        return path.endsWith(".sha1") || path.endsWith(".md5") || path.endsWith(".asc");
    }
}
