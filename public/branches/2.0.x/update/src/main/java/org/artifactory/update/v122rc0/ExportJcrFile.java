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

import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.update.jcr.BasicExportJcrFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Example of file node from jcr:
 * * ********************************************
 * +-maven-resources-plugin-2.2.pom[@jcr:uuid=9160de4d-2856-4111-8e59-c7a4acaf8746, @jcr:mixinTypes=artifactory:xmlAware, @artifactory:repoKey=repo1-cache, @artifactory:modifiedBy=, @jcr:created=2008-12-01T11:37:28.390+02:00, @artifactory:downloadCount=1, @artifactory:lastUpdated=2008-12-01T11:37:28.421+02:00, @jcr:primaryType=artifactory:file, @artifactory:name=maven-resources-plugin-2.2.pom]
 * |  \-jcr:content[@jcr:uuid=8ca8524c-a677-4a0f-83b5-06029168e8b9, @jcr:data=THE FILE CONTENT, @jcr:encoding=, @jcr:mimeType=application/xml, @jcr:lastModified=2006-05-14T07:19:25.000+03:00, @jcr:primaryType=nt:resource]
 * |  +-artifactory:xml[@jcr:primaryType=artifactory:xmlcontent]
 * |  |  +-project[@jcr:primaryType=nt:unstructured]
 * |  |  |  +-parent[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  +-artifactId[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=maven-plugins, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  +-groupId[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=org.apache.maven.plugins, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  +-version[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=1, @jcr:primaryType=nt:unstructured]
 * |  |  |  +-modelVersion[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=4.0.0, @jcr:primaryType=nt:unstructured]
 * |  |  |  +-artifactId[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=maven-resources-plugin, @jcr:primaryType=nt:unstructured]
 * |  |  |  +-packaging[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=maven-plugin, @jcr:primaryType=nt:unstructured]
 * |  |  |  +-name[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=Maven Resources Plugin, @jcr:primaryType=nt:unstructured]
 * |  |  |  +-version[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=2.2, @jcr:primaryType=nt:unstructured]
 * |  |  |  +-scm[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  +-connection[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=scm:svn:https://svn.apache.org/repos/asf/maven/plugins/tags/maven-resources-plugin-2.2, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  +-developerConnection[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=scm:svn:https://svn.apache.org/repos/asf/maven/plugins/tags/maven-resources-plugin-2.2, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  +-url[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=https://svn.apache.org/repos/asf/maven/plugins/tags/maven-resources-plugin-2.2, @jcr:primaryType=nt:unstructured]
 * |  |  |  +-dependencies[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  +-dependency[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-groupId[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=org.apache.maven, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-artifactId[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=maven-plugin-api, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-version[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=2.0, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  +-dependency[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-groupId[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=org.apache.maven, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-artifactId[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=maven-project, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-version[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=2.0, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  +-dependency[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-groupId[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=org.apache.maven, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-artifactId[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=maven-model, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-version[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=2.0, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  +-dependency[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-groupId[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=org.apache.maven.shared, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-artifactId[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=maven-plugin-testing-harness, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-version[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=1.0-beta-1, @jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  +-scope[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=test, @jcr:primaryType=nt:unstructured]
 * |  |  |  +-distributionManagement[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  +-status[@jcr:primaryType=nt:unstructured]
 * |  |  |  |  |  \-jcr:xmltext[@jcr:xmlcharacters=deployed, @jcr:primaryType=nt:unstructured]
 * ********************************************
 * @author Yoav Landman
 * @author Yossi Shaul
 */
public class ExportJcrFile extends BasicExportJcrFile {
    @SuppressWarnings({"UnusedDeclaration"})
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

    @Override
    protected FileInfo createFileInfo() throws RepositoryException {
        Node jcrNode = getNode();
        FileInfo fileInfo = new FileInfo(new RepoPath(getJcrRepoKey(jcrNode), getRelativePath()));
        Node resourceNode = getResourceNode(jcrNode);
        JcrExporterImpl.fillWithGeneralMetadata(fileInfo, jcrNode);
        fileInfo.setLastUpdated(getLastUpdated(jcrNode));
        fileInfo.setLastModified(getLastModified(resourceNode));
        fileInfo.setSize(getSize(resourceNode));
        fileInfo.setMimeType(getMimeType(resourceNode));
        // set the checksums
        String sha1Value = getSha1(jcrNode);
        ChecksumInfo sha1 = new ChecksumInfo(ChecksumType.sha1, sha1Value, sha1Value);
        String md5Value = getMd5(jcrNode);
        ChecksumInfo md5 = new ChecksumInfo(ChecksumType.md5, md5Value, md5Value);
        fileInfo.setChecksums(new HashSet<ChecksumInfo>(Arrays.asList(sha1, md5)));
        return fileInfo;
    }

    @Override
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

    public String getJcrRepoKey(Node node) throws RepositoryException {
        return node.getProperty(PROP_ARTIFACTORY_REPO_KEY).getString();
    }
}
