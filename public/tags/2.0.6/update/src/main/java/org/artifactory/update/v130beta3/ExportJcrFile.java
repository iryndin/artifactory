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

import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.update.jcr.BasicExportJcrFile;
import org.artifactory.update.jcr.BasicJcrExporter;
import org.artifactory.update.md.MetadataConverter;
import org.artifactory.update.md.MetadataType;
import org.artifactory.update.md.MetadataVersion;
import org.artifactory.update.md.v130beta3.ArtifactoryFileConverter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.List;

/**
 * Example of file node from jcr: * ******************************************** +-commons-beanutils-1.7.0.pom[@jcr:uuid=fb9809e5-b5bf-4aa1-b7cf-8f367de43533,
 *
 * @author freds
 * @jcr:created=2008-12-03T12:28:58.187+02:00, @jcr:primaryType=artifactory:file, @artifactory:name=commons-beanutils-1.7.0.pom]
 * |  +-artifactory:metadata[@jcr:uuid=c5b54a65-0698-444a-bb24-f349f4edd609, @jcr:mixinTypes=mix:referenceable,mix:lockable,
 * @jcr:primaryType=nt:unstructured] |  |  +-artifactory.file[@artifactory:lastModifiedMetadata=2008-12-03T12:28:58.203+02:00,
 * @jcr:primaryType=nt:unstructured] |  |  |  +-artifactory:xml[@jcr:primaryType=nt:unstructured] |  |  |  |
 * +-artifactory.file[@jcr:primaryType=nt:unstructured] |  |  |  |  |  +-repoKey[@jcr:primaryType=nt:unstructured] |  |
 * |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=ext-releases-local, @jcr:primaryType=nt:unstructured] |  |  |  |  |
 * +-relPath[@jcr:primaryType=nt:unstructured] |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=commons-beanutils/commons-beanutils/1.7.0/commons-beanutils-1.7.0.pom,
 * @jcr:primaryType=nt:unstructured] |  |  |  |  |  +-created[@jcr:primaryType=nt:unstructured] |  |  |  |  |  |
 * \-jcr:xmltext[@jcr:xmlcharacters=0, @jcr:primaryType=nt:unstructured] |  |  |  |  |
 * +-modifiedBy[@jcr:primaryType=nt:unstructured] |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=admin,
 * @jcr:primaryType=nt:unstructured] |  |  |  |  |  +-lastUpdated[@jcr:primaryType=nt:unstructured] |  |  |  |  |  |
 * \-jcr:xmltext[@jcr:xmlcharacters=1228300138187, @jcr:primaryType=nt:unstructured] |  |  |  |  |
 * +-lastModified[@jcr:primaryType=nt:unstructured] |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=1228300138187,
 * @jcr:primaryType=nt:unstructured] |  |  |  |  |  +-size[@jcr:primaryType=nt:unstructured] |  |  |  |  |  |
 * \-jcr:xmltext[@jcr:xmlcharacters=254, @jcr:primaryType=nt:unstructured] |  |  |  |  |
 * +-mimeType[@jcr:primaryType=nt:unstructured] |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=text/xml,
 * @jcr:primaryType=nt:unstructured] |  |  |  |  |  +-sha1[@jcr:primaryType=nt:unstructured] |  |  |  |  |  |
 * \-jcr:xmltext[@jcr:xmlcharacters=2d779fac0b4616addfde19bdfa24cbfff31a6ff5, @jcr:primaryType=nt:unstructured] |  |  |
 * |  |  +-md5[@jcr:primaryType=nt:unstructured] |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=b31a716477d9fe941a056a2743537a42,
 * @jcr:primaryType=nt:unstructured] |  |  |  \-jcr:content[@jcr:uuid=c025eed0-b912-42ac-a952-3201b18f53f6,
 * @jcr:data=<artifactory.file> <repoKey>ext-releases-local</repoKey> <relPath>commons-beanutils/commons-beanutils/1.7.0/commons-beanutils-1.7.0.pom</relPath>
 * <created>0</created> <modifiedBy>admin</modifiedBy> <lastUpdated>1228300138187</lastUpdated>
 * <lastModified>1228300138187</lastModified> <size>254</size> <mimeType>text/xml</mimeType>
 * <sha1>2d779fac0b4616addfde19bdfa24cbfff31a6ff5</sha1> <md5>b31a716477d9fe941a056a2743537a42</md5>
 * </artifactory.file>, @jcr:encoding=utf-8, @jcr:mimeType=text/xml, @jcr:lastModified=2008-12-03T12:28:58.203+02:00,
 * @jcr:primaryType=nt:resource] |  \-jcr:content[@jcr:uuid=102a4d65-ed0d-46e3-985e-ed2a6ab7a256,
 * @jcr:mixinTypes=mix:lockable, @jcr:data=THE FILE CONTENT, @jcr:encoding=utf-8, @jcr:mimeType=text/xml,
 * @jcr:lastModified=2008-12-03T12:28:58.187+02:00, @jcr:primaryType=nt:resource]
 * @date Nov 16, 2008
 */
public class ExportJcrFile extends BasicExportJcrFile {
    private static final String ARTIFACTORY_STATS = "artifactory.stats";

    public ExportJcrFile(Node node, String repoKey) {
        super(node, repoKey);
    }

    @Override
    protected FileInfo createFileInfo() throws RepositoryException {
        Node mdNode = BasicJcrExporter.getMetadataNode(getNode(), ArtifactoryFileConverter.ARTIFACTORY_FILE);
        List<MetadataConverter> converters = MetadataVersion.getConvertersFor(
                MetadataVersion.v130beta3, MetadataType.file);
        String xmlContent = BasicJcrExporter.getXmlContent(mdNode, converters);
        FileInfo fileInfo = (FileInfo) getXStream().fromXML(xmlContent);
        // Beta3 was missing the timestamps
        BasicJcrExporter.fillTimestamps(fileInfo, getResourceNode(getNode()));
        return fileInfo;
    }

    @Override
    protected StatsInfo createStatsInfo() throws RepositoryException {
        StatsInfo statsInfo;
        if (BasicJcrExporter.hasMetadataNode(getNode(), ARTIFACTORY_STATS)) {
            List<MetadataConverter> converters = MetadataVersion.getConvertersFor(
                    MetadataVersion.v130beta3, MetadataType.stats);
            Node mdNode = BasicJcrExporter.getMetadataNode(getNode(), ARTIFACTORY_STATS);
            String xmlContent = BasicJcrExporter.getXmlContent(mdNode, converters);
            statsInfo = (StatsInfo) getXStream().fromXML(xmlContent);
        } else {
            statsInfo = new StatsInfo(0);
        }
        return statsInfo;
    }
}
