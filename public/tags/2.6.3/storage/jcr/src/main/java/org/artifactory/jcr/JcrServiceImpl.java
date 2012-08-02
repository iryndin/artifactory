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

package org.artifactory.jcr;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataRecord;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.replication.ReplicationAddon;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.ArtifactCount;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.storage.GarbageCollectorInfo;
import org.artifactory.checksum.ChecksumStorageHelper;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.config.xml.ArtifactoryXmlFactory;
import org.artifactory.config.xml.EntityResolvingContentHandler;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.io.NonClosingInputStream;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.ChecksumInputStream;
import org.artifactory.io.checksum.ChecksumPaths;
import org.artifactory.jcr.factory.JcrFsItemFactory;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.fs.JcrTreeNode;
import org.artifactory.jcr.fs.JcrTreeNodeFileFilter;
import org.artifactory.jcr.jackrabbit.ExtendedDbDataStore;
import org.artifactory.jcr.lock.aop.LockingAdvice;
import org.artifactory.jcr.md.MetadataDefinitionService;
import org.artifactory.jcr.spring.ArtifactoryStorageContext;
import org.artifactory.jcr.spring.StorageContextHelper;
import org.artifactory.jcr.trash.Trashman;
import org.artifactory.jcr.utils.JcrHelper;
import org.artifactory.jcr.utils.JcrUtils;
import org.artifactory.jcr.version.JcrVersion;
import org.artifactory.log.LoggerFactory;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.interceptor.ImportInterceptors;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.data.VfsNode;
import org.artifactory.sapi.data.VfsNodeType;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.sapi.search.InvalidQueryRuntimeException;
import org.artifactory.sapi.search.VfsComparatorType;
import org.artifactory.sapi.search.VfsQueryResult;
import org.artifactory.sapi.search.VfsQueryService;
import org.artifactory.sapi.search.VfsRepoQuery;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.ContextReadinessListener;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.StorageConstants;
import org.artifactory.tx.SessionResource;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.LoggingUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.extensions.jcr.SessionFactoryUtils;
import org.springframework.stereotype.Repository;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.jcr.Binary;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

import static org.apache.jackrabbit.JcrConstants.*;
import static org.artifactory.storage.StorageConstants.*;

/**
 * Spring based session factory for tx jcr sessions
 *
 * @author yoavl
 */
@Repository
@Reloadable(beanClass = JcrService.class, initAfter = MetadataDefinitionService.class)
public class JcrServiceImpl implements JcrService, JcrRepoService, ContextReadinessListener {
    private static final Logger log = LoggerFactory.getLogger(JcrServiceImpl.class);

    private static final String PING_TEST_NODE = "pingTestNode";

    public final String[] OCM_CLASSES = {
            "org.artifactory.jcr.ocm.OcmStorable",
            "org.artifactory.security.User",
            "org.artifactory.security.Group",
            "org.artifactory.security.Acl",
            "org.artifactory.security.Ace",
            "org.artifactory.security.PermissionTarget"
    };

    @Autowired
    @Qualifier("nodeTypesResource")
    private ResourceStreamHandle nodeTypes;

    @Autowired
    @Qualifier("repoXmlResource")
    private ResourceStreamHandle repoXml;

    @Autowired
    private VfsQueryService queryService;

    @Autowired
    private AddonsManager addonsManager;

    private Mapper ocmMapper;

    //Don't inject or CGLib will crash the key for Tx resource
    private JcrSessionFactory sessionFactory;

    private static final ThreadLocal<JcrSession> UNMANAGED_SESSION_HOLDER = new ThreadLocal<JcrSession>();

    private Semaphore emptyTrashSemaphore = new Semaphore(1);

    @Override
    public javax.jcr.Repository getRepository() {
        return getManagedSession().getRepository();
    }

    @Override
    public JcrSession getManagedSession() {
        if (sessionFactory == null) {
            throw new IllegalStateException("JCR Service not initialized yet");
        }
        if (UNMANAGED_SESSION_HOLDER.get() != null) {
            return UNMANAGED_SESSION_HOLDER.get();
        }
        if (!LockingAdvice.isInJcrTransaction()) {
            throw new RepositoryRuntimeException("Trying to get session outside transaction scope.");
        }
        JcrSession session;
        try {
            session = (JcrSession) SessionFactoryUtils.doGetSession(sessionFactory, true);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        return session;
    }

    @Override
    public JcrSession getUnmanagedSession() {
        if (sessionFactory == null) {
            throw new IllegalStateException("JCR Service not initialized yet");
        }
        JcrSession session;
        try {
            session = (JcrSession) sessionFactory.getSession();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        return session;
    }

    public static void keepUnmanagedSession(JcrSession session) {
        UNMANAGED_SESSION_HOLDER.set(session);
    }

    public static void removeUnmanagedSession() {
        UNMANAGED_SESSION_HOLDER.remove();
    }

    @Override
    public <T extends SessionResource> T getSessionResource(Class<T> resourceClass) {
        return getManagedSession().getOrCreateResource(resourceClass);
    }

    void setOcmMapper(Mapper ocmMapper) {
        this.ocmMapper = ocmMapper;
    }

    /**
     * Inits the singleton JCR repository
     */
    @PostConstruct
    private void initJcrRepository() throws Exception {
        initJcrRepository(true);
    }

    private void initJcrRepository(boolean preInit) throws Exception {
        if (sessionFactory != null) {
            throw new IllegalStateException("JCR Service was already initialized!");
        }
        // Don't call the parent the repository is not created yet
        cleanupAfterLastShutdown();
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        System.setProperty("derby.stream.error.file",
                new File(artifactoryHome.getLogDir(), "derby.log").getAbsolutePath());

        //Create the repository
        JackrabbitRepository repository = JcrRepoInitHelper.createJcrRepository(repoXml, preInit);
        // Create the session pool
        sessionFactory = new JcrSessionFactory();
        sessionFactory.setRepository(repository);
        // Now that the repo is half done we can call super
        sessionFactory.afterPropertiesSet();

        JcrTransactionManager txMgr = StorageContextHelper.get().beanForType(JcrTransactionManager.class);
        txMgr.setSessionFactory(sessionFactory);
    }

    @Override
    public void init() {
        //Initialize the created jcr repository
        JcrRepoInitHelper.init(this);
        ArtifactoryHome artifactoryHome = StorageContextHelper.get().getArtifactoryHome();
        if (artifactoryHome.startedFromDifferentVersion()) {
            CompoundVersionDetails source = artifactoryHome.getOriginalVersionDetails();
            JcrVersion.values();
            JcrVersion originalVersion = source.getVersion().getSubConfigElementVersion(JcrVersion.class);

            /**
             * Keep track if we opened the session or not (might be opened by JCR metadata converter before us), and if
             * not, don't close it and remove from the thread local, as it is expected in other places
             */
            JcrSession unManagedSession = UNMANAGED_SESSION_HOLDER.get();
            boolean sessionOpenedByMe = false;
            if (unManagedSession == null) {
                sessionOpenedByMe = true;
                unManagedSession = getUnmanagedSession();
                UNMANAGED_SESSION_HOLDER.set(unManagedSession);
            }
            try {
                originalVersion.postInitConvert(unManagedSession);
            } finally {
                if (sessionOpenedByMe) {
                    UNMANAGED_SESSION_HOLDER.remove();
                    unManagedSession.logout();
                }
            }
        }
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
        // Nothing
    }

    @Override
    public void reCreateJcrRepository() throws Exception {
        initJcrRepository(false);
        // Don't call init it is going to be called by normal bean refresh
    }

    @Override
    public void destroy() {
        try {
            sessionFactory.destroy();
        } catch (Exception e) {
            log.warn("Failed to destroy the jcr session factory.", e);
        } finally {
            sessionFactory = null;
        }
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        //ATTENTION: Pre JCR init done in org.artifactory.jcr.JcrRepoInitHelper.preInitConvert() since this method is called too late
        JcrVersion.values();
        final JcrVersion originalVersion = source.getVersion().getSubConfigElementVersion(JcrVersion.class);
        JcrSession unmanagedSession = getUnmanagedSession();
        Session rawSession = unmanagedSession.getSession();
        try {
            UNMANAGED_SESSION_HOLDER.set(unmanagedSession);
            originalVersion.convert(rawSession);
        } finally {
            UNMANAGED_SESSION_HOLDER.remove();
            if (unmanagedSession.isLive()) {
                //We may have already closed the session if the repo was closed and reopened as part of the conversion
                unmanagedSession.logout();
            }
        }
    }

    @Override
    public ObjectContentManager getOcm() {
        if (ocmMapper == null) {
            return null;
        }
        JcrSession session = getManagedSession();
        return new ObjectContentManagerImpl(session, ocmMapper);
    }

    @Override
    public boolean itemNodeExists(String absPath) {
        return JcrHelper.itemNodeExists(absPath, getManagedSession());
    }

    @Override
    public boolean isFile(RepoPath repoPath) {
        String absPath = PathFactoryHolder.get().getAbsolutePath(repoPath);
        JcrSession jcrSession = getManagedSession();
        if (JcrHelper.itemNodeExists(absPath, jcrSession)) {
            Node node = (Node) jcrSession.getItem(absPath);
            return JcrHelper.isFile(node);
        }
        return false;
    }

    @Override
    public boolean isFolder(RepoPath repoPath) {
        String absPath = PathFactoryHolder.get().getAbsolutePath(repoPath);
        JcrSession jcrSession = getManagedSession();
        if (JcrHelper.itemNodeExists(absPath, jcrSession)) {
            Node node = (Node) jcrSession.getItem(absPath);
            return JcrHelper.isFolder(node);
        }
        return false;
    }

    @Override
    public int delete(String absPath) {
        return JcrHelper.delete(absPath, getManagedSession());
    }

    @Override
    public void emptyTrash() {
        // allow only one thread to empty the trash
        if (emptyTrashSemaphore.tryAcquire()) {
            int deletedItemsCount;
            try {
                do {
                    deletedItemsCount = 0;
                    log.debug("Starting to empty the trash directory.");
                    JcrSession session = getUnmanagedSession();
                    try {
                        Node trashNode = getTrashNode(session);
                        NodeIterator trashChildren = trashNode.getNodes();
                        while (trashChildren.hasNext()) {
                            Node trashChild = trashChildren.nextNode();
                            deletedItemsCount += deleteFromTrash(trashChild, session);
                        }
                        log.debug("Done emptying the trash. {} item(s) were deleted.", deletedItemsCount);
                    } finally {
                        session.logout();
                    }
                    // continue running only if previous run deleted some nodes (if no item were deleted it might got
                    // corrupted so we don't want to keep iterating forever)
                } while (deletedItemsCount > 0);
            } catch (Exception e) {
                //Fail gracefully
                log.error("Could not empty trash.", e);
            } finally {
                emptyTrashSemaphore.release();
            }
        } else {
            log.debug("Empty trash is already in progress");
        }
    }

    @Override
    public void emptyTrashAfterCommit() {
        emptyTrash();   // called after TX commit; just delegate to empty trash
    }

    private Node getTrashNode(JcrSession session) {
        return (Node) session.getItem(PathFactoryHolder.get().getTrashRootPath());
    }

    private int deleteFromTrash(Node trashNode, JcrSession session) {
        String folderName = null;
        try {
            folderName = trashNode.getName();
            log.debug("Emptying nodes from trash folder '{}'.", folderName);
            int deletedItems = JcrHelper.delete(trashNode.getPath(), session);
            return deletedItems;
        } catch (Exception e) {
            //Fail gracefully
            LoggingUtils.warnOrDebug(log, "Could not empty trash folder " + folderName + ".", e);
            return 0;
        }
    }

    /**
     * "Shallow" folder import in a new tx
     */
    @Override
    public RepoPath importFolder(JcrFsItemFactory repo, RepoPath jcrFolder, ImportSettings settings) {
        JcrFolder folder = repo.getLockedJcrFolder(jcrFolder, true);
        folder.importFrom(settings);
        return folder.getRepoPath();
    }

    @Override
    public JcrFile importFile(JcrFolder parentFolder, File file, ImportSettings settings)
            throws RepoRejectException {
        RepoPath targetRepoPath = InternalRepoPathFactory.create(parentFolder.getRepoPath(), file.getName());
        log.debug("Importing '{}'.", targetRepoPath);
        //Takes a read lock
        ArtifactoryStorageContext context = StorageContextHelper.get();
        context.getRepositoryService().assertValidDeployPath(targetRepoPath);
        JcrFile jcrFile = null;
        try {
            jcrFile = parentFolder.getRepo().getLockedJcrFile(targetRepoPath, true);
            jcrFile.importFrom(file, settings);
            //Mark for indexing (if needed)
            context.beanForType(ImportInterceptors.class).afterImport(jcrFile, settings.getStatusHolder());
            targetRepoPath = jcrFile.getRepoPath();
            log.debug("Imported '{}'.", targetRepoPath);
            AccessLogger.deployed(targetRepoPath);
        } catch (RepoRejectException rre) {
            throw rre;
        } catch (Exception e) {
            MutableStatusHolder status = settings.getStatusHolder();
            status.setWarning("Could not import file '" + file.getAbsolutePath() + "' into " + jcrFile + ".", e, log);
            //Remove the file, so that save will not be attempted at the end of the transaction
            if (jcrFile != null) {
                jcrFile.bruteForceDelete();
            }
        }
        return jcrFile;
    }

    @Override
    public boolean delete(JcrFsItem fsItem) {
        JcrSession session = getManagedSession();
        String absPath = fsItem.getAbsolutePath();
        if (!session.itemExists(absPath)) {
            return false;
        }

        //Done here for tx propagation
        fsItem.getRepo().onDelete(fsItem);
        return true;
    }

    @Override
    public void move(String fromAbsPath, String toAbsPath) {
        JcrSession session = getManagedSession();
        session.move(fromAbsPath, toAbsPath);
    }

    @Override
    public void copy(String fromAbsPath, String toAbsPath) {
        JcrSession session = getManagedSession();
        session.copy(fromAbsPath, toAbsPath);
    }

    @Override
    public List<String> getChildrenNames(String absPath) {
        JcrSession session = getManagedSession();
        if (!session.itemExists(absPath)) {
            throw new RepositoryRuntimeException("Tried to get children of a non exiting node '" + absPath + "'.");
        }
        Node node = (Node) session.getItem(absPath);
        List<String> names = new ArrayList<String>();
        try {
            NodeIterator nodes = node.getNodes();
            while (nodes.hasNext()) {
                Node childNode = nodes.nextNode();
                String name = childNode.getName();
                if (!NODE_ARTIFACTORY_METADATA.equals(name)) {
                    names.add(name);
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to get node's children names.", e);
        }
        return names;
    }

    @Override
    public void trash(List<VfsItem> items) {
        try {
            Trashman trashman = getManagedSession().getOrCreateResource(Trashman.class);
            trashman.addItemsToTrash(items, this);
        } catch (Exception e) {
            log.error("Could not move items to trash.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JcrFsItem getFsItem(RepoPath repoPath, JcrFsItemFactory repo) {
        JcrSession session = getManagedSession();
        String absPath = PathFactoryHolder.get().getAbsolutePath(repoPath);
        try {
            Node node = (Node) session.getItem(absPath);
            return getFsItem(node, repo);
        } catch (RuntimeException e) {
            if (ExceptionUtils.getCauseOfTypes(e, PathNotFoundException.class, RepositoryException.class) != null) {
                log.debug("Path not found : {}.", repoPath);
                return null;
            }
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNodeTypeName(RepoPath repoPath) {
        JcrSession session = getManagedSession();
        String absPath = PathFactoryHolder.get().getAbsolutePath(repoPath);
        boolean exits = itemNodeExists(absPath);
        if (exits) {
            Node node = (Node) session.getItem(absPath);
            return JcrHelper.getPrimaryTypeName(node);
        } else {
            return null;
        }
    }

    @Override
    public void writeMetadataEntries(JcrFsItem fsItem, MutableStatusHolder status, File folder, boolean incremental) {
        fsItem.writeMetadataEntries(status, folder, incremental);
    }

    @Override
    public void bruteForceDeleteAndReplicateEvent(VfsItem item) {
        item.bruteForceDelete();
        addonsManager.addonByType(ReplicationAddon.class).offerLocalReplicationDeleteEvent(item.getRepoPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<JcrFsItem> getChildren(JcrFolder folder, boolean writeLock) {
        String absPath = folder.getAbsolutePath();
        List<JcrFsItem> items = new ArrayList<JcrFsItem>();
        if (!folder.exists()) {
            return items;
        }
        Node parentNode = folder.getNode();
        try {
            NodeIterator nodes = parentNode.getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                try {
                    if (JcrHelper.isFsItemType(node)) {
                        JcrFsItem item;
                        if (writeLock) {
                            item = folder.getRepo().getLockedJcrFsItem(node);
                        } else {
                            item = folder.getRepo().getJcrFsItem(node);
                        }
                        if (item != null) {
                            items.add(item);
                        } else {
                            log.warn("Node was removed during children loop for " + absPath);
                        }
                    }
                } catch (Exception e) {
                    log.error("Could not list child node '" + node + "'.", e);
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to retrieve folder node items.", e);
        }
        return items;
    }

    @Override
    public Node getNode(String absPath) {
        JcrSession session = getManagedSession();
        Node root = session.getRootNode();
        return JcrHelper.getNode(root, absPath);
    }

    @Override
    public Node getOrCreateUnstructuredNode(String absPath) {
        JcrSession session = getManagedSession();
        Node root = session.getRootNode();
        return getOrCreateUnstructuredNode(root, absPath);
    }

    @Override
    public Node getNode(Node parent, String relPath) {
        return JcrHelper.getNode(parent, relPath);
    }

    @Override
    public Node getOrCreateUnstructuredNode(Node parent, String relPath) {
        return JcrHelper.getOrCreateNode(parent, relPath, StorageConstants.NT_UNSTRUCTURED, "mix:referenceable");
    }

    @Override
    public Node getOrCreateNode(Node parent, String relPath, String type, String... mixins) {
        return JcrHelper.getOrCreateNode(parent, relPath, type, mixins);
    }

    @Override
    public void exportFile(JcrFile jcrFile, ExportSettings settings) {
        jcrFile.exportTo(settings);
    }

    @Override
    public String getString(String nodePath) {
        InputStream is = null;
        try {
            is = getStream(nodePath);
            return IOUtils.toString(is, "utf-8");
        } catch (Exception e) {
            throw new RepositoryRuntimeException(e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @Override
    public InputStream getStream(String nodePath) {
        JcrSession session = getManagedSession();
        Node node = (Node) session.getItem(nodePath);
        try {
            return JcrHelper.getStream(node);
        } catch (Exception e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public void setString(String parentPath, String nodeName, String value, String mimeType, String userId) {
        JcrSession session = getManagedSession();
        Node parentNode = (Node) session.getItem(parentPath);
        setString(parentNode, nodeName, value, mimeType, userId, false);
    }

    @Override
    public void setString(Node parent, String nodeName, String value, String mimeType, String userId,
            boolean saveXmlHierarchy
    ) {
        InputStream stringStream = null;
        try {
            if (value != null) {
                stringStream = IOUtils.toInputStream(value, "utf-8");
            }
            //Null value -> remove
        } catch (IOException e) {
            throwSetException(parent, nodeName, e);
        }
        setStream(parent, nodeName, stringStream, mimeType, userId, saveXmlHierarchy);
    }

    /**
     * Save a string stream into the child node.
     * <p/>
     * NOTE: Caller is expected to close the provided stream.
     */
    @Override
    public void setStream(Node parent, String nodeName, InputStream value, String mimeType, String userId,
            boolean saveXmlHierarchy) {
        try {
            Node metadataNode;
            Node stringNode;
            boolean exists = parent.hasNode(nodeName);
            if (exists) {
                metadataNode = parent.getNode(nodeName);
                if (value == null) {
                    //Just the remove the md and return
                    metadataNode.remove();
                    return;
                }
            } else {
                if (value == null) {
                    return;
                }
                //Add the metadata node and mark the create time
                Calendar created = Calendar.getInstance();
                metadataNode = parent.addNode(nodeName);
                metadataNode.setProperty(PROP_ARTIFACTORY_METADATA_NAME, nodeName);
                metadataNode.setProperty(PROP_ARTIFACTORY_CREATED, created);
                metadataNode.setProperty(PROP_ARTIFACTORY_CREATED_BY, userId);
            }
            //Update the last modified on the specific metadata
            Calendar lastModified = Calendar.getInstance();
            metadataNode.setProperty(PROP_ARTIFACTORY_LAST_MODIFIED, lastModified);
            metadataNode.setProperty(PROP_ARTIFACTORY_LAST_MODIFIED_BY, userId);

            //Set the original raw xml
            Node resourceNode;
            boolean hasResourceNode = metadataNode.hasNode(JCR_CONTENT);
            if (hasResourceNode) {
                resourceNode = metadataNode.getNode(JCR_CONTENT);
            } else {
                resourceNode = metadataNode.addNode(JCR_CONTENT, NT_RESOURCE);
            }
            resourceNode.setProperty(JCR_MIMETYPE, mimeType);
            resourceNode.setProperty(JCR_ENCODING, "utf-8");
            resourceNode.setProperty(JCR_LASTMODIFIED, lastModified);
            //Write the raw data and calculate checksums on it
            ChecksumInputStream checksumInputStream = new ChecksumInputStream(value,
                    new Checksum(ChecksumType.md5),
                    new Checksum(ChecksumType.sha1));
            //Save the data and calculate the checksums
            Binary binary = resourceNode.getSession().getValueFactory().createBinary(checksumInputStream);
            resourceNode.setProperty(JCR_DATA, binary);
            checksumInputStream.close();
            setChecksums(metadataNode, checksumInputStream.getChecksums(), true);

            //Import the xml as a jcr document if needed - an xmlNode (with artifactory:xml/metadataName as its root
            //element) will be created from the stored input stream
            if (saveXmlHierarchy) {
                Property property = resourceNode.getProperty(JCR_DATA);
                InputStream rawStream = property.getBinary().getStream();
                try {
                    if (metadataNode.hasNode(NODE_ARTIFACTORY_XML)) {
                        stringNode = metadataNode.getNode(NODE_ARTIFACTORY_XML);
                    } else {
                        stringNode = metadataNode.addNode(NODE_ARTIFACTORY_XML);
                    }
                    saveXmlHierarchy(stringNode, rawStream);
                } finally {
                    IOUtils.closeQuietly(rawStream);
                }
            }
        } catch (Exception e) {
            throwSetException(parent, nodeName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveChecksums(JcrFsItem fsItem, String metadataName, Checksum[] checksums) {
        // TODO: Maybe write lock the fsItem?
        Node metadataNode = JcrHelper.safeGetNode(fsItem.getNode(), NODE_ARTIFACTORY_METADATA, metadataName);
        if (metadataNode != null) {
            setChecksums(metadataNode, checksums, false);
        }
    }

    @Override
    public JcrTreeNode getTreeNode(RepoPath itemPath, MultiStatusHolder multiStatusHolder,
            @Nullable JcrTreeNodeFileFilter fileFilter) {
        Node itemNode = getNode(PathFactoryHolder.get().getAbsolutePath(itemPath));
        if (itemNode == null) {
            return null;
        }

        return processTreeNodeRecursively(itemNode, multiStatusHolder, fileFilter);
    }

    @Override
    @Nullable
    public InputStream getDataStreamBySha1Checksum(String sha1) throws DataStoreException {
        JcrSession managedSession = getManagedSession();
        RepositoryImpl rep = (RepositoryImpl) managedSession.getRepository();
        DataStore dataStore = rep.getDataStore();
        DataRecord record = dataStore.getRecordIfStored(new DataIdentifier(sha1));
        if (record != null) {
            return record.getStream();
        }

        return null;
    }

    @Override
    public synchronized void ping() {
        JcrSession session = null;
        try {
            session = getUnmanagedSession();
            RepositoryImpl repository = (RepositoryImpl) session.getRepository();
            ExtendedDbDataStore dataStore = JcrUtils.getExtendedDataStore(repository);
            dataStore.ping();
            Node trashNode = session.getNode(PathFactoryHolder.get().getTrashRootPath());
            Node pingTestNode;
            if (!trashNode.hasNode(PING_TEST_NODE)) {
                pingTestNode = trashNode.addNode(PING_TEST_NODE);
            } else {
                pingTestNode = trashNode.getNode(PING_TEST_NODE);
            }
            pingTestNode.setProperty(Property.JCR_LAST_MODIFIED, Calendar.getInstance());
            // TODO: SearchManager exception (invalid index),
            // TODO: are swallowed by Jackrabbit need to find a way to listen to JCR errors!
            session.save();
        } catch (Exception e) {
            throw new RuntimeException("Testing validity of JCR storage failed with: " + e.getMessage(), e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    @Override
    public void activateMarkerFileConverters() {
        ArtifactoryStorageContext context = StorageContextHelper.get();
        SecurityService securityService = context.beanForType(SecurityService.class);
        securityService.authenticateAsSystem();
        try {
            JcrVersion.applyMarkerConvertersConversion();
        } finally {
            securityService.nullifyContext();
        }
    }

    private JcrTreeNode processTreeNodeRecursively(Node itemNode, MultiStatusHolder multiStatusHolder,
            JcrTreeNodeFileFilter fileFilter) {

        try {
            String absolutePath = itemNode.getPath();
            boolean isFolder = JcrHelper.isFolder(itemNode);

            RepoPath repoPath = PathFactoryHolder.get().getRepoPath(absolutePath);
            if (isFolder) {
                Set<JcrTreeNode> children = Sets.newHashSet();
                NodeIterator childNodes = itemNode.getNodes();

                while (childNodes.hasNext()) {
                    Node childNode = childNodes.nextNode();
                    JcrTreeNode childTreeNode = processTreeNodeRecursively(childNode, multiStatusHolder, fileFilter);
                    if (childTreeNode != null) {
                        children.add(childTreeNode);
                    }
                }

                return new JcrTreeNode(repoPath, isFolder, getCreated(itemNode), children);
            } else if (JcrHelper.isFile(itemNode) && ((fileFilter == null) || fileFilter.acceptsFile(repoPath))) {
                return new JcrTreeNode(repoPath, isFolder, getCreated(itemNode), null);
            }
        } catch (RepositoryException e) {
            multiStatusHolder.setError("Error while processing tree node " + JcrHelper.display(itemNode) + ".", log);
        }

        return null;
    }

    private Calendar getCreated(Node itemNode) throws RepositoryException {
        if (itemNode.hasProperty(StorageConstants.PROP_ARTIFACTORY_CREATED)) {
            return itemNode.getProperty(StorageConstants.PROP_ARTIFACTORY_CREATED).getDate();
        } else {
            log.info("Item's {} artifactory:created property is missing.", itemNode.getPath());
            return Calendar.getInstance();
        }
    }

    private void setChecksums(Node metadataNode, Checksum[] checksums, boolean override) {
        try {
            for (Checksum checksum : checksums) {
                //Save the checksum as a property
                String metadataName = metadataNode.getName();
                String checksumStr = checksum.getChecksum();
                ChecksumType checksumType = checksum.getType();
                String propName = ChecksumStorageHelper.getActualPropName(checksumType);
                if (override || !metadataNode.hasProperty(propName)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Saving checksum for '" + metadataName + "' as '" + propName + "' (checksum=" +
                                checksumStr + ").");
                    }
                    metadataNode.setProperty(propName, checksumStr);
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Could not set checksums.", e);
        }
    }

    /**
     * Import xml with characters entity resolving
     */
    @Override
    public void saveXmlHierarchy(Node xmlNode, InputStream in) throws RepositoryException, IOException {
        if (!xmlNode.isNew()) {
            NodeIterator children = xmlNode.getNodes();
            while (children.hasNext()) {
                Node child = (Node) children.next();
                child.remove();
            }
        }
        final String absPath = xmlNode.getPath();
        JcrSession session = getManagedSession();
        ContentHandler contentHandler =
                session.getImportContentHandler(absPath, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
        EntityResolvingContentHandler resolvingContentHandler = new EntityResolvingContentHandler(contentHandler);
        NonClosingInputStream ncis = null;
        try {
            SAXParserFactory factory = ArtifactoryXmlFactory.getFactory();
            SAXParser parser = factory.newSAXParser();
            ncis = new NonClosingInputStream(in);
            parser.parse(ncis, resolvingContentHandler);
        } catch (ParserConfigurationException e) {
            //Here ncis is always null
            throw new RepositoryException("SAX parser configuration error when parsing " + absPath, e);
        } catch (Throwable e) {
            //Check for wrapped repository exception
            if (e instanceof SAXException) {
                Exception e1 = ((SAXException) e).getException();
                if (e1 instanceof RepositoryException) {
                    if (ncis != null) {
                        ncis.forceClose();
                    }
                    throw (RepositoryException) e1;
                }
            }
            LoggingUtils.warnOrDebug(log, "Ignoring bad XML stream while importing '" + absPath + "'", e);
            throw new RepositoryException("Parsing of " + absPath + " error", e);
        }
    }

    QNodeTypeDefinition[] getArtifactoryNodeTypes() throws IOException, InvalidNodeTypeDefException {
        QNodeTypeDefinition[] types;
        try {
            types = NodeTypeReader.read(nodeTypes.getInputStream());
        } finally {
            nodeTypes.close();
        }
        return types;
    }

    private static void cleanupAfterLastShutdown() {
        //Cleanup any left over bin*.tmp files from the temp directory, due to unclean shutdown
        String tmpDirPath;
        try {
            File tmp = File.createTempFile("bin", null);
            tmpDirPath = tmp.getParent();
        } catch (IOException e) {
            tmpDirPath = System.getProperty("java.io.tmpdir");
        }
        if (tmpDirPath != null) {
            if (log.isDebugEnabled()) {
                log.debug("Cleaning up temp files in '" + tmpDirPath + "'.");
            }
            File tmpDir = new File(tmpDirPath);
            try {
                Collection<File> tmpfiles =
                        FileUtils.listFiles(tmpDir, new WildcardFileFilter("bin*.tmp"), null);
                for (File tmpfile : tmpfiles) {
                    tmpfile.delete();
                }
            } catch (Exception e) {
                log.error("Could not clean up old temp files. This may indicate a badly configured temp dir ('{}').",
                        tmpDirPath, e);
            }
        } else {
            log.warn("Not cleaning up any temp files: failed to determine temp dir.");
        }
    }

    @Override
    public JcrFsItem getFsItem(Node node, JcrFsItemFactory repo) {
        String typeName = JcrHelper.getPrimaryTypeName(node);
        if (typeName.equals(NT_ARTIFACTORY_FOLDER)) {
            return new JcrFolder(node, repo, null);
        } else if (typeName.equals(NT_ARTIFACTORY_FILE)) {
            return new JcrFile(node, repo, null);
        } else {
            throw new RepositoryRuntimeException(
                    "Cannot create file system item from unknown node type " + typeName + " for node " + node);
        }
    }

    @Override
    public QueryResult executeQuery(JcrQuerySpec spec) {
        JcrSession session = getManagedSession();
        return executeQuery(spec, session);
    }

    @Override
    public QueryResult executeQuery(JcrQuerySpec spec, JcrSession session) {
        long start = System.currentTimeMillis();
        Workspace workspace = session.getWorkspace();
        QueryResult queryResult;
        String queryString = null;
        try {
            QueryManager queryManager = workspace.getQueryManager();
            queryString = spec.query();
            log.debug("Executing JCR query: {}", queryString);
            Query query = queryManager.createQuery(queryString, spec.jcrType());
            int limit = spec.limit();
            if (limit >= 0) {
                query.setLimit(limit);
            }
            queryResult = query.execute();
        } catch (InvalidQueryException e) {
            throw new InvalidQueryRuntimeException("Got invalid query " + queryString, e);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Error while executing query.", e);
        }
        log.debug("{} query execution time: {} ms", spec.jcrType(), System.currentTimeMillis() - start);
        return queryResult;
    }

    @Override
    public ArtifactCount getArtifactCount() {
        return getArtifactCount(null);
    }

    @Override
    public ArtifactCount getArtifactCount(@Nullable String repoKey) {

        if (StringUtils.isNotBlank(repoKey) && ConstantValues.searchArtifactSearchUseV2Storage.getBoolean()) {
            repoKey = StringUtils.isBlank(repoKey) ? "" : repoKey + "/";
            ChecksumPaths checksumPaths = ContextHelper.get().beanForType(ChecksumPaths.class);
            ImmutableCollection<String> fileOrPathsLike = checksumPaths.getFileOrPathsLike(Lists.newArrayList("%"),
                    Lists.newArrayList("/repositories/" + repoKey + "%"));
            int realArtifactsCount = 0;
            // remove metadata entries
            for (String s : fileOrPathsLike) {
                if (!s.contains(NamingUtils.METADATA_PREFIX)) {
                    realArtifactsCount++;
                }
            }
            return new ArtifactCount(realArtifactsCount);
        }

        StringBuilder sb = new StringBuilder();

        if (StringUtils.isNotBlank(repoKey)) {
            sb.append("/jcr:root").append(PathFactoryHolder.get().getAllRepoRootPath()).append("/").append(repoKey);
        }

        //The 'order by @jcr:score descending' suffix guarantees result count is calculated in JR 2.0
        sb.append("//element(*, ").append(NT_FILE).append(") [@").append(PROP_ARTIFACTORY_NAME).
                append("] order by @jcr:score descending");

        QueryResult result = executeQuery(JcrQuerySpec.xpath(sb.toString()).noLimit());
        long count = 0;
        try {
            count = result.getNodes().getSize();
        } catch (RepositoryException e) {
            //Just log - do not fail
            log.error("Could not get artifacts count on '{}'.", repoKey, e);
        }
        return new ArtifactCount(count);
    }

    @Override
    public List<JcrFile> getPluginPomFiles(JcrFsItemFactory repo) {
        // search for the existence of maven plugin property on the file nodes
        // e.g.: /jcr:root/repositories/plugins-local//element(*, artifactory:file) [@artifactory:maven.mavenPlugin]
        VfsRepoQuery repoQuery = queryService.createRepoQuery();
        repoQuery.setSingleRepoKey(repo.getKey());
        repoQuery.addAllSubPathFilter();
        repoQuery.setNodeTypeFilter(VfsNodeType.FILE);
        repoQuery.addCriterion(StorageConstants.PROP_ARTIFACTORY_MAVEN_PLUGIN, VfsComparatorType.ANY, (String) null);
        VfsQueryResult queryResult = repoQuery.execute(false);

        List<JcrFile> pluginPomFiles = new ArrayList<JcrFile>();

        for (VfsNode vfsNode : queryResult.getNodes()) {
            try {
                RepoPath repoPath = PathFactoryHolder.get().getRepoPath(vfsNode.absolutePath());
                JcrFile jcrFile = repo.getJcrFile(repoPath);
                pluginPomFiles.add(jcrFile);
            } catch (Exception e) {
                // node might be deleted in the mean time
                log.warn(e.getMessage());
            }
        }
        return pluginPomFiles;
    }

    @Override
    public GarbageCollectorInfo garbageCollect(boolean fixConsistency) {
        JcrSession session = getUnmanagedSession();
        try {
            DataStore store = JcrUtils.getDataDtore(session);
            if (store == null) {
                // GC activated before data store initialized
                log.info("Datastore not yet initialized. Not running garbage collector.");
                return null;
            }

            //fixConsistency for v2 storage should only run at startup time - no need to call it
            ChecksumPaths cspaths = ContextHelper.get().beanForType(ChecksumPaths.class);
            GarbageCollectorInfo collect = cspaths.cleanupDeleted();
            long storageSize = -1;
            //Only calculate the size on debug
            if (LoggerFactory.getLogger(GarbageCollectorInfo.class).isDebugEnabled()) {
                try {
                    storageSize = JcrUtils.getExtendedDataStore(session).getStorageSize();
                } catch (RepositoryException e) {
                    log.debug("Unable to measure storage size for the GC summary.", e);
                }
            }
            collect.printCollectionInfo(storageSize);
            return collect;
        } finally {
            session.logout();
        }
    }

    private void throwSetException(Node parent, String nodeName, Exception e) {
        String parentPath;
        try {
            parentPath = parent.getPath();
        } catch (RepositoryException e1) {
            parentPath = "unknown";
        }
        throw new RepositoryRuntimeException(
                "Could not set string '" + nodeName + "' on '" + parentPath + "'.", e);
    }

    @Override
    public void onContextCreated() {
        if (JcrVersion.hasMarkerConvertersInNeed()) {
            StorageContextHelper.get().getJcrService().activateMarkerFileConverters();
        }
    }

    @Override
    public void onContextReady() {
    }

    @Override
    public void onContextUnready() {
    }
}
