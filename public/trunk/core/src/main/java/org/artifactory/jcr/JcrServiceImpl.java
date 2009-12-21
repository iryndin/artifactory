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

package org.artifactory.jcr;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import static org.apache.jackrabbit.JcrConstants.*;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.GarbageCollectorFactory;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.util.ISO9075;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.DeployableUnit;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.repo.ArtifactCount;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.storage.GarbageCollectorInfo;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.config.xml.ArtifactoryXmlFactory;
import org.artifactory.config.xml.EntityResolvingContentHandler;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.io.NonClosingInputStream;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.ChecksumInputStream;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.gc.JcrGarbageCollector;
import org.artifactory.jcr.lock.aop.LockingAdvice;
import org.artifactory.jcr.md.MetadataService;
import org.artifactory.jcr.trash.Trashman;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.jcr.JcrHelper;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.schedule.TaskService;
import org.artifactory.search.InternalSearchService;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.traffic.InternalTrafficService;
import org.artifactory.tx.SessionResource;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.LoggingUtils;
import org.artifactory.util.PathUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.extensions.jcr.SessionFactoryUtils;
import org.springframework.stereotype.Repository;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Spring based session factory for tx jcr sessions
 *
 * @author yoavl
 */
@Repository
@Reloadable(beanClass = JcrService.class)
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
    @Qualifier("initStrategy")
    private InitStrategyFactory initStrategyFactory;

    @Autowired
    @Qualifier("indexConfigLocation")
    private ResourceStreamHandle indexConfig;

    @Autowired
    @Qualifier("nodeTypesResource")
    private ResourceStreamHandle nodeTypes;

    @Autowired
    @Qualifier("repoXmlResource")
    private ResourceStreamHandle repoXml;

    @Autowired
    private InternalRepositoryService repositoryService;

    @Autowired
    private MetadataService mdService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private InternalSearchService searchService;

    @Autowired
    InternalTrafficService trafficService;

    private Mapper ocmMapper;
    private JcrRepoInitStrategy strategy;

    //Don't inject or CGLib will crash the key for Tx resorce
    private JcrSessionFactory sessionFactory = new JcrSessionFactory();

    public javax.jcr.Repository getRepository() {
        return getManagedSession().getRepository();
    }

    public JcrSession getManagedSession() {
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

    public void init() {
        //Initialize the created jcr repository
        strategy.init();
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[0];
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        // Nothing
    }

    public void destroy() {
        try {
            sessionFactory.destroy();
        } catch (Exception e) {
            log.warn("Failed to destroy the jcr session factory.", e);
        }
    }

    /**
     * Nothing to convert: all jcr related conversions are handled by the repository creation at bootstap - by
     * org.artifactory.jcr.JcrServiceImpl.initJcrRepository() and later - org.artifactory.jcr.JcrRepoInitStrategy.updateCurrentWorkspaces().
     * The security service manipulating the ocms.
     */
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        //noop
    }

    public ObjectContentManager getOcm() {
        if (ocmMapper == null) {
            return null;
        }
        JcrSession session = getManagedSession();
        return new ObjectContentManagerImpl(session, ocmMapper);
    }

    public boolean itemNodeExists(String absPath) {
        return itemNodeExists(absPath, getManagedSession());
    }

    public boolean itemNodeExists(String absPath, JcrSession session) {
        if (absPath == null || !absPath.startsWith("/")) {
            return false;
        }
        try {
            return session.itemExists(absPath);
        } catch (RepositoryRuntimeException e) {
            if (ExceptionUtils.getCauseOfTypes(e, MalformedPathException.class) != null) {
                return false;
            } else {
                throw e;
            }
        }
    }

    public boolean fileNodeExists(String absPath) {
        if (itemNodeExists(absPath)) {
            Node node = (Node) getManagedSession().getItem(absPath);
            try {
                return JcrFile.NT_ARTIFACTORY_FILE.equals(node.getPrimaryNodeType().getName());
            } catch (RepositoryException e) {
                throw new RepositoryRuntimeException("Cannot get node type for non exiting item at '" + absPath + "'.");
            }
        }
        return false;
    }

    public int delete(String absPath) {
        return delete(absPath, getManagedSession());
    }

    public void emptyTrash() {
        JcrSession session = getUnmanagedSession();
        try {
            Node trashNode = (Node) session.getItem(JcrPath.get().getTrashJcrRootPath());
            NodeIterator trashChildren = trashNode.getNodes();
            while (trashChildren.hasNext()) {
                Node trashChild = trashChildren.nextNode();
                deleteFromTrash(trashChild.getName());
            }
        } catch (Exception e) {
            //Fail gracefully
            log.error("Could not empty trash.", e);
        } finally {
            session.logout();
        }
    }

    public void deleteFromTrash(String sessionFolderName) {
        if (StringUtils.isBlank(sessionFolderName)) {
            log.info("Received blank folder name as trash removal target. Ignoring.");
            return;
        }
        String sessionFolderPath = JcrPath.get().getTrashJcrRootPath() + "/" + sessionFolderName;
        JcrSession session = getUnmanagedSession();
        try {
            int deletedItems = delete(sessionFolderPath, session);
            if (deletedItems > 0) {
                log.debug("Emptied " + deletedItems + " nodes from trash folder " + sessionFolderName + ".");
            }
        } catch (Exception e) {
            //Fail gracefully
            LoggingUtils.warnOrDebug(log, "Could not empty trash folder " + sessionFolderName + ".", e);
        } finally {
            session.logout();
        }
    }

    private int delete(String absPath, JcrSession session) {
        if (itemNodeExists(absPath, session)) {
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

    public JcrFile importFile(JcrFolder parentFolder, File file, ImportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        String name = file.getName();
        RepoPath repoPath = new RepoPath(parentFolder.getRepoPath(), name);
        log.debug("Importing '{}'.", repoPath);
        //Takes a read lock
        assertValidDeployment(parentFolder, name);
        JcrFile jcrFile = null;
        try {
            jcrFile = parentFolder.getRepo().getLockedJcrFile(repoPath, true);
            jcrFile.importFrom(file, settings);
            //Mark for indexing (if needed)
            searchService.markArchiveForIndexing(jcrFile, false);
            repoPath = jcrFile.getRepoPath();
            log.debug("Imported '{}'.", repoPath);
            AccessLogger.deployed(repoPath);
        } catch (Exception e) {
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
                    log.error("Could not list child node '{}'.", node, e);
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to retrieve folder node items.", e);
        }
        return items;
    }

    public Node getOrCreateUnstructuredNode(String absPath) {
        JcrSession session = getManagedSession();
        Node root = session.getRootNode();
        return getOrCreateUnstructuredNode(root, absPath);
    }

    public Node getOrCreateUnstructuredNode(Node parent, String relPath) {
        return getOrCreateNode(parent, relPath, JcrHelper.NT_UNSTRUCTURED);
    }

    public Node getOrCreateNode(Node parent, String relPath, String type) {
        String cleanPath = relPath.startsWith("/") ? relPath.substring(1) : relPath;
        try {
            if (!parent.hasNode(cleanPath)) {
                Node node = parent.addNode(cleanPath, type);
                //Make it lockable and referencable
                node.addMixin("mix:lockable");
                node.addMixin("mix:referenceable");
                log.debug("Created node: {}.", relPath);
                return node;
            } else {
                return parent.getNode(cleanPath);
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void exportFile(JcrFile jcrFile, ExportSettings settings) {
        jcrFile.exportTo(settings);
    }

    public String getXml(String nodePath) throws RepositoryRuntimeException {
        JcrSession session = getManagedSession();
        Node node = (Node) session.getItem(nodePath);
        try {
            return IOUtils.toString(getRawXmlStream(node), "utf-8");
        } catch (Exception e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void setXml(String parentPath, String nodeName, String xml, String userId)
            throws RepositoryRuntimeException {
        JcrSession session = getManagedSession();
        Node parentNode = (Node) session.getItem(parentPath);
        setXml(parentNode, nodeName, xml, false, userId);
    }

    public void setXml(Node parent, String nodeName, String xml, boolean importXmlDocument, String userId)
            throws RepositoryRuntimeException {
        try {
            Node metadataNode;
            Node xmlNode;
            boolean exists = parent.hasNode(nodeName);
            if (exists) {
                metadataNode = parent.getNode(nodeName);
                if (xml == null) {
                    //Just the remove the md and return
                    metadataNode.remove();
                    return;
                }
            } else {
                if (xml == null) {
                    return;
                }
                Calendar created = Calendar.getInstance();
                //Add the metdata node and its xml child
                metadataNode = parent.addNode(nodeName);
                metadataNode.setProperty(JcrService.PROP_ARTIFACTORY_CREATED, created);
            }
            //Update the last modified on the specific metadata
            Calendar lastModified = Calendar.getInstance();
            metadataNode.setProperty(JcrService.PROP_ARTIFACTORY_LAST_MODIFIED, lastModified);
            metadataNode.setProperty(JcrService.PROP_ARTIFACTORY_LAST_MODIFIED_BY, userId);

            //Add the xml child
            //Cache the xml in memory since when reading from an http stream directly we cannot mark
            InputStream xmlStream = IOUtils.toInputStream(xml, "utf-8");

            //Import the xml as a jcr documemt if needed - an xmlNode (with artifactory:xml/metadataName as its root
            //element) will be created from the input stream
            if (importXmlDocument) {
                if (metadataNode.hasNode(JcrService.NODE_ARTIFACTORY_XML)) {
                    xmlNode = metadataNode.getNode(JcrService.NODE_ARTIFACTORY_XML);
                } else {
                    xmlNode = metadataNode.addNode(JcrService.NODE_ARTIFACTORY_XML);
                }
                importXml(xmlNode, xmlStream);
            }
            xmlStream.reset();
            //Set the original raw xml
            Node resourceNode;
            boolean hasResourceNode = metadataNode.hasNode(JCR_CONTENT);
            if (hasResourceNode) {
                resourceNode = metadataNode.getNode(JCR_CONTENT);
            } else {
                resourceNode = metadataNode.addNode(JCR_CONTENT, NT_RESOURCE);
            }
            resourceNode.setProperty(JCR_MIMETYPE, ContentType.applicationXml.getMimeType());
            resourceNode.setProperty(JCR_ENCODING, "utf-8");
            resourceNode.setProperty(JCR_LASTMODIFIED, lastModified);
            //Write the raw data and calculate checksums on it
            ChecksumInputStream checksumInputStream = new ChecksumInputStream(xmlStream,
                    new Checksum(ChecksumType.md5),
                    new Checksum(ChecksumType.sha1));
            //Save the data and calc the checksums
            resourceNode.setProperty(JCR_DATA, checksumInputStream);
            checksumInputStream.close();
            setChecksums(metadataNode, checksumInputStream.getChecksums(), true);
        } catch (Exception e) {
            String parentPath;
            try {
                parentPath = parent.getPath();
            } catch (RepositoryException e1) {
                parentPath = "unknown";
            }
            throw new RepositoryRuntimeException(
                    "Could not save xml element '" + nodeName + "' on '" + parentPath + "'.", e);
        }
    }

    public static void setChecksums(Node metadataNode, Checksum[] checksums, boolean override)
            throws RepositoryException {
        for (Checksum checksum : checksums) {
            //Save the checksum as a property
            String metadataName = metadataNode.getName();
            String checksumStr = checksum.getChecksum();
            ChecksumType checksumType = checksum.getType();
            String propName = getChecksumPropertyName(checksumType);
            if (override || !metadataNode.hasProperty(propName)) {
                if (log.isDebugEnabled()) {
                    log.debug("Saving checksum for '" + metadataName + "' as '" + propName + "' (checksum=" +
                            checksumStr + ").");
                }
                metadataNode.setProperty(propName, checksumStr);
            }
        }
    }

    /**
     * Import xml with characters entity resolving
     */
    public void importXml(Node xmlNode, InputStream in) throws RepositoryException, IOException {
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
                if (e1 != null && e1 instanceof RepositoryException) {
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

    NodeTypeDef[] getArtifactoryNodeTypes() throws IOException, InvalidNodeTypeDefException {
        NodeTypeDef[] types;
        try {
            types = NodeTypeReader.read(nodeTypes.getInputStream());
        } finally {
            nodeTypes.close();
        }
        return types;
    }

    /**
     * Inits the singleton JCR repository
     *
     * @throws Exception
     */
    @PostConstruct
    private void initJcrRepository() throws Exception {
        // Don't call the parent the repository is not created yet
        cleanupAfterLastShutdown();
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        System.setProperty("derby.stream.error.file",
                new File(artifactoryHome.getLogDir(), "derby.log").getAbsolutePath());

        //Copy the indexing config
        try {
            File indexDir = new File(artifactoryHome.getJcrRootDir(), "index");
            FileUtils.forceMkdir(indexDir);
            File indexConfigFile = new File(indexDir, "index_config.xml");
            FileOutputStream fos = new FileOutputStream(indexConfigFile);
            IOUtils.copy(indexConfig.getInputStream(), fos);
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to config jcr repo indexing.", e);
        } finally {
            if (indexConfig != null) {
                indexConfig.close();
            }
        }

        //Get the initializer strategy -
        strategy = initStrategyFactory.getInitJcrRepoStrategy(this);
        //Create the repository
        final JackrabbitRepository repository = strategy.createJcrRepository(repoXml);
        sessionFactory.setRepository(repository);
        // Now that the repo is half done we can call super
        sessionFactory.afterPropertiesSet();

        InternalArtifactoryContext context = InternalContextHelper.get();
        JcrTransactionManager txMgr = context.beanForType(JcrTransactionManager.class);
        txMgr.setSessionFactory(sessionFactory);
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
     *
     * @param parentFolder
     * @param name
     */
    private void assertValidDeployment(JcrFolder parentFolder, String name) {
        String repoKey = parentFolder.getRepoKey();
        String targetPath = parentFolder.getPath() + "/" + name;
        int relPathStart = targetPath.indexOf(repoKey + "/");
        String relPath = targetPath.substring(relPathStart + repoKey.length() + 1);
        LocalRepo repo = repositoryService.localOrCachedRepositoryByKey(repoKey);
        if (repo == null) {
            throw new RepositoryRuntimeException("The repository '" + repoKey + "' is not configured.");
        }
        StatusHolder statusHolder = repositoryService.assertValidDeployPath(repo, relPath);
        if (statusHolder.isError()) {
            if (statusHolder.getException() != null) {
                throw new RepositoryRuntimeException(statusHolder.getStatusMsg(), statusHolder.getException());
            }
            throw new RepositoryRuntimeException(statusHolder.getStatusMsg());
        }
    }

    public JcrFsItem getFsItem(Node node, StoringRepo repo) {
        String typeName = JcrHelper.getPrimaryTypeName(node);
        if (typeName.equals(JcrFolder.NT_ARTIFACTORY_FOLDER)) {
            return new JcrFolder(node, repo);
        } else if (typeName.equals(JcrFile.NT_ARTIFACTORY_FILE)) {
            return new JcrFile(node, repo);
        } else {
            throw new RepositoryRuntimeException(
                    "Cannot create file system item from unknown node type " + typeName + " for node " + node);
        }
    }

    public QueryResult executeXpathQuery(String xpathQuery) throws RepositoryException {
        log.debug("Executing xpath query: {}", xpathQuery);
        return executeJcrQuery(xpathQuery, Query.XPATH);
    }

    public QueryResult executeSqlQuery(String sqlQuery) throws RepositoryException {
        log.debug("Executing sql query: {}", sqlQuery);
        return executeJcrQuery(sqlQuery, Query.SQL);
    }

    private QueryResult executeJcrQuery(String queryStr, String queryType) throws RepositoryException {
        long start = System.currentTimeMillis();
        JcrSession session = getManagedSession();
        Workspace workspace = session.getWorkspace();
        QueryManager queryManager = workspace.getQueryManager();
        Query query = queryManager.createQuery(queryStr, queryType);
        QueryResult queyResult = query.execute();
        log.debug("{} query execution time: {} ms", queryType, System.currentTimeMillis() - start);
        return queyResult;
    }

    public ArtifactCount getArtifactCount() throws RepositoryException {
        return getArtifactCount(null);
    }

    public ArtifactCount getArtifactCount(String repoKey) throws RepositoryException {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM nt:file WHERE ");

        if (StringUtils.isNotBlank(repoKey)) {
            sb.append("jcr:path LIKE '").append(JcrPath.get().getRepoJcrRootPath()).append("/").append(repoKey).
                    append("/%' AND ");
        }
        sb.append("artifactory:name LIKE ");

        String count_query = sb.toString();
        QueryResult resultJar = executeSqlQuery(count_query + "'%.jar'");
        QueryResult resultPom = executeSqlQuery(count_query + "'%.pom'");
        int jarCount = (int) resultJar.getNodes().getSize();
        int pomCount = (int) resultPom.getNodes().getSize();
        return new ArtifactCount(jarCount, pomCount);
    }

    public List<DeployableUnit> getDeployableUnitsUnder(RepoPath repoPath) throws RepositoryException {
        QueryResult result = searchService.searchPomInPath(repoPath);

        List<DeployableUnit> deployableUnits = new ArrayList<DeployableUnit>();
        NodeIterator nodeIterator = result.getNodes();
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.nextNode();
            // this is the parent node of the pom files - the version folder
            Node versionNode = node.getParent();
            RepoPath folderRepoPath = JcrPath.get().getRepoPath(versionNode.getPath());
            DeployableUnit du = new DeployableUnit(folderRepoPath);
            deployableUnits.add(du);
        }

        return deployableUnits;
    }

    public List<JcrFile> getPluginPomNodes(RepoPath repoPath, StoringRepo repo) throws RepositoryException {
        StringBuilder qb = new StringBuilder();
        constructXpathPrefix(repoPath, qb);
        // append the path to the packaging text node
        qb.append("//artifactory:xml/project/packaging/jcr:xmltext ");
        // select packaging of type maven-plugin
        qb.append("[jcr:like(@jcr:xmlcharacters,'maven-plugin')]");
        QueryResult result;
        try {
            result = executeXpathQuery(qb.toString());
        } catch (javax.jcr.query.InvalidQueryException e) {
            // might happen if the 'ns' namespace is not registered yet (e.g., import with no poms)  
            log.debug("Plugins poms query exception", e);
            return Collections.emptyList();
        }
        List<JcrFile> pluginPomFiles = new ArrayList<JcrFile>();
        NodeIterator nodeIterator = result.getNodes();
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.nextNode();
            // go all the way up to the the pom main node....
            Node pomFileNode = node.getParent().getParent().getParent().getParent();
            JcrFsItem jcrFile = getFsItem(pomFileNode, repo);
            pluginPomFiles.add((JcrFile) jcrFile);
        }

        return pluginPomFiles;
    }

    private void constructXpathPrefix(RepoPath repoPath, StringBuilder qb) {
        qb.append("/jcr:root/repositories/");
        qb.append(repoPath.getRepoKey());
        if (PathUtils.hasText(repoPath.getPath())) {
            // the path might contain node names starting with numbers so we must escape
            // the path for the xpath query
            String relativePath = ISO9075.encodePath(repoPath.getPath());
            qb.append('/').append(relativePath);
        }
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

    public static String getChecksumPropertyName(ChecksumType checksumType) {
        return ARTIFACTORY_PREFIX + checksumType.alg();
    }

    public static InputStream getRawXmlStream(Node metadataNode) throws RepositoryException {
        Node xmlNode = metadataNode.getNode(JCR_CONTENT);
        Property property = xmlNode.getProperty(JCR_DATA);
        log.trace("Read xml data '{}' with length: {}.", xmlNode.getPath(), property.getLength());
        Value attachedDataValue = property.getValue();
        InputStream is = attachedDataValue.getStream();
        return is;
    }

    private int deleteNodeRecursively(Node node) throws RepositoryException {
        int count = 0;
        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node child = (Node) nodes.next();
            count += deleteNodeRecursively(child);
        }
        //Only delete known or unstructured node types (may be containers of nodes of other types)
        String nodeType = node.getPrimaryNodeType().getName();
        if (JcrFile.NT_ARTIFACTORY_FILE.equals(nodeType) || JcrFolder.NT_ARTIFACTORY_FOLDER.equals(nodeType) ||
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
}
