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
package org.artifactory.update.v130beta3;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.update.jcr.BasicExportJcrFolder;
import org.artifactory.update.jcr.BasicJcrExporter;
import org.artifactory.update.md.v130beta3.ArtifactoryFolderConverter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.File;

/**
 * @author freds
 * @date Nov 16, 2008
 */
public class ExportJcrFolder extends BasicExportJcrFolder {

    public ExportJcrFolder(Node node, String repoKey) {
        super(node, repoKey);
    }

    protected FolderInfo createFolderInfo() throws RepositoryException {
        ArtifactoryFolderConverter converter = new ArtifactoryFolderConverter();
        Node mdNode = JcrExporterImpl.getMetadataNode(getNode(), ArtifactoryFolderConverter.OLD_METADATA_NAME);
        String xmlContent = JcrExporterImpl.getXmlContent(mdNode, converter);
        FolderInfo folderInfo = (FolderInfo) getXStream().fromXML(xmlContent);
        // Beta3 was missing the timestamps
        BasicJcrExporter.fillTimestamps(folderInfo, getNode());
        return folderInfo;
    }

    protected void createExportJcrFile(File exportDir, StatusHolder status, Node node) throws Exception {
        new ExportJcrFile(node, repoKey).exportTo(exportDir, status);
    }

    protected void createExportJcrFolder(File exportDir, StatusHolder status, Node node) {
        new ExportJcrFolder(node, repoKey).exportTo(exportDir, status);
    }

}