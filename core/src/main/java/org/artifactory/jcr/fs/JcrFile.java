/*
 * This file is part of Artifactory.
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.ExportCallback;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.fs.FileAdditionalInfo;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FileInfoImpl;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.ChecksumInputStream;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.io.checksum.policy.ChecksumPolicyException;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.jackrabbit.DataStoreRecordNotFoundException;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.MavenModelUtils;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.RealRepoBase;
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
import java.util.Set;

import static org.apache.jackrabbit.JcrConstants.*;

/**
 * @author yoavl
 */
public class JcrFile extends JcrFsItem<FileInfo> {
    private static final Logger log = LoggerFactory.getLogger(JcrFile.class);

    public static final String NT_ARTIFACTORY_FILE = "artifactory:file";

    public JcrFile(RepoPath repoPath, StoringRepo repo) {
        super(repoPath, repo);
    }

    public JcrFile(JcrFile copy, StoringRepo repo) {
        super(copy, repo);
    }

    /**
     * Constructor used when reading JCR content and creating JCR file item from it. Will not create anything in JCR but
     * will read the JCR content of the node.
     *
     * @param fileNode the JCR node this file represent
     * @param repo
     * @throws RepositoryRuntimeException if the node a file or cannot be read
     */
    public JcrFile(Node fileNode, StoringRepo repo) {
        super(fileNode, repo);
    }

    @Override
    protected FileInfo createInfo(RepoPath repoPath) {
        return new FileInfoImpl(repoPath);
    }

    @Override
    protected FileInfo createInfo(FileInfo copy) {
        return new FileInfoImpl(copy);
    }

    @Override
    protected void updateInfoFromNode(Node node) {
        super.updateInfoFromNode(node);
        FileInfo fileInfo = getInfo();
        Node resNode = JcrHelper.getResourceNode(node);
        fileInfo.setSize(JcrHelper.getLength(resNode));
        fileInfo.setMimeType(JcrHelper.getMimeType(resNode));
        fileInfo.setLastModified(JcrHelper.getJcrLastModified(resNode));
        FileAdditionalInfo additionalInfo;
        additionalInfo = getXmlMetdataObject(FileAdditionalInfo.class);
        if (additionalInfo != null) {
            fileInfo.setAdditionalInfo(additionalInfo);
        }
    }

    @Override
    protected void updateNodeFromInfo() {
        super.updateNodeFromInfo();
        Node node = getNode();
        JcrHelper.setMimeType(node, getInfo().getMimeType());
    }

    /**
     * fill the data file from stream
     */
    public void fillData(InputStream in) throws IOException {
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
    public void fillData(long lastModified, InputStream is) throws IOException {
        checkMutable("fillData");
        try {
            createOrGetFileNode(getParentNode(), getName());
            setModifiedInfoFields(lastModified, System.currentTimeMillis());
            setResourceNode(is);
        } catch (IOException e) {
            throw e;    // rethrow ioexceptions
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
    protected Node createOrGetFileNode(Node parentNode, String name) {
        try {
            //Create the node, unless it already exists (ideally we'd remove the exiting node first,
            //but we can't until JCR-1554 is resolved)
            boolean exists = parentNode.hasNode(name);
            if (exists) {
                return parentNode.getNode(name);
            }
            //Create the file node
            Node node = parentNode.addNode(name, JcrFile.NT_ARTIFACTORY_FILE);
            //Create the metadata container
            createMetadataContainer();
            return node;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to create node '" + getAbsolutePath() + "'.", e);
        }
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
    public void importFrom(File file, ImportSettings settings) throws IOException {
        MultiStatusHolder status = settings.getStatusHolder();
        try {
            if (!file.isFile()) {
                String message = "Cannot import non existant file or directory '" + file.getAbsolutePath() +
                        "' into " + getRepoPath();
                status.setError("Import Error", new IllegalArgumentException(message), log);
                return;
            }

            createOrGetFileNode(getParentNode(), getName());

            updateFileInfoFromImportedFile(file);

            if (settings.isIncludeMetadata()) {
                importMetadata(file, status, settings);
            }

            // Save checksum for later compare (following method calls will change the info)
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
        ContentType ct = NamingUtils.getContentType(file.getName());
        String actualMimeType = ct.getMimeType();
        info.setMimeType(actualMimeType);
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
        checkMutable("setLastUpdated");
        getInfo().setLastUpdated(lastUpdated);
    }

    public void updateDownloadStats() {
        StatsInfo statsInfo = getMdService().getXmlMetadataObject(this, StatsInfo.class, true);
        statsInfo.setDownloadCount(statsInfo.getDownloadCount() + 1);
        statsInfo.setLastDownloaded(System.currentTimeMillis());
        statsInfo.setLastDownloadedBy(getAuthorizationService().currentUsername());
        getMdService().setXmlMetadata(this, statsInfo);
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
    public JcrFsItem save() {
        if (isDeleted()) {
            throw new IllegalStateException("Cannot save item " + getRepoPath() + " it is scheduled for deletion.");
        }
        checkMutable("save");
        updateNodeFromInfo();
        return new JcrFile(getNode(), getRepo());
    }

    @Override
    public boolean isIdentical(JcrFsItem item) {
        return item instanceof JcrFile && super.isIdentical(item);
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
            if (settings.hasCallback()) {
                ExportCallback callback = settings.getCallback();
                callback.callback(getRepoPath());
            }

            if (!targetFile.getParentFile().exists()) {
                FileUtils.forceMkdir(targetFile.getParentFile());
            }

            exportFileContent(targetFile, settings);

            if (settings.isIncludeMetadata()) {
                exportMetadata(targetFile, status, settings.isIncremental());
            }
            if (settings.isM2Compatible()) {
                writeChecksums(targetFile.getParentFile(), getInfo().getChecksumsInfo(), targetFile.getName(),
                        getLastModified());
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

    public void importInternalMetadata(MetadataDefinition definition, Object md) {
        // For the moment we support only FileInfo as transient MD
        if (definition.getMetadataName().equals(FileInfo.ROOT) && md instanceof FileInfo) {
            FileInfo importedFileInfo = (FileInfo) md;
            FileInfo info = getInfo();
            info.setAdditionalInfo(importedFileInfo.getInternalXmlInfo());
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

    @Override
    public boolean setLastModified(long time) {
        checkMutable("setLastModified");
        Node resourceNode = JcrHelper.getResourceNode(getNode());
        return JcrHelper.setLastModified(resourceNode, time);
    }

    public static boolean isFileNode(Node node) throws RepositoryException {
        NodeType primaryNodeType = node.getPrimaryNodeType();
        return JcrFile.NT_ARTIFACTORY_FILE.equals(primaryNodeType.getName());
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

    private void setResourceNode(InputStream in) throws RepositoryException, IOException {
        Node node = getNode();
        Node resourceNode = getJcrRepoService().getOrCreateNode(node, JCR_CONTENT, NT_RESOURCE);
        String name = getName();
        FileInfo info = getInfo();
        ContentType ct = NamingUtils.getContentType(name);
        info.setMimeType(ct.getMimeType());
        //If it is an XML document save the XML in memory since marking does not always work on the
        //remote stream, and import its xml content into the repo for indexing
        if (ct.isXml()) {
            in = processXmlStream(node, resourceNode, name, in);
        }
        try {
            fillJcrData(resourceNode, in);
        } finally {
            //Make sure the replaced stream is closed (orginal stream is taken care of by caller)
            if (ct.isXml()) {
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
        Node xmlNode = getJcrRepoService().getOrCreateUnstructuredNode(node, JcrService.NODE_ARTIFACTORY_XML);
        getJcrRepoService().importXml(xmlNode, in);
        //Reset the stream
        in.reset();
        return in;
    }

    private void fillJcrData(Node resourceNode, InputStream in) throws RepositoryException, IOException {

        //Check if needs to create checksum and not checksum file
        log.debug("Calculating checksums for '{}'.", getRepoPath());
        Checksum[] checksums = getChecksumsToCompute();
        ChecksumInputStream resourceInputStream = new ChecksumInputStream(in, checksums);

        //Do this after xml import: since Jackrabbit 1.4
        //org.apache.jackrabbit.core.value.BLOBInTempFile.BLOBInTempFile will close the stream
        resourceNode.setProperty(JCR_DATA, resourceInputStream);

        FileInfo info = getInfo();
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
                log.debug(checksumType + " checksum info not found for '" + info.getRepoPath().getPath() +
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

}
