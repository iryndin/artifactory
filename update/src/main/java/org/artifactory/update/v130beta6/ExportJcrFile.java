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

import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.update.jcr.BasicExportJcrFile;
import org.artifactory.update.jcr.BasicJcrExporter;
import org.artifactory.update.v130beta3.JcrExporterImpl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * @author freds
 * @date Nov 16, 2008
 */
public class ExportJcrFile extends BasicExportJcrFile {
    public ExportJcrFile(Node node, String repoKey) {
        super(node, repoKey);
    }

    protected FileInfo createFileInfo() throws RepositoryException {
        Node jcrNode = getNode();
        FileInfo fileInfo = new FileInfo(new RepoPath(getNewRepoKey(), getRelativePath()));
        Node resourceNode = getResourceNode(jcrNode);
        BasicJcrExporter.fillTimestamps(fileInfo, jcrNode);
        fileInfo.setLastModified(getLastModified(resourceNode));
        // TODO: read jcr entries and extra xml
        //fileInfo.setSize(getSize(resourceNode));
        //fileInfo.setMimeType(getMimeType(resourceNode));
        return fileInfo;
    }

    protected StatsInfo createStatsInfo() throws RepositoryException {
        Node mdNode = BasicJcrExporter.getMetadataNode(getNode(), StatsInfo.ROOT);
        String xmlContent = JcrExporterImpl.getXmlContent(mdNode, null);
        StatsInfo statsInfo = (StatsInfo) getXStream().fromXML(xmlContent);
        return statsInfo;
    }
}