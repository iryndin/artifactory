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

package org.artifactory.repo.jcr;

import com.google.common.collect.MapMaker;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.FilteredResourcesAddon;
import org.artifactory.addon.PropertiesAddon;
import org.artifactory.addon.RestCoreAddon;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.FolderExpectedException;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.exception.CancelException;
import org.artifactory.exception.IllegalNameException;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.ItemInfo;
import org.artifactory.fs.MutableFileInfo;
import org.artifactory.fs.MutableItemInfo;
import org.artifactory.fs.RepoResource;
import org.artifactory.fs.StatsInfo;
import org.artifactory.fs.ZipEntryRepoResource;
import org.artifactory.io.SimpleResourceStreamHandle;
import org.artifactory.io.StringResourceStreamHandle;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.factory.VfsItemFactory;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.lock.FsItemLockEntry;
import org.artifactory.jcr.lock.LockEntryId;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.jcr.lock.MonitoringReadWriteLock;
import org.artifactory.jcr.lock.aop.LockingAdvice;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.jcr.md.MetadataPersistenceHandler;
import org.artifactory.jcr.utils.JcrHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.MavenModelUtils;
import org.artifactory.md.MetadataInfo;
import org.artifactory.md.Properties;
import org.artifactory.mime.MavenNaming;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.SaveResourceContext;
import org.artifactory.repo.interceptor.StorageInterceptors;
import org.artifactory.repo.jcr.cache.expirable.CacheExpiry;
import org.artifactory.repo.jcr.local.LocalNonCacheOverridable;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.InternalRequestContext;
import org.artifactory.request.RepoRequests;
import org.artifactory.request.Request;
import org.artifactory.request.RequestContext;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.MetadataResource;
import org.artifactory.resource.MutableRepoResourceInfo;
import org.artifactory.resource.RepoResourceInfo;
import org.artifactory.resource.ResolvedResource;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.sapi.fs.VfsFolder;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.storage.StorageConstants;
import org.artifactory.util.ExceptionUtils;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class StoringRepoMixin<T extends RepoDescriptor> implements StoringRepo<T> {
    private static final Logger log = LoggerFactory.getLogger(StoringRepoMixin.class);

    private final JcrFileCreator jcrFileCreator = new JcrFileCreator();
    private final JcrFolderCreator jcrFolderCreator = new JcrFolderCreator();
    private String repoRootPath;
    private ConcurrentMap<RepoPath, MonitoringReadWriteLock> locks;
    private Map<RepoPath, JcrFsItem> fsItemCache;
    private StorageInterceptors interceptors;
    private JcrRepoService jcrRepoService;
    private JcrService jcrService;
    private AddonsManager addonsManager;

    private RepoPath rootRepoPath;
    private final StoringRepo<T> delegator;
    private StoringRepoMixin<T> oldStoringRepo;
    private AuthorizationService authorizationService;

    public StoringRepoMixin(StoringRepo<T> delegator, StoringRepo<T> oldStoringRepo) {
        this.delegator = delegator;
        if (oldStoringRepo instanceof StoringRepoMixin) {
            this.oldStoringRepo = (StoringRepoMixin<T>) oldStoringRepo;
        }
    }

    @Override
    public void init() {
        InternalArtifactoryContext context = InternalContextHelper.get();

        authorizationService = context.beanForType(AuthorizationService.class);

        // TODO: should select the interceptors depending on the repo type
        this.interceptors = context.beanForType(StorageInterceptors.class);
        this.jcrRepoService = context.getJcrRepoService();
        this.jcrService = context.getJcrService();
        addonsManager = context.beanForType(AddonsManager.class);

        //init caches
        if (oldStoringRepo != null) {
            // The locks should be kept at all cost :)
            locks = oldStoringRepo.locks;

            // Copy the cached fsitems if the cache behavior in descriptors did not changed
            RepoDescriptor oldDescriptor = oldStoringRepo.getDescriptor();
            RepoDescriptor currentDescriptor = delegator.getDescriptor();
            if (oldDescriptor == null || currentDescriptor == null) {
                // Just logged the bad state, the cached will be cleaned
                IllegalStateException ex = new IllegalStateException("Current or Old Descriptors are null!");
                log.warn(ex.getMessage(), ex);
            } else {
                //Reuse the fsItem cache
                if (currentDescriptor.identicalCache(oldDescriptor)) {
                    fsItemCache = oldStoringRepo.fsItemCache;
                } else {
                    fsItemCache = createFsItemCache();
                }
            }
            oldStoringRepo = null;
        } else {
            fsItemCache = createFsItemCache();
            locks = new MapMaker().softValues().initialCapacity(2000).expireAfterWrite(
                    ConstantValues.fsItemCacheIdleTimeSecs.getLong(), TimeUnit.SECONDS).makeMap();
        }

        //Create the repo node if it doesn't exist
        if (!jcrService.itemNodeExists(PathFactoryHolder.get().getAbsolutePath(rootRepoPath))) {
            JcrFolder rootJcrFolder = getLockedJcrFolder(rootRepoPath, true);
            rootJcrFolder.mkdir();
        }
    }

    @Override
    public void destroy() {
    }

    @Override
    public String getKey() {
        return delegator.getKey();
    }

    @Override
    public String getDescription() {
        return delegator.getDescription();
    }

    @Override
    public boolean isReal() {
        return delegator.isReal();
    }

    @Override
    public boolean isLocal() {
        return delegator.isLocal();
    }

    @Override
    public boolean isCache() {
        return delegator.isCache();
    }

    @Override
    public InternalRepositoryService getRepositoryService() {
        return delegator.getRepositoryService();
    }

    @Override
    public String getRepoRootPath() {
        return repoRootPath;
    }

    @Override
    public void clearCaches() {
        fsItemCache.clear();
        locks.clear();
    }

    @Override
    public VfsFolder getRootFolder() {
        return getJcrFolder(rootRepoPath);
    }

    @Override
    public JcrFolder getLockedRootFolder() {
        return getLockedJcrFolder(rootRepoPath, false);
    }

    @Override
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
    @Override
    public JcrFsItem getLocalJcrFsItem(String relPath) {
        RepoPath repoPath = InternalRepoPathFactory.create(getKey(), relPath);
        return getJcrFsItem(repoPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JcrFsItem getJcrFsItem(RepoPath repoPath) {
        return internalGetFsItem(new JcrFsItemLocator(repoPath, true, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JcrFsItem getJcrFsItem(Node node) {
        return internalGetFsItem(new JcrFsItemLocator(node, true));
    }

    @Override
    public JcrFile getLocalJcrFile(String relPath) throws FileExpectedException {
        RepoPath repoPath = InternalRepoPathFactory.create(getKey(), relPath);
        return getJcrFile(repoPath);
    }

    @Override
    public JcrFile getJcrFile(RepoPath repoPath) throws FileExpectedException {
        JcrFsItem item = getJcrFsItem(repoPath);
        if (item != null && !item.isFile()) {
            throw new FileExpectedException(repoPath);
        }
        return (JcrFile) item;
    }

    @Override
    public JcrFolder getLocalJcrFolder(String relPath) throws FolderExpectedException {
        RepoPath repoPath = InternalRepoPathFactory.create(getKey(), relPath);
        return getJcrFolder(repoPath);
    }

    @Override
    public JcrFolder getJcrFolder(RepoPath repoPath) throws FolderExpectedException {
        JcrFsItem item = getJcrFsItem(repoPath);
        if (item != null && !item.isDirectory()) {
            throw new FolderExpectedException(repoPath);
        }
        return (JcrFolder) item;
    }

    @Override
    public JcrFsItem getLockedJcrFsItem(String relPath) {
        RepoPath repoPath = InternalRepoPathFactory.create(getKey(), relPath);
        return getLockedJcrFsItem(repoPath);
    }

    @Override
    public JcrFsItem getLockedJcrFsItem(RepoPath repoPath) {
        JcrFsItemLocator locator = new JcrFsItemLocator(repoPath, false, false);
        return internalGetLockedJcrFsItem(locator);
    }

    @Override
    public JcrFsItem getLockedJcrFsItem(Node node) {
        JcrFsItemLocator locator = new JcrFsItemLocator(node, false);
        return internalGetLockedJcrFsItem(locator);
    }

    @Override
    public JcrFile getLockedJcrFile(String relPath, boolean createIfMissing) throws FileExpectedException {
        RepoPath repoPath = InternalRepoPathFactory.create(getKey(), relPath);
        return getLockedJcrFile(repoPath, createIfMissing);
    }

    @Override
    public JcrFile getLockedJcrFile(RepoPath repoPath, boolean createIfMissing) throws FileExpectedException {
        JcrFsItemLocator locator = new JcrFsItemLocator(repoPath, false, createIfMissing);
        locator.setCreator(jcrFileCreator);
        return (JcrFile) internalGetLockedJcrFsItem(locator);
    }

    @Override
    public JcrFolder getLockedJcrFolder(String relPath, boolean createIfMissing) throws FolderExpectedException {
        RepoPath repoPath = InternalRepoPathFactory.create(getKey(), relPath);
        return getLockedJcrFolder(repoPath, createIfMissing);
    }

    @Override
    public JcrFolder getLockedJcrFolder(RepoPath repoPath, boolean createIfMissing) throws FolderExpectedException {
        JcrFsItemLocator locator = new JcrFsItemLocator(repoPath, false, createIfMissing);
        locator.setCreator(jcrFolderCreator);
        return (JcrFolder) internalGetLockedJcrFsItem(locator);
    }

    /**
     * Return <b>locally</b> stored file resource. Unfound resource will be returned if the requested resource doesn't
     * exist locally.
     */
    @Override
    public RepoResource getInfo(InternalRequestContext context) throws FileExpectedException {
        RestCoreAddon restCoreAddon = addonsManager.addonByType(RestCoreAddon.class);
        context = restCoreAddon.getDynamicVersionContext(this, context, false);
        String path = context.getResourcePath();
        RepoPath repoPath = InternalRepoPathFactory.create(getKey(), path);
        JcrFsItem<?, ?> item = getPathItem(repoPath);
        if (item == null) {
            RepoRequests.logToContext("Unable to find resource in %s", repoPath);
            return new UnfoundRepoResource(repoPath, "File not found.");
        }
        RepoResource localRes;
        if (NamingUtils.isMetadata(path)) {
            //When requesting a property/metadata return a special resource class that contains the parent node
            //path and the metadata name.
            String metadataName = NamingUtils.getMetadataName(path);
            RepoRequests.logToContext("Identified resource as metadata '%s'", metadataName);
            MetadataDefinition definition;
            try {
                definition = InternalContextHelper.get().getMetadataDefinitionService()
                        .getMetadataDefinition(metadataName, true);
            } catch (IllegalNameException e) {
                RepoRequests.logToContext("Error occurred while retrieving metadata definition - " +
                        "returning unfound resource: %s", e.getMessage());
                return new UnfoundRepoResource(repoPath, e.getMessage());
            }
            MetadataInfo info = definition.getPersistenceHandler().getMetadataInfo(item);
            if (info == null) {
                RepoRequests.logToContext("Unable to retrieve metadata info - returning unfound resource");
                return new UnfoundRepoResource(repoPath, "metadata " + " not found for " + item);
            }

            PropertiesAddon propertiesAddon = addonsManager.addonByType(PropertiesAddon.class);
            localRes = propertiesAddon.assembleDynamicMetadata(info, item, context, repoPath);
            if ((localRes instanceof MetadataResource) && MavenNaming.isSnapshotMavenMetadata(path)) {
                localRes = resolveMavenMetadataForCompatibility(item, ((MetadataResource) localRes), context);
            }
        } else {
            if (item.isDirectory()) {
                RepoRequests.logToContext("Identified resource as a directory - throwing file expected exception");
                throw new FileExpectedException(repoPath);
            }
            RepoRequests.logToContext("Identified resource as a file");
            //Handle query-aware get
            Properties queryProperties = context.getProperties();
            boolean exactMatch = true;
            if (!queryProperties.isEmpty()) {
                RepoRequests.logToContext("Request includes query properties");
                Properties properties = item.getMetadata(Properties.class);
                Properties.MatchResult matchResult = properties.matchQuery(queryProperties);
                if (matchResult == Properties.MatchResult.NO_MATCH) {
                    exactMatch = false;
                    RepoRequests.logToContext("Request query properties don't match those that annotate the artifact");
                } else if (matchResult == Properties.MatchResult.CONFLICT) {
                    RepoRequests.logToContext("Request query properties conflict with those that annotate the " +
                            "artifact - returning unfound resource");
                    return new UnfoundRepoResource(repoPath, "File '" + repoPath +
                            "' was found, but mandatory properties do not match.");
                }
            }

            localRes = getFilteredOrFileResource((JcrFile) item, repoPath, context, exactMatch);

            if (!localRes.isFound()) {
                return localRes;
            }
        }
        //Release the read lock early
        RepoRequests.logToContext("Releasing the read lock from the resource");
        RepoPath lockedRepoPath = NamingUtils.getLockingTargetRepoPath(repoPath);
        LockingHelper.releaseReadLock(lockedRepoPath);
        return localRes;
    }

    @Override
    public ResourceStreamHandle getResourceStreamHandle(InternalRequestContext requestContext, final RepoResource res)
            throws IOException, RepositoryException {
        RepoRequests.logToContext("Creating a resource handle from '%s'", res.getResponseRepoPath().getRepoKey());
        String relPath = res.getRepoPath().getPath();
        //If we are dealing with metadata will return the md container item
        JcrFsItem item = getLocalJcrFsItem(relPath);
        //If resource does not exist throw an IOException
        if (item == null) {
            RepoRequests.logToContext("Unable to find the resource - throwing exception");
            throw new IOException("Could not get resource stream. Path '" + relPath + "' not found in " + this);
        }
        if (item.isDeleted() || !item.exists()) {
            RepoRequests.logToContext("Requested resource was deleted - throwing exception");
            item.setDeleted(true);
            throw new IOException("Could not get resource stream. Item " + item + " was deleted!");
        }
        ResourceStreamHandle handle;
        if (res.isMetadata()) {
            String metadataName = res.getInfo().getName();
            RepoRequests.logToContext("Identified requested resource as metadata of type '%s'", metadataName);
            String xmlMetadata = item.getXmlMetadata(metadataName);
            if (xmlMetadata == null) {
                RepoRequests.logToContext("Unable to find the metadata content - throwing exception");
                throw new IOException("Could not get resource stream. Stream not found: " + res + ".");
            } else {
                RepoRequests.logToContext("Resolved metadata content - using a string resource handle");
                handle = new StringResourceStreamHandle(xmlMetadata);
            }
        } else if (item.isFile()) {
            RepoRequests.logToContext("Identified requested resource as a file");
            JcrFile jcrFile = (JcrFile) item;
            final InputStream is = jcrFile.getStream();
            //Update the stats queue counter
            if (jcrFile.updateDownloadStats() && !willOrIsWriteLocked(jcrFile.getRepoPath())) {
                //Send the async event to save the stats
                getRepositoryService().markForSaveOnCommit(jcrFile.getRepoPath());
            }

            Request request = requestContext.getRequest();
            if (request != null && request.isZipResourceRequest() && (res instanceof ZipEntryRepoResource)) {
                RepoRequests.logToContext("Requested resource is contained within an archive - " +
                        "using specialized handle");
                handle = addonsManager.addonByType(FilteredResourcesAddon.class).getZipResourceHandle(res, is);
            } else {
                RepoRequests.logToContext("Requested resource is an ordinary artifact - " +
                        "using normal content handle with length '%s'", jcrFile.getSize());
                handle = new SimpleResourceStreamHandle(is, jcrFile.getSize());
            }

        } else {
            RepoRequests.logToContext("Identified requested resource as a folder - throwing exception");
            throw new IOException("Could not get resource stream from a folder " + res + ".");
        }
        return handle;
    }

    @Override
    public ModuleInfo getItemModuleInfo(String itemPath) {
        return delegator.getItemModuleInfo(itemPath);
    }

    @Override
    public ModuleInfo getArtifactModuleInfo(String artifactPath) {
        return delegator.getArtifactModuleInfo(artifactPath);
    }

    @Override
    public ModuleInfo getDescriptorModuleInfo(String descriptorPath) {
        return delegator.getDescriptorModuleInfo(descriptorPath);
    }

    @Override
    public RepoPath getRepoPath(String path) {
        return delegator.getRepoPath(path);
    }

    @Override
    public String getChecksum(String checksumFileUrl, RepoResource resource) throws IOException {
        if (resource == null || !resource.isFound()) {
            throw new IOException("Could not get resource stream. Path not found: " + checksumFileUrl + ".");
        }
        ChecksumType checksumType = ChecksumType.forFilePath(checksumFileUrl);
        if (checksumType == null) {
            throw new IllegalArgumentException("Checksum type not found for path " + checksumFileUrl);
        }
        return getChecksumPolicy().getChecksum(checksumType, resource.getInfo().getChecksums(),
                resource.getRepoPath());
    }

    @Override
    public ChecksumPolicy getChecksumPolicy() {
        return delegator.getChecksumPolicy();
    }

    @Override
    public void undeploy(RepoPath repoPath) {
        undeploy(repoPath, true);
    }

    @Override
    public void undeploy(RepoPath repoPath, boolean calcMavenMetadata) {
        VfsItem item = getLockedJcrFsItem(repoPath);
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
                        if (!folderForMetadataCalculation.isRoot()) {
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
    }

    /**
     * Create the resource in the local repository
     */
    @Override
    public RepoResource saveResource(SaveResourceContext context) throws IOException, RepoRejectException {
        RepoResource res = context.getRepoResource();
        RepoPath repoPath = InternalRepoPathFactory.create(getKey(), res.getRepoPath().getPath());
        try {
            InputStream in = context.getInputStream();
            if (res.isMetadata()) {
                //If we are dealing with metadata set it on the containing fsitem
                RepoPath metadataContainerRepoPath = NamingUtils.getLockingTargetRepoPath(repoPath);
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
                        //If there is no container return un-found
                        return new UnfoundRepoResource(repoPath,
                                "No metadata container exists: " + metadataContainerRepoPath
                        );
                    }
                }
                String metadataName = res.getInfo().getName();
                String metadataContent = IOUtils.toString(in, "utf-8");
                jcrFsItem.setXmlMetadata(metadataName, metadataContent);
                updateResourceSize(res, metadataContent.length());
            } else {
                //Create the parent folder if it does not exist
                RepoPath parentPath = repoPath.getParent();
                if (parentPath == null) {
                    throw new RepositoryException("Cannot save resource, no parent repo path exists");
                }

                //Write lock auto upgrade supported LockingHelper.releaseReadLock(repoPath);
                JcrFile jcrFile = getLockedJcrFile(repoPath, true);
                onBeforeCreate(jcrFile);
                createParentDir(parentPath);

                /**
                 * If the file isn't a non-unique snapshot and it already exists, create a defensive of the checksums
                 * info for later comparison
                 */
                boolean artifactNonUniqueSnapshot = MavenNaming.isNonUniqueSnapshot(repoPath.getId());
                ChecksumsInfo existingChecksumsToCompare = setResourceChecksums(res, jcrFile,
                        artifactNonUniqueSnapshot);

                fillJcrFileData(res, in, jcrFile);

                if (jarValidationRequired(repoPath)) {
                    validateArtifactIfRequired(jcrFile, repoPath);
                }

                /**
                 * Populating info details from request should come after fillJcrFileData() since the method sets it's
                 * own values too
                 */
                populateItemInfoDetails(context, jcrFile);

                setArtifactActualChecksums(res, jcrFile, artifactNonUniqueSnapshot, existingChecksumsToCompare);

                //Add default stats
                jcrFile.setMetadata(StatsInfo.class, InfoFactoryHolder.get().createStats());
                updateResourceSize(res, jcrFile.getSize());
                createJcrFile(res, jcrFile);

                //Save properties
                Properties properties = context.getProperties();
                if ((properties != null) && !properties.isEmpty()) {
                    jcrFile.setMetadata(Properties.class, properties);
                }
            }
            return res;
        } catch (Exception e) {
            // throw back any RepoRejectException (ChecksumPolicyException or wrapped CancelException)
            Throwable rejectException = ExceptionUtils.getCauseOfTypes(e, RepoRejectException.class);
            if (rejectException != null) {
                context.setException(rejectException);
                throw (RepoRejectException) rejectException;
            }
            //Unwrap any IOException and throw it
            Throwable ioCause = ExceptionUtils.getCauseOfTypes(e, IOException.class);
            if (ioCause != null) {
                log.warn("Could not save resource '{}': {}", res.getRepoPath(), ioCause.getMessage());
                context.setException(ioCause);
                throw (IOException) ioCause;
            }
            context.setException(e);
            throw new RuntimeException("Could not save resource '" + res.getRepoPath() + "'.", e);
        }
    }

    private void updateResourceSize(RepoResource res, long size) {
        // if the resource info is mutable, update/set the real resource size
        RepoResourceInfo info = res.getInfo();
        if (res.getInfo() instanceof MutableRepoResourceInfo) {
            ((MutableRepoResourceInfo) info).setSize(size);
        }
    }

    private boolean jarValidationRequired(RepoPath repoPath) {
        if (!NamingUtils.isJarVariant(repoPath.getPath())) {
            return false;
        }

        RemoteRepoDescriptor remoteRepoDescriptor;

        T repoDescriptor = getDescriptor();
        if (repoDescriptor instanceof RemoteRepoDescriptor) {
            remoteRepoDescriptor = ((RemoteRepoDescriptor) repoDescriptor);
        } else if (repoDescriptor instanceof LocalCacheRepoDescriptor) {
            remoteRepoDescriptor = ((LocalCacheRepoDescriptor) repoDescriptor).getRemoteRepo();
        } else {
            return false;
        }

        if (!remoteRepoDescriptor.isRejectInvalidJars()) {
            return false;
        }
        return true;
    }

    private void validateArtifactIfRequired(JcrFile file, RepoPath repoPath) throws IOException, RepoRejectException {
        String pathId = repoPath.getId();
        InputStream jarStream = file.getStream();
        try {
            log.info("Validating the content of '{}'.", pathId);
            JarInputStream jarInputStream = new JarInputStream(jarStream, true);
            JarEntry entry = jarInputStream.getNextJarEntry();

            if (entry == null) {
                throw new IllegalStateException("Could not find entries within the archive.");
            }
            do {
                log.trace("Found the entry '{}' validating the content of '{}'.", entry.getName(), pathId);
            } while ((jarInputStream.available() == 1) && (entry = jarInputStream.getNextJarEntry()) != null);

            log.info("Finished validating the content of '{}'.", pathId);
        } catch (Exception e) {
            String message = String.format("Failed to validate the content of '%s': %s", pathId, e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug(message, e);
            } else {
                log.error(message);
            }
            file.bruteForceDelete();
            throw new RepoRejectException(message, HttpStatus.SC_CONFLICT);
        } finally {
            IOUtils.closeQuietly(jarStream);
        }
    }

    private void onBeforeCreate(JcrFile jcrFile) throws RepoRejectException {
        //Test if beforeCreate caused cancellation
        BasicStatusHolder statusHolder = new BasicStatusHolder();
        interceptors.beforeCreate(jcrFile, statusHolder);
        CancelException cancelException = statusHolder.getCancelException();
        if (cancelException != null) {
            LockingHelper.removeLockEntry(jcrFile.getRepoPath());
            throw new RepoRejectException(cancelException);
        }
    }

    private void createParentDir(RepoPath parentPath) {
        if (!itemExists(parentPath.getPath())) {
            JcrFolder jcrFolder = getLockedJcrFolder(parentPath, true);
            jcrFolder.mkdirs();
        }
    }

    private ChecksumsInfo setResourceChecksums(RepoResource res, JcrFile jcrFile,
            boolean artifactNonUniqueSnapshot) {
        ChecksumsInfo existingChecksumsToCompare = null;
        if (!artifactNonUniqueSnapshot && jcrFile.exists()) {
            existingChecksumsToCompare = new ChecksumsInfo(jcrFile.getInfo().getChecksumsInfo());
            log.debug("Overriding {} with checksums: {}", jcrFile.getRepoPath(), existingChecksumsToCompare);
        }

        // set the file extension checksums (only needed if the file is currently being downloaded)
        Set<ChecksumInfo> resourceChecksums = res.getInfo().getChecksums();
        jcrFile.getMutableInfo().setChecksums(resourceChecksums);
        return existingChecksumsToCompare;
    }

    private void populateItemInfoDetails(SaveResourceContext context, JcrFile jcrFile) {
        if (!authorizationService.isAdmin()) {
            return;
        }
        MutableFileInfo info = jcrFile.getMutableInfo();

        long created = context.getCreated();
        if (created > 0) {
            info.setCreated(created);
        }

        String createBy = context.getCreateBy();
        if (StringUtils.isNotBlank(createBy)) {
            info.setCreatedBy(createBy);
        }

        String modifiedBy = context.getModifiedBy();
        if (StringUtils.isNotBlank(modifiedBy)) {
            info.setModifiedBy(modifiedBy);
        }
    }

    private long fillJcrFileData(RepoResource res, InputStream in, JcrFile jcrFile) throws Exception {
        //Deploy
        long lastModified = res.getInfo().getLastModified();
        final long start = System.currentTimeMillis();
        try {
            jcrFile.fillData(lastModified, in); //May throw a checksum error
        } catch (Exception e) {
            //Make sure the file is not saved
            jcrFile.bruteForceDelete();
            throw e;
        }
        return start;
    }

    private void setArtifactActualChecksums(RepoResource res, JcrFile jcrFile, boolean artifactNonUniqueSnapshot,
            ChecksumsInfo existingChecksumsToCompare) throws RepositoryException {
        ChecksumsInfo newlyCalculatedChecksums = jcrFile.getInfo().getChecksumsInfo();
        // update the resource with actual checksums (calculated in fillData) - RTFACT-3112
        ((MutableRepoResourceInfo) res.getInfo()).setChecksums(newlyCalculatedChecksums.getChecksums());

        /**
         * If the artifact is not a non-unique snapshot and already exists but with a different checksum, remove
         * all the existing metadata
         */
        if (!artifactNonUniqueSnapshot && jcrFile.exists() && (existingChecksumsToCompare != null)) {

            if (checksumsDiffer(existingChecksumsToCompare, newlyCalculatedChecksums)) {
                Set<MetadataDefinition<?, ?>> metadataDefinitions = VfsItemFactory.getExistingMetadata(jcrFile, false);
                for (MetadataDefinition<?, ?> metadataDefinition : metadataDefinitions) {
                    MetadataPersistenceHandler<?, ?> persistenceHandler = metadataDefinition.getPersistenceHandler();
                    persistenceHandler.remove(jcrFile);
                }
            }
        }
    }

    private void createJcrFile(RepoResource res, JcrFile jcrFile) {
        if (log.isDebugEnabled()) {
            log.debug("Saved resource '{}' with length {} into repository '{}'.",
                    new Object[]{res, jcrFile.getSize(), this});
        }
        onCreate(jcrFile);
    }

    /**
     * Compares the actual checksums of the given checksums info
     *
     * @param existingChecksumsToCompare Checksums info of existing file
     * @param newChecksumsToCompare      Checksums info of newly deployed file
     * @return True if the checksums are different
     */
    private boolean checksumsDiffer(
            ChecksumsInfo existingChecksumsToCompare, ChecksumsInfo newChecksumsToCompare) {
        for (ChecksumType checksumType : ChecksumType.values()) {
            ChecksumInfo existingChecksum = existingChecksumsToCompare.getChecksumInfo(checksumType);
            ChecksumInfo newChecksum = newChecksumsToCompare.getChecksumInfo(checksumType);
            if (newChecksum == null || newChecksum.getActual() == null) {
                log.warn("New {} checksum is null ({}).", checksumType, newChecksum);
                return true;
            }
            if (existingChecksum == null || existingChecksum.getActual() == null) {
                log.warn("Existing {} checksum is null ({}).", checksumType, existingChecksum);
                return true;
            }
            if (!existingChecksum.getActual().equals(newChecksum.getActual())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldProtectPathDeletion(String path, boolean overwrite) {
        if (NamingUtils.isChecksum(path)) {
            //Never protect checksums
            return false;
        }

        if (overwrite) {
            if (isCache()) {
                if ((ContextHelper.get().beanForType(CacheExpiry.class).isExpirable(
                        ((LocalCacheRepo) delegator), path))) {
                    return false;
                }
            } else {
                if (ContextHelper.get().beanForType(LocalNonCacheOverridable.class)
                        .isOverridable(((LocalRepo) delegator), path)) {
                    return false;
                }
            }
            if (NamingUtils.isMetadata(path)) {
                return false;
            }

            /**
             * This condition determines:
             * - If the item is a file (means that is also exists), protect.
             * - If the item is not a file (it's either a folder or doesn't exist), don't protect.
             */
            RepoPath repoPath = InternalRepoPathFactory.create(getKey(), path);
            if (!jcrService.isFile(repoPath)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean itemExists(String relPath) {
        assert relPath != null;
        if (relPath.length() > 0) {
            return jcrService.itemNodeExists(repoRootPath + "/" + relPath);
        } else {
            //The repo itself
            return true;
        }
    }

    @Override
    public List<String> getChildrenNames(String relPath) {
        return jcrRepoService.getChildrenNames(repoRootPath + "/" + relPath);
    }

    /**
     * Deletes the contents of this repository.
     */
    public void deleteRepositoryContent() {
        JcrFolder rootFolder = getLockedRootFolder();
        //Delete 1st level children
        List<VfsItem> children = rootFolder.getItems(true);
        for (VfsItem child : children) {
            try {
                child.delete();
            } catch (Exception e) {
                log.error("Could not delete repository child node '{}'.", child.getName(), e);
            }
        }
        //Move the deleted item to the trash
        jcrRepoService.trash(children);
    }

    @Override
    public void onCreate(JcrFsItem fsItem) {
        MutableStatusHolder holder = new MultiStatusHolder();
        // update the item info as late as possible, just before calling the on create interceptors (normally the update
        // is called after the tx commit but it is too late for the interceptors)
        if (fsItem.isFile()) {
            JcrFile jcrFile = (JcrFile) fsItem;
            InternalContextHelper.get().getMetadataDefinitionService()
                    .getFileInfoMd().getPersistenceHandler().update(jcrFile, jcrFile.getMutableInfo());
        }
        interceptors.afterCreate(fsItem, holder);
        // TODO: Check the statusHolder
        RepoPath repoPath = fsItem.getRepoPath();
        AccessLogger.deployed(repoPath);
    }

    @Override
    public void onDelete(JcrFsItem fsItem) {
        interceptors.afterDelete(fsItem, new BasicStatusHolder());
        AccessLogger.deleted(fsItem.getRepoPath());
    }

    @Override
    public void setDescriptor(T descriptor) {
        repoRootPath = PathFactoryHolder.get().getRepoRootPath(getKey());
        rootRepoPath = InternalRepoPathFactory.create(getKey(), "");
    }

    @Override
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
                    "Trying to retrieve resource '" + repoPath + "' from local repo '" + getKey() + "'.");
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

    @Override
    public boolean willOrIsWriteLocked(RepoPath path) {
        MonitoringReadWriteLock lock = locks.get(path);
        return lock != null && lock.willOrIsWriteLock();
    }

    @Override
    public StoringRepo<T> getStorageMixin() {
        return this;
    }

    private MonitoringReadWriteLock getLock(RepoPath path) {
        MonitoringReadWriteLock newLock = new MonitoringReadWriteLock();
        MonitoringReadWriteLock oldLock = locks.putIfAbsent(path, newLock);
        return oldLock != null ? oldLock : newLock;
    }

    private JcrFsItem internalGetLockedJcrFsItem(JcrFsItemLocator locator) {
        RepoPath repoPath = locator.getRepoPath();
        try {
            // Create a lock entry for the repo path
            LockEntryId lockEntryId = new LockEntryId(getLock(repoPath), repoPath);
            // acquire the write lock
            FsItemLockEntry sessionLockEntry = LockingHelper.writeLock(lockEntryId);
            if (sessionLockEntry.getLockedFsItem() != null) {
                return (JcrFsItem) sessionLockEntry.getLockedFsItem();
            }
            JcrFsItem fsItem = internalGetFsItem(locator);
            FsItemCreator creator;
            if (fsItem != null) {
                creator = locator.getCreator();
                creator.verifyItemType(fsItem);
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
    private <T extends JcrFsItem<? extends ItemInfo, ? extends MutableItemInfo>> T internalGetLockedJcrFsItem(
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

    private Map<RepoPath, JcrFsItem> createFsItemCache() {
        return new MapMaker().softValues().initialCapacity(5000).expireAfterWrite(
                ConstantValues.fsItemCacheIdleTimeSecs.getLong(), TimeUnit.SECONDS).makeMap();
    }

    private RepoResource getFilteredOrFileResource(JcrFile file, RepoPath repoPath, RequestContext context,
            boolean exactMatch) {
        Request request = context.getRequest();
        if (request != null && file.getRepo().isReal()) {
            FilteredResourcesAddon filteredResourcesAddon = addonsManager.addonByType(FilteredResourcesAddon.class);
            if (filteredResourcesAddon.isFilteredResourceFile(repoPath)) {
                RepoRequests.logToContext("Resource is marked as filtered - sending it through the engine");
                InputStream stream = file.getStream();
                try {
                    return filteredResourcesAddon.getFilteredResource(request, file.getInfo(), stream);
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            }
        }

        if (request != null && request.isZipResourceRequest()) {
            RepoRequests.logToContext("Resource is contained within an archiving - retrieving");
            InputStream stream = file.getStream();
            try {
                return addonsManager.addonByType(FilteredResourcesAddon.class).getZipResource(
                        request, file.getInfo(), stream);
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }

        return new FileResource(file.getInfo(), exactMatch);
    }

    @Override
    public boolean isSuppressPomConsistencyChecks() {
        return false;
    }

    private RepoResource resolveMavenMetadataForCompatibility(JcrFsItem<?, ?> item, MetadataResource metadataResource,
            InternalRequestContext context) {
        if (ConstantValues.mvnMetadataVersion3Enabled.getBoolean() && !context.clientSupportsM3SnapshotVersions()) {
            RepoRequests.logToContext("Use of v3 Maven metadata is enabled, but the requesting client doesn't " +
                    "support it - checking if the response should be modified for compatibility");
            MetadataInfo info = metadataResource.getInfo();
            MetadataDefinition metadataDefinition = InternalContextHelper.get().getMetadataDefinitionService()
                    .getMetadataDefinition(info.getName(), false);
            try {
                Metadata metadata = MavenModelUtils.toMavenMetadata(
                        (String) metadataDefinition.getPersistenceHandler().read(item));
                Versioning versioning = metadata.getVersioning();
                if (versioning != null) {
                    List<SnapshotVersion> snapshotVersions = versioning.getSnapshotVersions();
                    if ((snapshotVersions != null) && !snapshotVersions.isEmpty()) {
                        RepoRequests.logToContext("Found snapshot versions - modifying the response for compatibility");
                        versioning.setSnapshotVersions(null);
                        return new ResolvedResource(metadataResource, MavenModelUtils.mavenMetadataToString(metadata));
                    }
                }
            } catch (IOException e) {
                RepoPath repoPath = info.getRepoPath();
                log.error("An error occurred while filtering Maven metadata '{}' for compatibility: " +
                        "{}\nReturning original content.", repoPath, e.getMessage());
                RepoRequests.logToContext("An error occurred while filtering for compatibility. Returning the " +
                        "original content: %s", e.getMessage());
            }
        }
        //In case none of the above apply, or if we encountered an error, return the original resource
        return metadataResource;
    }

    private class JcrFsItemLocator {
        private final RepoPath repoPath;
        private final Node node;
        private final boolean acquireReadLock;
        private final boolean createIfEmpty;
        private FsItemCreator creator = null;

        JcrFsItemLocator(RepoPath repoPath, boolean acquireReadLock, boolean createIfEmpty) {
            //If we are dealing with metadata return the containing fsitem
            this.repoPath = NamingUtils.getLockingTargetRepoPath(repoPath);
            this.node = null;
            this.acquireReadLock = acquireReadLock;
            this.createIfEmpty = createIfEmpty;
        }

        JcrFsItemLocator(Node node, boolean acquireReadLock) {
            this.repoPath = PathFactoryHolder.get().getRepoPath(JcrHelper.getAbsolutePath(node));
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
            if (StorageConstants.NT_ARTIFACTORY_FILE.equals(typeName)) {
                creator = jcrFileCreator;
            } else if (StorageConstants.NT_ARTIFACTORY_FOLDER.equals(typeName)) {
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

    private static interface FsItemCreator<T extends JcrFsItem<? extends ItemInfo, ? extends MutableItemInfo>> {
        public void verifyItemType(JcrFsItem item);

        public T newFsItem(RepoPath repoPath, StoringRepo repo);

        public T newFsItem(JcrFsItem copy, StoringRepo repo);
    }

    private static class JcrFileCreator implements FsItemCreator<JcrFile> {
        @Override
        public void verifyItemType(JcrFsItem item) {
            if (item.isDirectory()) {
                throw new FileExpectedException(item.getRepoPath());
            }
        }

        @Override
        public JcrFile newFsItem(RepoPath repoPath, StoringRepo repo) {
            return new JcrFile(repoPath, repo);
        }

        @Override
        public JcrFile newFsItem(JcrFsItem copy, StoringRepo repo) {
            return new JcrFile((JcrFile) copy, repo);
        }
    }

    private static class JcrFolderCreator implements FsItemCreator<JcrFolder> {
        @Override
        public void verifyItemType(JcrFsItem item) {
            if (!item.isDirectory()) {
                throw new FolderExpectedException(item.getRepoPath());
            }
        }

        @Override
        public JcrFolder newFsItem(RepoPath repoPath, StoringRepo repo) {
            return new JcrFolder(repoPath, repo);
        }

        @Override
        public JcrFolder newFsItem(JcrFsItem copy, StoringRepo repo) {
            return new JcrFolder((JcrFolder) copy, repo);
        }
    }
}
