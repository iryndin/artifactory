/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.jcr.fs;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.MutableFileInfo;
import org.artifactory.fs.MutableStatsInfo;
import org.artifactory.fs.StatsInfo;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.ChecksumInputStream;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.io.checksum.policy.ChecksumPolicyException;
import org.artifactory.ivy.IvyNaming;
import org.artifactory.jcr.data.JcrVfsHelper;
import org.artifactory.jcr.factory.JcrFsItemFactory;
import org.artifactory.jcr.jackrabbit.MissingOrInvalidDataStoreRecordException;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.jcr.md.MetadataPersistenceHandler;
import org.artifactory.jcr.utils.JcrHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.PomTargetPathValidator;
import org.artifactory.mime.MavenNaming;
import org.artifactory.mime.MimeType;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.fs.VfsFile;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.storage.StorageConstants;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import java.io.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.apache.jackrabbit.JcrConstants.*;

/**
 * @author yoavl
 */
public class JcrFile extends JcrFsItem<FileInfo, MutableFileInfo> implements VfsFile {
    private static final Logger log = LoggerFactory.getLogger(JcrFile.class);

    private BlockingQueue<StatsInfo> downloads = null;

    public JcrFile(RepoPath repoPath, JcrFsItemFactory repo) {
        super(repoPath, repo);
    }

    public JcrFile(JcrFile copy, JcrFsItemFactory repo) {
        super(copy, repo);
        this.downloads = copy.downloads;
    }

    /**
     * Constructor used when reading JCR content and creating JCR file item from it. Will not create anything in JCR but
     * will read the JCR content of the node.
     *
     * @param fileNode the JCR node this file represent
     * @param repo     the storing repo
     * @param original an immutable origin
     * @throws RepositoryRuntimeException if the node a file or cannot be read
     */
    public JcrFile(Node fileNode, JcrFsItemFactory repo, JcrFile original) {
        super(fileNode, repo, original);
        if (original != null) {
            this.downloads = original.downloads;
        }
    }

    @Override
    protected FileInfo createInfo(RepoPath repoPath) {
        return InfoFactoryHolder.get().createFileInfo(repoPath);
    }

    @Override
    protected MetadataPersistenceHandler<FileInfo, MutableFileInfo> getInfoPersistenceHandler() {
        return getMdService().getFileInfoMd().getPersistenceHandler();
    }

    /**
     * fill the data file from stream
     */
    public void fillData(InputStream in) throws RepoRejectException {
        fillData(System.currentTimeMillis(), in);
    }

    /**
     * Fill the jcr file system item (file) from stream under the parent folder. This constructor creates the entry in
     * JCR, and so should be called from a transactional scope. Fill the JCR content of this file from the input stream
     *
     * @param lastModified the date of modification of this file
     * @param is           the input stream for this file content. Will be closed in this method.
     * @throws RepositoryRuntimeException if the parentNode cannot be read or the JCR node elements cannot be created
     * @throws org.artifactory.concurrent.LockingException
     *                                    if the JCrFile is immutable or not locked for this thread
     */
    public void fillData(long lastModified, InputStream is) throws RepoRejectException {
        checkMutable("fillData");
        try {
            getOrCreateFileNode(getParentNode(), getName());
            setModifiedInfoFields(lastModified, System.currentTimeMillis());
            setResourceNode(is);
        } catch (RepoRejectException rre) {
            throw rre;
        } catch (Exception e) {
            throw new RepositoryRuntimeException(
                    "Could not create file node resource at '" + getAbsolutePath() + "'.", e);
        }
    }

    /**
     * Create a JCR File system item under this parent node. This method creates the entry in JCR, and so should be
     * called from a transactional scope.
     *
     * @param parentNode The folder JCR node to create this element in
     * @param name       The name of this new element
     * @throws RepositoryRuntimeException if the parentNode cannot be read or the JCR node elements cannot be created
     */
    protected Node getOrCreateFileNode(Node parentNode, String name) {
        return JcrHelper.getOrCreateNode(parentNode, name, StorageConstants.NT_ARTIFACTORY_FILE,
                StorageConstants.MIX_ARTIFACTORY_BASE);
    }

    /**
     * Create a new jcr file system item (file) from a file under the parent folder. This constructor creates the entry
     * in JCR, and so should be called from a transactional scope. Fill the JCR content of this file from the input
     * stream
     *
     * @param file The file system content file
     * @throws RepositoryRuntimeException if the parentNode cannot be read or the JCR node elements cannot be created
     * @throws IOException                if the stream cannot be read or closed
     */
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void importFrom(File file, ImportSettings settings) throws IOException, RepoRejectException {
        MutableStatusHolder status = settings.getStatusHolder();
        try {
            if (!file.isFile()) {
                String message = "Cannot import non existent file or directory '" + file.getAbsolutePath() +
                        "' into " + getRepoPath();
                status.setError("Import Error", new IllegalArgumentException(message), log);
                return;
            }

            getOrCreateFileNode(getParentNode(), getName());

            checkMutable("import-file");
            MutableFileInfo mutableFileInfo = getMutableInfo();

            updateFileInfoFromImportedFile(file, mutableFileInfo);
            setMetadata(StatsInfo.class, InfoFactoryHolder.get().createStats());

            if (settings.isIncludeMetadata()) {
                importMetadata(file, status, settings);
            }

            // trust server checksums if the settings said so and client checksum is missing
            if (settings.isTrustServerChecksums()) {
                Set<ChecksumInfo> checksums = getInfo().getChecksumsInfo().getChecksums();
                if (checksums == null || checksums.isEmpty()) {
                    mutableFileInfo.createTrustedChecksums();
                } else {
                    Set<ChecksumInfo> checksumsCopy = Sets.newHashSet(checksums);
                    for (ChecksumInfo checksum : checksumsCopy) {
                        if (StringUtils.isBlank(checksum.getOriginal())) {
                            // replace with checksum that contains trusted mark, but same actual
                            mutableFileInfo.addChecksumInfo(new ChecksumInfo(
                                    checksum.getType(), ChecksumInfo.TRUSTED_FILE_MARKER, checksum.getActual()));
                        }
                    }
                }
            }

            // Save actual checksum from metadata/old file for later comparison with actual of newly uploaded file
            String md5 = mutableFileInfo.getMd5();
            String sha1 = mutableFileInfo.getSha1();
            //Stream the file directly into JCR
            InputStream is = null;
            try {
                is = new BufferedInputStream(new FileInputStream(file));
                setResourceNode(is);
            } finally {
                IOUtils.closeQuietly(is);
            }
            if (PathUtils.hasText(md5) && !getInfo().getMd5().equals(md5)) {
                status.setWarning("Received file " + getRepoPath() + " with Checksum error on MD5 actual=" +
                        getInfo().getMd5() + " expected=" + md5, log);
            }
            if (PathUtils.hasText(sha1) && !getInfo().getSha1().equals(sha1)) {
                status.setWarning("Received file " + getRepoPath() + " with Checksum error on SHA1 actual=" +
                        getInfo().getSha1() + " expected=" + sha1, log);
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Could not create file node resource at '" + getAbsolutePath() + "': " + e.getMessage(), e);
        }
    }

    private void updateFileInfoFromImportedFile(File file, MutableFileInfo mutableFileInfo) {
        mutableFileInfo.setLastModified(file.lastModified());
        mutableFileInfo.setLastUpdated(System.currentTimeMillis());
        mutableFileInfo.setCreatedBy(getAuthorizationService().currentUsername());
        mutableFileInfo.setModifiedBy(getAuthorizationService().currentUsername());
        mutableFileInfo.setSize(file.length());
        MimeType ct = NamingUtils.getMimeType(file.getName());
        String actualMimeType = ct.getType();
        mutableFileInfo.setMimeType(actualMimeType);
    }

    public long getLastModified() {
        return getInfo().getLastModified();
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

    public void updateDownloadStats() {
        if (log.isTraceEnabled()) {
            log.trace("Adding +1 download count to " + getRepoPath() + " from " + Thread.currentThread().getName());
        }
        BlockingQueue<StatsInfo> localDownloads = getOrCreateDownloads();
        MutableStatsInfo statsInfo = InfoFactoryHolder.get().createStats();
        statsInfo.setDownloadCount(1);
        statsInfo.setLastDownloaded(System.currentTimeMillis());
        statsInfo.setLastDownloadedBy(getAuthorizationService().currentUsername());
        localDownloads.add(statsInfo);
    }

    /**
     * Retrieve JCR content as input stream, and do working copy commit if needed. The consumer is responsible for
     * calling close() on the returned stream.
     *
     * @return null if deleted, the InputStream of the content of the file otherwise
     */
    public InputStream getStream() {
        try {
            Node node = getNode();
            Node resNode = JcrHelper.getResourceNode(node);
            Value attachedDataValue = resNode.getProperty(JCR_DATA).getValue();
            InputStream is = attachedDataValue.getBinary().getStream();
            return is;
        } catch (RepositoryException e) {
            Throwable notFound = ExceptionUtils.getCauseOfTypes(e,
                    MissingOrInvalidDataStoreRecordException.class, PathNotFoundException.class,
                    FileNotFoundException.class);
            if (notFound != null) {
                log.warn("Jcr file node {} does not have binary content!", getPath());
                if (ConstantValues.jcrAutoRemoveMissingBinaries.getBoolean()) {
                    log.warn("Auto-deleting item {}.", getPath());
                    bruteForceDelete(true);
                }
                return null;
            } else {
                throw new RepositoryRuntimeException(
                        "Failed to retrieve file node's " + getRepoPath() + " data stream.", e);
            }
        }
    }

    public long getSize() {
        return getInfo().getSize();
    }

    public boolean isDirectory() {
        return false;
    }

    public VfsItem save(VfsItem originalFsItem) {
        if (isDeleted()) {
            throw new IllegalStateException("Cannot save item " + getRepoPath() + " it is scheduled for deletion.");
        }
        checkMutable("save");
        // Save the main info only if new (original null, or main info non identical)
        if (originalFsItem == null || !originalFsItem.getInfo().isIdentical(getInfo())) {
            getInfoPersistenceHandler().update(this, getMutableInfo());
        }
        saveDirtyState();
        log.trace("Creating '{}' at {}.", getRepoPath(), new Date(getCreated()));
        return new JcrFile(getNode(), getRepo(), this);
    }

    @Override
    protected void saveDirtyState() {
        super.saveDirtyState();
        if (downloads != null) {
            LinkedList<StatsInfo> dumpTo = new LinkedList<StatsInfo>();
            downloads.drainTo(dumpTo);
            if (!dumpTo.isEmpty()) {
                MutableStatsInfo statsInfo = getMetadata(MutableStatsInfo.class);
                if (statsInfo == null) {
                    statsInfo = InfoFactoryHolder.get().createStats();
                }
                log.trace("Existing download count is {} and now adding another {}", statsInfo.getDownloadCount(),
                        dumpTo.size());
                statsInfo.setDownloadCount(statsInfo.getDownloadCount() + dumpTo.size());
                StatsInfo last = dumpTo.getLast();
                statsInfo.setLastDownloaded(last.getLastDownloaded());
                statsInfo.setLastDownloadedBy(last.getLastDownloadedBy());
                setMetadata(MutableStatsInfo.class, statsInfo);
                if (log.isTraceEnabled()) {
                    log.trace("Dumping stats with a download count of {} for {} from {}",
                            new Object[]{dumpTo.size(), getRepoPath(), Thread.currentThread().getName()});
                }
            }
        }
    }

    private synchronized BlockingQueue<StatsInfo> getOrCreateDownloads() {
        if (downloads == null) {
            downloads = new LinkedBlockingQueue<StatsInfo>();
        }
        return downloads;
    }

    @Override
    public boolean isIdentical(VfsItem item) {
        return item instanceof JcrFile && super.isIdentical(item);
    }

    @Override
    public boolean isDirty() {
        // The only dirty state in a JcrFile is the download queue
        return super.isDirty() || (downloads != null && !downloads.isEmpty());
    }

    public void exportTo(ExportSettings settings) {
        MutableStatusHolder status = settings.getStatusHolder();
        status.setDebug("Exporting file '" + getRelativePath() + "'...", log);
        File targetFile = new File(settings.getBaseDir(), getRelativePath());
        try {
            //Invoke the callback if exists
            settings.executeCallbacks(getRepoPath());

            File parentFile = targetFile.getParentFile();
            if (!parentFile.exists()) {
                FileUtils.forceMkdir(parentFile);
            }

            exportFileContent(targetFile, settings);

            if (settings.isIncludeMetadata()) {
                exportMetadata(targetFile, status, settings.isIncremental());
            }
            if (settings.isM2Compatible()) {
                writeChecksums(targetFile, getInfo().getChecksumsInfo(), getLastModified());
            }
            //If a file export fails, we collect the error but not fail the whole export
        } catch (FileNotFoundException e) {
            status.setError("Failed to export " + getAbsolutePath() + " since it is non-accessible.", e, log);
        } catch (Exception e) {
            status.setError("Failed to export " + getAbsolutePath() + " to file '" +
                    targetFile.getPath() + "'.", e, log);
        } finally {
            //Release the file read lock immediately
            LockingHelper.releaseReadLock(getRepoPath());
        }
    }

    private void exportFileContent(File targetFile, ExportSettings settings) throws IOException {
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
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }

        if (getLastModified() >= 0) {
            targetFile.setLastModified(getLastModified());
        }
    }

    public long length() {
        return getInfo().getSize();
    }

    public String[] list() {
        return null;
    }

    public String[] list(FilenameFilter filter) {
        return null;
    }

    public File[] listFiles() {
        return null;
    }

    public File[] listFiles(FilenameFilter filter) {
        return null;
    }

    public File[] listFiles(FileFilter filter) {
        return null;
    }

    public boolean mkdir() {
        return false;
    }

    public boolean mkdirs() {
        return false;
    }

    /**
     * OVERIDDEN FROM java.io.File END
     */

    public static boolean isFileNode(Node node) throws RepositoryException {
        NodeType primaryNodeType = node.getPrimaryNodeType();
        return StorageConstants.NT_ARTIFACTORY_FILE.equals(primaryNodeType.getName());
    }

    /**
     * Do not import metadata,index folders and checksums
     */
    public static boolean isStorable(String name) {
        return !name.endsWith(METADATA_FOLDER) && !NamingUtils.isChecksum(name);
    }

    private void setResourceNode(InputStream in) throws RepositoryException, IOException, ChecksumPolicyException {
        checkMutable("set-resource-node");
        MutableFileInfo mutableFileInfo = getMutableInfo();

        Node node = getNode();
        Node resourceNode = getJcrRepoService().getOrCreateNode(node, JCR_CONTENT, NT_RESOURCE);
        String name = getName();
        MimeType ct = NamingUtils.getMimeType(name);
        mutableFileInfo.setMimeType(ct.getType());

        /**
         * If it is an XML dde document save the XML in memory since marking does not always work on the remote stream,
         * and import its xml content into the repo for indexing.
         * Process the XML stream only if it's a real repo. Virtual repos don't needed the XML parsing and also may fall
         * On POM consistency checks
         */
        boolean processXml = getRepo().isReal() && (shouldIndexXmlFile(name, ct) || MavenNaming.isPom(name));
        if (processXml) {
            in = processXmlStream(node, resourceNode, name, in);
        }
        try {
            fillJcrData(resourceNode, in);
        } finally {
            //Make sure the replaced stream is closed (original stream is taken care of by caller)
            if (processXml) {
                IOUtils.closeQuietly(in);
            }
        }
    }

    private boolean shouldIndexXmlFile(String name, MimeType ct) {
        return ConstantValues.searchXmlIndexing.getBoolean() && (ct.isIndex() || IvyNaming.isIvyFileName(name));
    }

    /**
     * Method that will do all the extra processing when inserting an XML file in the repo.
     */
    private InputStream processXmlStream(Node node, Node resourceNode, String name, InputStream in)
            throws IOException, RepositoryException {

        in = copyToMarkableStream(in);

        //If it is a pom, verify that its groupId/artifactId/version match the dest path and check if it's a plugin
        if (MavenNaming.isPom(name)) {
            in.mark(Integer.MAX_VALUE);
            JcrFsItemFactory storingRepo = getRepo();
            boolean suppressPomConsistencyChecks = storingRepo.isSuppressPomConsistencyChecks();
            String relativePath = getRelativePath();
            ModuleInfo moduleInfo = storingRepo.getItemModuleInfo(relativePath);
            PomTargetPathValidator pomValidator = new PomTargetPathValidator(relativePath, moduleInfo);
            pomValidator.validate(in, suppressPomConsistencyChecks);
            if (pomValidator.isMavenPlugin()) {
                log.debug("Marking {} as maven plugin", getRepoPath());
                node.setProperty(StorageConstants.PROP_ARTIFACTORY_MAVEN_PLUGIN, true);
            }
            in.reset();
        }

        if (shouldIndexXmlFile(name, NamingUtils.getMimeType(name))) {
            in.mark(Integer.MAX_VALUE); // mark the start of the stream before reading
            resourceNode.setProperty(JCR_ENCODING, "utf-8");
            Node xmlNode = getJcrRepoService().getOrCreateUnstructuredNode(node, StorageConstants.NODE_ARTIFACTORY_XML);
            getJcrRepoService().saveXmlHierarchy(xmlNode, in);
            in.reset(); // reset the stream
        }
        return in;
    }

    /**
     * Copies the input stream to a new input stream that supports marking.
     *
     * @param in The input stream to consume and close
     * @return A stream that supports marking and holds the same data as the original input.
     */
    private InputStream copyToMarkableStream(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(in, bos);
        in.close();
        return new ByteArrayInputStream(bos.toByteArray());
    }

    private void fillJcrData(Node resourceNode, InputStream in) throws RepositoryException, ChecksumPolicyException {
        //Check if needs to create checksum and not checksum file
        log.debug("Calculating checksums of '{}'.", getRepoPath());
        Checksum[] checksums = JcrVfsHelper.getChecksumsToCompute();
        ChecksumInputStream checksumInputStream = new ChecksumInputStream(in, checksums);

        //Do this after xml import: since Jackrabbit 1.4
        //org.apache.jackrabbit.core.value.BLOBInTempFile.BLOBInTempFile will close the stream
        Binary binary = resourceNode.getSession().getValueFactory().createBinary(checksumInputStream);
        resourceNode.setProperty(JCR_DATA, binary);

        // make sure the stream is closed. Jackrabbit doesn't close the stream if the file is small (violating the contract)
        IOUtils.closeQuietly(checksumInputStream);  // if close is failed we'll fail in setting the checksums
        if (log.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            for (Checksum checksum : checksums) {
                sb.append(checksum.getType()).append(":").append(checksum.getChecksum()).append(" ");
            }
            log.trace("Calculated checksums of '{}': {}", getRepoPath(), sb.toString());
        }

        MutableFileInfo mutableFileInfo = getMutableInfo();
        // set the actual checksums on the file extra info
        setFileActualChecksums(mutableFileInfo, checksums);

        // apply the checksum policy
        JcrFsItemFactory repository = getRepo();
        ChecksumPolicy policy = repository.getChecksumPolicy();
        Set<ChecksumInfo> checksumInfos = mutableFileInfo.getChecksumsInfo().getChecksums();
        boolean passes = policy.verify(checksumInfos);
        if (!passes) {
            throw new ChecksumPolicyException(policy, checksumInfos, getName());
        }
        // The size needs to be always consistent so no rely on MD
        mutableFileInfo.setSize(JcrHelper.getLength(resourceNode));
    }

    private void setFileActualChecksums(FileInfo info, Checksum[] checksums) {
        ChecksumsInfo checksumsInfo = info.getChecksumsInfo();
        log.trace("Updating checksum info of '{}'. Current checksums: {}", getRepoPath(), checksumsInfo);
        for (Checksum checksum : checksums) {
            ChecksumType checksumType = checksum.getType();
            String calculatedChecksum = checksum.getChecksum();
            ChecksumInfo checksumInfo = checksumsInfo.getChecksumInfo(checksumType);
            if (checksumInfo != null) {
                // set the actual checksum
                String original = checksumInfo.isMarkedAsTrusted() ?
                        ChecksumInfo.TRUSTED_FILE_MARKER : checksumInfo.getOriginal();
                checksumInfo = new ChecksumInfo(checksumType, original, calculatedChecksum);
                checksumsInfo.addChecksumInfo(checksumInfo);
                if (!checksumInfo.checksumsMatch()) {
                    log.debug("Checksum mismatch {}. original: {} calculated: {}",
                            new String[]{checksumType.toString(), checksumInfo.getOriginal(), calculatedChecksum});
                }
            } else {
                log.debug(checksumType + " checksum info not found for '" + info.getRepoPath().getPath() +
                        ". Creating one with empty original checksum.");
                ChecksumInfo missingChecksumInfo = new ChecksumInfo(checksumType, null, calculatedChecksum);
                checksumsInfo.addChecksumInfo(missingChecksumInfo);
            }
        }
        log.trace("Updated checksum info of '{}'. Current checksums: {}", getRepoPath(), info.getChecksumsInfo());
    }

}
