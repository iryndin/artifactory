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
import org.artifactory.update.md.MetadataConverter;
import org.artifactory.update.md.MetadataType;
import org.artifactory.update.md.MetadataVersion;
import org.artifactory.update.md.v130beta3.ArtifactoryFolderConverter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.File;
import java.util.List;

/**
 * Example of folder node from jcr: * |  +-.index[@jcr:uuid=6ce7f0c2-9b17-4cd5-85ad-a3a64db263b0,
 *
 * @author freds
 * @jcr:created=2008-12-01T10:00:00.656+02:00, @jcr:primaryType=artifactory:folder, @artifactory:name=.index] |  |
 * +-artifactory:metadata[@jcr:uuid=e0f864bf-e5f9-42da-b4b8-984ed9d999ea, @jcr:mixinTypes=mix:referenceable,mix:lockable,
 * @jcr:primaryType=nt:unstructured] |  |  |  +-artifactory.folder[@artifactory:lastModifiedMetadata=2008-12-01T10:00:00.656+02:00,
 * @jcr:primaryType=nt:unstructured] |  |  |  |  +-artifactory:xml[@jcr:primaryType=nt:unstructured] |  |  |  |  |
 * +-artifactory.folder[@jcr:primaryType=nt:unstructured] |  |  |  |  |  |  +-repoKey[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=plugins-snapshots-local, @jcr:primaryType=nt:unstructured] |  |
 * |  |  |  |  +-relPath[@jcr:primaryType=nt:unstructured] |  |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=.index,
 * @jcr:primaryType=nt:unstructured] |  |  |  |  |  |  +-created[@jcr:primaryType=nt:unstructured] |  |  |  |  |  |  |
 * \-jcr:xmltext[@jcr:xmlcharacters=0, @jcr:primaryType=nt:unstructured] |  |  |  |  |  |
 * +-modifiedBy[@jcr:primaryType=nt:unstructured] |  |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=_system_,
 * @jcr:primaryType=nt:unstructured] |  |  |  |  \-jcr:content[@jcr:uuid=abea04a8-2cd2-45ec-b3c7-b37b16644a9c,
 * @jcr:data=<artifactory.folder> <repoKey>plugins-snapshots-local</repoKey> <relPath>.index</relPath>
 * <created>0</created> <modifiedBy>_system_</modifiedBy> </artifactory.folder>, @jcr:encoding=utf-8,
 * @jcr:mimeType=text/xml, @jcr:lastModified=2008-12-01T10:00:00.656+02:00, @jcr:primaryType=nt:resource]
 * @date Nov 16, 2008
 */
public class ExportJcrFolder extends BasicExportJcrFolder {

    public ExportJcrFolder(Node node, String repoKey) {
        super(node, repoKey);
    }

    @Override
    protected FolderInfo createFolderInfo() throws RepositoryException {
        Node mdNode = JcrExporterImpl.getMetadataNode(getNode(), ArtifactoryFolderConverter.ARTIFACTORY_FOLDER);
        List<MetadataConverter> converters = MetadataVersion.getConvertersFor(
                MetadataVersion.v130beta3, MetadataType.folder);
        String xmlContent = JcrExporterImpl.getXmlContent(mdNode, converters);
        FolderInfo folderInfo = (FolderInfo) getXStream().fromXML(xmlContent);
        // Beta3 was missing the timestamps
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