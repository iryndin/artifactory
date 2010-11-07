/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.FileInfoImpl;
import org.artifactory.api.fs.InternalFileInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.fs.FileInfo;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.ChecksumInputStream;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.io.checksum.policy.ChecksumPolicyException;
import org.artifactory.ivy.IvyNaming;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.jcr.jackrabbit.DataStoreRecordNotFoundException;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.jcr.md.MetadataPersistenceHandler;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.MavenModelUtils;
import org.artifactory.mime.MimeType;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.RealRepoBase;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.jcr.JcrHelper;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import java.io.*;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.apache.jackrabbit.JcrConstants.*;

/**
 * @author yoavl
 */
public class JcrFile extends JcrFsItem<InternalFileInfo> {
    private static final Logger log = LoggerFactory.getLogger(JcrFile.class);

    private BlockingQueue<StatsInfo> downloads = null;

    public JcrFile(RepoPath repoPath, StoringRepo repo) {
        super(repoPath, repo);
    }

    public JcrFile(JcrFile copy, StoringRepo repo) {
        super(copy, repo);
        this.downloads = copy.downloads;
    }

    /**
     * Constructor used when reading JCR content and creating JCR file item from it. Will not create anything in JCR but
     * will read the JCR content of the node.
     *
     * @param fileNode  the JCR node this file represent
     * @param repo
     * @param downloads
     * @throws RepositoryRuntimeException if the node a file or cannot be read
     */
    public JcrFile(Node fileNode, StoringRepo repo, JcrFile original) {
        super(fileNode, repo, original);
        if (original != null) {
            this.downloads = original.downloads;
        }
    }

    @Override
    protected InternalFileInfo createInfo(RepoPath repoPath) {
        return new FileInfoImpl(repoPath);
    }

    @Override
    protected MetadataPersistenceHandler<InternalFileInfo> getInfoPersistenceHandler() {
        return getRepoGeneric().getFileInfoMd().getPersistenceHandler();
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
        return JcrHelper.getOrCreateNode(parentNode, name, JcrTypes.NT_ARTIFACTORY_FILE, JcrTypes.MIX_ARTIFACTORY_BASE);
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
        MultiStatusHolder status = settings.getStatusHolder();
        try {
            if (!file.isFile()) {
                String message = "Cannot import non existant file or directory '" + file.getAbsolutePath() +
                        "' into " + getRepoPath();
                status.setError("Import Error", new IllegalArgumentException(message), log);
                return;
            }

            getOrCreateFileNode(getParentNode(), getName());

            updateFileInfoFromImportedFile(file);

            if (settings.isIncludeMetadata()) {
                importMetadata(file, status, settings);
            }

            // trust server checksums if the settings said so and client checksum is missing
            if (settings.isTrustServerChecksums()) {
                Set<ChecksumInfo> checksums = getInfo().getChecksumsInfo().getChecksums();
                if (checksums == null || checksums.isEmpty()) {
                    getInfo().createTrustedChecksums();
                } else {
                    Set<ChecksumInfo> checksumsCopy = Sets.newHashSet(checksums);
                    for (ChecksumInfo checksum : checksumsCopy) {
                        if (StringUtils.isBlank(checksum.getOriginal())) {
                            // replace with checksum that contains trusted mark, but same actual
                            getInfo().addChecksumInfo(new ChecksumInfo(
                                    checksum.getType(), ChecksumInfo.TRUSTED_FILE_MARKER, checksum.getActual()));
                        }
                    }
                }
            }

            // Save actual checksum from metadata/old file for later comparison with actual of newly uploaded file
            String md5 = getInfo().getMd5();
            String sha1 = getInfo().getSha1();
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

    private void updateFileInfoFromImportedFile(File file) {
        FileInfo info = getInfo();
        info.setLastModified(file.lastModified());
        info.setLastUpdated(System.currentTimeMillis());
        info.setCreatedBy(getAuthorizationService().currentUsername());
        info.setModifiedBy(getAuthorizationService().currentUsername());
        info.setSize(file.length());
        MimeType ct = NamingUtils.getMimeType(file.getName());
        String actualMimeType = ct.getType();
        info.setMimeType(actualMimeType);
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
        StatsInfo statsInfo = new StatsInfo();
        statsInfo.setDownloadCount(1);
        statsInfo.setLastDownloaded(System.currentTimeMillis());
        statsInfo.setLastDownloadedBy(getAuthorizationService().currentUsername());
        localDownloads.add(statsInfo);
    }

    /**
     * Retrieve JCR content, and do working copy commit if needed.
     *
     * @return null if deleted, the InputStream of the content of the file otherwise
     */
    public InputStream getStream() {
        try {
            Node node = getNode();
            Node resNode = JcrHelper.getResourceNode(node);
            Value attachedDataValue = resNode.getProperty(JCR_DATA).getValue();
            InputStream is = attachedDataValue.getStream();
            return is;
        } catch (RepositoryException e) {
            Throwable notFound = ExceptionUtils.getCauseOfTypes(e,
                    DataStoreRecordNotFoundException.class, PathNotFoundException.class, FileNotFoundException.class);
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

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public JcrFsItem save(JcrFsItem originalFsItem) {
        if (isDeleted()) {
            throw new IllegalStateException("Cannot save item " + getRepoPath() + " it is scheduled for deletion.");
        }
        checkMutable("save");
        // Save the main info only if new (original null, or main info non identical)
        if (originalFsItem == null || !originalFsItem.getInfo().isIdentical(getInfo())) {
            getInfoPersistenceHandler().update(this, getInfo());
        }
        saveDirtyState();
        return new JcrFile(getNode(), getRepo(), this);
    }

    @Override
    protected void saveDirtyState() {
        super.saveDirtyState();
        if (downloads != null) {
            LinkedList<StatsInfo> dumpTo = new LinkedList<StatsInfo>();
            downloads.drainTo(dumpTo);
            if (!dumpTo.isEmpty()) {
                StatsInfo statsInfo = getMetadata(StatsInfo.class);
                if (statsInfo == null) {
                    statsInfo = new StatsInfo();
                }
                statsInfo.setDownloadCount(statsInfo.getDownloadCount() + dumpTo.size());
                StatsInfo last = dumpTo.getLast();
                statsInfo.setLastDownloaded(last.getLastDownloaded());
                statsInfo.setLastDownloadedBy(last.getLastDownloadedBy());
                setMetadata(StatsInfo.class, statsInfo);
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
    public boolean isIdentical(JcrFsItem item) {
        return item instanceof JcrFile && super.isIdentical(item);
    }

    @Override
    public boolean isDirty() {
        // The only dirty state in a JcrFile is the download queue
        return super.isDirty() || (downloads != null && !downloads.isEmpty());
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

    public void exportTo(ExportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        status.setDebug("Exporting file '" + getRelativePath() + "'...", log);
        File targetFile = new File(settings.getBaseDir(), getRelativePath());
        try {
            //Invoke the callback if exists
            settings.executeCallbacks(getRepoPath());

            if (!targetFile.getParentFile().exists()) {
                FileUtils.forceMkdir(targetFile.getParentFile());
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
            status.setError("Failed to export " + getAbsolutePath() + " since it was deleted.", log);
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

    @Override
    public long length() {
        return getInfo().getSize();
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

    public static boolean isFileNode(Node node) throws RepositoryException {
        NodeType primaryNodeType = node.getPrimaryNodeType();
        return JcrTypes.NT_ARTIFACTORY_FILE.equals(primaryNodeType.getName());
    }

    /**
     * Do not import metadata,index folders and checksums
     */
    public static boolean isStorable(String name) {
        return !name.endsWith(METADATA_FOLDER) && !NamingUtils.isChecksum(name);
    }

    /**
     * OVERIDDEN FROM java.io.File END
     */

    private void setResourceNode(InputStream in) throws RepositoryException, IOException, ChecksumPolicyException {
        Node node = getNode();
        Node resourceNode = getJcrRepoService().getOrCreateNode(node, JCR_CONTENT, NT_RESOURCE);
        String name = getName();
        org.artifactory.fs.FileInfo info = getInfo();
        MimeType ct = NamingUtils.getMimeType(name);
        info.setMimeType(ct.getType());
        /**
         * If it is an XML document save the XML in memory since marking does not always work on the remote stream, and
         * import its xml content into the repo for indexing.
         * Process the XML stream only if it's a real repo. Virtual repos don't needed the XML parsing and also may fall
         * On POM consistency checks
         */
        if (getRepo().isReal() && (ct.isIndex() || IvyNaming.isIvyFileName(name))) {
            in = processXmlStream(node, resourceNode, name, in);
        }
        try {
            fillJcrData(resourceNode, in);
        } finally {
            //Make sure the replaced stream is closed (original stream is taken care of by caller)
            if (ct.isIndex() || IvyNaming.isIvyFileName(name)) {
                IOUtils.closeQuietly(in);
            }
        }
    }

    /**
     * Method that will do all the extra processing when inserting an XML file in the repo.
     */
    private InputStream processXmlStream(Node node, Node resourceNode, String name, InputStream in)
            throws IOException, RepositoryException {
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
            boolean suppressPomConsistencyChecks = false;
            StoringRepo storingRepo = getRepo();
            if (storingRepo instanceof RealRepoBase) {
                RealRepoDescriptor descriptor;
                if (storingRepo.isCache()) {
                    //If its a cache repo, need to get the local CacheRepoDescriptor to check the pom consistency check suppression.
                    descriptor = ((LocalCacheRepo) storingRepo).getDescriptor().getRemoteRepo();
                } else {
                    // if its a real repo (must be for poms) use the descriptor value`
                    descriptor = ((RealRepoBase) storingRepo).getDescriptor();
                }
                suppressPomConsistencyChecks = descriptor.isSuppressPomConsistencyChecks();
            }
            MavenModelUtils.validatePomTargetPath(in, getRelativePath(), suppressPomConsistencyChecks);
            in.reset();
        }
        Node xmlNode = getJcrRepoService().getOrCreateUnstructuredNode(node, JcrTypes.NODE_ARTIFACTORY_XML);
        getJcrRepoService().saveXmlHierarchy(xmlNode, in);
        //Reset the stream
        in.reset();
        return in;
    }

    private void fillJcrData(Node resourceNode, InputStream in) throws RepositoryException, ChecksumPolicyException {

        //Check if needs to create checksum and not checksum file
        log.debug("Calculating checksums for '{}'.", getRepoPath());
        Checksum[] checksums = getChecksumsToCompute();
        ChecksumInputStream resourceInputStream = new ChecksumInputStream(in, checksums);

        //Do this after xml import: since Jackrabbit 1.4
        //org.apache.jackrabbit.core.value.BLOBInTempFile.BLOBInTempFile will close the stream
        resourceNode.setProperty(JCR_DATA, resourceInputStream);

        org.artifactory.fs.FileInfo info = getInfo();
        // set the actual checksums on the file extra info
        setFileActualChecksums(info, checksums);

        // apply the checksum policy
        StoringRepo repository = getRepo();
        ChecksumPolicy policy = repository.getChecksumPolicy();
        Set<ChecksumInfo> checksumInfos = info.getChecksums();
        boolean passes = policy.verify(checksumInfos);
        if (!passes) {
            throw new ChecksumPolicyException(policy, checksumInfos, getName());
        }
        // The size needs to be always consistent so no rely on MD
        info.setSize(JcrHelper.getLength(resourceNode));
    }

    private void setFileActualChecksums(org.artifactory.fs.FileInfo info, Checksum[] checksums) {
        ChecksumsInfo checksumsInfo = info.getChecksumsInfo();
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
                ChecksumInfo
                        missingChecksumInfo = new ChecksumInfo(checksumType, null, calculatedChecksum);
                checksumsInfo.addChecksumInfo(missingChecksumInfo);
            }
        }
    }

    private Checksum[] getChecksumsToCompute() {
        ChecksumType[] checksumTypes = ChecksumType.values();
        Checksum[] checksums = new Checksum[checksumTypes.length];
        for (int i = 0; i < checksumTypes.length; i++) {
            checksums[i] = new Checksum(checksumTypes[i]);
        }
        return checksums;
    }

}
