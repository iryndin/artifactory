/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.replication.ReplicationAddon;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.fs.ItemInfo;
import org.artifactory.fs.MetadataEntryInfo;
import org.artifactory.fs.MutableItemInfo;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.factory.JcrFsItemFactory;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.jcr.md.MetadataAware;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.jcr.md.MetadataDefinitionService;
import org.artifactory.jcr.md.MetadataPersistenceHandler;
import org.artifactory.jcr.md.XmlMetadataProvider;
import org.artifactory.jcr.spring.StorageContextHelper;
import org.artifactory.jcr.utils.JcrHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.MetadataInfo;
import org.artifactory.md.Properties;
import org.artifactory.md.PropertiesInfo;
import org.artifactory.mime.MavenNaming;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.interceptor.StorageInterceptors;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.data.VfsNode;
import org.artifactory.sapi.fs.MetadataReader;
import org.artifactory.sapi.fs.VfsFolder;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.sapi.fs.VfsService;
import org.artifactory.sapi.fs.Visitor;
import org.artifactory.security.AccessLogger;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.artifactory.storage.StorageConstants.NODE_ARTIFACTORY_METADATA;

/**
 * Metadata structure:
 * <p/>
 * x.jar/art:md/art.file/
 * <p/>
 * ......................art:xml/
 * <p/>
 * ..............................art.file/
 * <p/>
 * ......................................lastModified/
 * <p/>
 * ......................jcr:content (optional)/
 * <p/>
 * ..................................some-meta-data
 * <p/>
 * x.jar/art:md/art.stats/
 * <p/>
 * ......................art:xml/
 * <p/>
 * ..............................art.stats/
 */
public abstract class JcrFsItem<T extends ItemInfo, MT extends MutableItemInfo>
        extends File implements VfsItem<T, MT>, MetadataAware {
    private static final Logger log = LoggerFactory.getLogger(JcrFsItem.class);

    private final String absPath;
    private final String uuid;
    private final T info;
    private final boolean mutable;
    private boolean deleted;

    /**
     * The repo that manage/contains this item.
     * NOTE: This member should be the only transient member, and the only one
     * which points to global configuration objects (services, ...)
     */
    private transient JcrFsItemFactory repo;

    /**
     * The queue that receive all metadata object that need to be saved at the end of a write lock transaction.
     */
    protected BlockingQueue<SetMetadataMessage> metadataToSave = null;

    protected void saveDirtyState() {
        if (metadataToSave != null) {
            LinkedList<SetMetadataMessage> dumpTo = new LinkedList<SetMetadataMessage>();
            metadataToSave.drainTo(dumpTo);
            if (!dumpTo.isEmpty()) {
                Map<String, String> toSave = Maps.newLinkedHashMap();
                for (SetMetadataMessage message : dumpTo) {
                    toSave.put(message.metadataName, message.xmlContent);
                }
                for (Map.Entry<String, String> entry : toSave.entrySet()) {
                    setXmlMetadata(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    protected static class SetMetadataMessage {
        final long threadId;
        final String metadataName;
        // TODO: [by fs] support Metadata object
        final String xmlContent;

        private SetMetadataMessage(String metadataName, String xmlContent) {
            if (metadataName == null) {
                throw new IllegalArgumentException("Cannot create a metadata message with no name!");
            }
            // TODO: [by fs] Visible immediately to all threads waiting to move the queue to SessionRessource
            this.threadId = 1;
            //this.threadId = Thread.currentThread().getId();
            this.metadataName = metadataName;
            this.xmlContent = xmlContent;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            SetMetadataMessage that = (SetMetadataMessage) o;

            if (threadId != that.threadId) {
                return false;
            }
            if (!metadataName.equals(that.metadataName)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (threadId ^ (threadId >>> 32));
            result = 31 * result + metadataName.hashCode();
            return result;
        }
    }

    /**
     * Copy constructor used to create a mutable version of this fsItem out of JCR based one.
     *
     * @param copy A JCR based fsItem to copy
     * @param repo The local repo that this fsItem belongs to
     */
    public JcrFsItem(JcrFsItem<T, MT> copy, JcrFsItemFactory repo) {
        // TODO: Sanity check that copy is immutable and repo is the same than in copy
        super(copy.getPath());
        this.absPath = copy.absPath;
        this.repo = repo;
        this.uuid = copy.uuid;
        this.mutable = true;
        this.info = (T) getInfoPersistenceHandler().copy(copy.info);
        this.metadataToSave = copy.metadataToSave;
    }

    /**
     * Constructor of a new fsItem out of a repo path
     *
     * @param repoPath The key of this fsItem
     * @param repo     The local repo that this fsItem belongs to
     */
    public JcrFsItem(RepoPath repoPath, JcrFsItemFactory repo) {
        super(PathFactoryHolder.get().getAbsolutePath(repoPath));
        this.repo = repo;
        this.absPath = PathUtils.formatPath(super.getPath());
        this.info = createInfo(repoPath);
        this.uuid = null;
        this.mutable = true;
    }

    public JcrFsItem(final VfsNode node, JcrFsItemFactory repo) {
        super(node.absolutePath());
        this.repo = repo;
        this.uuid = node.storageId();
        this.absPath = node.absolutePath();
        final RepoPath repoPath = PathFactoryHolder.get().getRepoPath(node.absolutePath());
        if (repoPath == null) {
            //Item does not exist in a current repo
            throw new ItemNotFoundRuntimeException("No valid fs item exists in path '" + absPath + "'.");
        }
        /*
        VfsMetadataAware dummyNodeWrapper = new VfsMetadataAware() {
            public String getAbsolutePath() {
                return absPath;
            }

            public RepoPath getRepoPath() {
                return repoPath;
            }

            public Node getNode() {
                return node;
            }

            public boolean isFile() {
                return !folder;
            }

            public boolean isDirectory() {
                return folder;
            }
        };
        this.info = getInfoPersistenceHandler().read(dummyNodeWrapper);
        */
        this.info = null;
        this.mutable = false;
    }

    /**
     * Constructor used when reading JCR content and creating JCR file system item from it. Will not create anything in
     * JCR but will read the JCR content of the node.
     *
     * @param node the JCR node this item represent
     * @param repo The local repo that this fsItem belongs to
     * @throws RepositoryRuntimeException if the node info cannot be read
     */
    public JcrFsItem(final Node node, JcrFsItemFactory repo, JcrFsItem<T, MT> original) {
        super(JcrHelper.getAbsolutePath(node));
        this.repo = repo;
        this.uuid = JcrHelper.getUuid(node);
        this.absPath = PathUtils.formatPath(super.getPath());
        final RepoPath repoPath = PathFactoryHolder.get().getRepoPath(absPath);
        if (repoPath == null) {
            //Item does not exist in a current repo
            throw new ItemNotFoundRuntimeException("No valid fs item exists in path '" + absPath + "'.");
        }
        if (original != null) {
            // Copy the dirty state queue
            this.metadataToSave = original.metadataToSave;
        }
        final boolean folder = isDirectory();
        MetadataAware dummyNodeWrapper = new MetadataAware() {
            @Override
            public String getAbsolutePath() {
                return absPath;
            }

            @Override
            public RepoPath getRepoPath() {
                return repoPath;
            }

            @Override
            public Node getNode() {
                return node;
            }

            @Override
            public boolean isFile() {
                return !folder;
            }

            @Override
            public boolean isDirectory() {
                return folder;
            }
        };
        this.info = getInfoPersistenceHandler().read(dummyNodeWrapper);
        this.mutable = false;
    }

    protected abstract T createInfo(RepoPath repoPath);

    protected abstract MetadataPersistenceHandler<T, MT> getInfoPersistenceHandler();

    @Override
    public boolean isMutable() {
        return mutable;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public final JcrFsItemFactory getRepo() {
        if (repo == null) {
            repo = StorageContextHelper.get().storingRepositoryByKey(getRepoKey());
        }
        return repo;
    }

    /**
     * Updating all the fields in Info when an update is done. This method will always change the modifiedBy to the
     * current user, set the createdBy if needed, and update the timestamps with the values passed.
     *
     * @param modified The new last modified timestamp
     * @param updated  The new last updated timestamp (internal value for up-to-date in cache)
     */
    @Override
    public final void setModifiedInfoFields(long modified, long updated) {
        checkMutable("setModifiedInfoFields");
        MutableItemInfo mInfo = getMutableInfo();
        mInfo.setLastModified(modified);
        mInfo.setLastUpdated(updated);
        String userId = getAuthorizationService().currentUsername();
        mInfo.setModifiedBy(userId);
        if (mInfo.getCreatedBy() == null) {
            mInfo.setCreatedBy(userId);
        }
    }

    @Override
    public String getName() {
        return info.getName();
    }

    /**
     * Get the absolute path of the item
     */
    @Override
    public String getAbsolutePath() {
        return absPath;
    }

    /**
     * Get the relative path of the item
     */
    @Override
    public String getRelativePath() {
        return getRepoPath().getPath();
    }

    @Override
    public long getCreated() {
        return getInfo().getCreated();
    }

    @Override
    public RepoPath getRepoPath() {
        return getInfo().getRepoPath();
    }

    @Override
    public String getRepoKey() {
        return getRepoPath().getRepoKey();
    }

    @Override
    public boolean delete() {
        checkMutable("delete");
        StorageInterceptors interceptors = StorageContextHelper.get().beanForType(StorageInterceptors.class);
        BasicStatusHolder statusHolder = new BasicStatusHolder();
        statusHolder.setFastFail(true);
        interceptors.beforeDelete(this, statusHolder);
        setDeleted(true);
        return getJcrRepoService().delete(this);
    }

    @Override
    public void bruteForceDelete() {
        bruteForceDelete(false);
    }

    public void bruteForceDelete(boolean nonTxNodeRemoval) {
        JcrSession session = null;
        try {
            if (nonTxNodeRemoval) {
                session = StorageContextHelper.get().getJcrService().getUnmanagedSession();
            } else {
                session = getSession();
            }
            Node node = getNode(session);
            if (node != null) {
                node.remove();
                if (nonTxNodeRemoval) {
                    session.save();
                }
            }
        } catch (Exception e) {
            log.warn("Could not brute force delete node: {}", e.getMessage());
            log.debug("Could not brute force delete node", e);
        } finally {
            if (nonTxNodeRemoval && session != null) {
                session.logout();
            }
            setDeleted(true);
            updateCache();
            LockingHelper.removeLockEntry(getRepoPath());
        }
    }

    /**
     * @return Parent folder of this folder or null if parent doesn't exist
     */
    @Override
    public VfsFolder getParentFolder() {
        RepoPath parentRepoPath = getRepoPath().getParent();
        if (parentRepoPath == null) {
            return null;
        } else {
            return getRepo().getJcrFolder(parentRepoPath);
        }
    }

    /**
     * @return Parent folder of this folder with write lock or null if parent doesn't exist
     */
    @Override
    public VfsFolder getLockedParentFolder() {
        RepoPath parentRepoPath = getRepoPath().getParent();
        if (parentRepoPath == null) {
            return null;
        } else {
            return getRepo().getLockedJcrFolder(parentRepoPath, false);
        }
    }

    /**
     * @param degree The degree of the ancestor (1 - parent, 2 - grandparent, etc)
     * @return Returns the n-th ancestor of this item. Null if doesn't exist.
     */
    @Override
    public VfsFolder getAncestor(int degree) {
        if (degree < 1) {
            throw new IllegalArgumentException("Ancestor degree must be greater than 1");
        }

        VfsFolder result = getParentFolder();   // first ancestor
        for (int i = degree - 1; i > 0 && result != null; i--) {
            result = result.getParentFolder();
        }
        return result;
    }

    /**
     * OVERIDDEN FROM FILE BEGIN
     */
    @Override
    public String getParent() {
        return PathUtils.getParent(getAbsolutePath());
    }

    @Override
    public File getParentFile() {
        return (File) getParentFolder();
    }

    @Override
    public boolean isAbsolute() {
        return true;
    }

    @Override
    public File getAbsoluteFile() {
        return this;
    }

    @Override
    public String getCanonicalPath() throws IOException {
        return getAbsolutePath();
    }

    @Override
    public File getCanonicalFile() throws IOException {
        return this;
    }

    @Override
    public boolean canRead() {
        return getAuthorizationService().canRead(getRepoPath());
    }

    @Override
    public boolean canWrite() {
        return getAuthorizationService().canDeploy(getRepoPath());
    }

    @Override
    public boolean exists() {
        JcrSession session = getSession();
        return exists(session);
    }

    public final boolean exists(JcrSession session) {
        //Cannot check by uuid since the item mey have been trashed (so it does exist in the trash)
        String absPath = getAbsolutePath();
        return session.itemExists(absPath);
    }

    @Override
    public boolean isFile() {
        return !isDirectory();
    }

    @Override
    public boolean isHidden() {
        return getName().startsWith(".");
    }

    @Override
    public boolean createNewFile() throws IOException {
        return false;
    }

    @Override
    public void deleteOnExit() {
        throw new UnsupportedOperationException("deleteOnExit() is not supported for jcr.");
    }

    @Override
    public boolean renameTo(File dest) {
        throw new UnsupportedOperationException("renameTo() is not supported for jcr.");
    }

    @Override
    public boolean setReadOnly() {
        return false;
    }

    @Override
    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return false;
    }

    @Override
    public boolean setWritable(boolean writable) {
        return false;
    }

    @Override
    public boolean setReadable(boolean readable, boolean ownerOnly) {
        return false;
    }

    @Override
    public boolean setReadable(boolean readable) {
        return false;
    }

    @Override
    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        return false;
    }

    @Override
    public boolean setExecutable(boolean executable) {
        return false;
    }

    @Override
    public boolean canExecute() {
        return false;
    }

    @Override
    public long getTotalSpace() {
        throw new UnsupportedOperationException("getTotalSpace() is not supported for jcr.");
    }

    @Override
    public long getFreeSpace() {
        throw new UnsupportedOperationException("getFreeSpace() is not supported for jcr.");
    }

    @Override
    public long getUsableSpace() {
        throw new UnsupportedOperationException("getUsableSpace() is not supported for jcr.");
    }

    @Override
    public int compareTo(File item) {
        return getName().compareTo(item.getName());
    }

    @Override
    public String toString() {
        return absPath;
    }

    @Override
    public String getPath() {
        return absPath;
    }

    @SuppressWarnings({"deprecation"})
    @Override
    @Deprecated
    public URL toURL() throws MalformedURLException {
        return new URL("jcr", "", absPath);
    }

    @Override
    public URI toURI() {
        try {
            return new URI("jcr", null, absPath, null);
        } catch (URISyntaxException x) {
            throw new Error(x);// Can't happen
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JcrFsItem)) {
            return false;
        }
        JcrFsItem item = (JcrFsItem) o;
        return absPath.equals(item.absPath);
    }

    @Override
    public int hashCode() {
        return absPath.hashCode();
    }

    @Override
    public final long lastModified() {
        return info.getLastModified();
    }

    @Override
    public abstract long length();

    @Override
    public abstract String[] list();

    @Override
    public abstract String[] list(FilenameFilter filter);

    @Override
    public abstract File[] listFiles();

    @Override
    public abstract File[] listFiles(FilenameFilter filter);

    @Override
    public abstract File[] listFiles(FileFilter filter);

    @Override
    public abstract boolean mkdir();

    @Override
    public abstract boolean mkdirs();

    @Override
    public final boolean setLastModified(long time) {
        checkMutable("setLastModified");
        ((MutableItemInfo) info).setLastModified(time);
        return true;
    }

    /**
     * OVERIDDEN FROM FILE END
     */

    @Override
    public abstract boolean isDirectory();

    @Override
    public boolean isFolder() {
        return isDirectory();
    }

    @Override
    public final T getInfo() {
        return info;
    }

    @Override
    public final MT getMutableInfo() {
        checkMutable("generic-get-mutable");
        return (MT) info;
    }

    @Override
    public final Node getNode() {
        JcrSession session = getSession();
        return getNode(session);
    }

    public final Node getNode(JcrSession session) {
        if (uuid != null) {
            try {
                return session.getNodeByUUID(uuid);
            } catch (RepositoryRuntimeException e) {
                Throwable cause = ExceptionUtils
                        .unwrapThrowablesOfTypes(e, RepositoryRuntimeException.class, RepositoryException.class);
                if (cause instanceof ItemNotFoundException) {
                    log.debug("Could not find item with uuid " + uuid + ". Item might have been trashed.");
                } else {
                    throw e;
                }
            }
        }
        return (Node) session.getItem(absPath);
    }

    @Override
    public void accept(Visitor<VfsItem> visitor) {
        visitor.visit(this);
    }

    protected Node getParentNode() {
        JcrSession session = getSession();
        String absPath = PathUtils.getParent(getAbsolutePath());
        return (Node) session.getItem(absPath);
    }

    protected JcrSession getSession() {
        return getJcrRepoService().getManagedSession();
    }

    protected JcrRepoService getJcrRepoService() {
        return StorageContextHelper.get().getJcrRepoService();
    }

    protected AuthorizationService getAuthorizationService() {
        return StorageContextHelper.get().getAuthorizationService();
    }

    protected ReplicationAddon getReplicationAddon() {
        AddonsManager addonsManager = StorageContextHelper.get().beanForType(AddonsManager.class);
        return addonsManager.addonByType(ReplicationAddon.class);
    }

    /**
     * Export all metadata as the real xml content (jcr:data, including comments etc.) into a
     * {item-name}.artifactory-metadata folder, where each metadata is named {metadata-name}.xml
     */
    protected void exportMetadata(File targetPath, MutableStatusHolder status, boolean incremental) {
        try {
            File metadataFolder = getMetadataContainerFolder(targetPath);
            try {
                FileUtils.forceMkdir(metadataFolder);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to create metadata folder '" + metadataFolder.getPath() + "'.", e);
            }
            // Save all metadata
            getJcrRepoService().writeMetadataEntries(this, status, metadataFolder, incremental);
        } catch (Exception e) {
            status.setError("Failed to export metadata for '" + getAbsolutePath() + "'.", e, log);
        }
    }

    /**
     * Write the checksum files next to the file which they belong to.
     *
     * @param targetFile    The file the checksums belong to
     * @param checksumsInfo The checksum info
     * @param modified      Last modify date to use
     * @throws IOException If failed to create the checksum files
     */
    protected void writeChecksums(File targetFile, ChecksumsInfo checksumsInfo, long modified)
            throws IOException {
        for (ChecksumInfo checksumInfo : checksumsInfo.getChecksums()) {
            File checksumFile = new File(targetFile + checksumInfo.getType().ext());
            FileUtils.writeStringToFile(checksumFile, checksumInfo.getActual(), "utf-8");
            if (modified > 0) {
                checksumFile.setLastModified(modified);
            }
        }
    }

    @Override
    public void writeMetadataEntries(MutableStatusHolder status, File metadataFolder, boolean incremental) {
        File metadataFile;
        Set<MetadataDefinition<?, ?>> metadataDefinitions;
        try {
            metadataDefinitions = getExistingMetadata(true);
        } catch (RepositoryRuntimeException e) {
            status.setError("Unable to retrieve existing metadata definitions for node " + getAbsolutePath() +
                    ". Skipping metadata entry writing.", e, log);
            return;
        }
        for (MetadataDefinition<?, ?> definition : metadataDefinitions) {
            MetadataPersistenceHandler<?, ?> mdph = definition.getPersistenceHandler();
            String metadataName = definition.getMetadataName();
            // add .xml prefix to all metadata files
            String fileName = metadataName + ".xml";
            metadataFile = new File(metadataFolder, fileName);
            long lastModified = lastModified();
            Object metadata = mdph.read(this);
            MetadataInfo metadataInfo = mdph.getMetadataInfo(this);
            if (metadataInfo != null) {
                lastModified = metadataInfo.getLastModified();
            }
            if (incremental && isFileNewerOrEquals(metadataFile, lastModified, metadataName)) {
                // incremental export and file system is current. skip
                continue;
            }
            XmlMetadataProvider<Object, Object> xmlProvider = (XmlMetadataProvider<Object, Object>) definition.getXmlProvider();
            writeFile(status, metadataFile, xmlProvider.toXml(metadata), lastModified);
        }
    }

    /**
     * @param metadataFile The metadata file to check if newer or equals (file might not exist)
     * @param lastModified The last modified timestamp of the metadata
     * @param metadataName The metadata name
     * @return True if the metadata file exists and its lsatModified is newer or equals to the input
     */
    private boolean isFileNewerOrEquals(File metadataFile, long lastModified, String metadataName) {
        if (metadataFile.exists() && lastModified <= metadataFile.lastModified()) {
            log.debug("Skipping not modified metadata {} of {}", metadataName, getPath());
            return true;
        }
        return false;
    }

    protected void writeFile(MutableStatusHolder status, File metadataFile, String xmlData, long modified) {
        if (StringUtils.isBlank(xmlData)) {
            return;
        }
        try {
            FileUtils.writeStringToFile(metadataFile, xmlData, "utf-8");
            if (modified >= 0) {
                metadataFile.setLastModified(modified);
            }
        } catch (Exception e) {
            status.setError("Failed to export xml metadata to '" + metadataFile.getPath() + "' from '" +
                    this + "'.", e, log);
            status.setError("Removing '" + metadataFile.getPath() + "'.", log);
            FileUtils.deleteQuietly(metadataFile);
        }
    }

    protected boolean importMetadata(File sourcePath, MutableStatusHolder status, ImportSettings settings) {
        try {
            File metadataFolder = getMetadataContainerFolder(sourcePath);
            if (!metadataFolder.exists()) {
                // If root folder of repository do not send a warning
                if (getRelativePath().length() > 1) {
                    String msg = "No metadata files found for '" + sourcePath.getAbsolutePath() +
                            "' during import into " + getRepoPath();
                    status.setWarning(msg, log);
                }
                return false;
            }

            MetadataReader metadataReader = getVfsDataService().fillBestMatchMetadataReader(settings, metadataFolder);
            List<MetadataEntryInfo> metadataEntries = metadataReader.getMetadataEntries(metadataFolder, settings,
                    status);
            for (MetadataEntryInfo entry : metadataEntries) {
                String metadataName = entry.getMetadataName();
                MetadataDefinition definition = getMdService().getMetadataDefinition(metadataName, true);
                MetadataPersistenceHandler mdph = definition.getPersistenceHandler();
                /* TODO: manage import/export condition in xmlprovider or persistence handler?
                if (!mdph.shouldImport()) {
                    continue;
                }
                */
                if (MavenNaming.MAVEN_METADATA_NAME.equals(metadataName)) {
                    // maven metadata is recalculated for local repos, so only import when importing to cache repo
                    if (!getRepo().isCache()) {
                        continue;
                    }
                }
                Object metadata = definition.getXmlProvider().fromXml(entry.getXmlContent());
                if (mdph == getInfoPersistenceHandler()) {
                    updateTimestampsFromImport((MutableItemInfo) metadata, (MutableItemInfo) info);
                    // Just update the info object without saving to JCR since it'll be done on save
                    ((MutableItemInfo) info).merge((MutableItemInfo) metadata);
                } else {
                    mdph.update(this, metadata);
                }
            }
        } catch (Exception e) {
            String msg =
                    "Failed to import metadata of " + sourcePath.getAbsolutePath() + " into '" + getRepoPath() + "'.";
            status.setError(msg, e, log);
            return false;
        }

        return true;
    }

    private VfsService getVfsDataService() {
        return StorageContextHelper.get().beanForType(VfsService.class);
    }

    protected File getMetadataContainerFolder(File targetFile) {
        return new File(targetFile.getParentFile(), targetFile.getName() + METADATA_FOLDER);
    }

    @Override
    public void updateCache() {
        // Nullify the only transient field the repo which contains the cache itself :)
        repo = null;
        // Then doing getRepo() will ensure I get the latest repo cache for me.
        JcrFsItemFactory storingRepo = getRepo();
        if (storingRepo != null) {
            storingRepo.updateCache(this);
        } else {
            // If repo is null, may be we are just going to the trash with the whole repository
            if (!isDeleted()) {
                throw new IllegalStateException(
                        "The repository of " + this + " was deleted, but not the file or folder itself!");
            } else {
                log.debug("Item " + this + " was deleted in the same time than its own repository");
            }
        }
    }

    @Override
    public abstract VfsItem save(VfsItem originalFsItem);

    private void updateTimestampsFromImport(MutableItemInfo importedInfo, MutableItemInfo info) {
        long created = importedInfo.getCreated();
        if (created <= 0) {
            created = System.currentTimeMillis();
        }
        long lm = importedInfo.getLastModified();
        if (lm <= 0) {
            lm = created;
        }
        long lu = importedInfo.getLastUpdated();
        if (lu <= 0) {
            lu = lm;
        }

        info.setCreated(created);
        info.setLastModified(lm);
        info.setLastUpdated(lu);
    }

    @Override
    public void setLastUpdated(long lastUpdated) {
        checkMutable("setLastUpdated");
        ((MutableItemInfo) info).setLastUpdated(lastUpdated);
    }

    /**
     * Reset the resource age so it is kept being cached (only relevant to cached snapshots and maven-metadata)
     */
    @Override
    public void unexpire() {
        //TODO: Change this mechanism since the last updated is used for artifact popularity measurement
        setLastUpdated(System.currentTimeMillis());
        log.debug("Unexpired '{}' from local cache '{}'.", getRelativePath(), repo.getKey());
    }

    @Override
    public boolean isIdentical(VfsItem item) {
        return absPath.equals(item.getAbsolutePath()) && info.isIdentical(item.getInfo());
    }

    protected void checkMutable(String action) {
        if (!isMutable() || !(info instanceof MutableItemInfo)) {
            throw new IllegalStateException(
                    "Cannot execute " + action + " on item " + getRepoPath() + " it is an immutable item.");
        }
    }

    /**
     * Returns a set of metadata definitions that this item adorns
     *
     * @param includeInternal True if the list should include internal metadata
     * @return Set of metadata definitions that annotate this item
     */
    public Set<MetadataDefinition<?, ?>> getExistingMetadata(boolean includeInternal) {
        Set<MetadataDefinition<?, ?>> metadataDefinitions = Sets.newHashSet();
        // add system metadata definitions (like file info) that might not be on the general metadata node
        Set<MetadataDefinition<?, ?>> cachedDefs = getMdService().getAllMetadataDefinitions(includeInternal);
        for (MetadataDefinition<?, ?> cachedDef : cachedDefs) {
            if (cachedDef.getPersistenceHandler().hasMetadata(this)) {
                metadataDefinitions.add(cachedDef);
            }
        }

        // add all user defined metadata definitions (all under the metadata node)
        Node itemNode = getNode();
        try {
            if (itemNode.hasNode(NODE_ARTIFACTORY_METADATA)) {
                Node metadataContainerNode = itemNode.getNode(NODE_ARTIFACTORY_METADATA);
                NodeIterator metadataNodes = metadataContainerNode.getNodes();
                while (metadataNodes.hasNext()) {
                    Node metadataNode = metadataNodes.nextNode();
                    String metadataName = metadataNode.getName();
                    MetadataDefinition definition = getMdService().getMetadataDefinition(metadataName, true);
                    if ((includeInternal || !definition.isInternal())) {
                        metadataDefinitions.add(definition);
                    }
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }

        return metadataDefinitions;
    }

    /**
     * Returns the metadata of the given type class.<br> To be used only with non-generic metadata classes.<br> Generic
     * (String class) will be ignored.<br>
     *
     * @param mdClass Class of metadata type. Cannot be generic or null
     * @param <MDT>   Metadata type
     * @return Requested metadata if found. Null if not
     * @throws IllegalArgumentException If given a null metadata class
     */
    @Override
    public <MDT> MDT getMetadata(Class<MDT> mdClass) {
        if (mdClass == null) {
            throw new IllegalArgumentException("Metadata type class to locate cannot be null.");
        }

        MetadataDefinition<MDT, ?> definition = getMdService().getMetadataDefinition(mdClass);
        MetadataPersistenceHandler<MDT, ?> mdph = definition.getPersistenceHandler();
        return mdph.read(this);
    }

    /**
     * Returns the metadata of the given name.
     *
     * @param metadataName Name of metadata to return. Cannot be null
     * @return Requested metadata if found. Null if not
     * @throws IllegalArgumentException If given a blank metadata name
     */
    @Override
    public Object getMetadata(String metadataName) {
        MetadataDefinition metadataDefinition = getMetadataDefinition(metadataName);
        if (metadataDefinition == null) {
            return null;
        }
        return metadataDefinition.getPersistenceHandler().read(this);
    }

    /**
     * Returns the metadata of the given name.
     *
     * @param metadataName Name of metadata to return. Cannot be null
     * @return Requested metadata if found. Null if not
     * @throws IllegalArgumentException If given a blank metadata name
     */
    @Override
    public String getXmlMetadata(String metadataName) {
        if (metadataToSave != null && !metadataToSave.isEmpty()) {
            SetMetadataMessage toFind = new SetMetadataMessage(metadataName, null);
            String result = null;
            for (SetMetadataMessage message : metadataToSave) {
                if (toFind.equals(message)) {
                    result = message.xmlContent;
                }
            }
            if (result != null) {
                return result;
            }
        }

        MetadataDefinition metadataDefinition = getMetadataDefinition(metadataName);
        if (metadataDefinition == null) {
            return null;
        }

        MetadataPersistenceHandler mdph = metadataDefinition.getPersistenceHandler();
        Object metadata = mdph.read(this);
        if (metadata == null) {
            return null;
        }
        return metadataDefinition.getXmlProvider().toXml(metadata);
    }

    /**
     * Indicates whether this item adorns the given metadata
     *
     * @param metadataName Name of metadata to locate
     * @return True if annotated by the given metadata. False if not
     * @throws IllegalArgumentException If given a blank metadata name
     */
    @Override
    public boolean hasMetadata(String metadataName) {
        if (metadataToSave != null && !metadataToSave.isEmpty()) {
            for (SetMetadataMessage message : metadataToSave) {
                if (message.metadataName.equals(metadataName)) {
                    return true;
                }
            }
        }
        MetadataDefinition metadataDefinition = getMetadataDefinition(metadataName);
        return metadataDefinition != null && metadataDefinition.getPersistenceHandler().hasMetadata(this);
    }

    /**
     * Sets the given metadata on the item.<br> To be used only with non-generic metadata classes.<br> Generic (String
     * class) will be ignored.<br>
     *
     * @param mdClass  Class of metadata type to set. Cannot be generic
     * @param metadata Metadata value to set. Cannot be null
     * @param <T>      Metadata type
     * @throws IllegalArgumentException When given a null metadata value
     */
    @Override
    public <T> void setMetadata(Class<T> mdClass, T metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("Cannot set a null value for metadata " + mdClass.getSimpleName() +
                    " on item " + getAbsolutePath() + ".");
        }
        MetadataDefinition<T, T> definition = getMdService().getMetadataDefinition(mdClass);
        MetadataPersistenceHandler<T, T> mdph = definition.getPersistenceHandler();
        mdph.update(this, metadata);
        String metadataName = definition.getMetadataName();
        getReplicationAddon().offerLocalReplicationMetadataDeploymentEvent(this.getRepoPath(), metadataName);
        if (metadataName.equals(PropertiesInfo.ROOT)) {
            if (!((Properties) metadata).isEmpty()) {
                // only log the properties as metadata/annotate access (the rest are internal)
                AccessLogger.annotated(getRepoPath(), "properties");
            }
        }
    }

    /**
     * Sets the given metadata on the item.<br>
     *
     * @param metadataName Name of metadata type to set
     * @param xmlData      Metadata value to set. Cannot be null
     * @throws IllegalArgumentException When given a null metadata value
     */
    @Override
    public void setXmlMetadata(String metadataName, String xmlData) {
        if (xmlData == null) {
            throw new IllegalArgumentException("Cannot set a null value for metadata " + metadataName + " on item " +
                    getAbsolutePath() + ".");
        }
        MetadataDefinition definition = getMdService().getMetadataDefinition(metadataName, true);
        MetadataPersistenceHandler mdph = definition.getPersistenceHandler();
        mdph.update(this, definition.getXmlProvider().fromXml(xmlData));
        getReplicationAddon().offerLocalReplicationMetadataDeploymentEvent(this.getRepoPath(), metadataName);
        AccessLogger.annotated(getRepoPath(), metadataName);
    }

    protected MetadataDefinitionService getMdService() {
        return StorageContextHelper.get().getMetadataDefinitionService();
    }

    /**
     * Removes the metadata of the given name
     *
     * @param metadataName Name of metadata to remove
     */
    @Override
    public void removeMetadata(String metadataName) {
        MetadataDefinition definition = getMdService().getMetadataDefinition(metadataName, true);
        MetadataPersistenceHandler metadataPersistenceHandler = definition.getPersistenceHandler();
        if (metadataPersistenceHandler.hasMetadata(this)) {
            metadataPersistenceHandler.remove(this);
            getReplicationAddon().offerLocalReplicationMetadataDeleteEvent(this.getRepoPath(), metadataName);
            AccessLogger.annotationDeleted(getRepoPath(), metadataName);
        }
    }

    @Override
    public void setXmlMetadataLater(String name, String content) {
        getOrCreateMetadataToSave().add(new SetMetadataMessage(name, content));
    }

    private synchronized BlockingQueue<SetMetadataMessage> getOrCreateMetadataToSave() {
        if (metadataToSave == null) {
            metadataToSave = new LinkedBlockingQueue<SetMetadataMessage>();
        }
        return metadataToSave;
    }


    /**
     * Returns the metadata definition of the given name only and only if it exists on the item
     *
     * @param metadataName Name of metadata to locate
     * @return Metadata definition if found. Null if not
     */
    public MetadataDefinition getMetadataDefinition(String metadataName) {
        MetadataDefinition cachedDef = getMdService().getMetadataDefinition(metadataName, false);
        if ((cachedDef != null) && cachedDef.getPersistenceHandler().hasMetadata(this)) {
            return cachedDef;
        }

        Node itemNode = getNode();
        try {
            if (itemNode.hasNode(NODE_ARTIFACTORY_METADATA)) {
                Node metadataContainerNode = itemNode.getNode(NODE_ARTIFACTORY_METADATA);
                if (!metadataContainerNode.hasNode(metadataName)) {
                    return null;
                }

                MetadataDefinition newDef = getMdService().getMetadataDefinition(metadataName, true);
                MetadataPersistenceHandler metadataPersistenceHandler = newDef.getPersistenceHandler();
                if (metadataPersistenceHandler.hasMetadata(this)) {
                    return newDef;
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }

        return null;
    }

    @Override
    public boolean isDirty() {
        return metadataToSave != null && !metadataToSave.isEmpty();
    }
}
