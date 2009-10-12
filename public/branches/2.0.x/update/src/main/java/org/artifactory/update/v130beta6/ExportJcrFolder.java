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
package org.artifactory.update.v130beta6;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.FolderAdditionaInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.update.jcr.BasicExportJcrFolder;
import org.artifactory.update.jcr.BasicJcrExporter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.File;

/**
 * Example of folder node from jcr:
 *********************************************
 *+-java.net.m2-cache[@jcr:uuid=7d709a71-f1a9-4575-8dc6-f49bb1b20b9e, @jcr:created=2008-12-03T14:17:18.968+02:00, @jcr:primaryType=artifactory:folder, @artifactory:name=]
 *|  +-artifactory:metadata[@jcr:uuid=0f875a54-48e6-4400-86a4-3f1cb908d1d7, @jcr:mixinTypes=mix:referenceable,mix:lockable, @jcr:primaryType=nt:unstructured]
 *|  |  +-artifactory-folder-ext[@artifactory:lastModifiedMetadata=2008-12-03T14:17:18.968+02:00, @artifactory:MD5=3044249ad38c9b34ee4891a283bfb914, @jcr:primaryType=nt:unstructured, @artifactory:SHA-1=6de5c898b8e9e4633b631b01342ed4e51e041862]
 *|  |  |  +-artifactory:xml[@jcr:primaryType=nt:unstructured]
 *|  |  |  |  +-artifactory-folder-ext[@jcr:primaryType=nt:unstructured]
 *|  |  |  |  |  +-createdBy[@jcr:primaryType=nt:unstructured]
 *|  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=_system_, @jcr:primaryType=nt:unstructured]
 *|  |  |  |  |  +-modifiedBy[@jcr:primaryType=nt:unstructured]
 *|  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=_system_, @jcr:primaryType=nt:unstructured]
 *|  |  |  |  |  +-lastUpdated[@jcr:primaryType=nt:unstructured]
 *|  |  |  |  |  |  \-jcr:xmltext[@jcr:primaryType=nt:unstructured]
 *|  |  |  \-jcr:content[@jcr:uuid=fa775105-e839-4b13-ba34-53584b11ade1, @jcr:data=BINARY, @jcr:encoding=utf-8, @jcr:mimeType=application/xml, @jcr:lastModified=2008-12-03T14:17:18.968+02:00, @jcr:primaryType=nt:resource]
 *********************************************
 *
 * @author freds
 * @date Nov 16, 2008
 */
public class ExportJcrFolder extends BasicExportJcrFolder {
    private final static String ARTIFACTORY_FOLDER_EXT_MD_NODE = "artifactory-folder-ext";

    public ExportJcrFolder(Node node, String repoKey) {
        super(node, repoKey);
    }

    @Override
    protected FolderInfo createFolderInfo() throws RepositoryException {
        // in this version the xml file was saved under a node ARTIFACTORY_FOLDER_EXT_MD_NODE
        // and contains the additional info only. The folder itself didn't have xml content.
        //org.artifactory.jcr.utils.JcrNodeTraversal.preorder(getNode());
        Node folderExtNode = BasicJcrExporter.getMetadataNode(getNode(), ARTIFACTORY_FOLDER_EXT_MD_NODE);
        String xmlContent = BasicJcrExporter.getXmlContent(folderExtNode, null);
        FolderAdditionaInfo folderAdditionalInfo = (FolderAdditionaInfo) getXStream().fromXML(xmlContent);
        FolderInfo folderInfo = new FolderInfo(new RepoPath(getNewRepoKey(), getRelativePath()));
        folderInfo.setAdditionalInfo(folderAdditionalInfo);
        BasicJcrExporter.fillTimestamps(folderInfo, getNode());
        return folderInfo;
    }

    @Override
    protected void createExportJcrFile(File exportDir, StatusHolder status, Node node) throws Exception {
        new ExportJcrFile(node, repoKey).exportTo(exportDir, status);
    }

    @Override
    protected void createExportJcrFolder(File exportDir, StatusHolder status, Node node) {
        new ExportJcrFolder(node, repoKey).exportTo(exportDir, status);
    }
}
