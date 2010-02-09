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
package org.artifactory.jcr.fs;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.IOUtils;
import static org.apache.jackrabbit.JcrConstants.*;
import org.apache.jackrabbit.value.DateValue;
import org.apache.jackrabbit.value.LongValue;
import org.apache.jackrabbit.value.StringValue;
import org.apache.log4j.Logger;
import org.artifactory.config.xml.ArtifactoryXmlFactory;
import org.artifactory.config.xml.EntityResolvingContentHandler;
import org.artifactory.fs.FileMetadata;
import org.artifactory.fs.FsItemMetadata;
import org.artifactory.io.NonClosingInputStream;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.ChecksumInputStream;
import org.artifactory.io.checksum.ChecksumType;
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.jcr.JcrWrapper;
import org.artifactory.jcr.NodeLock;
import org.artifactory.maven.MavenUtils;
import org.artifactory.process.StatusHolder;
import org.artifactory.spring.ContextHelper;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import sun.net.www.MimeTable;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.Calendar;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrFile extends JcrFsItem {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrFile.class);
    public static final String NT_ARTIFACTORY_FILE = "artifactory:file";
    public static final String NT_ARTIFACTORY_JAR = "artifactory:archive";
    public static final String NT_ARTIFACTORY_XML_CONTENT = "artifactory:xmlContent";
    public static final String NODE_ARTIFACTORY_XML = "artifactory:xml";
    public static final String MIX_ARTIFACTORY_XML_AWARE = "artifactory:xmlAware";
    public static final String PROP_ARTIFACTORY_ARCHIVE_ENTRY = "artifactory:archiveEntry";
    public static final String PROP_ARTIFACTORY_DOWNLOAD_COUNT = "artifactory:downloadCount";
    public static final String PROP_ARTIFACTORY_LAST_UPDATED = "artifactory:lastUpdated";
    public static final String PROP_ARTIFACTORY_MD5 = "artifactory:" + ChecksumType.md5.alg();
    public static final String PROP_ARTIFACTORY_SHA1 = "artifactory:" + ChecksumType.sha1.alg();
    private static XStream xstream;

    public static JcrFile create(
            JcrFolder parentFolder, String name, long lastModified, InputStream in)
            throws RepositoryException, IOException {
        String targetPath = parentFolder.getPath() + "/" + name;
        //Create a temp folder node and add the file there
        Node parentNode = parentFolder.getNode();
        Session session = parentNode.getSession();
        Node rootNode = session.getRootNode();
        Node tempParentNode =
                rootNode.addNode(name + "_" + System.currentTimeMillis(), "nt:folder");
        JcrFile file = new JcrFile(tempParentNode, targetPath, name, lastModified, in);
        //Lock both the parent and the grandparent nodes in order to avoid a situation of
        //pending changes on the parent node (we locked a child folder first in the same
        //transaction). For example, the version metadata.xml is deployed (by the deployer)
        //after the artifact metadata and the after the artifacts. We cannot unlock the children
        //(required for locking the parent - no pending changes on children) without saving them
        //first which will compromise the 'atomicity' of the transaction.
        //Do not go up with locking more than 2 levels in order not to fail on pending changes
        //(grandparent node was already locked so you cannot lock the grand-grandparent).
        //We assume we are not importing files under the root node (or getParent() will fail).
        //-
        //For an artifact deploy this locks the artifactId and last group suffix folders.
        //We need to be watching http://issues.apache.org/jira/browse/JCR-314, since this is a
        //pretty coarse locking scope.
        //See also the discussion at: http://www.nabble.com/Jackrabbit-Performance-Tuning---Large-%22Transaction%22---Concurrent-Access-to-Repository-t3095196.html)
        Node grandParentNode = parentNode.getParent();
        if (!NodeLock.isLockedByCurrentSession(parentNode)) {
            NodeLock.lock(grandParentNode);
            NodeLock.lock(parentNode);
        }
        //Remove any existing node with the same file name
        if (parentNode.hasNode(name)) {
            Node oldFileNode = parentNode.getNode(name);
            oldFileNode.remove();
        }
        //Move the imported file from the temp parent to the real parent node
        file.renameTo(targetPath);
        //Delete the temp parent node
        tempParentNode.remove();
        return file;
    }


    public static JcrFile create(JcrFolder parent, String name, InputStream in)
            throws IOException {
        Node parentNode = parent.getNode();
        return new JcrFile(parentNode, null, name, System.currentTimeMillis(), in);
    }

    public JcrFile(Node fileNode) {
        super(fileNode);
        //Sanity check
        try {
            if (!isFileNode(fileNode)) {
                throw new RuntimeException("Node is not a file node.");
            }
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to create Jcr File.", e);
        }
    }

    public JcrFile(String absPath) {
        super(absPath);
    }

    public JcrFile(String parent, String child) {
        super(parent, child);
    }

    public JcrFile(File parent, String child) {
        super(parent, child);
    }

    protected JcrFile(Node parentNode, String targetAbsPath, String name, long lastModified,
            InputStream in) throws IOException {
        super(parentNode, targetAbsPath, name);
        if (targetAbsPath == null) {
            targetAbsPath = getAbsolutePath();
        }
        setLastUpdated(System.currentTimeMillis());
        setDownloadCount(0);
        try {
            setResourceNode(in, lastModified);
        } catch (RepositoryException e) {
            throw new RuntimeException(
                    "Failed to create file node resource at '" + targetAbsPath + "'.", e);
        }
    }

    /**
     * When was a cacheable file last updated
     *
     * @return the last update time as UTC milliseconds
     */
    public long getLastUpdated() {
        try {
            return getPropValue(PROP_ARTIFACTORY_LAST_UPDATED).getDate().getTimeInMillis();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve node file's last updated time.", e);
        }
    }

    public long getLastModified() {
        Node resourceNode = getResourceNode();
        try {
            Property prop = resourceNode.getProperty(JCR_LASTMODIFIED);
            return prop.getDate().getTimeInMillis();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve file node's last modified time.", e);
        }
    }

    public String getMimeType() {
        Node resourceNode = getResourceNode();
        try {
            Property prop = resourceNode.getProperty(JCR_MIMETYPE);
            return prop.getString();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve file node's myme type.", e);
        }
    }

    public long getDownloadCount() {
        try {
            return getPropValue(PROP_ARTIFACTORY_DOWNLOAD_COUNT).getLong();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve node file's download count.", e);
        }
    }

    public void setLastUpdated(long lastUpdated) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(lastUpdated);
        DateValue value = new DateValue(calendar);
        setPropValue(PROP_ARTIFACTORY_LAST_UPDATED, value);
    }

    public String getChecksum(ChecksumType checksumType) {
        try {
            return getPropValue("artifactory:" + checksumType.alg()).getString();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve modified-by.", e);
        }
    }

    public void setDownloadCount(long downloadCount) {
        LongValue value = new LongValue(downloadCount);
        setPropValue(PROP_ARTIFACTORY_DOWNLOAD_COUNT, value);
    }

    public void setXmlAware(boolean xmlAware) {
        try {
            Node node = getNode();
            if (xmlAware && !isXmlAware()) {
                node.addMixin(MIX_ARTIFACTORY_XML_AWARE);
            } else if (isXmlAware()) {
                node.removeMixin(MIX_ARTIFACTORY_XML_AWARE);
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(
                    "Failed to " + (xmlAware ? "set" : "unset") + " the xmlAware property.", e);
        }
    }

    /**
     * Get a resource stream and increment the download counter
     *
     * @return the resource stream
     */
    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public InputStream getStreamForDownload() {
        InputStream is = getStream();
        //Update the download count
        LongValue value = new LongValue(getDownloadCount() + 1);
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
    public long getSize() {
        if (!exists()) {
            return 0;
        }
        Node resNode = getResourceNode();
        try {
            long size = resNode.getProperty(JCR_DATA).getLength();
            return size;
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve file node's size.", e);
        }
    }

    public void getXmlStream(OutputStream out) {
        if (!isXmlAware()) {
            throw new RuntimeException("No xml to export.");
        }
        try {
            Node xmlNode = getNode().getNode(NODE_ARTIFACTORY_XML);
            Session session = xmlNode.getSession();
            String absPath = xmlNode.getPath();
            session.exportDocumentView(absPath, out, false, false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve file node's xml stream.", e);
        }
    }

    public boolean isXmlAware() {
        try {
            return getNode().isNodeType(MIX_ARTIFACTORY_XML_AWARE);
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to get node mixins", e);
        }
    }

    public boolean isDirectory() {
        return false;
    }

    public void export(File targetFile) {
        export(targetFile, false);
    }

    public void export(File targetFile, boolean includeMetadata) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Exporting file '" + getRelativePath() + "'...");
        }
        OutputStream os = null;
        InputStream is = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(targetFile));
            is = getStream();
            IOUtils.copy(is, os);
            long modified = getLastModified();
            if (modified >= 0) {
                targetFile.setLastModified(modified);
            }
            if (includeMetadata) {
                FileMetadata metadata = getMetadata();
                File parentFile = targetFile.getParentFile();
                File metadataFile =
                        new File(parentFile, targetFile.getName() + FsItemMetadata.SUFFIX);
                IOUtils.closeQuietly(os);
                os = new BufferedOutputStream(new FileOutputStream(metadataFile));
                XStream xstream = getXStream();
                xstream.toXML(metadata, os);
                if (modified >= 0) {
                    metadataFile.setLastModified(modified);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to export to file '" +
                    targetFile.getPath() + "'.", e);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }

    private synchronized static XStream getXStream() {
        if (xstream == null) {
            xstream = new XStream();
            xstream.processAnnotations(FileMetadata.class);
        }
        return xstream;
    }

    public void exportTo(File exportDir, StatusHolder status) {
        File targetFile = new File(exportDir, getRelativePath());
        export(targetFile, true);
    }

    public void importFrom(String basePath, StatusHolder status) {
        InputStream is = null;
        try {
            //Read metadata into the node
            File file = new File(basePath, getRelativePath());
            File parentFile = file.getParentFile();
            File metadataFile = new File(parentFile, file.getName() + FsItemMetadata.SUFFIX);
            if (metadataFile.exists()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Importing metadata from '" + metadataFile.getPath() + "'.");
                }
                IOUtils.closeQuietly(is);
                is = new BufferedInputStream(new FileInputStream(metadataFile));
                XStream xstream = getXStream();
                FileMetadata metadata = (FileMetadata) xstream.fromXML(is);
                String name = metadata.getArtifactoryName();
                setArtifactoryName(name != null ? name : file.getName());
                if (!getNode().hasProperty(PROP_ARTIFACTORY_REPO_KEY)) {
                    //Do not override the repo key (when importing to a repo with a different key)
                    setRepoKey(metadata.getRepoKey());
                }
                setLastUpdated(System.currentTimeMillis());
                setModifiedBy(metadata.getModifiedBy());
                setXmlAware(metadata.isXmlAware());
                Node resNode = getResourceNode();
                resNode.setProperty(JCR_ENCODING, "utf-8");
                Calendar lastModified = Calendar.getInstance();
                resNode.setProperty(JCR_LASTMODIFIED, lastModified);
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("No metadata found for '" + file.getPath() + "'.");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to file import into '" + getRelativePath() + "'.",
                    e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public FileMetadata getMetadata() {
        Node node = getNode();
        long downloadCount = 0;
        try {
            if (node.hasProperty(PROP_ARTIFACTORY_DOWNLOAD_COUNT)) {
                downloadCount = getDownloadCount();
            }
        } catch (RepositoryException ignored) {
        }
        return new FileMetadata(
                getRepoKey(),
                getRelativePath(),
                getArtifactoryName(),
                getCreated(),
                getModifiedBy(),
                getLastUpdated(),
                getLastModified(),
                downloadCount,
                isXmlAware());
    }

    /**
     * OVERIDDEN FROM FILE BEGIN
     */

    @Override
    public long lastModified() {
        if (!exists()) {
            return 0;
        }
        return getLastModified();
    }

    @Override
    public long length() {
        return getSize();
    }

    @Override
    public String[] list() {
        return null;
    }

    @Override
    public String[] list(FilenameFilter filter) {
        return null;
    }

    @Override
    public File[] listFiles() {
        return null;
    }

    @Override
    public File[] listFiles(FilenameFilter filter) {
        return null;
    }

    @Override
    public File[] listFiles(FileFilter filter) {
        return null;
    }

    @Override
    public boolean mkdir() {
        return false;
    }

    @Override
    public boolean mkdirs() {
        return false;
    }

    @Override
    public boolean setLastModified(long time) {
        Node resourceNode = getResourceNode();
        Calendar lastModifiedCalendar = Calendar.getInstance();
        lastModifiedCalendar.setTimeInMillis(time);
        try {
            resourceNode.setProperty(JCR_LASTMODIFIED, lastModifiedCalendar);
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to set file node's last modified time.", e);
        }
        return true;
    }

    /**
     * OVERIDDEN FROM FILE END
     */

    void setResourceNode(InputStream in, long lastModified)
            throws RepositoryException, IOException {
        String name = getName();
        MimeTable mimeTable = ArtifactoryXmlFactory.getMimeTable();
        String mimeType = mimeTable.getContentTypeFor(name);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        //Check whether to create the file as xml (w. xml-aware imported data) or regular file
        boolean xml = MavenUtils.isXml(name);
        if (xml) {
            setXmlAware(xml);
        }
        Node node = getNode();
        //Remove any existing content node
        if (node.hasNode(JCR_CONTENT)) {
            Node oldresourceNode = node.getNode(JCR_CONTENT);
            oldresourceNode.remove();
        }
        Node resourceNode = node.addNode(JCR_CONTENT, NT_RESOURCE);
        resourceNode.setProperty(JCR_MIMETYPE, mimeType);
        //Default encoding
        resourceNode.setProperty(JCR_ENCODING, "utf-8");
        //If it is an xml document import its native xml content into the repo
        if (xml) {
            in.mark(Integer.MAX_VALUE);
            //If it is a pom, verify that its groupId/artifactId/version match the dest path
            if (MavenUtils.isPom(name)) {
                MavenUtils.validatePomTargetPath(in, getRelativePath());
                in.reset();
            }
            //Reset the stream
            Node xmlNode = node.addNode(NODE_ARTIFACTORY_XML, NT_ARTIFACTORY_XML_CONTENT);
            importXml(xmlNode, in);
            //session.importXML(absPath, in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            in.reset();
        }
        //Check if needs to create checksum and not checksum file
        InputStream resourceInputStream;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Calculating checksum for '" + name + "'.");
        }
        resourceInputStream = new ChecksumInputStream(in,
                new Checksum(name, ChecksumType.md5),
                new Checksum(name, ChecksumType.sha1));
        //Do this after xml import since Jackrabbit 1.4
        //org.apache.jackrabbit.core.value.BLOBInTempFile.BLOBInTempFile will close the stream
        resourceNode.setProperty(JCR_DATA, resourceInputStream);
        setLastModified(lastModified);
        Checksum[] checksums = ((ChecksumInputStream) resourceInputStream).getChecksums();
        for (Checksum checksum : checksums) {
            //Save the checksum
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Saving checksum for '" + name + "' (checksum=" +
                        checksum.getChecksum() + ").");
            }
            String checksumStr = checksum.getChecksum();
            ChecksumType checksumType = checksum.getType();
            setPropValue("artifactory:" + checksumType.alg(), new StringValue(checksumStr));
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private Node getResourceNode() {
        try {
            Node resNode = getNode().getNode(JCR_CONTENT);
            return resNode;
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to get resource node.", e);
        }
    }

    public static boolean isFileNode(Node node) throws RepositoryException {
        NodeType primaryNodeType = node.getPrimaryNodeType();
        return JcrFile.NT_ARTIFACTORY_FILE.equals(primaryNodeType.getName());
    }

    /**
     * Import xml with characters entity resolving
     *
     * @param xmlNode
     * @param in
     * @throws RepositoryException
     * @throws IOException
     */
    private static void importXml(Node xmlNode, InputStream in)
            throws RepositoryException, IOException {
        JcrWrapper jcr = ContextHelper.get().getJcr();
        final String absPath = xmlNode.getPath();
        ContentHandler contentHandler = jcr.doInSession(new JcrCallback<ContentHandler>() {
            @SuppressWarnings({"UnnecessaryLocalVariable"})
            public ContentHandler doInJcr(JcrSessionWrapper session) throws RepositoryException {
                ContentHandler contentHandler = session.getImportContentHandler(
                        absPath, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
                return contentHandler;
            }
        });
        EntityResolvingContentHandler resolvingContentHandler =
                new EntityResolvingContentHandler(contentHandler);
        NonClosingInputStream ncis = null;
        try {
            SAXParserFactory factory = ArtifactoryXmlFactory.getFactory();
            SAXParser parser = factory.newSAXParser();
            ncis = new NonClosingInputStream(in);
            parser.parse(ncis, resolvingContentHandler);
        } catch (ParserConfigurationException e) {
            // Here ncis is always null
            throw new RepositoryException("SAX parser configuration error", e);
        } catch (Exception e) {
            //Check for wrapped repository exception
            if (e instanceof SAXException) {
                Exception e1 = ((SAXException) e).getException();
                if (e1 != null && e1 instanceof RepositoryException) {
                    if (ncis != null) {
                        ncis.forceClose();
                    }
                    throw (RepositoryException) e1;
                }
            }
            LOGGER.warn("Failed to parse XML stream to import into '" + absPath + "'.", e);
        }
    }
}
