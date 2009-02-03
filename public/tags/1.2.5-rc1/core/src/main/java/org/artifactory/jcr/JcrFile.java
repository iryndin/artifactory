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
package org.artifactory.jcr;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.Annotations;
import org.apache.commons.io.IOUtils;
import static org.apache.jackrabbit.JcrConstants.*;
import org.apache.jackrabbit.value.DateValue;
import org.apache.jackrabbit.value.LongValue;
import org.apache.log4j.Logger;
import org.artifactory.fs.FileMetadata;
import org.artifactory.fs.FsItemMetadata;
import static org.artifactory.jcr.ArtifactoryJcrConstants.*;
import org.artifactory.process.StatusHolder;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrFile extends JcrFsItem {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrFile.class);

    public JcrFile(Node fileNode) {
        super(fileNode);
        //Sanity check
        try {
            if (!JcrHelper.isFileNode(fileNode)) {
                throw new RuntimeException("Node is not a file node.");
            }
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to create Jcr File.", e);
        }
    }

    /**
     * When was a cacheable file last updated
     *
     * @return
     * @throws RepositoryException
     */
    public Date lastUpdated() {
        try {
            return getPropValue(PROP_ARTIFACTORY_LAST_UPDATED).getDate().getTime();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve node file's last updated.", e);
        }
    }

    public Date lastModified() {
        Node resourceNode = getResourceNode();
        try {
            Property prop = resourceNode.getProperty(JCR_LASTMODIFIED);
            return prop.getDate().getTime();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve file node's last modified.", e);
        }
    }

    public long lastModifiedTime() {
        Date date = lastModified();
        return date != null ? date.getTime() : 0;
    }

    //TODO: [by yl] Include mimeType as part of RepoResource where jar, md5, sha1 will also be
    //accounted for. Get rid of org.artifactory.runtime.utils.MimeTypes
    public String mimeType() {
        Node resourceNode = getResourceNode();
        try {
            Property prop = resourceNode.getProperty(JCR_MIMETYPE);
            return prop.getString();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve file node's myme type.", e);
        }
    }

    public long downloadCount() {
        try {
            return getPropValue(PROP_ARTIFACTORY_DOWNLOAD_COUNT).getLong();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve node file's download count.", e);
        }
    }

    /**
     * Get a resource stream and increment the download counter
     *
     * @return
     */
    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public InputStream getStreamForDownload() {
        InputStream is = getStream();
        //Update the download count
        LongValue value = new LongValue(downloadCount() + 1);
        setPropValue(PROP_ARTIFACTORY_DOWNLOAD_COUNT, value);
        return is;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public InputStream getStream() {
        Node resNode = getResourceNode();
        try {
            Value attachedDataValue = resNode.getProperty(JCR_DATA).getValue();
            InputStream is = attachedDataValue.getStream();
            return is;
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve file node's data stream.", e);
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public long size() {
        Node resNode = getResourceNode();
        try {
            long size = resNode.getProperty(JCR_DATA).getLength();
            return size;
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve file node's size.", e);
        }
    }

    public void xmlStream(OutputStream out) {
        if (!isXmlAware()) {
            throw new RuntimeException("No xml to export.");
        }
        try {
            Node xmlNode = node.getNode(ARTIFACTORY_XML);
            Session session = xmlNode.getSession();
            String absPath = xmlNode.getPath();
            session.exportDocumentView(absPath, out, false, false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve file node's xml stream.", e);
        }
    }

    public boolean isXmlAware() {
        NodeType[] mixins;
        try {
            mixins = node.getMixinNodeTypes();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to get node mixins", e);
        }
        for (NodeType mixin : mixins) {
            if (mixin.getName().equals(MIX_ARTIFACTORY_XML_AWARE)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFolder() {
        return false;
    }

    public void export(File targetFile) {
        export(targetFile, false);
    }

    public void export(File targetFile, boolean includeMetadata) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Exporting file '" + relPath() + "'...");
        }
        OutputStream os = null;
        InputStream is = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(targetFile));
            is = getStream();
            IOUtils.copy(is, os);
            long modified = lastModifiedTime();
            targetFile.setLastModified(modified);
            if (includeMetadata) {
                FileMetadata metadata = getMetadata();
                File parentFile = targetFile.getParentFile();
                File metadataFile =
                        new File(parentFile, targetFile.getName() + FsItemMetadata.SUFFIX);
                IOUtils.closeQuietly(os);
                os = new BufferedOutputStream(new FileOutputStream(metadataFile));
                XStream xstream = new XStream();
                Annotations.configureAliases(xstream, FileMetadata.class);
                xstream.toXML(metadata, os);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to export to file '" +
                    targetFile.getPath() + "'.", e);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }

    public void exportTo(String basePath, StatusHolder status) {
        File targetFile = new File(basePath, relPath());
        export(targetFile, true);
    }

    public void importFrom(String basePath, StatusHolder status) {
        InputStream is = null;
        try {
            //Read metadata into the node
            File file = new File(basePath, relPath());
            File parentFile = file.getParentFile();
            File metadataFile = new File(parentFile, file.getName() + FsItemMetadata.SUFFIX);
            if (metadataFile.exists()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Importing metadata from '" + metadataFile.getPath() + "'.");
                }
                IOUtils.closeQuietly(is);
                is = new BufferedInputStream(new FileInputStream(metadataFile));
                XStream xstream = new XStream();
                Annotations.configureAliases(xstream, FileMetadata.class);
                FileMetadata metadata = (FileMetadata) xstream.fromXML(is);
                node.setProperty(PROP_ARTIFACTORY_NAME, file.getName());
                if (!node.hasProperty(PROP_ARTIFACTORY_REPO_KEY)) {
                    //Do not override the repo key (when importing to a repo with a different key)
                    node.setProperty(PROP_ARTIFACTORY_REPO_KEY, metadata.getRepoKey());
                }
                Calendar lastUpdated = Calendar.getInstance();
                node.setProperty(PROP_ARTIFACTORY_LAST_UPDATED, lastUpdated);
                node.setProperty(PROP_ARTIFACTORY_MODIFIED_BY, metadata.getModifiedBy());
                if (metadata.isXmlAware() && !node.isNodeType(MIX_ARTIFACTORY_XML_AWARE)) {
                    node.addMixin(MIX_ARTIFACTORY_XML_AWARE);
                }
                Node resNode = getResourceNode();
                resNode.setProperty(JCR_MIMETYPE, metadata.getMimeType());
                resNode.setProperty(JCR_ENCODING, "");
                Calendar lastModified = Calendar.getInstance();
                lastModified.setTimeInMillis(metadata.getLastModified());
                resNode.setProperty(JCR_LASTMODIFIED, lastModified);
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("No metadata found for '" + file.getPath() + "'.");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to file import into '" + relPath() + "'.", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public void setLastUpdatedTime(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        DateValue value = new DateValue(calendar);
        setPropValue(PROP_ARTIFACTORY_LAST_UPDATED, value);
    }

    public FileMetadata getMetadata() {
        return new FileMetadata(repoKey(), relPath(), modifiedBy(),
                lastUpdated() != null ? lastUpdated().getTime() : -1,
                lastModified() != null ? lastModified().getTime() : -1,
                downloadCount(), mimeType(), isXmlAware());
    }

    protected Value getPropValue(String prop) {
        try {
            return node.getProperty(prop).getValue();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve value for property '" + prop + "'.", e);
        }
    }

    protected void setPropValue(String prop, Value value) {
        try {
            node.setProperty(prop, value);
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to set value for property '" + prop + "'.", e);
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private Node getResourceNode() {
        try {
            Node resNode = node.getNode(JCR_CONTENT);
            return resNode;
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to get resource node.", e);
        }
    }
}
