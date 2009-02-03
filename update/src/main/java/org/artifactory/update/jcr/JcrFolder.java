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
import org.apache.log4j.Logger;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.FolderInfo;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * @author Yoav Landman
 * @author Yossi Shaul
 */
public class JcrFolder extends JcrFsItem {
    private final static Logger LOGGER = Logger.getLogger(JcrFolder.class);

    //Folder specific properties
    public static final String NT_ARTIFACTORY_FOLDER = "artifactory:folder";
    public static final String NEXUS_INDEX_DIR = ".index";

    public JcrFolder(Node node, String repoKey) {
        super(node, repoKey);
    }

    /**
     * @param exportDir The root directory for the exported repository
     * @param status    The status holder
     */
    public void exportTo(File exportDir, StatusHolder status) {
        try {
            if (getNode().getName().equals(NEXUS_INDEX_DIR)) {
                // no need to export .index directories introduced in 1.3.0-beta-2
                return;
            }
            exportContent(exportDir);
            exportMetadata(exportDir, true);

            //Export child nodes
            NodeIterator nodeIterator = getNode().getNodes();
            while (nodeIterator.hasNext()) {
                Node node = nodeIterator.nextNode();
                String typeName = node.getPrimaryNodeType().getName();
                if (typeName.equals(JcrFolder.NT_ARTIFACTORY_FOLDER)) {
                    new JcrFolder(node, repoKey).exportTo(exportDir, status);
                } else if (typeName.equals(JcrFile.NT_ARTIFACTORY_FILE)) {
                    new JcrFile(node, repoKey).exportTo(exportDir, status);
                }
            }
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to export jcr folder: %s. Skipping folder.",
                    e.getMessage()));
            LOGGER.debug("Stack trace: ", e);
        }
    }

    private void exportContent(File exportDir) throws RepositoryException {
        File folderDir = getFolderDirectory(exportDir);
        folderDir.mkdirs();
    }

    private File getFolderDirectory(File exportDir) throws RepositoryException {
        File folderDir = new File(exportDir, getRelativePath());
        return folderDir;
    }

    private void exportMetadata(File exportDir, boolean abortOnError)
            throws FileNotFoundException, RepositoryException {
        try {
            FolderInfo folderInfo = createFolderInfo();

            writeToFile(exportDir, folderInfo);
        } catch (Exception e) {
            String msg = "Failed to export metadata of '" + getRelativePath() + "'.";
            if (abortOnError) {
                throw new RuntimeException(msg, e);
            } else {
                LOGGER.warn(msg, e);
            }
        }
    }

    private void writeToFile(File exportDir, FolderInfo folderInfo)
            throws RepositoryException, FileNotFoundException {
        File folderDirectory = getFolderDirectory(exportDir);
        File parentFolder = folderDirectory.getParentFile();
        File metadataFolder =
                new File(parentFolder, folderDirectory.getName() + FolderInfo.METADATA_FOLDER);
        metadataFolder.mkdir();
        File metadataFile = new File(metadataFolder, FolderInfo.ROOT + ".xml");
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(metadataFile);
            XStream xstream = getXStream();
            xstream.toXML(folderInfo, os);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    private FolderInfo createFolderInfo() throws RepositoryException {
        FolderInfo folderInfo = new FolderInfo();
        fillWithGeneralMetadata(folderInfo);
        return folderInfo;
    }

    public boolean isDirectory() {
        return true;
    }
}
