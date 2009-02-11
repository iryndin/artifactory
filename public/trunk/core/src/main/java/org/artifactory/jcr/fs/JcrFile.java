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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import static org.apache.jackrabbit.JcrConstants.*;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.fs.FileAdditionalInfo;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.repo.exception.maven.BadPomException;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.ChecksumInputStream;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.io.checksum.policy.ChecksumPolicyException;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.lock.LockingException;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.PathUtils;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import java.io.*;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrFile extends JcrFsItem<FileInfo> {
    private static final Logger log = LoggerFactory.getLogger(JcrFile.class);

    private static final String WORKING_COPY_ABS_PATH = "workingCopyAbsPath";

    public static final String NT_ARTIFACTORY_FILE = "artifactory:file";
    public static final String NT_ARTIFACTORY_JAR = "artifactory:archive";
    public static final String PROP_ARTIFACTORY_CONTENT_UNCOMMITTED = "artifactory:uncommitted";
    public static final String PROP_ARTIFACTORY_ARCHIVE_ENTRY = "artifactory:archiveEntry";
    public static final String NODE_ARTIFACTORY_XML = "artifactory:xml";

    private boolean uncommitted;

    @Override
    protected FileInfo createInfo(RepoPath repoPath) {
        return new FileInfo(repoPath);
    }

    @Override
    protected FileInfo createInfo(FileInfo copy) {
        return new FileInfo(copy);
    }

    public JcrFile(RepoPath repoPath, LocalRepo repo) {
        super(repoPath, repo);
    }

    public JcrFile(JcrFile copy, LocalRepo repo) {
        super(copy, repo);
        this.uncommitted = copy.uncommitted;
    }

    /**
     * Constructor used when reading JCR content and creating JCR file item from it. Will not create anything in JCR but
     * will read the JCR content of the node.
     *
     * @param fileNode the JCR node this file represent
     * @param repo
     * @throws RepositoryRuntimeException if the node a file or cannot be read
     */
    public JcrFile(Node fileNode, LocalRepo repo) {
        super(fileNode, repo);
    }

    @Override
    protected void setAdditionalInfoFields(Node node) throws RepositoryException {
        FileInfo fileInfo = getInfo();
        if (isUncommitted(node)) {
            uncommitted = true;
            File workingCopy = getWorkingCopyFile();
            ContentType ct = NamingUtils.getContentType(workingCopy.getName());
            String mimeType = ct.getMimeType();
            fileInfo.setSize(workingCopy.length());
            fileInfo.setMimeType(mimeType);
            fileInfo.setLastModified(workingCopy.lastModified());
        } else {
            Node resNode = getResourceNode(node);
            fileInfo.setSize(resNode.getProperty(JCR_DATA).getLength());
            fileInfo.setMimeType(resNode.getProperty(JCR_MIMETYPE).getString());
            long lastModified = getJcrLastModified(node);
            if (lastModified > 0) {
                fileInfo.setLastModified(lastModified);
            }
        }
        FileAdditionalInfo additionalInfo = getXmlMetdataObject(FileAdditionalInfo.class);
        if (additionalInfo != null) {
            fileInfo.setAdditionalInfo(additionalInfo);
        }
    }

    @Override
    protected void setMandatoryInfoFields() {
        super.setMandatoryInfoFields();
        long length = getLength();
        if (length != 0) {
            getInfo().setSize(length);
        }
    }

    @Override
    protected long getJcrLastModified(Node node) throws RepositoryException {
        // The file last modified is on the resource node
        return super.getJcrLastModified(getResourceNode(node));
    }

    /**
     * fill the data file from stream
     */
    public void fillData(InputStream in) {
        fillData(System.currentTimeMillis(), in);
    }

    /**
     * Fill the jcr file system item (file) from stream under the parent folder. This constructor creates the entry in
     * JCR, and so should be called from a transactional scope. Fill the JCR content of this file from the input stream
     *
     * @param lastModified the date of modification of this file
     * @param in           the input stream for this file content. Will be closed in this method.
     * @throws RepositoryRuntimeException if the parentNode cannot be read or the JCR node elements cannot be created
     * @throws LockingException           if the JCrFile is immutable or not locked for this thread
     */
    public void fillData(long lastModified, InputStream in) {
        if (!isMutable()) {
            throw new LockingException("Cannot modified immutable " + this);
        }
        try {
            createOrGetFileNode(getParentNode(), getName());
            setResourceNode(in, lastModified, true);
            saveModifiedInfo();
        } catch (Exception e) {
            throw new RepositoryRuntimeException(
                    "Failed to create file node resource at '" + getAbsolutePath() + "'.", e);
        }
    }

    /**
     * Create a new jcr file system item (file) from a file under the parent folder. This constructor creates the entry
     * in JCR, and so should be called from a transactional scope. Fill the JCR content of this file from the input
     * stream
     *
     * @param file   The file system content file
     * @param status
     * @throws RepositoryRuntimeException if the parentNode cannot be read or the JCR node elements cannot be created
     * @throws IOException                if the stream cannot be read or closed
     */
    public void importFrom(File file, ImportSettings settings, StatusHolder status) throws IOException {
        try {
            createOrGetFileNode(getParentNode(), getName());
            importFrom(settings, status);
            FileInfo info = getInfo();
            long lastModified = info.getLastModified();
            if (lastModified <= 0) {
                lastModified = file.lastModified();
            }
            long actualSize = file.length();
            if (info.getSize() != 0 && info.getSize() != actualSize) {
                status.setWarning("Imported file " + file.getAbsolutePath() + " has an actual size of " + actualSize +
                        " when information size is " + info.getSize(), log);
                info.setSize(actualSize);
            }
            super.setMandatoryInfoFields();
            //Determine how to process the file - either store it in wc as a copy, or as a symlink
            //or just import it directly
            if (settings.isCopyToWorkingFolder()) {
                File targetWcFile = getWorkingCopyFile();
                FileUtils.forceMkdir(targetWcFile.getParentFile());
                if (settings.isUseSymLinks()) {
                    //Create a symlink in the wokring copy
                    org.artifactory.util.FileUtils.createSymLink(file, targetWcFile);
                } else {
                    //Copy the file to the working copy, overriding any previously existing file
                    FileUtils.copyFile(file, targetWcFile);
                }
                //Sanity check
                if (!targetWcFile.exists()) {
                    log.error("Failed to import '" + file.getAbsolutePath() +
                            "' into working copy file '" + targetWcFile.getAbsolutePath() + "'.");
                }
                //Create a place holder content (empty JCR_DATA)
                Node node = getNode();
                node.setProperty(PROP_ARTIFACTORY_CONTENT_UNCOMMITTED, true);
                uncommitted = true;
                Node resourceNode = getJcrService().getOrCreateNode(node, JCR_CONTENT, NT_RESOURCE);
                //Add mandatory jcr:lastModified and jcr:mimeType
                setContentType(resourceNode, file.getName(), info);
                setLastModified(resourceNode, lastModified);
                //Add empty jcr:data (mandatory)
                resourceNode.setProperty(JCR_DATA, new ByteArrayInputStream(new byte[]{}));
            } else {
                // Save checksum for later compare
                String md5 = info.getMd5();
                String sha1 = info.getSha1();
                //Stream the file directly into JCR
                InputStream is = null;
                try {
                    is = new BufferedInputStream(new FileInputStream(file));
                    setResourceNode(is, lastModified, false);
                } finally {
                    IOUtils.closeQuietly(is);
                }
                if (PathUtils.hasText(md5) && !info.getMd5().equals(md5)) {
                    status.setWarning("Received file " + getRepoPath() + " with Checksum error on MD5 actual=" +
                            info.getMd5() + " expected=" + md5, log);
                }
                if (PathUtils.hasText(sha1) && !info.getSha1().equals(sha1)) {
                    status.setWarning("Received file " + getRepoPath() + " with Checksum error on SHA1 actual=" +
                            info.getSha1() + " expected=" + sha1, log);
                }
            }
            getMdService().setXmlMetadata(this, info.getInernalXmlInfo());
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Could not create file node resource at '" + getAbsolutePath() + "': " + e.getMessage(), e);
        }
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
            log.warn("Skipping unknown checksum type: " + checksumType + ".");
            return null;
        }
    }

    @Override
    public void setLastUpdated(long lastUpdated) {
        if (!isMutable()) {
            throw new LockingException("Cannot modified immutable " + this);
        }
        getInfo().setLastUpdated(lastUpdated);
        saveModifiedInfo();
    }

    public void updateDownloadCount() {
        StatsInfo statsInfo = getMdService().getXmlMetadataObject(this, StatsInfo.class, true);
        statsInfo.setDownloadCount(statsInfo.getDownloadCount() + 1);
        getMdService().setXmlMetadata(this, statsInfo);
    }

    public static class ExtractWorkingCopyFileJob extends QuartzCommand {
        @Override
        protected void onExecute(JobExecutionContext callbackContext) {
            JobDataMap map = callbackContext.getMergedJobDataMap();
            String wcAbsPath = (String) map.get(WORKING_COPY_ABS_PATH);
            StatusHolder status = (StatusHolder) map.get(StatusHolder.class.getName());
            try {
                JcrService service = InternalContextHelper.get().getJcrService();
                boolean success = service.commitSingleFile(wcAbsPath);
                if (success) {
                    status.setDebug("File " + wcAbsPath + " was committed succesfully", log);
                } else {
                    status.setStatus(HttpStatus.SC_NOT_FOUND, "File " + wcAbsPath + " was deleted",
                            log);
                }
            } catch (Exception e) {
                status.setError("Cannot commit single file " + wcAbsPath, e, log);
            }
        }
    }

    /**
     * Retrieve JCR content, and do working copy commit if needed.
     *
     * @return null if deleted, the InputStream of the content of the file otherwise
     */
    public InputStream getStream() {
        try {
            if (uncommitted) {
                JcrFile meLocked = (JcrFile) LockingHelper.getIfLockedByMe(getRepoPath());
                if (meLocked != null) {
                    if (extractWorkingCopyFile()) {
                        return null;
                    }
                } else {
                    QuartzTask task = new QuartzTask(ExtractWorkingCopyFileJob.class, "ExtractWorkingCopyFile");
                    StatusHolder status = new StatusHolder();
                    task.addAttribute(StatusHolder.class.getName(), status);
                    task.addAttribute(WORKING_COPY_ABS_PATH, getAbsolutePath());
                    TaskService taskService = InternalContextHelper.get().getTaskService();
                    taskService.startTask(task);
                    boolean wasReadLocked = LockingHelper.releaseReadLock(getRepoPath());
                    taskService.waitForTaskCompletion(task.getToken());
                    if (status.isError()) {
                        IOException exception = new IOException(status.getStatusMsg());
                        exception.initCause(status.getException());
                        throw exception;
                    }
                    if (status.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                        // was deleted
                        return null;
                    }
                    if (wasReadLocked) {
                        LockingHelper.reacquireReadLock(getRepoPath());
                    }
                    // TODO: May be return the workingCopyFile
                    /*
                    File file = getWorkingCopyFile();
                    return FileUtils.openInputStream(file);
                    */
                }
            }
            Node node = getNode();
            Node resNode = getResourceNode(node);
            Value attachedDataValue = resNode.getProperty(JCR_DATA).getValue();
            InputStream is = attachedDataValue.getStream();
            return is;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Failed to retrieve file node's " + getRepoPath() + " data stream.", e);
        } catch (IOException e) {
            throw new RepositoryRuntimeException(
                    "Failed to retrieve file node's " + getRepoPath() + " data stream.", e);
        }
    }

    /**
     * Fill JCR data with the working copy file, and return delete status.
     *
     * @return true if deleted, false otherwise
     * @throws RepositoryException
     */
    public boolean extractWorkingCopyFile() throws RepositoryException {
        File workingCopyFile = getWorkingCopyFile();
        //If we have a an uncommitted resource content, try to import it from the working copy
        Node node = getNode();
        if (!isUncommitted(node)) {
            // Someone did it
            uncommitted = false;
            if (workingCopyFile.exists()) {
                // Delete the WC file left over
                deleteWcFile(workingCopyFile);
            }
            return isDeleted();
        }
        if (workingCopyFile.exists()) {
            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(new FileInputStream(workingCopyFile));
                setResourceNode(bis, workingCopyFile.lastModified(), false);
                if (log.isDebugEnabled()) {
                    log.debug("Imported working copy file at '" +
                            workingCopyFile.getAbsolutePath() + " into '" + getPath() +
                            "'.");
                }
                //Remove the file and the uncommitted mark
                node.setProperty(PROP_ARTIFACTORY_CONTENT_UNCOMMITTED, (Value) null);
                saveBasicInfo();
                //Double check that we can save the tx before deleting the physical file
                getJcrService().getManagedSession().save();
                deleteWcFile(workingCopyFile);
            } catch (IOException e) {
                //File might have been imported in parallel/removed
                log.warn("Failed to import working copy file at '" +
                        workingCopyFile.getAbsolutePath() + " into '" + getPath() + "'", e);
            } finally {
                IOUtils.closeQuietly(bis);
            }
        } else {
            log.error("Cannot find the working copy file at '" +
                    workingCopyFile.getAbsolutePath() + " for uncommitted file at '" +
                    getPath() + "'. Removing inconsistent node.");
            //Remove the bad file
            delete();
            return true;
        }
        return false;
    }

    @Override
    public boolean delete() {
        boolean result = super.delete();
        File workingCopyFile = getWorkingCopyFile();
        if (workingCopyFile.exists()) {
            deleteWcFile(workingCopyFile);
        }
        return result;
    }

    private void deleteWcFile(File workingCopyFile) {
        boolean deleted = workingCopyFile.delete();
        if (!deleted) {
            log.warn("Failed to delete imported file: " + workingCopyFile.getAbsolutePath() +
                    ". File might have been removed externally.");
        }
    }

    public long getSize() {
        return getInfo().getSize();
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public JcrFsItem save() {
        if (isDeleted()) {
            throw new IllegalStateException("Cannot save item " + getRepoPath() + " it is schedule for deletion");
        }
        //Node fileNode = createOrGetFileNode(getParentNode(), getName());
        //saveModifiedInfo();
        return new JcrFile(getNode(), getLocalRepo());
    }

    @Override
    public boolean isIdentical(JcrFsItem item) {
        if (!(item instanceof JcrFile)) {
            return false;
        }
        JcrFile jcrFile = (JcrFile) item;
        return this.uncommitted == jcrFile.uncommitted && super.isIdentical(item);
    }

    @Override
    public int zap(long expiredLastUpdated) {
        if (MavenNaming.isNonUniqueSnapshot(getPath())) {
            // zap has a meaning only on non unique snapshot files
            setLastUpdated(expiredLastUpdated);
            return 1;
        } else {
            return 0;
        }
    }

    public void exportTo(ExportSettings settings, StatusHolder status) {
        status.setDebug("Exporting file '" + getRelativePath() + "'...", log);
        File targetFile = new File(settings.getBaseDir(), getRelativePath());
        try {
            exportFileContent(targetFile, status, settings);

            if (settings.isIncludeMetadata()) {
                exportMetadata(targetFile, status, settings.isIncremental());
            }
            if (settings.isM2Compatible()) {
                writeChecksums(targetFile.getParentFile(), getInfo().getChecksumsInfo(), targetFile.getName(),
                        getLastModified());
            }
        } catch (FileNotFoundException e) {
            status.setError("Failed to export " + getAbsolutePath() + " since it was deleted.", log);
        } catch (Exception e) {
            status.setError("Failed to export " + getAbsolutePath() + " to file '" +
                    targetFile.getPath() + "'.", e, log);
        }
    }

    private void exportFileContent(File targetFile, StatusHolder status, ExportSettings settings)
            throws IOException {
        if (settings.isIncremental() && targetFile.exists()) {
            // incremental export - only export the file if it is newer
            if (getInfo().getLastModified() <= targetFile.lastModified()) {
                log.debug("Skipping not modified file {}", getPath());
                return;
            }
        }
        log.debug("Exporting file content to {}", targetFile.getAbsolutePath());
        OutputStream os = null;
        InputStream is = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(targetFile));
            is = getStream();
            if (is == null) {
                // File was deleted
                throw new FileNotFoundException();
            }

            IOUtils.copy(is, os);
            if (getLastModified() >= 0) {
                targetFile.setLastModified(getLastModified());
            }
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void importFrom(ImportSettings settings, StatusHolder status) {
        File baseDir = settings.getBaseDir();
        if (baseDir == null) {
            String message = "Cannot import null directory '" + baseDir + "'.";
            IllegalArgumentException ex = new IllegalArgumentException(message);
            status.setError("Import Error", ex, log);
            return;
        }
        File file = new File(baseDir, getRelativePath());
        if (!file.exists() || file.isDirectory()) {
            String message = "Cannot import non existant file or directory '" + file.getAbsolutePath() + "' into " +
                    this.getRepoPath();
            IllegalArgumentException ex = new IllegalArgumentException(message);
            status.setError("Import Error", ex, log);
            return;
        }
        if (settings.isIncludeMetadata()) {
            try {
                //Read metadata into the node
                importMetadata(file, status, settings);
            } catch (Exception e) {
                String msg = "Failed to import file " + file.getAbsolutePath() + " into '" + getRepoPath() + "'.";
                status.setError(msg, e, log);
            }
        }
    }

    public void importInternalMetadata(MetadataDefinition definition, Object md) {
        // For the moment we support only FileInfo as transient MD
        if (definition.getMetadataName().equals(FileInfo.ROOT) && md instanceof FileInfo) {
            FileInfo importedFileInfo = (FileInfo) md;
            FileInfo info = getInfo();
            info.setAdditionalInfo(importedFileInfo.getInernalXmlInfo());
            info.setMimeType(importedFileInfo.getMimeType());
            info.setSize(importedFileInfo.getSize());
            updateTimestamps(importedFileInfo, info);
        } else {
            throw new IllegalStateException(
                    "Metadata " + definition + " for object " + md + " is not supported has transient!");
        }
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
        Node resourceNode = getResourceNode(getNode());
        return setLastModified(resourceNode, time);
    }

    public static boolean isFileNode(Node node) throws RepositoryException {
        NodeType primaryNodeType = node.getPrimaryNodeType();
        return JcrFile.NT_ARTIFACTORY_FILE.equals(primaryNodeType.getName());
    }

    public File getWorkingCopyFile() {
        File wcDir = ArtifactoryHome.getWorkingCopyDir();
        return new File(wcDir, getPath());
    }

    /**
     * Do not import metadata,index folders and checksums
     */
    public static boolean isStorable(String name) {
        return !name.endsWith(ItemInfo.METADATA_FOLDER) && !NamingUtils.isChecksum(name);
    }

    /**
     * OVERIDDEN FROM java.io.File END
     */

    private void setResourceNode(InputStream in, long lastModified, boolean updateInfo)
            throws RepositoryException, IOException {
        Node node = getNode();
        Node resourceNode = getJcrService().getOrCreateNode(node, JCR_CONTENT, NT_RESOURCE);
        String name = getName();
        FileInfo info = getInfo();
        ContentType ct = setContentType(resourceNode, name, info);
        //If it is an XML document save the XML in memory since marking does not always work on the
        //remote stream, and import its xml content into the repo for indexing
        if (ct.isXml()) {
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
            if (MavenNaming.isPom(name)) {
                try {
                    MavenUtils.validatePomTargetPath(in, getRelativePath());
                } catch (BadPomException e) {
                    throw new RepositoryException("Could not store POM file.", e);
                }
                in.reset();
            }
            Node xmlNode = getJcrService().getOrCreateUnstructuredNode(node, NODE_ARTIFACTORY_XML);
            getJcrService().importXml(xmlNode, in);
            //Reset the stream
            in.reset();
        }
        try {
            fillJcrData(resourceNode, name, in, lastModified, info, updateInfo);
        } finally {
            //Make sure the replaced stream is closed (orginal stream is taken care of by caller)
            if (ct.isXml()) {
                IOUtils.closeQuietly(in);
            }
        }
    }

    @SuppressWarnings({"MethodMayBeStatic"})
    private ContentType setContentType(Node resourceNode, String name, FileInfo info)
            throws RepositoryException {
        ContentType ct = NamingUtils.getContentType(name);
        String mimeType = ct.getMimeType();
        resourceNode.setProperty(JCR_MIMETYPE, mimeType);
        info.setMimeType(mimeType);
        return ct;
    }

    @SuppressWarnings({"OverlyComplexMethod"})
    private void fillJcrData(Node resourceNode, String name, InputStream in, long lastModified,
            FileInfo info, boolean updateInfo) throws RepositoryException {

        //Check if needs to create checksum and not checksum file
        log.debug("Calculating checksums for '{}'.", name);
        Checksum[] checksums = getChecksumsToCompute();
        ChecksumInputStream resourceInputStream = new ChecksumInputStream(in, checksums);

        //Do this after xml import: since Jackrabbit 1.4
        //org.apache.jackrabbit.core.value.BLOBInTempFile.BLOBInTempFile will close the stream
        resourceNode.setProperty(JCR_DATA, resourceInputStream);
        setLastModified(resourceNode, lastModified);

        // set the actual checksums on the file extra info
        setFileActualChecksums(info, checksums);

        // apply the checksum policy
        LocalRepo repository = getLocalRepo();
        ChecksumPolicy policy = repository.getChecksumPolicy();
        Set<ChecksumInfo> checksumInfos = info.getChecksums();
        boolean passes = policy.verify(checksumInfos);
        if (!passes) {
            throw new ChecksumPolicyException(policy, checksumInfos, name);
        }

        long actualSize = getLength();
        if (updateInfo) {
            //Update the info
            info.setLastModified(lastModified);
            info.setLastUpdated(System.currentTimeMillis());
            info.setSize(actualSize);
        } else {
            // Sanity check on the size
            if (info.getSize() != actualSize) {
                // Print message only if info actually present
                if (info.getSize() != 0) {
                    log.warn("Received file " + getRepoPath() + " with size info " + info.getSize() +
                            " but actual size is " + actualSize);
                }
                info.setSize(actualSize);
            }
        }
    }

    private void setFileActualChecksums(FileInfo info, Checksum[] checksums) {
        Set<ChecksumInfo> checksumInfos = info.getChecksums();
        for (Checksum checksum : checksums) {
            ChecksumType checksumType = checksum.getType();
            String calculatedChecksum = checksum.getChecksum();
            ChecksumInfo checksumInfo = getChecksumInfo(checksumType, checksumInfos);
            if (checksumInfo != null) {
                // set the actual checksum
                checksumInfo.setActual(calculatedChecksum);
                // original checksum migh be null
                String originalChecksum = checksumInfo.getOriginal();
                if (!checksumInfo.checksumsMatch()) {
                    log.debug("Checksum mismatch {}. original: {} calculated: {}",
                            new String[]{checksumType.toString(), originalChecksum, calculatedChecksum});
                }
            } else {
                log.warn(checksumType + " checksum info not found for '" + info.getRepoPath().getPath() +
                        ". Creating one with empty original checksum.");
                ChecksumInfo missingChecksumInfo = new ChecksumInfo(checksumType);
                missingChecksumInfo.setActual(calculatedChecksum);
                info.addChecksumInfo(missingChecksumInfo);
            }
        }
    }

    private ChecksumInfo getChecksumInfo(ChecksumType type, Set<ChecksumInfo> infos) {
        for (ChecksumInfo info : infos) {
            if (type.equals(info.getType())) {
                return info;
            }
        }
        return null;
    }

    private Checksum[] getChecksumsToCompute() {
        ChecksumType[] checksumTypes = ChecksumType.values();
        Checksum[] checksums = new Checksum[checksumTypes.length];
        for (int i = 0; i < checksumTypes.length; i++) {
            checksums[i] = new Checksum(checksumTypes[i]);
        }
        return checksums;
    }

    private Node getResourceNode(Node node) {
        try {
            Node resNode = node.getNode(JCR_CONTENT);
            return resNode;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to get resource node for " + getRepoPath(), e);
        }
    }

    private long getLength() {
        if (!exists()) {
            return 0;
        }
        Node node = getNode();
        return getLength(node);
    }

    private long getLength(Node node) {
        try {
            long size;
            if (isUncommitted(node)) {
                size = getWorkingCopyFile().length();
            } else {
                Node resNode = getResourceNode(node);
                size = resNode.getProperty(JCR_DATA).getLength();
            }
            return size;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to retrieve file node's size.", e);
        }
    }

    private boolean isUncommitted(Node node) throws RepositoryException {
        return node.hasProperty(PROP_ARTIFACTORY_CONTENT_UNCOMMITTED);
    }
}
