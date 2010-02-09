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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.ChecksumsInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.fs.MetadataInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.md.MetadataEntry;
import org.artifactory.api.md.MetadataReader;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.jcr.md.MetadataAware;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.jcr.md.MetadataPersistenceHandler;
import org.artifactory.jcr.md.XmlMetadataProvider;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.jcr.JcrHelper;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.update.md.MetadataVersion;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
public abstract class JcrFsItem<T extends ItemInfo> extends File implements Comparable<File>, MetadataAware {
    private static final Logger log = LoggerFactory.getLogger(JcrFsItem.class);

    private final String absPath;
    private final String uuid;
    private final T info;
    private final boolean mutable;
    private boolean deleted;

    /**
     * The repo that manage/contais this item. NOTE: This member should be the only transient member, and the only one
     * which points to global configuration objects (services, ...)
     */
    private transient StoringRepo repo;

    /**
     * Copy constructor used to create a mutable version of this fsItem out of JCR based one.
     *
     * @param copy A JCR based fsItem to copy
     * @param repo The local repo that this fsItem belongs to
     */
    public JcrFsItem(JcrFsItem<T> copy, StoringRepo repo) {
        // TODO: Sanity check that copy is immutable and repo is the same than in copy
        super(copy.getPath());
        this.absPath = copy.absPath;
        this.repo = repo;
        this.uuid = copy.uuid;
        this.mutable = true;
        this.info = getInfoPersistenceHandler().copy(copy.info);
    }

    /**
     * Constructor of a new fsItem out of a repo path
     *
     * @param repoPath The key of this fsItem
     * @param repo     The local repo that this fsItem belongs to
     */
    public JcrFsItem(RepoPath repoPath, StoringRepo repo) {
        super(JcrPath.get().getAbsolutePath(repoPath));
        this.repo = repo;
        this.absPath = PathUtils.formatPath(super.getPath());
        this.info = createInfo(repoPath);
        this.uuid = null;
        this.mutable = true;
    }

    /**
     * Constructor used when reading JCR content and creating JCR file system item from it. Will not create anything in
     * JCR but will read the JCR content of the node.
     *
     * @param node the JCR node this item represent
     * @param repo The local repo that this fsItem belongs to
     * @throws RepositoryRuntimeException if the node info cannot be read
     */
    public JcrFsItem(final Node node, StoringRepo repo) {
        super(JcrHelper.getAbsolutePath(node));
        this.repo = repo;
        this.uuid = JcrHelper.getUuid(node);
        this.absPath = PathUtils.formatPath(super.getPath());
        final RepoPath repoPath = JcrPath.get().getRepoPath(absPath);
        if (repoPath == null) {
            //Item does not exist in a current repo
            throw new ItemNotFoundRuntimeException("No valid fs item exists in path '" + absPath + "'.");
        }
        final boolean folder = isDirectory();
        MetadataAware dummyNodeWrapper = new MetadataAware() {
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
        this.mutable = false;
    }

    protected abstract T createInfo(RepoPath repoPath);

    protected abstract MetadataPersistenceHandler<T> getInfoPersistenceHandler();

    public boolean isMutable() {
        return mutable;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @SuppressWarnings({"unchecked"})
    public final StoringRepo<RepoDescriptor> getRepoGeneric() {
        return getRepo();
    }

    public final StoringRepo getRepo() {
        if (repo == null) {
            repo = InternalContextHelper.get().beanForType(InternalRepositoryService.class)
                    .storingRepositoryByKey(getRepoKey());
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
    public final void setModifiedInfoFields(long modified, long updated) {
        checkMutable("setModifiedInfoFields");
        info.setLastModified(modified);
        info.setLastUpdated(updated);
        String userId = getAuthorizationService().currentUsername();
        info.setModifiedBy(userId);
        if (info.getCreatedBy() == null) {
            info.setCreatedBy(userId);
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
    public String getRelativePath() {
        return getRepoPath().getPath();
    }

    public long getCreated() {
        return getInfo().getCreated();
    }

    public RepoPath getRepoPath() {
        return getInfo().getRepoPath();
    }

    public String getRepoKey() {
        return getRepoPath().getRepoKey();
    }

    @Override
    public boolean delete() {
        checkMutable("delete");
        setDeleted(true);
        return getJcrRepoService().delete(this);
    }

    public void bruteForceDelete() {
        bruteForceDelete(false);
    }

    public void bruteForceDelete(boolean nonTxNodeRemoval) {
        JcrSession session = null;
        try {
            if (nonTxNodeRemoval) {
                session = InternalContextHelper.get().getJcrService().getUnmanagedSession();
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
    public JcrFolder getParentFolder() {
        RepoPath parentRepoPath = getParentRepoPath();
        return getRepo().getJcrFolder(parentRepoPath);
    }

    /**
     * @return Parent folder of this folder with write lock or null if parent doesn't exist
     */
    public JcrFolder getLockedParentFolder() {
        RepoPath parentRepoPath = getParentRepoPath();
        return getRepo().getLockedJcrFolder(parentRepoPath, false);
    }

    public RepoPath getParentRepoPath() {
        RepoPath myRepoPath = getRepoPath();
        return new RepoPath(myRepoPath.getRepoKey(), PathUtils.getParent(myRepoPath.getPath()));
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
        return getParentFolder();
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

    // No override here since it's methods valid in 1.6 only

    //@Override

    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return false;
    }

    //@Override

    public boolean setWritable(boolean writable) {
        return false;
    }

    //@Override

    public boolean setReadable(boolean readable, boolean ownerOnly) {
        return false;
    }

    //@Override

    public boolean setReadable(boolean readable) {
        return false;
    }

    //@Override

    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        return false;
    }

    //@Override

    public boolean setExecutable(boolean executable) {
        return false;
    }

    //@Override

    public boolean canExecute() {
        return false;
    }

    //@Override

    public long getTotalSpace() {
        throw new UnsupportedOperationException("getTotalSpace() is not supported for jcr.");
    }

    //@Override

    public long getFreeSpace() {
        throw new UnsupportedOperationException("getFreeSpace() is not supported for jcr.");
    }

    //@Override

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
        getInfo().setLastModified(time);
        return true;        
    }

    /**
     * OVERIDDEN FROM FILE END
     */

    @Override
    public abstract boolean isDirectory();

    public boolean isFolder() {
        return isDirectory();
    }

    public final T getInfo() {
        return info;
    }

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

    protected Node getParentNode() {
        JcrSession session = getSession();
        String absPath = PathUtils.getParent(getAbsolutePath());
        return (Node) session.getItem(absPath);
    }

    protected JcrSession getSession() {
        return getJcrRepoService().getManagedSession();
    }

    protected JcrRepoService getJcrRepoService() {
        return InternalContextHelper.get().getJcrRepoService();
    }

    protected AuthorizationService getAuthorizationService() {
        return InternalContextHelper.get().getAuthorizationService();
    }

    /**
     * Export all metadata as the real xml content (jcr:data, including comments etc.) into a
     * {item-name}.artifactory-metadata folder, where each metadata is named {metadata-name}.xml
     */
    protected void exportMetadata(File targetPath, StatusHolder status, boolean incremental) {
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

    public void writeMetadataEntries(StatusHolder status, File metadataFolder, boolean incremental) {
        File metadataFile;
        Set<MetadataDefinition<?>> metadataDefinitions = getExistingMetadata(true);
        for (MetadataDefinition<?> definition : metadataDefinitions) {
            MetadataPersistenceHandler<?> mdph = definition.getPersistenceHandler();
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
            XmlMetadataProvider<Object> xmlProvider = (XmlMetadataProvider<Object>) definition.getXmlProvider();
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

    protected void writeFile(StatusHolder status, File metadataFile, String xmlData, long modified) {
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

    protected void writeChecksums(File targetPath, ChecksumsInfo checksumsInfo, String fileName, long modified) {
        File sha1 = new File(targetPath, fileName + ".sha1");
        File md5 = new File(targetPath, fileName + ".md5");
        try {
            FileUtils.writeStringToFile(sha1, checksumsInfo.getSha1(), "utf-8");
            FileUtils.writeStringToFile(md5, checksumsInfo.getMd5(), "utf-8");
            if (modified > 0) {
                sha1.setLastModified(modified);
                md5.setLastModified(modified);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected void importMetadata(File sourcePath, StatusHolder status, ImportSettings settings) {
        try {
            File metadataFolder = getMetadataContainerFolder(sourcePath);
            if (!metadataFolder.exists()) {
                // If root folder of repository do not send a warning
                if (getRelativePath().length() > 1) {
                    String msg = "No metadata files found for '" + sourcePath.getAbsolutePath() +
                            "' during import into " + getRepoPath();
                    status.setWarning(msg, log);
                }
                return;
            }

            MetadataReader metadataReader = settings.getMetadataReader();
            if (metadataReader == null) {
                if (settings.getExportVersion() != null) {
                    metadataReader = MetadataVersion.findVersion(settings.getExportVersion());
                } else {
                    //try to find the version from the format of the metadata folder
                    metadataReader = MetadataVersion.findVersion(metadataFolder);
                }
                settings.setMetadataReader(metadataReader);
            }
            List<MetadataEntry> metadataEntries = metadataReader.getMetadataEntries(metadataFolder, settings, status);
            for (MetadataEntry entry : metadataEntries) {
                String metadataName = entry.getMetadataName();
                MetadataDefinition definition = getRepoGeneric().getMetadataDefinition(metadataName, true);
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
                    updateTimestampsFromImport((T) metadata, getInfo());
                    // Just update the info object without saving to JCR since it'll be done on save
                    getInfo().merge((T) metadata);
                } else {
                    mdph.update(this, metadata);
                }
            }
        } catch (Exception e) {
            String msg =
                    "Failed to import metadata of " + sourcePath.getAbsolutePath() + " into '" + getRepoPath() + "'.";
            status.setError(msg, e, log);
        }
    }

    protected File getMetadataContainerFolder(File targetFile) {
        return new File(targetFile.getParentFile(), targetFile.getName() + ItemInfo.METADATA_FOLDER);
    }

    public void updateCache() {
        // Nullify the only transient field the repo which contains the cache itself :)
        repo = null;
        // Then doing getRepo() will ensure I get the latest repo cache for me.
        StoringRepo storingRepo = getRepo();
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

    public abstract JcrFsItem save(JcrFsItem originalFsItem);

    private void updateTimestampsFromImport(ItemInfo importedInfo, ItemInfo info) {
        long created = importedInfo.getCreated();
        if (created <= 0) {
            created = System.currentTimeMillis();
        }
        long lm = importedInfo.getLastModified();
        if (lm <= 0) {
            lm = created;
        }
        long lu = importedInfo.getAdditionalInfo().getLastUpdated();
        if (lu <= 0) {
            lu = lm;
        }

        info.setCreated(created);
        info.setLastModified(lm);
        info.getAdditionalInfo().setLastUpdated(lu);
    }

    /**
     * Sets the last updated value of snapshot files and folders to a value that will make them expired.
     *
     * @param expiredLastUpdated The time to set (will cause the expiration of the cached snapshots)
     * @return Number of files and folders affected
     */
    public abstract int zap(long expiredLastUpdated);

    public void setLastUpdated(long lastUpdated) {
        checkMutable("setLastUpdated");
        getInfo().setLastUpdated(lastUpdated);
    }

    public boolean isIdentical(JcrFsItem item) {
        return absPath.equals(item.absPath) && info.isIdentical(item.info);
    }

    protected void checkMutable(String action) {
        if (!isMutable()) {
            throw new IllegalStateException(
                    "Cannot execute " + action + " on item " + getRepoPath() + " it is an immutable item.");
        }
    }

    public Set<MetadataDefinition<?>> getExistingMetadata(boolean includeInternal) {
        Set<MetadataDefinition<?>> allMdDefs = getRepoGeneric().getAllMetadataDefinitions(includeInternal);
        Iterator<MetadataDefinition<?>> mdDefIterator = allMdDefs.iterator();
        while (mdDefIterator.hasNext()) {
            MetadataDefinition definition = mdDefIterator.next();
            if (!definition.getPersistenceHandler().hasMetadata(this)) {
                mdDefIterator.remove();
            }
        }
        return allMdDefs;
    }

    public <T> void setMetadata(Class<T> mdClass, T metadata) {
        MetadataDefinition<T> definition = getRepoGeneric().getMetadataDefinition(mdClass);
        MetadataPersistenceHandler<T> mdph = definition.getPersistenceHandler();
        if (metadata == null) {
            mdph.remove(this);
        } else {
            mdph.update(this, metadata);
        }
    }

    public void setMetadata(String metadataName, Object metadata) {
        MetadataDefinition definition = getRepoGeneric().getMetadataDefinition(metadataName, true);
        MetadataPersistenceHandler mdph = definition.getPersistenceHandler();
        if (metadata == null) {
            mdph.remove(this);
        } else {
            mdph.update(this, metadata);
        }
    }

    public void setXmlMetadata(String metadataName, String xmlData) {
        MetadataDefinition definition = getRepoGeneric().getMetadataDefinition(metadataName, true);
        MetadataPersistenceHandler mdph = definition.getPersistenceHandler();
        if (xmlData == null) {
            mdph.remove(this);
        } else {
            mdph.update(this, definition.getXmlProvider().fromXml(xmlData));
        }
    }

    public <T> T getMetadata(Class<T> mdClass) {
        MetadataDefinition<T> definition = getRepoGeneric().getMetadataDefinition(mdClass);
        MetadataPersistenceHandler<T> mdph = definition.getPersistenceHandler();
        return mdph.read(this);
    }

    public Object getMetadata(String metadataName) {
        MetadataDefinition definition = getRepoGeneric().getMetadataDefinition(metadataName, false);
        if (definition == null) {
            return null;
        }
        MetadataPersistenceHandler mdph = definition.getPersistenceHandler();
        return mdph.read(this);
    }

    public String getXmlMetadata(String metadataName) {
        MetadataDefinition definition = getRepoGeneric().getMetadataDefinition(metadataName, false);
        if (definition == null) {
            return null;
        }
        MetadataPersistenceHandler mdph = definition.getPersistenceHandler();
        Object metadata = mdph.read(this);
        if (metadata == null) {
            return null;
        }
        return definition.getXmlProvider().toXml(metadata);
    }

    public boolean hasMetadata(String metadataName) {
        MetadataDefinition definition = getRepoGeneric().getMetadataDefinition(metadataName, false);
        if (definition == null) {
            return false;
        }
        MetadataPersistenceHandler mdph = definition.getPersistenceHandler();
        return mdph.hasMetadata(this);
    }
}