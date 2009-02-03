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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import static org.apache.jackrabbit.JcrConstants.*;
import org.apache.log4j.Logger;
import org.artifactory.api.common.PackagingType;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.config.xml.ArtifactoryXmlFactory;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.ChecksumInputStream;
import org.artifactory.io.checksum.ChecksumType;
import org.artifactory.jcr.md.MetadataValue;
import org.artifactory.maven.MavenUtils;
import sun.net.www.MimeTable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import java.io.*;
import java.util.Calendar;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrFile extends JcrFsItem<FileInfo> {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrFile.class);

    public static final String NT_ARTIFACTORY_FILE = "artifactory:file";
    public static final String NT_ARTIFACTORY_JAR = "artifactory:archive";
    public static final String PROP_ARTIFACTORY_CONTENT_UNCOMMITTED = "artifactory:uncommitted";
    public static final String PROP_ARTIFACTORY_ARCHIVE_ENTRY = "artifactory:archiveEntry";
    public static final String NODE_ARTIFACTORY_XML = "artifactory:xml";

    /**
     * Create a new jcr file system item (file) from stream under the parent folder. This
     * constructor creates the entry in JCR, and so should be called from a transactional scope.
     * Fill the JCR content of this file from the input stream
     *
     * @param parent       The folder JCR node to create this element in
     * @param newName      The name of this new element
     * @param lastModified the date of modification of this file
     * @param in           the input stream for this file content. Will be closed in this method.
     * @throws RepositoryRuntimeException if the parentNode cannot be read or the JCR node elements
     *                                    cannot be created
     * @throws IOException                if the stream cannot be read or closed
     */
    public JcrFile(JcrFolder parent, String newName, long lastModified, InputStream in) {
        super(parent.getAbsolutePath() + "/" + newName);
        try {
            createOrGetFileNode(parent.getNode(), newName);
            FileInfo info = getLockedInfo();
            setResourceNode(in, lastModified, info);
            saveModifiedInfo(info);
        } catch (Exception e) {
            throw new RepositoryRuntimeException(
                    "Failed to create file node resource at '" + getAbsolutePath() + "'.", e);
        }
    }

    /**
     * Create a new jcr file system item (file) from a file under the parent folder. This
     * constructor creates the entry in JCR, and so should be called from a transactional scope.
     * Fill the JCR content of this file from the input stream
     *
     * @param parent The folder JCR node to create this element in
     * @param file   The file system content file
     * @param status
     * @throws RepositoryRuntimeException if the parentNode cannot be read or the JCR node elements
     *                                    cannot be created
     * @throws IOException                if the stream cannot be read or closed
     */
    public JcrFile(JcrFolder parent, File file, ImportSettings settings, StatusHolder status)
            throws IOException {
        super(parent.getAbsolutePath() + "/" + file.getName());
        try {
            createOrGetFileNode(parent.getNode(), file.getName());
            importFrom(settings, status);
            FileInfo info = getLockedInfo();
            long lastModified = file.lastModified();
            info.setSize(file.length());
            super.setMandatoryInfoFields(info);
            /* Don't change those they come from metadata
            info.setLastModified(lastModified);
            info.setLastUpdated(System.currentTimeMillis());
            */
            //Determine how to process the file - either store it in wc as a copy, or as a symlink
            //or just import it directly
            if (settings.isCopyToWorkingFolder()) {
                File targetWcFile = getWorkingCopyFile();
                FileUtils.forceMkdir(targetWcFile.getParentFile());
                if (settings.isUseSymLinks()) {
                    //Create a symlink in the wokring copy
                    org.artifactory.utils.FileUtils.createSymLink(file, targetWcFile);
                } else {
                    //Copy the file to the working copy, overriding any previously existing file
                    FileUtils.copyFile(file, targetWcFile);
                }
                //Sanity check
                if (!targetWcFile.exists()) {
                    LOGGER.error("Failed to import '" + file.getAbsolutePath() +
                            "' into working copy file '" + targetWcFile.getAbsolutePath() + "'.");
                }
                //Create a place holder content (empty JCR_DATA)
                Node node = getNode();
                node.setProperty(PROP_ARTIFACTORY_CONTENT_UNCOMMITTED, true);
                Node resourceNode = node.addNode(JCR_CONTENT, NT_RESOURCE);
                //Add mandatory jcr:lastModified and jcr:mimeType
                setPackagingType(resourceNode, file.getName(), info);
                //Last modified is mandatory on nt:resource (jcr:content)
                Calendar lastModifiedCal = Calendar.getInstance();
                lastModifiedCal.setTimeInMillis(lastModified);
                resourceNode.setProperty(JCR_LASTMODIFIED, lastModifiedCal);
                //Add empty jcr:data (mandatory)
                resourceNode.setProperty(JCR_DATA, new ByteArrayInputStream(new byte[]{}));
            } else {
                //Stream the file directly into JCR
                InputStream is = null;
                try {
                    is = new BufferedInputStream(new FileInputStream(file));
                    setResourceNode(is, lastModified, info);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
            getMdService().setXmlMetadata(this, info);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Failed to create file node resource at '" + getAbsolutePath() + "'.", e);
        }
    }

    /**
     * Create a new file from stream
     */
    public JcrFile(JcrFolder parent, String name, InputStream in) {
        this(parent, name, System.currentTimeMillis(), in);
    }

    /**
     * Constructor used when reading JCR content and creating JCR file item from it. Will not create
     * anything in JCR but will read the JCR content of the node.
     *
     * @param fileNode the JCR node this file represent
     * @throws RepositoryRuntimeException if the node a file or cannot be read
     */
    public JcrFile(Node fileNode) {
        super(fileNode);
        //Sanity check
        try {
            if (!isFileNode(fileNode)) {
                throw new RepositoryRuntimeException("Node is not a file node.");
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to create Jcr File.", e);
        }
    }

    @Override
    public MetadataValue lock() {
        return getMdService().lockCreateIfEmpty(FileInfo.class, getAbsolutePath());
    }

    /**
     * Simple constructor with absolute path. Does not create or read anything in JCR.
     *
     * @param absPath The absolute path of this JCR File System item
     */
    public JcrFile(String absPath) {
        super(absPath);
    }

    /**
     * Simple constructor with parent and child path. Does not create or read anything in JCR.
     *
     * @param parent absolute parent path
     * @param child  a relative to parent path
     */
    public JcrFile(String parent, String child) {
        super(parent, child);
    }

    /**
     * Simple constructor with parent as File and child path. Does not create or read anything in
     * JCR except if parent is a JcrFolder.
     *
     * @param parent a file that will provide absolute path with parent.getAbsolutePath()
     * @param child  a relative to parent path
     */
    public JcrFile(File parent, String child) {
        super(parent, child);
    }

    @Override
    public FileInfo getInfo() {
        return getMdService().getXmlMetadataObject(this, FileInfo.class, false);
    }

    @Override
    public FileInfo getLockedInfo() {
        return getMdService().getLockedXmlMetadataObject(this, FileInfo.class);
    }

    @Override
    protected void setMandatoryInfoFields(FileInfo info) {
        super.setMandatoryInfoFields(info);
        long length = getLength();
        if (length != 0) {
            info.setSize(length);
        }
    }

    @Override
    protected void unlockNoSave() {
        getMdService().unlockNoSave(FileInfo.class, getAbsolutePath());
    }

    /**
     * When was a cacheable file last updated
     *
     * @return the last update time as UTC milliseconds
     */
    public long getLastUpdated() {
        return getInfo().getLastUpdated();
    }

    public long getLastModified() {
        return getInfo().getLastModified();
    }

    public String getMimeType() {
        return getInfo().getMimeType();
    }

    public long getDownloadCount() {
        StatsInfo statsInfo = getMdService().getXmlMetadataObject(this, StatsInfo.class);
        return statsInfo.getDownloadCount();
    }

    public String getChecksum(ChecksumType checksumType) {
        if (checksumType.equals(ChecksumType.sha1)) {
            return getInfo().getSha1();
        } else if (checksumType.equals(ChecksumType.md5)) {
            return getInfo().getMd5();
        } else {
            LOGGER.warn("Skipping unknown checksum type: " + checksumType + ".");
            return null;
        }
    }

    public void setLastUpdated(long lastUpdated) {
        FileInfo lockedInfo = getLockedInfo();
        lockedInfo.setLastUpdated(lastUpdated);
        saveModifiedInfo(lockedInfo);
    }

    public void updateDownloadCount() {
        StatsInfo statsInfo = getMdService().getLockedXmlMetadataObject(this, StatsInfo.class);
        statsInfo.setDownloadCount(statsInfo.getDownloadCount() + 1);
        getMdService().setXmlMetadata(this, statsInfo);
    }

    /**
     * Get a resource stream and increment the download counter
     *
     * @return the resource stream
     */
    public InputStream getStreamForDownload() {
        InputStream is = getStream();
        //TODO: We better use async stats updates instead of pessimistic locking to avoid
        //optimistic lock exceptions. Also, if another thread updates the dl count we will overwrite
        //the value (locking done only in setMetadata).
        //Update the download count
        updateDownloadCount();
        return is;
    }

    public InputStream getStream() {
        Node node = getNode();
        try {
            boolean uncommitted = isUncommitted();
            if (uncommitted) {
                // Lock early before checking the working copy, then retest the uncommited
                FileInfo info = getLockedInfo();
                uncommitted = isUncommitted();
                if (uncommitted) {
                    if (extractWorkingCopyFile(node, info)) {
                        return null;
                    }
                }
            }
            Node resNode = getResourceNode();
            Value attachedDataValue = resNode.getProperty(JCR_DATA).getValue();
            InputStream is = new BufferedInputStream(attachedDataValue.getStream());
            return is;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to retrieve file node's data stream.", e);
        }
    }

    private boolean extractWorkingCopyFile(Node node, FileInfo info) throws RepositoryException {
        //If we have a an uncommitted resource content, try to import it from the working
        //copy
        File workingCopyFile = getWorkingCopyFile();
        if (workingCopyFile.exists()) {
            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(new FileInputStream(workingCopyFile));
                setResourceNode(bis, workingCopyFile.lastModified(), info);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Imported working copy file at '" +
                            workingCopyFile.getAbsolutePath() + " into '" + getPath() +
                            "'.");
                }
                //Remove the file and the uncommitted mark
                node.setProperty(PROP_ARTIFACTORY_CONTENT_UNCOMMITTED, (Value) null);
                saveBasicInfo(info);
                //Double check that we can save the tx before deleting the physical file
                getJcrService().getManagedSession().save();
                boolean deleted = workingCopyFile.delete();
                if (!deleted) {
                    LOGGER.warn("Failed to delete imported file: " +
                            workingCopyFile.getAbsolutePath() +
                            ". File might have been removed externally.");
                }
            } catch (IOException e) {
                //File might have been imported in parallel/removed
                LOGGER.warn("Failed to import working copy file at '" +
                        workingCopyFile.getAbsolutePath() + " into '" + getPath() + "'.");
            } finally {
                IOUtils.closeQuietly(bis);
            }
        } else {
            LOGGER.error("Cannot find the working copy file at '" +
                    workingCopyFile.getAbsolutePath() + " for uncommitted file at '" +
                    getPath() + "'. Removing inconsistent node.");
            //Remove the bad file
            delete();
            return true;
        }
        return false;
    }

    public long getSize() {
        return getInfo().getSize();
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    public void exportNoMetadata(File targetFile) {
        export(targetFile, null, false);
    }

    void export(File targetFile, StatusHolder status, boolean includeMetadata) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Exporting file '" + getRelativePath() + "'...");
        }
        OutputStream os = null;
        InputStream is = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(targetFile));
            is = getStream();
            IOUtils.copy(is, os);
            IOUtils.closeQuietly(os);
            long modified = getLastModified();
            if (modified >= 0) {
                targetFile.setLastModified(modified);
            }
            if (includeMetadata) {
                exportMetadata(targetFile, modified, status);
            }
        } catch (Exception e) {
            if (status != null) {
                status.setError("Failed to export to file '" + targetFile.getPath() + "'.", e);
            }
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }

    public void exportTo(ExportSettings settings, StatusHolder status) {
        File targetFile = new File(settings.getBaseDir(), getRelativePath());
        export(targetFile, status, settings.isIncludeMetadata());
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void importFrom(ImportSettings settings, StatusHolder status) {
        File baseDir = settings.getBaseDir();
        if (baseDir == null) {
            String message = "Cannot import null directory '" + baseDir + "'.";
            IllegalArgumentException ex = new IllegalArgumentException(message);
            status.setError(message, ex, LOGGER);
            return;
        }
        try {
            //Read metadata into the node
            File file = new File(baseDir, getRelativePath());
            importMetadata(file, status);
        } catch (Exception e) {
            String msg = "Failed to import file into '" + getRelativePath() + "'.";
            status.setError(msg, e);
        }
    }

    /**
     * OVERIDDEN FROM java.io.File BEGIN
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
        return getLength();
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
            throw new RepositoryRuntimeException("Failed to set file node's last modified time.",
                    e);
        }
        return true;
    }

    public static boolean isFileNode(Node node) throws RepositoryException {
        NodeType primaryNodeType = node.getPrimaryNodeType();
        return JcrFile.NT_ARTIFACTORY_FILE.equals(primaryNodeType.getName());
    }

    public File getWorkingCopyFile() {
        File wcDir = ArtifactoryHome.getWorkingCopyDir();
        return new File(wcDir, getPath());
    }

    public static boolean isStorable(String name) {
        return !name.endsWith(ItemInfo.METADATA_FOLDER) && !MavenUtils.isChecksum(name);
    }

    /**
     * OVERIDDEN FROM java.io.File END
     */

    private void setResourceNode(InputStream in, long lastModified, FileInfo info)
            throws RepositoryException, IOException {
        Node node = getNode();
        Node resourceNode = getJcrService().getOrCreateNode(node, JCR_CONTENT, NT_RESOURCE);
        String name = getName();
        PackagingType pt = setPackagingType(resourceNode, name, info);
        //If it is an XML document save the XML in memory since marking does not always work on the
        //remote stream, and import its xml content into the repo for indexing
        if (pt.isXml()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtils.copy(in, bos);
            in.close();
            byte[] xmlBytes = bos.toByteArray();
            //Stream is replaced!
            in = new ByteArrayInputStream(xmlBytes);
            //Default encoding
            resourceNode.setProperty(JCR_ENCODING, "utf-8");
            //Get
            in.mark(Integer.MAX_VALUE);
            //If it is a pom, verify that its groupId/artifactId/version match the dest path
            if (MavenUtils.isPom(name)) {
                MavenUtils.validatePomTargetPath(in, getRelativePath());
                in.reset();
            }
            //Reset the stream
            Node xmlNode = getJcrService().getOrCreateUnstructuredNode(node, NODE_ARTIFACTORY_XML);
            getMdService().importXml(xmlNode, in);
            in.reset();
        }
        try {
            fillJcrData(resourceNode, name, in, lastModified, info);
        } finally {
            //Make sure the replaced stream is closed (orginal stream is taken care of by caller)
            if (pt.isXml()) {
                IOUtils.closeQuietly(in);
            }
        }
    }

    @SuppressWarnings({"MethodMayBeStatic"})
    private PackagingType setPackagingType(Node resourceNode, String name, FileInfo info)
            throws RepositoryException {
        String mimeType = null;
        PackagingType pt = PackagingType.getPackagingTypeByPath(name);
        if (pt == null) {
            pt = PackagingType.jar;
            // TODO: Need to get the MimeType from the original proxied stream or Packaging type
            MimeTable mimeTable = ArtifactoryXmlFactory.getMimeTable();
            mimeType = mimeTable.getContentTypeFor(name);
        }
        if (mimeType == null) {
            mimeType = pt.getContentType().getMimeType();
        }
        resourceNode.setProperty(JCR_MIMETYPE, mimeType);
        info.setMimeType(mimeType);
        return pt;
    }

    private void fillJcrData(Node resourceNode, String name, InputStream in, long lastModified,
            FileInfo info) throws RepositoryException {
        //Check if needs to create checksum and not checksum file
        InputStream resourceInputStream;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Calculating checksum for '" + name + "'.");
        }
        resourceInputStream = new ChecksumInputStream(in,
                new Checksum(name, ChecksumType.md5),
                new Checksum(name, ChecksumType.sha1));
        //Do this after xml import: since Jackrabbit 1.4
        //org.apache.jackrabbit.core.value.BLOBInTempFile.BLOBInTempFile will close the stream
        resourceNode.setProperty(JCR_DATA, resourceInputStream);
        Calendar lastModifiedCal = Calendar.getInstance();
        lastModifiedCal.setTimeInMillis(lastModified);
        //Last modified is mandatory on jcr:data
        resourceNode.setProperty(JCR_LASTMODIFIED, lastModifiedCal);
        Checksum[] checksums = ((ChecksumInputStream) resourceInputStream).getChecksums();
        for (Checksum checksum : checksums) {
            //Save the checksum
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Saving checksum for '" + name + "' (checksum=" +
                        checksum.getChecksum() + ").");
            }
            String checksumStr = checksum.getChecksum();
            ChecksumType checksumType = checksum.getType();
            if (checksumType.equals(ChecksumType.sha1)) {
                info.setSha1(checksumStr);
            } else if (checksumType.equals(ChecksumType.md5)) {
                info.setMd5(checksumStr);
            } else {
                LOGGER.warn("Skipping unknown checksum type: " + checksumType + ".");
            }
        }
        //Update the info
        info.setLastModified(lastModified);
        info.setSize(getLength());
        info.setLastUpdated(System.currentTimeMillis());
    }

    private Node getResourceNode() {
        try {
            Node resNode = getNode().getNode(JCR_CONTENT);
            return resNode;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to get resource node.", e);
        }
    }

    private long getLength() {
        if (!exists()) {
            return 0;
        }
        try {
            long size;
            if (isUncommitted()) {
                size = getWorkingCopyFile().length();
            } else {
                Node resNode = getResourceNode();
                size = resNode.getProperty(JCR_DATA).getLength();
            }
            return size;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to retrieve file node's size.", e);
        }
    }

    private boolean isUncommitted() throws RepositoryException {
        return getNode().hasProperty(PROP_ARTIFACTORY_CONTENT_UNCOMMITTED);
    }
}
