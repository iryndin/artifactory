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

import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.fs.FileAdditionalInfo;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.update.jcr.BasicExportJcrFile;
import org.artifactory.update.jcr.BasicJcrExporter;
import org.artifactory.update.md.MetadataConverter;
import org.artifactory.update.md.MetadataType;
import org.artifactory.update.md.MetadataVersion;
import org.artifactory.update.v130beta3.JcrExporterImpl;
import org.artifactory.version.ConverterUtils;
import org.jdom.Document;
import org.jdom.Element;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Example of file node from jcr (note: some files might not have the stats node):
 * ********************************************
 * +-maven-plugin-api-2.0.6.pom[@artifactory:uncommitted=true, @jcr:uuid=119e237b-a78c-4900-9d8b-cd644af3b8a2, @jcr:created=2008-12-03T14:23:46.515+02:00, @jcr:primaryType=artifactory:file, @artifactory:name=maven-plugin-api-2.0.6.pom]
 * |  +-artifactory:metadata[@jcr:uuid=1063c605-5650-4b56-987b-85bbbfe8a466, @jcr:mixinTypes=mix:referenceable,mix:lockable, @jcr:primaryType=nt:unstructured]
 * |  |  +-artifactory.stats[@artifactory:lastModifiedMetadata=2008-12-03T14:23:46.531+02:00, @artifactory:MD5=fdc4b91882a366b7550e2ca509bcefd7, @jcr:primaryType=nt:unstructured, @artifactory:SHA-1=33edd480324b67760b521e7ed5358a3c912b70b3]
 * |  |  |  +-artifactory:xml[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  +-artifactory.stats[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-downloadCount[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=1, @jcr:primaryType=nt:unstructured]
 * |  |  |  \-jcr:content[@jcr:uuid=8f56a428-9d5b-4b0e-8c65-d3489f0f68d4, @jcr:data=BINARY, @jcr:encoding=utf-8, @jcr:mimeType=application/xml, @jcr:lastModified=2008-12-03T14:23:46.531+02:00, @jcr:primaryType=nt:resource]
 * |  |  +-artifactory-file-ext[@artifactory:lastModifiedMetadata=2008-12-03T14:23:46.531+02:00, @artifactory:MD5=1248fe9eef261bc3e3c36c740b8a197c, @jcr:primaryType=nt:unstructured, @artifactory:SHA-1=93a174bb6623fb7db4091ff460068efbd38dcb2d]
 * |  |  |  +-artifactory:xml[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  +-artifactory-file-ext[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-createdBy[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=anonymous, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-modifiedBy[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=anonymous, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-lastUpdated[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=1228036497484, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-sha1[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=3af72b052dfefb73ecfae742613012b5396c8863, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-md5[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=5d1db4c23baa9cf42ad8f1799a436b37, @jcr:primaryType=nt:unstructured]
 * |  |  |  \-jcr:content[@jcr:uuid=9d00ab16-db56-4e12-86c2-26e0d2681e9f, @jcr:data=BINARY, @jcr:encoding=utf-8, @jcr:mimeType=application/xml, @jcr:lastModified=2008-12-03T14:23:46.531+02:00, @jcr:primaryType=nt:resource]
 * |  \-jcr:content[@jcr:uuid=40bdc969-716b-42b4-8145-6e8427bf5c53, @jcr:mixinTypes=mix:lockable, @jcr:data=BINARY, @jcr:mimeType=application/x-maven-pom+xml, @jcr:lastModified=2007-04-01T07:54:08.000+03:00, @jcr:primaryType=nt:resource]
 * ********************************************

 * @author freds
 * @date Nov 16, 2008
 */
public class ExportJcrFile extends BasicExportJcrFile {
    /** Names of the jcr nodes containing the metadata of artifactory file and stats
     * as were in the versions supported by this exporter */
    private final String ARTIFACTORY_FILE_MD_NODE = "artifactory.file";
    private final String ARTIFACTORY_STATS_MD_NODE = "artifactory.stats";

    public ExportJcrFile(Node node, String repoKey) {
        super(node, repoKey);
    }

    @Override
    protected FileInfo createFileInfo() throws RepositoryException {
        // in this version we only have the extra info xml content, so we cannot use it and we need
        // to read values directly from the jcr nodes
        FileInfo fileInfo = new FileInfo(new RepoPath(getNewRepoKey(), getRelativePath()));
        Node fileRootNode = getNode();
        // get the node with the content of the file
        Node contentNode = getResourceNode(fileRootNode);
        // extract timestam data from it
        BasicJcrExporter.fillTimestamps(fileInfo, fileRootNode);
        fileInfo.setLastModified(getLastModified(contentNode));
        fileInfo.setSize(getSize(contentNode));
        fileInfo.setMimeType(getMimeType(contentNode));
        FileAdditionalInfo additionalInfo = createAdditionalInfo();
        fileInfo.setAdditionalInfo(additionalInfo);
        return fileInfo;
    }

    private FileAdditionalInfo createAdditionalInfo() throws RepositoryException {
        FileAdditionalInfo additionalInfo = new FileAdditionalInfo();
        Node extraInfoNode = BasicJcrExporter.getMetadataNode(getNode(), "artifactory-file-ext");
        Document doc = ConverterUtils.parse(BasicJcrExporter.getRawXmlStream(extraInfoNode));
        Element root = doc.getRootElement();
        additionalInfo.setLastUpdated(getLastUpdated(root));
        additionalInfo.setCreatedBy(root.getChildText("createdBy"));
        additionalInfo.setModifiedBy(root.getChildText("modifiedBy"));
        String sha1 = root.getChildText("sha1");
        ChecksumInfo sha1Info = new ChecksumInfo(ChecksumType.sha1, sha1, sha1);
        String md5 = root.getChildText("md5");
        ChecksumInfo md5Info = new ChecksumInfo(ChecksumType.md5, md5, md5);
        additionalInfo.setChecksums(new HashSet<ChecksumInfo>(Arrays.asList(sha1Info, md5Info)));
        return additionalInfo;
    }

    private long getLastUpdated(Element parent) throws RepositoryException {
        String lastUpdated = parent.getChildText("lastUpdated");
        try {
            return Long.parseLong(lastUpdated);
        } catch (NumberFormatException e) {
            return System.currentTimeMillis();
        }
    }

    @Override
    protected StatsInfo createStatsInfo() throws RepositoryException {
        Node mdNode = BasicJcrExporter.getMetadataContainer(getNode());
        StatsInfo statsInfo;
        // in 1.3.0 beta 6.1 the stats node is created only on first download
        if (mdNode.hasNode(ARTIFACTORY_STATS_MD_NODE)) {
            Node statsNode = mdNode.getNode(ARTIFACTORY_STATS_MD_NODE);
            List<MetadataConverter> converters = MetadataVersion.getConvertersFor(
                    MetadataVersion.v130beta6, MetadataType.stats);
            String xmlContent = JcrExporterImpl.getXmlContent(statsNode, converters);
            statsInfo = (StatsInfo) getXStream().fromXML(xmlContent);
        } else {
            statsInfo = new StatsInfo(0);
        }
        return statsInfo;
    }
}