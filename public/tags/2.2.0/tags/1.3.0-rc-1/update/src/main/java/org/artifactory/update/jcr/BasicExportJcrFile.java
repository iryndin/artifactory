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
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.stat.StatsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @author freds
 * @date Nov 16, 2008
 */
public abstract class BasicExportJcrFile extends ExportJcrFsItem {
    private static final Logger log = LoggerFactory.getLogger(BasicExportJcrFile.class);

    public BasicExportJcrFile(Node node, String repoKey) {
        super(node, repoKey);
    }

    public void exportTo(File exportDir, StatusHolder status) throws Exception {
        try {
            if (isChecksumPath(getNode().getPath())) {
                //Don't bother exporting checksum files - will be recalculated during import
                return;
            }
            File targetFile = new File(exportDir, getRelativePath());
            /*
            //No need for special export - will be taken care of by the import process
            if (MavenNaming.isMavenMetadataFileName(targetFile.getName())) {
                //Special fondling for maven-metadata.xml - export it as the folder metadata
            }
            */
            export(targetFile, true);
        } catch (Exception e) {
            status.setError("Failed to export jcr file: '" + this.getRelativePath() + "'. Skipping file", e, log);
        }
    }

    public void export(File targetFile, boolean includeMetadata) throws Exception {
        log.debug("Exporting file '" + getRelativePath() + "'...");
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
                log.warn(msg, e);
            }
        }
    }

    protected abstract FileInfo createFileInfo() throws RepositoryException;

    protected abstract StatsInfo createStatsInfo() throws RepositoryException;

    public void exportFileContent(File targetFile) throws Exception {
        //Do not bother exporting if the target file exists with the same modification date
        long modified = getLastModified(getResourceNode(getNode()));
        if (targetFile.exists() && targetFile.lastModified() == modified) {
            if (log.isDebugEnabled()) {
                log.debug("Skipping export of unmodified file '" + targetFile.getAbsolutePath() + "'.");
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
        Node resNode = getResourceNode(getNode());
        Value attachedDataValue = resNode.getProperty(org.apache.jackrabbit.JcrConstants.JCR_DATA).getValue();
        InputStream is = attachedDataValue.getStream();
        return is;
    }

    protected Node getResourceNode(Node node) throws RepositoryException {
        return node.getNode(org.apache.jackrabbit.JcrConstants.JCR_CONTENT);
    }

    public long getLastModified(Node node) throws RepositoryException {
        if (node.hasProperty(JcrConstants.JCR_LASTMODIFIED)) {
            Property prop = node.getProperty(JcrConstants.JCR_LASTMODIFIED);
            return prop.getDate().getTimeInMillis();
        }
        return System.currentTimeMillis();
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

    protected long getSize(Node resourceNode) throws RepositoryException {
        if (resourceNode.hasProperty(org.apache.jackrabbit.JcrConstants.JCR_DATA)) {
            return resourceNode.getProperty(org.apache.jackrabbit.JcrConstants.JCR_DATA).getLength();
        }
        return 0;
    }

    public String getMimeType(Node resourceNode) throws RepositoryException {
        if (resourceNode.hasProperty(JcrConstants.JCR_MIMETYPE)) {
            Property prop = resourceNode.getProperty(JcrConstants.JCR_MIMETYPE);
            return prop.getString();
        }
        return null;
    }
}
