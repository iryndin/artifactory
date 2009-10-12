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

package org.artifactory.repo.jcr;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.FileInfo;
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
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.io.SimpleResourceStreamHandle;
import org.artifactory.io.StringResourceStreamHandle;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.io.checksum.policy.ChecksumPolicyException;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.lock.FsItemLockEntry;
import org.artifactory.jcr.lock.ImmutableLockEntry;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.jcr.md.MetadataService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.context.RequestContext;
import org.artifactory.repo.interceptor.RepoInterceptors;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.*;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.traffic.InternalTrafficService;
import org.artifactory.traffic.entry.UploadEntry;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import javax.jcr.Node;
import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class StoringRepoMixin<T extends RepoDescriptor> implements StoringRepo<T> {
    private static final Logger log = LoggerFactory.getLogger(StoringRepoMixin.class);

    private final JcrFileCreator jcrFileCreator = new JcrFileCreator();
    private final JcrFolderCreator jcrFolderCreator = new JcrFolderCreator();
    private String repoRootPath;
    private Map<RepoPath, ReentrantReadWriteLock> locks;
    private Map<RepoPath, JcrFsItem> fsItemCache;
    private RepoInterceptors interceptors;
    private JcrRepoService jcrService;
    private InternalTrafficService trafficService;
    private RepoPath rootRepoPath;

    private final StoringRepo<T> delegator;

    public StoringRepoMixin(StoringRepo<T> delegator) {
        this.delegator = delegator;
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

    public InternalRepositoryService getRepositoryService() {
        return delegator.getRepositoryService();
    }

    public MetadataService getMetadataService() {
        return delegator.getMetadataService();
    }

    public void init() {
        InternalArtifactoryContext context = InternalContextHelper.get();
        // TODO: should select the interceptors depending on the repo type
        interceptors = context.beanForType(RepoInterceptors.class);
        jcrService = context.getJcrRepoService();
        trafficService = context.beanForType(InternalTrafficService.class);

        //init caches
        CacheService cacheService = context.beanForType(CacheService.class);
        this.fsItemCache = cacheService.getRepositoryCache(getKey(), ArtifactoryCache.fsItemCache);
        this.locks = cacheService.getRepositoryCache(getKey(), ArtifactoryCache.locks);
        //Create the repo node if it doesn't exist
        JcrFolder rootJcrFolder = getLockedJcrFolder(rootRepoPath, true);
        rootJcrFolder.mkdir();
    }

    public String getRepoRootPath() {
        return repoRootPath;
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
        return internalGetFsItem(new JcrFsItemLocator(repoPath, true));
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
        JcrFsItem fsItem = internalGetFsItem(new JcrFsItemLocator(repoPath, false));
        if (fsItem != null) {
            return internalGetLockedJcrFsItem(fsItem, false, false, getCreator(fsItem));
        }
        return null;
    }

    public JcrFsItem tryGetLockedJcrFsItem(RepoPath repoPath) {
        JcrFsItem fsItem = internalGetFsItem(new JcrFsItemLocator(repoPath, false));
        if (fsItem != null) {
            return internalGetLockedJcrFsItem(fsItem, false, true, getCreator(fsItem));
        }
        return null;
    }

    public JcrFsItem getLockedJcrFsItem(Node node) {
        JcrFsItem fsItem = internalGetFsItem(new JcrFsItemLocator(node, false));
        if (fsItem != null) {
            return internalGetLockedJcrFsItem(fsItem, false, false, getCreator(fsItem));
        }
        return null;
    }

    @SuppressWarnings({"unchecked"})
    private <T extends JcrFsItem<? extends ItemInfo>> FsItemCreator<T> getCreator(T item) {
        if (item.isDirectory()) {
            return (FsItemCreator<T>) jcrFolderCreator;
        } else {
            return (FsItemCreator<T>) jcrFileCreator;
        }
    }

    public JcrFile getLockedJcrFile(String relPath, boolean createIfMissing) throws FileExpectedException {
        RepoPath repoPath = new RepoPath(getKey(), relPath);
        return getLockedJcrFile(repoPath, createIfMissing);
    }

    public JcrFile getLockedJcrFile(RepoPath repoPath, boolean createIfMissing) throws FileExpectedException {
        return internalGetLockedJcrFsItem(repoPath, createIfMissing, jcrFileCreator);
    }

    public JcrFolder getLockedJcrFolder(String relPath, boolean createIfMissing) throws FolderExpectedException {
        RepoPath repoPath = new RepoPath(getKey(), relPath);
        return getLockedJcrFolder(repoPath, createIfMissing);
    }

    public JcrFolder getLockedJcrFolder(RepoPath repoPath, boolean createIfMissing) throws FolderExpectedException {
        return internalGetLockedJcrFsItem(repoPath, createIfMissing, jcrFolderCreator);
    }

    public RepoResource getInfo(RequestContext context) throws FileExpectedException {
        final String path = context.getResourcePath();
        RepoPath repoPath = new RepoPath(getKey(), path);
        JcrFsItem item = getPathItem(repoPath);
        if (item == null) {
            return new UnfoundRepoResource(repoPath, "File not found.");
        }
        RepoResource localRes;
        //When requesting a property/metadata return a special resouce class that contains the parent node
        //path and the metadata name.
        if (NamingUtils.isMetadata(path)) {
            String metadataName = NamingUtils.getMetadataName(path);
            boolean metadataExists = item.hasXmlMetadata(metadataName);
            if (metadataExists) {
                MetadataInfo metadataInfo = delegator.getMetadataService().getMetadataInfo(item, metadataName);
                if (MavenNaming.isSnapshotMavenMetadata(path)) {
                    // this is hack - for snapshot maven metadata use the last updated time of the folder
                    // the cache repo will use this value to determine if the resource is expired
                    metadataInfo.setLastModified(item.getInfo().getInternalXmlInfo().getLastUpdated());
                }
                localRes = new MetadataResource(metadataInfo);
            } else {
                return new UnfoundRepoResource(repoPath, "metadata not found");
            }
        } else {
            if (item.isDirectory()) {
                throw new FileExpectedException(repoPath);
            }
            localRes = new FileResource((FileInfo) item.getInfo());
        }
        return localRes;
    }

    public ResourceStreamHandle getResourceStreamHandle(final RepoResource res) throws IOException {
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
            String medtadataName = res.getInfo().getName();
            String xmlMetdata = item.getXmlMetdata(medtadataName);
            if (xmlMetdata == null) {
                throw new IOException("Could not get resource stream. Stream not found: " + res + ".");
            } else {
                handle = new StringResourceStreamHandle(xmlMetdata);
            }
        } else if (item.isFile()) {
            JcrFile jcrFile = (JcrFile) item;
            final InputStream is = jcrFile.getStream();
            if (is == null) {
                throw new IOException("Could not get resource stream. Stream not found: " + item + ".");
            }
            //Update the stats
            getRepositoryService().updateStats(res.getResponseRepoPath());
            handle = new SimpleResourceStreamHandle(is);
        } else {
            throw new IOException("Could not get resource stream from a folder " + res + ".");
        }
        return handle;
    }

    public String getChecksum(String checksumFileUrl, RepoResource resource) throws IOException {
        if (resource == null || !resource.isFound()) {
            throw new IOException("Could not get resource stream. Path not found: " + checksumFileUrl + ".");
        }
        String extension = '.' + PathUtils.getExtension(checksumFileUrl);
        ChecksumType checksumType = ChecksumType.forExtension(extension);
        if (checksumType == null) {
            throw new IllegalArgumentException("Checksum type not found for path " + checksumFileUrl);
        }
        return getChecksumPolicy().getChecksum(checksumType, resource.getInfo().getChecksums());
    }

    public ChecksumPolicy getChecksumPolicy() {
        return delegator.getChecksumPolicy();
    }

    public void undeploy(RepoPath repoPath) {
        JcrFsItem item = getPathItem(repoPath);
        if (item.isDeleted()) {
            return;
        }
        String path = repoPath.getPath();
        if (NamingUtils.isMetadata(path)) {
            String metadataName = NamingUtils.getMetadataName(path);
            delegator.getMetadataService().removeXmlMetadata(item, metadataName);
        } else {
            if (!PathUtils.hasText(repoPath.getPath())) {
                // Delete the all repo
                delete();
            } else {
                JcrFsItem fsItem = getLockedJcrFsItem(repoPath);
                if (fsItem != null && !fsItem.isDeleted()) {
                    fsItem.delete();
                    //Move the deleted item to the trash
                    jcrService.trash(Collections.singletonList(fsItem));
                }
            }
        }
    }

    public Model getModel(ArtifactResource res) {
        String pom = getPomContent(res);
        if (pom == null) {
            return null;
        }
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            Model model = reader.read(new StringReader(pom));
            return model;
        } catch (Exception e) {
            log.warn("Failed to read pom from '" + pom + "'.", e);
            return null;
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
        log.debug("Saving resource '{}' into repository '{}'.", res, this);
        RepoPath repoPath = new RepoPath(getKey(), res.getRepoPath().getPath());
        try {
            if (res.isMetadata()) {
                //If we are dealing with metadata set it on the containing fsitem
                RepoPath metadataContainerRepoPath = RepoPath.getMetadataContainerRepoPath(repoPath);
                JcrFsItem metadataAware = getLockedJcrFsItem(metadataContainerRepoPath);
                if (metadataAware == null) {
                    //If we cannot find the container, and the metadata is of maven, assume it's a folder and create it
                    //on demand - we have to take this approach beacuse maven is deploying folder (version) metadata
                    //concurrently immediately after sending the artifact deploy method.
                    if (MavenNaming.isMavenMetadata(repoPath.getPath())) {
                        log.debug("Creating missing metadata container folder '{}'.", metadataContainerRepoPath);
                        metadataAware = getLockedJcrFolder(metadataContainerRepoPath, true);
                        metadataAware.mkdirs();
                    } else {
                        //If there is no container return unfound
                        return new UnfoundRepoResource(repoPath,
                                "No metadata container exists: " + metadataContainerRepoPath);
                    }
                }
                MetadataService metadataService = getMetadataService();
                String metadataName = res.getInfo().getName();
                metadataService.setXmlMetadata(metadataAware, metadataName, in);
            } else {
                //Create the parent folder if it does not exist
                RepoPath parentPath = repoPath.getParent();
                if (!itemExists(parentPath.getPath())) {
                    JcrFolder jcrFolder = getLockedJcrFolder(parentPath, true);
                    jcrFolder.mkdirs();
                }

                JcrFile jcrFile = getLockedJcrFile(repoPath, true);
                // set the file extension checksums (only needed if the file is currently being downloaded)
                jcrFile.getInfo().setChecksums(((FileResource) res).getInfo().getChecksums());
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
                //Save properties if they exist
                if (properties != null) {
                    MetadataService metadataService = getMetadataService();
                    metadataService.setXmlMetadata(jcrFile, properties);
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

    public String getPomContent(ArtifactResource res) {
        return getPomContent(res.getRepoPath().getPath());
    }

    public String getPomContent(ItemInfo itemInfo) {
        if (itemInfo.isFolder()) {
            // TODO: Try to extract a POM from the folder
            throw new IllegalArgumentException("Item " + itemInfo + " is not a maven artifact with a POM");
        }
        String relPath = itemInfo.getRelPath();
        return getPomContent(relPath);
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
                JcrFsItem fsItem = getLockedJcrFsItem(path);
                protect &= fsItem != null && fsItem.isFile();
            }
        }
        return protect;
    }

    public boolean itemExists(String relPath) {
        assert relPath != null;
        if (relPath.length() > 0) {
            JcrService jcr = InternalContextHelper.get().getJcrService();
            return jcr.itemNodeExists(repoRootPath + "/" + relPath);
        } else {
            //The repo itself
            return true;
        }
    }

    public List<String> getChildrenNames(String relPath) {
        return jcrService.getChildrenNames(repoRootPath + "/" + relPath);
    }

    public void delete() {
        JcrFolder rootFolder = getLockedRootFolder();
        List<JcrFsItem> children = jcrService.getChildren(rootFolder, true);
        for (JcrFsItem child : children) {
            child.delete();
        }
        //Move the deleted item to the trash
        jcrService.trash(children);
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
            }
        }
        locator.lock(fsItem);
        return fsItem;
    }

    private ReentrantReadWriteLock getLock(RepoPath path) {
        ReentrantReadWriteLock lockEntry = locks.get(path);
        if (lockEntry == null) {
            lockEntry = locks.put(path, new ReentrantReadWriteLock());
        }
        return lockEntry;
    }

    private String getPomContent(String relPath) {
        String relativePath;
        if (!relPath.endsWith(".pom")) {
            File file = new File(relPath);
            String fileName = file.getName();
            int dotIdx = fileName.lastIndexOf(".");
            if (dotIdx < 0) {
                return "No content found.";
            }
            String pomFileName = fileName.substring(0, dotIdx) + ".pom";
            relativePath = new File(file.getParent(), pomFileName).getPath();
        } else {
            relativePath = relPath;
        }
        JcrFile jcrFile;
        try {
            jcrFile = getLockedJcrFile(relativePath, true);
        } catch (FileExpectedException e) {
            throw new RuntimeException("Cannot read a POM from a folder name " + relPath, e);
        }
        if (jcrFile != null) {
            InputStream is = null;
            try {
                is = jcrFile.getStream();
                return IOUtils.toString(is, "utf-8");
            } catch (IOException e) {
                throw new RuntimeException("Failed to read pom from '" + relativePath + "'.", e);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked"})
    private <T extends JcrFsItem<? extends ItemInfo>> T internalGetLockedJcrFsItem(
            RepoPath repoPath,
            boolean createIfMissing,
            FsItemCreator<T> creator) throws FileExpectedException {
        // First check if we have already the write lock
        T item = (T) LockingHelper.getIfLockedByMe(repoPath);
        if (item != null) {
            creator.checkItemType(item);
            return item;
        }
        // get or create the JcrFile
        item = (T) internalGetFsItem(new JcrFsItemLocator(repoPath, false));
        if (item != null) {
            creator.checkItemType(item);
        }
        boolean created = false;
        if (item == null) {
            if (createIfMissing) {
                // Create the empty JcrFile from repoPath
                item = creator.newFsItem(repoPath, delegator);
                created = true;
            } else {
                return null;
            }
        }
        return internalGetLockedJcrFsItem(item, created, false, creator);
    }

    @SuppressWarnings({"unchecked"})
    private <T extends JcrFsItem<? extends ItemInfo>> T internalGetLockedJcrFsItem(
            T item,
            boolean created, //A newly created item in the current tx
            boolean tryLockUpgrade, //Whether to try upgrading to write lock or throw an exception immediately
            FsItemCreator<T> creator) {
        // First check if we have already the write lock
        RepoPath repoPath = item.getRepoPath();
        T result = (T) LockingHelper.getIfLockedByMe(repoPath);
        if (result != null) {
            return result;
        }
        if (created && !item.isMutable()) {
            throw new IllegalStateException("FsItem " + item + " cannot be created and immutable!");
        }
        if (!created && item.isMutable()) {
            throw new IllegalStateException("FsItem " + item + " cannot be mutable if already exists in JCR");
        }
        T original;
        if (created) {
            // If new item no immutable original
            original = null;
        } else {
            // Do a copy constructor to start modifying it
            original = item;
            item = creator.newFsItem(original, delegator);
        }
        // Create a lock entry with new item and original
        FsItemLockEntry lockEntry = new ImmutableLockEntry(getLock(repoPath), original, item);
        // acquire the write lock
        LockingHelper.writeLock(lockEntry, tryLockUpgrade);
        return item;
    }

    private JcrFsItem getPathItem(RepoPath repoPath) {
        // TODO: Why there is a need for lock here?
        //If we are dealing with metadata will return the md container item
        JcrFsItem item = getLockedJcrFsItem(repoPath);
        if (item != null && (item.isDeleted() || !item.exists())) {
            item.setDeleted(true);
            log.warn("File '{}' was deleted during request!", repoPath);
        }
        return item;
    }

    private class JcrFsItemLocator {
        private final RepoPath repoPath;
        private final Node node;
        private final boolean acquireReadLock;

        JcrFsItemLocator(RepoPath repoPath, boolean acquireReadLock) {
            //If we are dealing with metadata return the containing fsitem
            this.repoPath = RepoPath.getMetadataContainerRepoPath(repoPath);
            this.node = null;
            this.acquireReadLock = acquireReadLock;
        }

        JcrFsItemLocator(Node node, boolean acquireReadLock) {
            this.repoPath = JcrPath.get().getRepoPath(JcrHelper.getAbsolutePath(node));
            this.node = node;
            this.acquireReadLock = acquireReadLock;
        }

        public RepoPath getRepoPath() {
            return repoPath;
        }

        public JcrFsItem getFsItem() {
            if (node != null) {
                return jcrService.getFsItem(node, delegator);
            }
            if (repoPath != null) {
                return jcrService.getFsItem(repoPath, delegator);
            }
            throw new IllegalArgumentException("Need either repoPath or node");
        }

        public void lock(JcrFsItem fsItem) {
            if (acquireReadLock && fsItem != null) {
                if (fsItem.isMutable()) {
                    throw new IllegalStateException("Cannot acquire read lock on mutable object " + fsItem);
                }
                FsItemLockEntry lockEntry = new ImmutableLockEntry(getLock(fsItem.getRepoPath()), fsItem, null);
                LockingHelper.readLock(lockEntry);
            }
        }
    }

    private static interface FsItemCreator<T extends JcrFsItem<? extends ItemInfo>> {
        public boolean checkItemType(JcrFsItem item);

        public T newFsItem(RepoPath repoPath, StoringRepo repo);

        public T newFsItem(JcrFsItem copy, StoringRepo repo);

        public T newFsItem(Node node, StoringRepo repo);
    }

    private static class JcrFileCreator implements FsItemCreator<JcrFile> {
        public boolean checkItemType(JcrFsItem item) {
            if (item.isDirectory()) {
                throw new FileExpectedException(item.getRepoPath());
            }
            return true;
        }

        public JcrFile newFsItem(RepoPath repoPath, StoringRepo repo) {
            return new JcrFile(repoPath, repo);
        }

        public JcrFile newFsItem(JcrFsItem copy, StoringRepo repo) {
            return new JcrFile((JcrFile) copy, repo);
        }

        public JcrFile newFsItem(Node node, StoringRepo repo) {
            return new JcrFile(node, repo);
        }
    }

    private static class JcrFolderCreator implements FsItemCreator<JcrFolder> {
        public boolean checkItemType(JcrFsItem item) {
            if (!item.isDirectory()) {
                throw new FolderExpectedException(item.getRepoPath());
            }
            return true;
        }

        public JcrFolder newFsItem(RepoPath repoPath, StoringRepo repo) {
            return new JcrFolder(repoPath, repo);
        }

        public JcrFolder newFsItem(JcrFsItem copy, StoringRepo repo) {
            return new JcrFolder((JcrFolder) copy, repo);
        }

        public JcrFolder newFsItem(Node node, StoringRepo repo) {
            return new JcrFolder(node, repo);
        }
    }
}