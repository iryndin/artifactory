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
package org.artifactory.update.v122rc0;

import org.apache.jackrabbit.JcrConstants;
import static org.apache.jackrabbit.JcrConstants.JCR_DATA;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.update.jcr.BasicExportJcrFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * @author Yoav Landman
 * @author Yossi Shaul
 */
public class ExportJcrFile extends BasicExportJcrFile {
    private static final Logger log = LoggerFactory.getLogger(ExportJcrFile.class);

    //File specific properties
    public static final String NT_ARTIFACTORY_FILE = "artifactory:file";
    public static final String PROP_ARTIFACTORY_DOWNLOAD_COUNT = "artifactory:downloadCount";
    public static final String PROP_ARTIFACTORY_LAST_UPDATED = "artifactory:lastUpdated";
    public static final String PROP_ARTIFACTORY_MD5 = "artifactory:MD5";
    public static final String PROP_ARTIFACTORY_SHA1 = "artifactory:SHA1";//Properties shared by folders and files
    public static final String PROP_ARTIFACTORY_REPO_KEY = "artifactory:repoKey";
    public static final String PROP_ARTIFACTORY_MODIFIED_BY = "artifactory:modifiedBy";

    public ExportJcrFile(Node node, String repoKey) {
        super(node, repoKey);
    }

    protected FileInfo createFileInfo() throws RepositoryException {
        Node jcrNode = getNode();
        FileInfo fileInfo = new FileInfo(new RepoPath(getJcrRepoKey(jcrNode), getRelativePath()));
        Node resourceNode = getResourceNode(jcrNode);
        JcrExporterImpl.fillWithGeneralMetadata(fileInfo, jcrNode);
        fileInfo.setLastUpdated(getLastUpdated(jcrNode));
        fileInfo.setLastModified(getLastModified(resourceNode));
        fileInfo.setSize(getSize(resourceNode));
        fileInfo.setMimeType(getMimeType(resourceNode));
        fileInfo.setSha1(getSha1(jcrNode));
        fileInfo.setMd5(getMd5(jcrNode));
        return fileInfo;
    }

    protected StatsInfo createStatsInfo() throws RepositoryException {
        StatsInfo statsInfo = new StatsInfo();
        statsInfo.setDownloadCount(getDownloadCount(getNode()));
        return statsInfo;
    }

    /**
     * When was a cacheable file last updated
     *
     * @param node
     * @return the last update time as UTC milliseconds
     */
    public long getLastUpdated(Node node) throws RepositoryException {
        if (node.hasProperty(PROP_ARTIFACTORY_LAST_UPDATED)) {
            return node.getProperty(PROP_ARTIFACTORY_LAST_UPDATED).getDate().getTimeInMillis();
        }
        return System.currentTimeMillis();
    }

    public String getMimeType(Node resourceNode) throws RepositoryException {
        // TODO: [by fsi] See if we should use our ContentType enumeration
        // instead of what is saved in the JCR DB
        if (resourceNode.hasProperty(JcrConstants.JCR_MIMETYPE)) {
            Property prop = resourceNode.getProperty(JcrConstants.JCR_MIMETYPE);
            return prop.getString();
        }
        return null;
    }

    public long getDownloadCount(Node node) throws RepositoryException {
        if (node.hasProperty(PROP_ARTIFACTORY_DOWNLOAD_COUNT)) {
            return node.getProperty(PROP_ARTIFACTORY_DOWNLOAD_COUNT).getLong();
        }
        return 0;
    }

    public String getMd5(Node node) throws RepositoryException {
        if (node.hasProperty(PROP_ARTIFACTORY_MD5)) {
            return node.getProperty(PROP_ARTIFACTORY_MD5).getString();
        }
        return null;
    }

    public String getSha1(Node node) throws RepositoryException {
        if (node.hasProperty(PROP_ARTIFACTORY_SHA1)) {
            return node.getProperty(PROP_ARTIFACTORY_SHA1).getString();
        }
        return null;
    }

    public long getSize(Node resourceNode) throws RepositoryException {
        if (resourceNode.hasProperty(JCR_DATA)) {
            return resourceNode.getProperty(JCR_DATA).getLength();
        }
        return 0;
    }

    public String getJcrRepoKey(Node node) throws RepositoryException {
        return node.getProperty(PROP_ARTIFACTORY_REPO_KEY).getString();
    }
}
