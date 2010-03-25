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

package org.artifactory.repo.jcr;

import org.apache.commons.io.IOUtils;
import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.Cache;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.fs.ChecksumsInfo;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.fs.MetadataInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.md.Properties;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.FolderExpectedException;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.cache.InternalCacheService;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.io.SimpleResourceStreamHandle;
import org.artifactory.io.StringResourceStreamHandle;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.io.checksum.policy.ChecksumPolicyException;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.lock.FsItemLockEntry;
import org.artifactory.jcr.lock.LockEntryId;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.jcr.lock.MonitoringReadWriteLock;
import org.artifactory.jcr.lock.aop.LockingAdvice;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.jcr.md.MetadataDefinitionService;
import org.artifactory.jcr.md.MetadataPersistenceHandler;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.context.RequestContext;
import org.artifactory.repo.interceptor.RepoInterceptors;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.MetadataResource;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.traffic.InternalTrafficService;
import org.artifactory.traffic.entry.UploadEntry;
import org.artifactory.util.ExceptionUtils;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StoringRepoMixin<T extends RepoDescriptor> implements StoringRepo<T> {
    private static final Logger log = LoggerFactory.getLogger(StoringRepoMixin.class);

    private final JcrFileCreator jcrFileCreator = new JcrFileCreator();
    private final JcrFolderCreator jcrFolderCreator = new JcrFolderCreator();
    private MetadataDefinition<FileInfo> fileInfoMd;
    private MetadataDefinition<FolderInfo> folderInfoMd;
    private String repoRootPath;
    private Map<RepoPath, MonitoringReadWriteLock> locks;
    private Map<RepoPath, JcrFsItem> fsItemCache;
    private RepoInterceptors interceptors;
    private JcrRepoService jcrRepoService;
    private JcrService jcrService;
    private InternalTrafficService trafficService;
    private MetadataDefinitionService mdDefService;
    private RepoPath rootRepoPath;

    private final StoringRepo<T> delegator;
    private StoringRepoMixin<T> oldStoringRepo;

    public StoringRepoMixin(StoringRepo<T> delegator, StoringRepo<T> oldStoringRepo) {
        this.delegator = delegator;
        if (oldStoringRepo instanceof StoringRepoMixin) {
            this.oldStoringRepo = (StoringRepoMixin<T>) oldStoringRepo;
        }
    }

    public void init() {
        InternalArtifactoryContext context = InternalContextHelper.get();

        this.mdDefService = context.beanForType(MetadataDefinitionService.class);
        this.fileInfoMd = mdDefService.getMetadataDefinition(FileInfo.class);
        this.folderInfoMd = mdDefService.getMetadataDefinition(FolderInfo.class);

        // TODO: should select the interceptors depending on the repo type
        this.interceptors = context.beanForType(RepoInterceptors.class);
        this.jcrRepoService = context.getJcrRepoService();
        this.jcrService = context.getJcrService();
        this.trafficService = context.beanForType(InternalTrafficService.class);

        //init caches
        InternalCacheService cacheService = context.beanForType(InternalCacheService.class);
        if (oldStoringRepo != null) {
            // The locks should be kept at all cost :)
            cacheService
                    .setRepositoryCache(oldStoringRepo.getKey(), ArtifactoryCache.locks, (Cache) oldStoringRepo.locks);

            // Copy the cached fsitems if the cache behavior in descriptors did not changed
            RepoDescriptor oldDescriptor = oldStoringRepo.getDescriptor();
            RepoDescriptor currentDescriptor = delegator.getDescriptor();
            if (oldDescriptor == null || currentDescriptor == null) {
                // Just logged the bad state, the cached will be cleaned
                IllegalStateException ex = new IllegalStateException("Current or Old Descriptors are null!");
                log.warn(ex.getMessage(), ex);
            } else {
                if (currentDescriptor.identicalCache(oldDescriptor)) {
                    cacheService
                            .setRepositoryCache(oldStoringRepo.getKey(), ArtifactoryCache.fsItemCache,
                                    (Cache) oldStoringRepo.fsItemCache);
                }
            }
            oldStoringRepo = null;
        }
        this.fsItemCache = cacheService.getRepositoryCache(getKey(), ArtifactoryCache.fsItemCache);
        this.locks = cacheService.getRepositoryCache(getKey(), ArtifactoryCache.locks);

        //Create the repo node if it doesn't exist
        if (!jcrService.itemNodeExists(JcrPath.get().getAbsolutePath(rootRepoPath))) {
            JcrFolder rootJcrFolder = getLockedJcrFolder(rootRepoPath, true);
            rootJcrFolder.mkdir();
        }
    }

    public void destroy() {
    }

    public String getKey() {
        return delegator.getKey();
    }

    public String getDescription() {
        return delegator.getDescription();
    }

    public boolean isReal() {
        return delegator.isReal();
    }

    public boolean isLocal() {
        return delegator.isLocal();
    }

    public boolean isCache() {
        return delegator.isCache();
    }

    public MetadataDefinition<FileInfo> getFileInfoMd() {
        return fileInfoMd;
    }

    public MetadataDefinition<FolderInfo> getFolderInfoMd() {
        return folderInfoMd;
    }

    public InternalRepositoryService getRepositoryService() {
        return delegator.getRepositoryService();
    }

    public String getRepoRootPath() {
        return repoRootPath;
    }

    public <T> MetadataDefinition<T> getMetadataDefinition(Class<T> clazz) {
        return mdDefService.getMetadataDefinition(clazz);
    }

    public MetadataDefinition getMetadataDefinition(String metadataName, boolean createIfEmpty) {
        return mdDefService.getMetadataDefinition(metadataName, createIfEmpty);
    }

    public Set<MetadataDefinition<?>> getAllMetadataDefinitions(boolean includeInternal) {
        return mdDefService.getAllMetadataDefinitions(includeInternal);
    }

    public JcrFolder getRootFolder() {
        return getJcrFolder(rootRepoPath);
    }

    public JcrFolder getLockedRootFolder() {
        return getLockedJcrFolder(rootRepoPath, false);
    }

    public void updateCache(JcrFsItem fsItem) {
        RepoPath repoPath = fsItem.getRepoPath();
        if (fsItem.isDeleted()) {
            fsItemCache.remove(repoPath);
        } else {
            if (fsItem.isMutable()) {
                throw new IllegalStateException("Cannot add object " + fsItem + " into cache, it is mutable.");
            }
            fsItemCache.put(repoPath, fsItem);
        }
    }

    /**
     * {@inheritDoc}
     */
    public JcrFsItem getLocalJcrFsItem(String relPath) {
        RepoPath repoPath = new RepoPath(getKey(), relPath);
        return getJcrFsItem(repoPath);
    }

    /**
     * {@inheritDoc}
     */
    public JcrFsItem getJcrFsItem(RepoPath repoPath) {
        return internalGetFsItem(new JcrFsItemLocator(repoPath, true, false));
    }

    /**
     * {@inheritDoc}
     */
    public JcrFsItem getJcrFsItem(Node node) {
        return internalGetFsItem(new JcrFsItemLocator(node, true));
    }

    public JcrFile getLocalJcrFile(String relPath) throws FileExpectedException {
        RepoPath repoPath = new RepoPath(getKey(), relPath);
        return getJcrFile(repoPath);
    }

    public JcrFile getJcrFile(String relPath) throws FileExpectedException {
        RepoPath repoPath = new RepoPath(getKey(), relPath);
        return getJcrFile(repoPath);
    }

    public JcrFile getJcrFile(RepoPath repoPath) throws FileExpectedException {
        JcrFsItem item = getJcrFsItem(repoPath);
        if (item != null && !item.isFile()) {
            throw new FileExpectedException(repoPath);
        }
        return (JcrFile) item;
    }

    public JcrFolder getLocalJcrFolder(String relPath) throws FolderExpectedException {
        RepoPath repoPath = new RepoPath(getKey(), relPath);
        return getJcrFolder(repoPath);
    }

    public JcrFolder getJcrFolder(RepoPath repoPath) throws FolderExpectedException {
        JcrFsItem item = getJcrFsItem(repoPath);
        if (item != null && !item.isDirectory()) {
            throw new FolderExpectedException(repoPath);
        }
        return (JcrFolder) item;
    }

    public JcrFsItem getLockedJcrFsItem(String relPath) {
        RepoPath repoPath = new RepoPath(getKey(), relPath);
        return getLockedJcrFsItem(repoPath);
    }

    public JcrFsItem getLockedJcrFsItem(RepoPath repoPath) {
        JcrFsItemLocator locator = new JcrFsItemLocator(repoPath, false, false);
        return internalGetLockedJcrFsItem(locator);
    }

    public JcrFsItem getLockedJcrFsItem(Node node) {
        JcrFsItemLocator locator = new JcrFsItemLocator(node, false);
        return internalGetLockedJcrFsItem(locator);
    }

    public JcrFile getLockedJcrFile(String relPath, boolean createIfMissing) throws FileExpectedException {
        RepoPath repoPath = new RepoPath(getKey(), relPath);
        return getLockedJcrFile(repoPath, createIfMissing);
    }

    public JcrFile getLockedJcrFile(RepoPath repoPath, boolean createIfMissing) throws FileExpectedException {
        JcrFsItemLocator locator = new JcrFsItemLocator(repoPath, false, createIfMissing);
        locator.setCreator(jcrFileCreator);
        return (JcrFile) internalGetLockedJcrFsItem(locator);
    }

    public JcrFolder getLockedJcrFolder(String relPath, boolean createIfMissing) throws FolderExpectedException {
        RepoPath repoPath = new RepoPath(getKey(), relPath);
        return getLockedJcrFolder(repoPath, createIfMissing);
    }

    public JcrFolder getLockedJcrFolder(RepoPath repoPath, boolean createIfMissing) throws FolderExpectedException {
        JcrFsItemLocator locator = new JcrFsItemLocator(repoPath, false, createIfMissing);
        locator.setCreator(jcrFolderCreator);
        return (JcrFolder) internalGetLockedJcrFsItem(locator);
    }

    public RepoResource getInfo(RequestContext context) throws FileExpectedException {
        final String path = context.getResourcePath();
        RepoPath repoPath = new RepoPath(getKey(), path);
        JcrFsItem<?> item = getPathItem(repoPath);
        if (item == null) {
            return new UnfoundRepoResource(repoPath, "File not found.");
        }
        RepoResource localRes;
        //When requesting a property/metadata return a special resource class that contains the parent node
        //path and the metadata name.
        if (NamingUtils.isMetadata(path)) {
            String metadataName = NamingUtils.getMetadataName(path);
            MetadataDefinition definition = getMetadataDefinition(metadataName, true);
            if (definition != null) {
                MetadataPersistenceHandler mdph = definition.getPersistenceHandler();
                MetadataInfo mdi = mdph.getMetadataInfo(item);
                if (mdi != null) {
                    if (MavenNaming.isMavenMetadata(path)) {
                        // this is hack (YS: at least I think so) for maven metadata use the last updated time of the folder.
                        // the cache repo will use this value to determine if the resource is expired.
                        mdi.setLastModified(item.getInfo().getLastUpdated());
                    }
                    localRes = new MetadataResource(mdi);
                } else {
                    return new UnfoundRepoResource(repoPath, "metadata " + metadataName + " not found for " + item);
                }
            } else {
                return new UnfoundRepoResource(repoPath, "metadata " + metadataName + " not found");
            }
        } else {
            if (item.isDirectory()) {
                throw new FileExpectedException(repoPath);
            }
            //Handle query-aware get
            Properties queryProperties = context.getProperties();
            boolean exactMatch = true;
            if (!queryProperties.isEmpty()) {
                Properties properties = item.getMetadata(Properties.class);
                Properties.MatchResult matchResult = properties.matchQuery(queryProperties);
                if (matchResult == Properties.MatchResult.NO_MATCH) {
                    exactMatch = false;
                    log.debug("File '{}' was found, but has no matching properties.", repoPath);
                } else if (matchResult == Properties.MatchResult.CONFLICT) {
                    return new UnfoundRepoResource(repoPath,
                            "File '" + repoPath + "' was found, but mandatory properties do not match.");
                }
            }
            localRes = new FileResource((FileInfo) item.getInfo(), exactMatch);
        }
        //Release the read lock early
        RepoPath lockedRepoPath = RepoPath.getLockingTargetRepoPath(repoPath);
        LockingHelper.releaseReadLock(lockedRepoPath);
        return localRes;
    }

    public ResourceStreamHandle getResourceStreamHandle(final RepoResource res) throws IOException,
            RepositoryException {
        log.debug("Transferring {} directly to user from {}.", res, this);
        String relPath = res.getRepoPath().getPath();
        //If we are dealing with metadata will return the md container item
        JcrFsItem item = getLocalJcrFsItem(relPath);
        //If resource does not exist throw an IOException
        if (item == null) {
            throw new IOException("Could not get resource stream. Path not found: " + res + ".");
        }
        if (item.isDeleted() || !item.exists()) {
            item.setDeleted(true);
            throw new ItemNotFoundRuntimeException("Could not get resource stream. Item " + item + " was deleted!");
        }
        ResourceStreamHandle handle;
        if (res.isMetadata()) {
            String metadataName = res.getInfo().getName();
            String xmlMetadata = item.getXmlMetadata(metadataName);
            if (xmlMetadata == null) {
                throw new IOException("Could not get resource stream. Stream not found: " + res + ".");
            } else {
                handle = new StringResourceStreamHandle(xmlMetadata);
            }
        } else if (item.isFile()) {
            JcrFile jcrFile = (JcrFile) item;
            final InputStream is = jcrFile.getStream();
            if (is == null) {
                throw new IOException("Could not get resource stream. Stream not found: " + item + ".");
            }
            //Update the stats queue counter
            jcrFile.updateDownloadStats();
            //Send the async event to save the stats

            RepoPath responsePath = res.getResponseRepoPath();
            if (getRepositoryService().virtualRepositoryByKey(responsePath.getRepoKey()) == null) {
                getRepositoryService().updateStats(responsePath);
            }
            long size = jcrFile.getSize();
            handle = new SimpleResourceStreamHandle(is, size);
            log.trace("Created stream handle for '{}' with length {}.", res, size);
        } else {
            throw new IOException("Could not get resource stream from a folder " + res + ".");
        }
        return handle;
    }

    public String getChecksum(String checksumFileUrl, RepoResource resource) throws IOException {
        if (resource == null || !resource.isFound()) {
            throw new IOException("Could not get resource stream. Path not found: " + checksumFileUrl + ".");
        }
        ChecksumType checksumType = ChecksumType.forFilePath(checksumFileUrl);
        if (checksumType == null) {
            throw new IllegalArgumentException("Checksum type not found for path " + checksumFileUrl);
        }
        return getChecksumPolicy().getChecksum(checksumType, resource.getInfo().getChecksums(), resource.getRepoPath());
    }

    public ChecksumPolicy getChecksumPolicy() {
        return delegator.getChecksumPolicy();
    }

    public void undeploy(RepoPath repoPath) {
        undeploy(repoPath, true);
    }

    public void undeploy(RepoPath repoPath, boolean calcMavenMetadata) {
        JcrFsItem item = getLockedJcrFsItem(repoPath);
        if (item == null || item.isDeleted()) {
            // Unlock early if already deleted
            LockingHelper.removeLockEntry(repoPath);
            return;
        }
        String path = repoPath.getPath();
        if (NamingUtils.isMetadata(path)) {
            String metadataName = NamingUtils.getMetadataName(path);
            item.removeMetadata(metadataName);
        } else {
            if (repoPath.isRoot()) {
                // Delete the all repo
                deleteRepositoryContent();
                item.setDeleted(true);
            } else {
                if (!item.isDeleted()) {
                    item.delete();
                    //Move the deleted item to the trash
                    jcrRepoService.trash(Collections.singletonList(item));
                    if (calcMavenMetadata) {
                        // calculate maven metadata on the parent path
                        RepoPath folderForMetadataCalculation = repoPath.getParent();
                        if (item.isFile()) {
                            // calculate maven metadata on the artifactId node
                            folderForMetadataCalculation = folderForMetadataCalculation.getParent();
                        }
                        getRepositoryService().calculateMavenMetadataAsync(folderForMetadataCalculation);
                    }
                }
            }
        }
    }

    /**
     * Create the resource in the local repository
     *
     * @param res        the destination resource definition
     * @param in         the stream to save at the location
     * @param properties A set of keyval metadata to attach to the (file) resource as part of this storage process
     */
    public RepoResource saveResource(RepoResource res, final InputStream in, Properties properties) throws IOException {
        RepoPath repoPath = new RepoPath(getKey(), res.getRepoPath().getPath());
        try {
            if (res.isMetadata()) {
                //If we are dealing with metadata set it on the containing fsitem
                RepoPath metadataContainerRepoPath = RepoPath.getLockingTargetRepoPath(repoPath);
                // Write lock auto upgrade supported LockingHelper.releaseReadLock(metadataContainerRepoPath);
                JcrFsItem jcrFsItem = getLockedJcrFsItem(metadataContainerRepoPath);
                if (jcrFsItem == null) {
                    //If we cannot find the container, and the metadata is of maven, assume it's a folder and create it
                    //on demand - we have to take this approach because maven is deploying folder (version) metadata
                    //immediately after sending the artifact deploy method.
                    if (MavenNaming.isMavenMetadata(repoPath.getPath())) {
                        log.debug("Creating missing metadata container folder '{}'.", metadataContainerRepoPath);
                        jcrFsItem = getLockedJcrFolder(metadataContainerRepoPath, true);
                        jcrFsItem.mkdirs();
                    } else {
                        //If there is no container return unfound
                        return new UnfoundRepoResource(repoPath,
                                "No metadata container exists: " + metadataContainerRepoPath);
                    }
                }
                String metadataName = res.getInfo().getName();
                jcrFsItem.setXmlMetadata(metadataName, IOUtils.toString(in, "utf-8"));
            } else {
                //Create the parent folder if it does not exist
                RepoPath parentPath = repoPath.getParent();
                if (parentPath == null) {
                    throw new RepositoryException("Cannot save resource, no parent repo path exists");
                }

                //Write lock auto upgrade supported LockingHelper.releaseReadLock(repoPath);
                JcrFile jcrFile = getLockedJcrFile(repoPath, true);
                if (!itemExists(parentPath.getPath())) {
                    JcrFolder jcrFolder = getLockedJcrFolder(parentPath, true);
                    jcrFolder.mkdirs();
                }

                /**
                 * If the file isn't a non-unique snapshot and it already exists, create a defensive of the checksums
                 * info for later comparison
                 */
                boolean artifactNonUniqueSnapshot = MavenNaming.isNonUniqueSnapshot(repoPath.getId());
                boolean fileExists = jcrFile.exists();
                ChecksumsInfo existingChecksumsToCompare = null;
                if (!artifactNonUniqueSnapshot && fileExists) {
                    existingChecksumsToCompare = new ChecksumsInfo(jcrFile.getInfo().getChecksumsInfo());
                }

                // set the file extension checksums (only needed if the file is currently being downloaded)
                Set<ChecksumInfo> newChecksums = ((FileResource) res).getInfo().getChecksums();
                jcrFile.getInfo().setChecksums(newChecksums);

                //Deploy
                long lastModified = res.getInfo().getLastModified();
                BufferedInputStream bis = new BufferedInputStream(in);
                final long start = System.currentTimeMillis();
                try {
                    jcrFile.fillData(lastModified, bis); //May throw a checksum error
                } catch (Exception e) {
                    //Make sure the file is not saved
                    jcrFile.bruteForceDelete();
                    throw e;
                }

                ChecksumsInfo newChecksumsToCompare = jcrFile.getInfo().getChecksumsInfo();

                /**
                 * If the artifact is not a non-unique snapshot and already exists but with a different checksum, remove
                 * all the existing metadata 
                 */
                if (!artifactNonUniqueSnapshot && fileExists && (existingChecksumsToCompare != null)) {

                    if (checksumsDiffer(existingChecksumsToCompare, newChecksumsToCompare)) {
                        Set<MetadataDefinition<?>> metadataDefinitions = jcrFile.getExistingMetadata(false);
                        for (MetadataDefinition<?> metadataDefinition : metadataDefinitions) {
                            jcrFile.removeMetadata(metadataDefinition.getMetadataName());
                        }
                    }
                }

                //Save properties
                if (properties != null) {
                    jcrFile.setMetadata(Properties.class, properties);
                }

                if (log.isDebugEnabled()) {
                    log.debug("Saved resource '{}' with length {} into repository '{}'.",
                            new Object[]{res, jcrFile.getSize(), this});
                }
                onCreate(jcrFile);
                final UploadEntry uploadEntry =
                        new UploadEntry(repoPath.getId(), jcrFile.length(), System.currentTimeMillis() - start);
                trafficService.handleTrafficEntry(uploadEntry);
            }
            return res;
        } catch (Exception e) {
            // throw back ChecksumPolicyException if it is the cause
            Throwable checksumCause = ExceptionUtils.getCauseOfTypes(e, ChecksumPolicyException.class);
            if (checksumCause != null) {
                throw (ChecksumPolicyException) checksumCause;
            }
            //Unwrap any IOException and throw it
            Throwable ioCause = ExceptionUtils.getCauseOfTypes(e, IOException.class);
            if (ioCause != null) {
                log.warn("IO error while trying to save resource {}'': {}",
                        res.getRepoPath(), ioCause.getMessage());
                throw (IOException) ioCause;
            }
            throw new RuntimeException("Failed to save resource '" + res.getRepoPath() + "'.", e);
        }
    }

    /**
     * Compares the actual checksums of the given checksums info
     *
     * @param existingChecksumsToCompare Checksums info of existing file
     * @param newChecksumsToCompare      Checksums info of newly deployed file
     * @return True if the checksums are different
     */
    private boolean checksumsDiffer(ChecksumsInfo existingChecksumsToCompare, ChecksumsInfo newChecksumsToCompare) {
        for (ChecksumType checksumType : ChecksumType.values()) {
            ChecksumInfo existingChecksum = existingChecksumsToCompare.getChecksumInfo(checksumType);
            ChecksumInfo newChecksum = newChecksumsToCompare.getChecksumInfo(checksumType);
            if (!existingChecksum.getActual().equals(newChecksum.getActual())) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldProtectPathDeletion(String path, boolean overwrite) {
        //Never protect checksums
        boolean protect = !NamingUtils.isChecksum(path);

        if (overwrite) {
            //Snapshots should be overridable, except for unique ones
            protect &= !MavenNaming.isSnapshot(path) || !MavenNaming.isNonUniqueSnapshot(path);
            //Allow overriding of index files
            protect &= !MavenNaming.isIndex(path);
            //Any metadata should be overridable
            boolean metadata = NamingUtils.isMetadata(path);
            protect &= !metadata;
            //For non metadata, never protect folders
            if (!metadata) {
                // Should not acquire a FsItem here!
                RepoPath repoPath = new RepoPath(getKey(), path);
                protect &= jcrService.isFile(repoPath);
            }
        }
        return protect;
    }

    @SuppressWarnings({"SimplifiableIfStatement"})
    public boolean itemExists(String relPath) {
        assert relPath != null;
        if (relPath.length() > 0) {
            return jcrService.itemNodeExists(repoRootPath + "/" + relPath);
        } else {
            //The repo itself
            return true;
        }
    }

    public List<String> getChildrenNames(String relPath) {
        return jcrRepoService.getChildrenNames(repoRootPath + "/" + relPath);
    }

    /**
     * Deletes the contents of this repository.
     */
    public void deleteRepositoryContent() {
        JcrFolder rootFolder = getLockedRootFolder();
        //Delete 1st level children
        List<JcrFsItem> children;
        children = jcrRepoService.getChildren(rootFolder, true);
        for (JcrFsItem child : children) {
            try {
                child.delete();
            } catch (Exception e) {
                log.error("Could not delete repository child node '{}'.", child.getName(), e);
            }
        }
        //Move the deleted item to the trash
        jcrRepoService.trash(children);
    }

    public void onCreate(JcrFsItem fsItem) {
        StatusHolder holder = new MultiStatusHolder();
        interceptors.onCreate(fsItem, holder);
        // TODO: Check the statusHolder
        RepoPath repoPath = fsItem.getRepoPath();
        AccessLogger.deployed(repoPath);
    }

    public void onDelete(JcrFsItem fsItem) {
        interceptors.onDelete(fsItem, new StatusHolder());
        AccessLogger.deleted(fsItem.getRepoPath());
    }

    public void setDescriptor(T descriptor) {
        repoRootPath = JcrPath.get().getRepoJcrPath(getKey());
        rootRepoPath = new RepoPath(getKey(), "");
    }

    public T getDescriptor() {
        return delegator.getDescriptor();
    }

    @Override
    public String toString() {
        return getKey();
    }

    protected final void assertRepoPath(RepoPath repoPath) {
        if (!getKey().equals(repoPath.getRepoKey())) {
            throw new IllegalArgumentException(
                    "Trying to retrieve resource " + repoPath + " from local repo " + getKey());
        }
    }

    /**
     * Get from cache, or load from JCR. Read lock according to locator.
     *
     * @param locator
     * @return null if item does not exists, a JcrFsItem otherwise
     */
    private JcrFsItem internalGetFsItem(JcrFsItemLocator locator) {
        RepoPath repoPath = locator.getRepoPath();
        assertRepoPath(repoPath);
        // First check if we have already the write lock
        JcrFsItem item = LockingHelper.getIfLockedByMe(repoPath);
        if (item != null) {
            return item;
        }
        JcrFsItem fsItem = fsItemCache.get(repoPath);
        if (fsItem == null) {
            fsItem = locator.getFsItem();
            if (fsItem != null) {
                fsItemCache.put(repoPath, fsItem);
                if (log.isTraceEnabled()) {
                    log.trace("Got '{}' with size {} using locator. lm={}.",
                            new Object[]{repoPath, fsItem.length(), LockingAdvice.getLockManager().hashCode()});
                }
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Got '{}' with size {} from cache. lm={}.",
                        new Object[]{repoPath, fsItem.length(), LockingAdvice.getLockManager().hashCode()});
            }
        }
        JcrFsItem newFsItem = locator.lock(fsItem);
        if (newFsItem != fsItem) {
            if (log.isTraceEnabled()) {
                log.trace("Got '{}' from cache but changed after lock. lm={}.",
                        new Object[]{repoPath, LockingAdvice.getLockManager().hashCode()});
            }
            return newFsItem;
        }
        return fsItem;
    }

    public boolean isWriteLocked(RepoPath path) {
        MonitoringReadWriteLock lock = locks.get(path);
        return lock != null && lock.isWriteLocked();
    }

    public StoringRepo<T> getStorageMixin() {
        return this;
    }

    private MonitoringReadWriteLock getLock(RepoPath path) {
        MonitoringReadWriteLock lockEntry = locks.get(path);
        if (lockEntry == null) {
            lockEntry = locks.put(path, new MonitoringReadWriteLock());
        }
        return lockEntry;
    }

    private JcrFsItem internalGetLockedJcrFsItem(JcrFsItemLocator locator) {
        RepoPath repoPath = locator.getRepoPath();
        try {
            // Create a lock entry for the repo path
            LockEntryId lockEntryId = new LockEntryId(getLock(repoPath), repoPath);
            // acquire the write lock
            FsItemLockEntry sessionLockEntry = LockingHelper.writeLock(lockEntryId);
            if (sessionLockEntry.getLockedFsItem() != null) {
                return sessionLockEntry.getLockedFsItem();
            }
            JcrFsItem fsItem = internalGetFsItem(locator);
            FsItemCreator creator;
            if (fsItem != null) {
                creator = locator.getCreator();
                RuntimeException exception = creator.checkItemType(fsItem);
                if (exception != null) {
                    LockingHelper.removeLockEntry(repoPath);
                    throw exception;
                }
                return internalGetLockedJcrFsItem(fsItem, false, creator, sessionLockEntry);
            } else if (locator.createIfEmpty) {
                creator = locator.creator;
                if (creator == null) {
                    throw new IllegalStateException("Cannot create node " + repoPath + " if no creator provided");
                }
                // Create the new fs item
                JcrFsItem newFsItem = creator.newFsItem(locator.getRepoPath(), this.delegator);
                return internalGetLockedJcrFsItem(newFsItem, true, creator, sessionLockEntry);
            }
            // Empty item and should not be auto-created
            LockingHelper.removeLockEntry(repoPath);
            return null;
        } catch (RuntimeException e) {
            LockingHelper.removeLockEntry(repoPath);
            throw e;
        }
    }

    @SuppressWarnings({"unchecked"})
    private <T extends JcrFsItem<? extends ItemInfo>> T internalGetLockedJcrFsItem(
            T item,
            boolean created, //A newly created item in the current tx
            FsItemCreator<T> creator,
            FsItemLockEntry sessionLockEntry) {
        // We need to have the write lock here
        if (sessionLockEntry == null) {
            throw new IllegalStateException("sessionLockEntry is null");
        }
        if (!sessionLockEntry.isLockedByMe()) {
            throw new IllegalStateException(
                    "Cannot set mutable entry on non writable lock item " + sessionLockEntry.getRepoPath());
        }
        // First check if we have already the write lock
        RepoPath repoPath = sessionLockEntry.getRepoPath();
        T result = (T) sessionLockEntry.getLockedFsItem();
        // If result is not null the locked item passed as param should be the same as result
        if (result == null && !created && item.isMutable()) {
            // If not locked by me and not created, should be immutable
            throw new IllegalStateException("FsItem " + item + " cannot be mutable if already exists in JCR");
        }
        if (created && !item.isMutable()) {
            // If created the item should be the new one
            throw new IllegalStateException("FsItem " + item + " cannot be created and immutable!");
        }
        T original = (T) fsItemCache.get(repoPath);
        if (result != null) {
            item = result;
        } else if (created) {
            if (original != null) {
                // Someone create the element before me => fallback to copy
                item = creator.newFsItem(original, delegator);
            } else {
                // If new item no immutable original
                original = null;
            }
        } else {
            // Do a copy constructor to start modifying it
            if (original == null) {
                original = item;
            }
            item = creator.newFsItem(original, delegator);
        }
        sessionLockEntry.setWriteFsItem(original, item);
        return item;
    }

    private JcrFsItem getPathItem(RepoPath repoPath) {
        //If we are dealing with metadata will return the md container item
        JcrFsItem item = getJcrFsItem(repoPath);
        if (item != null && (item.isDeleted() || !item.exists())) {
            log.warn("File '{}' was deleted during request!", repoPath);
            return null;
        }
        return item;
    }

    private class JcrFsItemLocator {
        private final RepoPath repoPath;
        private final Node node;
        private final boolean acquireReadLock;
        private final boolean createIfEmpty;
        private FsItemCreator creator = null;

        JcrFsItemLocator(RepoPath repoPath, boolean acquireReadLock, boolean createIfEmpty) {
            //If we are dealing with metadata return the containing fsitem
            this.repoPath = RepoPath.getLockingTargetRepoPath(repoPath);
            this.node = null;
            this.acquireReadLock = acquireReadLock;
            this.createIfEmpty = createIfEmpty;
        }

        JcrFsItemLocator(Node node, boolean acquireReadLock) {
            this.repoPath = JcrPath.get().getRepoPath(JcrHelper.getAbsolutePath(node));
            this.node = node;
            this.acquireReadLock = acquireReadLock;
            this.createIfEmpty = false;
        }

        public RepoPath getRepoPath() {
            return repoPath;
        }

        public JcrFsItem getFsItem() {
            if (node != null) {
                return jcrRepoService.getFsItem(node, delegator);
            }
            if (repoPath != null) {
                return jcrRepoService.getFsItem(repoPath, delegator);
            }
            throw new IllegalArgumentException("Need either repoPath or node");
        }

        public FsItemCreator getCreator() {
            if (creator != null) {
                return creator;
            }
            String typeName;
            if (node != null) {
                typeName = JcrHelper.getPrimaryTypeName(node);
            } else if (repoPath != null) {
                typeName = jcrRepoService.getNodeTypeName(repoPath);
            } else {
                throw new IllegalArgumentException("Need either repoPath or node");
            }
            if (JcrTypes.NT_ARTIFACTORY_FILE.equals(typeName)) {
                creator = jcrFileCreator;
            } else if (JcrTypes.NT_ARTIFACTORY_FOLDER.equals(typeName)) {
                creator = jcrFolderCreator;
            } else {
                throw new IllegalStateException(
                        "Node " + repoPath + " has a type name " + typeName + " which is neither a file nor a folder?");
            }
            return creator;
        }

        public void setCreator(FsItemCreator creator) {
            this.creator = creator;
        }

        public JcrFsItem lock(JcrFsItem fsItem) {
            if (acquireReadLock && (fsItem != null)) {
                if (fsItem.isMutable()) {
                    throw new IllegalStateException("Cannot acquire read lock on mutable object " + fsItem);
                }
                if (!repoPath.equals(fsItem.getRepoPath())) {
                    throw new IllegalStateException(
                            "The repoPath '" + repoPath + "' is invalid for the object " + fsItem);
                }
                LockEntryId lockEntry = new LockEntryId(getLock(repoPath), repoPath);
                FsItemLockEntry sessionLockEntry = LockingHelper.readLock(lockEntry);
                // After the lock we know the entry cannot change so we recheck that fsItem is good
                JcrFsItem newFsItem = fsItemCache.get(repoPath);
                if (newFsItem != null && newFsItem != fsItem) {
                    // Change the fsItem and redo the read lock to update the session locks
                    fsItem = newFsItem;
                }
                sessionLockEntry.setReadFsItem(fsItem);
            }
            return fsItem;
        }
    }

    private static interface FsItemCreator<T extends JcrFsItem<? extends ItemInfo>> {
        public RuntimeException checkItemType(JcrFsItem item);

        public T newFsItem(RepoPath repoPath, StoringRepo repo);

        public T newFsItem(JcrFsItem copy, StoringRepo repo);
    }

    private static class JcrFileCreator implements FsItemCreator<JcrFile> {
        public RuntimeException checkItemType(JcrFsItem item) {
            if (item.isDirectory()) {
                return new FileExpectedException(item.getRepoPath());
            }
            return null;
        }

        public JcrFile newFsItem(RepoPath repoPath, StoringRepo repo) {
            return new JcrFile(repoPath, repo);
        }

        public JcrFile newFsItem(JcrFsItem copy, StoringRepo repo) {
            return new JcrFile((JcrFile) copy, repo);
        }
    }

    private static class JcrFolderCreator implements FsItemCreator<JcrFolder> {
        public RuntimeException checkItemType(JcrFsItem item) {
            if (!item.isDirectory()) {
                return new FolderExpectedException(item.getRepoPath());
            }
            return null;
        }

        public JcrFolder newFsItem(RepoPath repoPath, StoringRepo repo) {
            return new JcrFolder(repoPath, repo);
        }

        public JcrFolder newFsItem(JcrFsItem copy, StoringRepo repo) {
            return new JcrFolder((JcrFolder) copy, repo);
        }
    }
}
