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

package org.artifactory.jcr;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.GarbageCollectorFactory;
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
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.ArtifactCount;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.api.storage.GarbageCollectorInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.config.xml.ArtifactoryXmlFactory;
import org.artifactory.config.xml.EntityResolvingContentHandler;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.io.NonClosingInputStream;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.ChecksumInputStream;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.fs.JcrTreeNode;
import org.artifactory.jcr.fs.JcrTreeNodeFileFilter;
import org.artifactory.jcr.gc.JcrGarbageCollector;
import org.artifactory.jcr.lock.aop.LockingAdvice;
import org.artifactory.jcr.md.MetadataDefinitionService;
import org.artifactory.jcr.trash.Trashman;
import org.artifactory.jcr.version.JcrVersion;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.jcr.JcrHelper;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.search.InternalSearchService;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.traffic.InternalTrafficService;
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

import javax.annotation.PostConstruct;
import javax.jcr.Binary;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

import static org.apache.jackrabbit.JcrConstants.*;
import static org.artifactory.jcr.JcrTypes.*;

/**
 * Spring based session factory for tx jcr sessions
 *
 * @author yoavl
 */
@Repository
@Reloadable(beanClass = JcrService.class, initAfter = MetadataDefinitionService.class)
public class JcrServiceImpl implements JcrService, JcrRepoService {
    private static final Logger log = LoggerFactory.getLogger(JcrServiceImpl.class);

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
    private InternalRepositoryService repositoryService;

    @Autowired
    private InternalSearchService searchService;

    @Autowired
    InternalTrafficService trafficService;

    private Mapper ocmMapper;

    //Don't inject or CGLib will crash the key for Tx resource
    private JcrSessionFactory sessionFactory;

    private static final ThreadLocal<JcrSession> UNMANAGED_SESSION_HOLDER = new ThreadLocal<JcrSession>();

    private Semaphore emptyTrashSemaphore = new Semaphore(1);

    public javax.jcr.Repository getRepository() {
        return getManagedSession().getRepository();
    }

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

        InternalArtifactoryContext context = InternalContextHelper.get();
        JcrTransactionManager txMgr = context.beanForType(JcrTransactionManager.class);
        txMgr.setSessionFactory(sessionFactory);
    }

    public void init() {
        //Initialize the created jcr repository
        JcrRepoInitHelper.init(this);
        ArtifactoryHome artifactoryHome = InternalContextHelper.get().getArtifactoryHome();
        if (artifactoryHome.startedFromDifferentVersion()) {
            CompoundVersionDetails source = artifactoryHome.getOriginalVersionDetails();
            JcrVersion.values();
            JcrVersion originalVersion = source.getVersion().getSubConfigElementVersion(JcrVersion.class);

            /**
             * Keep track if we opened the session or not (might be opened by JCR metadata converter before us), and if
             * not, don't close it and remote from the thread local, as it is expected in other places
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

    public void reload(CentralConfigDescriptor oldDescriptor) {
        // Nothing
    }

    public void reCreateJcrRepository() throws Exception {
        initJcrRepository(false);
        // Don't call init it is going to be called by normal bean refresh
    }

    public void destroy() {
        try {
            sessionFactory.destroy();
        } catch (Exception e) {
            log.warn("Failed to destroy the jcr session factory.", e);
        } finally {
            sessionFactory = null;
        }
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        //ATTENTION: Pre JCR init done in org.artifactory.jcr.JcrRepoInitHelper.preInitConvert() since this method is called too late
        JcrVersion.values();
        JcrVersion originalVersion = source.getVersion().getSubConfigElementVersion(JcrVersion.class);
        JcrSession unmanagedSession = getUnmanagedSession();
        Session rawSession = unmanagedSession.getSession();
        //Convert the ocm storage
        try {
            UNMANAGED_SESSION_HOLDER.set(unmanagedSession);
            originalVersion.convert(rawSession);
        } finally {
            UNMANAGED_SESSION_HOLDER.remove();
            rawSession.logout();
        }
    }

    public ObjectContentManager getOcm() {
        if (ocmMapper == null) {
            return null;
        }
        JcrSession session = getManagedSession();
        return new ObjectContentManagerImpl(session, ocmMapper);
    }

    public boolean itemNodeExists(String absPath) {
        return JcrHelper.itemNodeExists(absPath, getManagedSession());
    }

    public boolean isFile(RepoPath repoPath) {
        String absPath = JcrPath.get().getAbsolutePath(repoPath);
        JcrSession jcrSession = getManagedSession();
        if (JcrHelper.itemNodeExists(absPath, jcrSession)) {
            Node node = (Node) jcrSession.getItem(absPath);
            return JcrHelper.isFile(node);
        }
        return false;
    }

    public boolean isFolder(RepoPath repoPath) {
        String absPath = JcrPath.get().getAbsolutePath(repoPath);
        JcrSession jcrSession = getManagedSession();
        if (JcrHelper.itemNodeExists(absPath, jcrSession)) {
            Node node = (Node) jcrSession.getItem(absPath);
            return JcrHelper.isFolder(node);
        }
        return false;
    }

    public int delete(String absPath) {
        return delete(absPath, getManagedSession());
    }

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

    public void emptyTrashAfterCommit() {
        emptyTrash();   // called after TX commit; just delegate to empty trash
    }

    private Node getTrashNode(JcrSession session) {
        return (Node) session.getItem(JcrPath.get().getTrashJcrRootPath());
    }

    private int deleteFromTrash(Node trashNode, JcrSession session) {
        String folderName = null;
        try {
            folderName = trashNode.getName();
            log.debug("Emptying nodes from trash folder '{}'.", folderName);
            int deletedItems = delete(trashNode.getPath(), session);
            return deletedItems;
        } catch (Exception e) {
            //Fail gracefully
            LoggingUtils.warnOrDebug(log, "Could not empty trash folder " + folderName + ".", e);
            return 0;
        }
    }

    private int delete(String absPath, JcrSession session) {
        if (JcrHelper.itemNodeExists(absPath, session)) {
            Node node = (Node) session.getItem(absPath);
            try {
                return deleteNodeRecursively(node);
            } catch (RepositoryException e) {
                log.warn("Attempting force removal of parent node at '{}.'", absPath);
                try {
                    node.remove();
                    //Saving the session manually after removal since we might not be in a transaction
                    node.getSession().save();
                    log.warn("Force removal of node at '{}' succeeded.", absPath);
                } catch (RepositoryException e1) {
                    LoggingUtils.warnOrDebug(
                            log, "Cannot complete force removal of node at '" + absPath + "'.", e);
                    //Continue with the other trash folders
                }
                //Rethrow
                throw new RepositoryRuntimeException(e);
            }
        }
        log.debug("Cannot delete a non exiting node at '{}'.", absPath);
        return 0;
    }

    /**
     * "Shallow" folder import in a new tx
     */
    public RepoPath importFolder(LocalRepo repo, RepoPath jcrFolder, ImportSettings settings) {
        JcrFolder folder = repo.getLockedJcrFolder(jcrFolder, true);
        folder.importFrom(settings);
        return folder.getRepoPath();
    }

    public JcrFile importFile(JcrFolder parentFolder, File file, ImportSettings settings)
            throws RepoRejectException {
        RepoPath targetRepoPath = new RepoPathImpl(parentFolder.getRepoPath(), file.getName());
        log.debug("Importing '{}'.", targetRepoPath);
        //Takes a read lock
        assertValidDeployment(targetRepoPath);
        JcrFile jcrFile = null;
        try {
            jcrFile = parentFolder.getRepo().getLockedJcrFile(targetRepoPath, true);
            jcrFile.importFrom(file, settings);
            //Mark for indexing (if needed)
            searchService.markArchiveForIndexing(jcrFile, false);
            targetRepoPath = jcrFile.getRepoPath();
            log.debug("Imported '{}'.", targetRepoPath);
            AccessLogger.deployed(targetRepoPath);
        } catch (RepoRejectException rre) {
            throw rre;
        } catch (Exception e) {
            MultiStatusHolder status = settings.getStatusHolder();
            status.setWarning("Could not import file '" + file.getAbsolutePath() + "' into " + jcrFile + ".", e, log);
            //Remove the file, so that save will not be attempted at the end of the transaction
            if (jcrFile != null) {
                jcrFile.bruteForceDelete();
            }
        }
        return jcrFile;
    }

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

    public void move(String fromAbsPath, String toAbsPath) {
        JcrSession session = getManagedSession();
        session.move(fromAbsPath, toAbsPath);
    }

    public void copy(String fromAbsPath, String toAbsPath) {
        JcrSession session = getManagedSession();
        session.copy(fromAbsPath, toAbsPath);
    }

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

    public void trash(List<JcrFsItem> items) {
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
    public JcrFsItem getFsItem(RepoPath repoPath, StoringRepo repo) {
        JcrSession session = getManagedSession();
        String absPath = JcrPath.get().getAbsolutePath(repoPath);
        boolean exits = itemNodeExists(absPath);
        if (exits) {
            Node node = (Node) session.getItem(absPath);
            return getFsItem(node, repo);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getNodeTypeName(RepoPath repoPath) {
        JcrSession session = getManagedSession();
        String absPath = JcrPath.get().getAbsolutePath(repoPath);
        boolean exits = itemNodeExists(absPath);
        if (exits) {
            Node node = (Node) session.getItem(absPath);
            return JcrHelper.getPrimaryTypeName(node);
        } else {
            return null;
        }
    }

    public void writeMetadataEntries(JcrFsItem fsItem, BasicStatusHolder status, File folder, boolean incremental) {
        fsItem.writeMetadataEntries(status, folder, incremental);
    }

    /**
     * {@inheritDoc}
     */
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

    public Node getNode(String absPath) {
        JcrSession session = getManagedSession();
        Node root = session.getRootNode();
        return JcrHelper.getNode(root, absPath);
    }

    public Node getOrCreateUnstructuredNode(String absPath) {
        JcrSession session = getManagedSession();
        Node root = session.getRootNode();
        return getOrCreateUnstructuredNode(root, absPath);
    }

    public Node getNode(Node parent, String relPath) {
        return JcrHelper.getNode(parent, relPath);
    }

    public Node getOrCreateUnstructuredNode(Node parent, String relPath) {
        return JcrHelper.getOrCreateNode(parent, relPath, JcrTypes.NT_UNSTRUCTURED, "mix:referenceable");
    }

    public Node getOrCreateNode(Node parent, String relPath, String type, String... mixins) {
        return JcrHelper.getOrCreateNode(parent, relPath, type, mixins);
    }

    public void exportFile(JcrFile jcrFile, ExportSettings settings) {
        jcrFile.exportTo(settings);
    }

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

    public InputStream getStream(String nodePath) {
        JcrSession session = getManagedSession();
        Node node = (Node) session.getItem(nodePath);
        try {
            return JcrHelper.getRawStringStream(node);
        } catch (Exception e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void setString(String parentPath, String nodeName, String value, String mimeType, String userId) {
        JcrSession session = getManagedSession();
        Node parentNode = (Node) session.getItem(parentPath);
        setString(parentNode, nodeName, value, mimeType, userId, false);
    }

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
    public void saveChecksums(JcrFsItem fsItem, String metadataName, Checksum[] checksums) {
        // TODO: Maybe write lock the fsItem?
        Node metadataNode = JcrHelper.safeGetNode(fsItem.getNode(), NODE_ARTIFACTORY_METADATA, metadataName);
        if (metadataNode != null) {
            setChecksums(metadataNode, checksums, false);
        }
    }

    public JcrTreeNode getTreeNode(RepoPath itemPath, MultiStatusHolder multiStatusHolder,
            JcrTreeNodeFileFilter fileFilter) {
        Node itemNode = getNode(JcrPath.get().getAbsolutePath(itemPath));
        if (itemNode == null) {
            return null;
        }

        return processTreeNodeRecursively(itemNode, multiStatusHolder, fileFilter);
    }

    public InputStream getDataStreamBySha1Checksum(String sha1) throws DataStoreException {
        JcrSession managedSession = getManagedSession();
        RepositoryImpl rep = (RepositoryImpl) managedSession.getRepository();
        DataStore dataStore = rep.getDataStore();
        DataRecord record = dataStore.getRecord(new DataIdentifier(sha1));
        if (record != null) {
            return record.getStream();
        }

        return null;
    }

    private JcrTreeNode processTreeNodeRecursively(Node itemNode, MultiStatusHolder multiStatusHolder,
            JcrTreeNodeFileFilter fileFilter) {

        try {
            String absolutePath = itemNode.getPath();
            boolean isFolder = JcrHelper.isFolder(itemNode);

            RepoPath repoPath = JcrPath.get().getRepoPath(absolutePath);
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
        if (itemNode.hasProperty(JcrTypes.PROP_ARTIFACTORY_CREATED)) {
            return itemNode.getProperty(JcrTypes.PROP_ARTIFACTORY_CREATED).getDate();
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
                String propName = checksumType.getActualPropName();
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

    @SuppressWarnings({"unchecked"})
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

    /**
     * Check that the repository approves the deployment
     */
    private void assertValidDeployment(RepoPath targetRepoPath) throws RepoRejectException {
        String repoKey = targetRepoPath.getRepoKey();
        LocalRepo repo = repositoryService.localOrCachedRepositoryByKey(repoKey);
        if (repo == null) {
            throw new RepoRejectException("The repository '" + repoKey + "' is not configured.");
        }
        repositoryService.assertValidDeployPath(repo, targetRepoPath.getPath());
    }

    public JcrFsItem getFsItem(Node node, StoringRepo repo) {
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

    public QueryResult executeQuery(JcrQuerySpec spec) {
        JcrSession session = getManagedSession();
        return executeQuery(spec, session);
    }

    public QueryResult executeQuery(JcrQuerySpec spec, JcrSession session) {
        long start = System.currentTimeMillis();
        Workspace workspace = session.getWorkspace();
        QueryResult queryResult;
        try {
            QueryManager queryManager = workspace.getQueryManager();
            String queryString = spec.query();
            log.debug("Executing JCR query: {}", queryString);
            Query query = queryManager.createQuery(queryString, spec.jcrType());
            int limit = spec.limit();
            if (limit >= 0) {
                query.setLimit(limit);
            }
            queryResult = query.execute();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Error while executing query.", e);
        }
        log.debug("{} query execution time: {} ms", spec.jcrType(), System.currentTimeMillis() - start);
        return queryResult;
    }

    public ArtifactCount getArtifactCount() {
        return getArtifactCount(null);
    }

    public ArtifactCount getArtifactCount(String repoKey) {
        StringBuilder sb = new StringBuilder();

        if (StringUtils.isNotBlank(repoKey)) {
            sb.append("/jcr:root").append(JcrPath.get().getRepoJcrRootPath()).append("/").append(repoKey);
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

    public List<JcrFile> getPluginPomNodes(StoringRepo repo) {
        StringBuilder qb = new StringBuilder("//element(*, ").append(JcrConstants.NT_UNSTRUCTURED).
                append(") [@jcr:xmlcharacters = 'maven-plugin']");
        QueryResult result;
        try {
            result = executeQuery(JcrQuerySpec.xpath(qb.toString()).noLimit());
        } catch (RepositoryRuntimeException e) {
            Throwable invalidQueryException = ExceptionUtils.getCauseOfTypes(e, InvalidQueryException.class);
            if (invalidQueryException != null) {
                // might happen if the 'ns' namespace is not registered yet (e.g., import with no poms)
                log.debug("Plugin poms query exception", e);
                return Collections.emptyList();
            } else {
                throw e;
            }
        }
        List<JcrFile> pluginPomFiles = new ArrayList<JcrFile>();
        String queryRepoKey = repo.getKey();
        String repoPrefix = "/repositories/" + queryRepoKey + "/";
        try {
            NodeIterator nodeIterator = result.getNodes();
            while (nodeIterator.hasNext()) {
                Node node = nodeIterator.nextNode();
                try {
                    String path = node.getPath();
                    if (path.startsWith(repoPrefix) && path.endsWith("artifactory:xml/project/packaging/jcr:xmltext")) {
                        String artifactPath = path.substring(0, path.lastIndexOf("/" + NODE_ARTIFACTORY_XML));
                        RepoPath repoPath = JcrPath.get().getRepoPath(artifactPath);
                        JcrFile jcrFile = repo.getJcrFile(repoPath);
                        pluginPomFiles.add(jcrFile);
                    }
                } catch (ItemNotFoundException e) {
                    // in some cases nodes are in the trash and can get deleted while this iteration executes
                    log.warn(e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RepositoryRuntimeException("Could not get plugin pom nodes under '" + queryRepoKey + "'.", e);
        }

        return pluginPomFiles;
    }

    public GarbageCollectorInfo garbageCollect() {
        GarbageCollectorInfo result = null;
        JcrSession session = getUnmanagedSession();
        JcrGarbageCollector gc = null;
        try {
            gc = GarbageCollectorFactory.createDataStoreGarbageCollector(session);
            if (gc != null) {
                log.debug("Running " + gc.getClass().getName() + " datastore garbage collector...");
                if (gc.scan()) {
                    gc.stopScan();
                    int count = gc.deleteUnused();
                    if (count > 0) {
                        log.info("Datastore garbage collector deleted " + count + " unreferenced item(s).");
                    } else {
                        log.debug("Datastore garbage collector deleted " + count + " unreferenced item(s).");
                    }
                } else {
                    log.debug("Datastore garbage collector execution completed.");
                }
                result = gc.getInfo();
                gc = null;
            }
        } catch (Exception e) {
            log.error("Datastore garbage collector execution failed.", e);
        } finally {
            if (gc != null) {
                try {
                    gc.stopScan();
                } catch (RepositoryException re) {
                    log.debug("Datastore garbage collector scanning could not be stopped.", re);
                }
            }
            session.logout();
        }
        return result;
    }

    private int deleteNodeRecursively(Node node) throws RepositoryException {
        int count = 0;
        String nodeType = node.getPrimaryNodeType().getName();
        if (!NT_ARTIFACTORY_FILE.equals(nodeType)) { // delete files directly, no need to check children
            NodeIterator nodes = node.getNodes();
            while (nodes.hasNext()) {
                Node child = (Node) nodes.next();
                count += deleteNodeRecursively(child);
            }
        }
        //Only delete known or unstructured node types (may be containers of nodes of other types)
        if (NT_ARTIFACTORY_FILE.equals(nodeType) || NT_ARTIFACTORY_FOLDER.equals(nodeType) ||
                "nt:unstructured".equals(nodeType)) {
            //Remove myself
            log.debug("Deleting node: {}", node.getPath());
            node.remove();
            count++;
        }
        //Flush - actually stores the changes and preserves in-session memory
        node.getSession().save();
        return count;
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
}
