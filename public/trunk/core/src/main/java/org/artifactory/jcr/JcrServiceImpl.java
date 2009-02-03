/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.jcr;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.data.GarbageCollector;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.util.ISO9075;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.DeployableUnit;
import org.artifactory.api.repo.ArtifactCount;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.config.InternalCentralConfigService;
import org.artifactory.config.xml.ArtifactoryXmlFactory;
import org.artifactory.config.xml.EntityResolvingContentHandler;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.io.NonClosingInputStream;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.md.MetadataAware;
import org.artifactory.jcr.md.MetadataService;
import org.artifactory.jcr.trash.Trashman;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.index.IndexerJob;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.schedule.TaskCallback;
import org.artifactory.schedule.TaskService;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.tx.SessionResource;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.PathUtils;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springmodules.jcr.SessionFactoryUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
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
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring based session factory for tx jcr sessions
 *
 * @author yoavl
 */
@Repository
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

    private Mapper ocmMapper;
    private InitJcrRepoStrategy strategy;

    //Don't inject or CGLib will crash the key for Tx resorce
    private JcrSessionFactory sessionFactory = new JcrSessionFactory();

    public javax.jcr.Repository getRepository() {
        return getManagedSession().getRepository();
    }

    public JcrSession getManagedSession() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
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
        return new Class[]{InternalCentralConfigService.class};
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

    public ObjectContentManager getOcm() {
        if (ocmMapper == null) {
            return null;
        }
        JcrSession session = getManagedSession();
        return new ObjectContentManagerImpl(session, ocmMapper);
    }

    @SuppressWarnings({"unchecked"})
    public boolean itemNodeExists(final String absPath) {
        JcrSession session = getManagedSession();
        //Sanity check to avoid exception in "session.itemExists()"
        if (StringUtils.isEmpty(absPath) || !absPath.startsWith("/")) {
            return false;
        }
        boolean exists;
        try {
            exists = session.itemExists(absPath);
        } catch (RepositoryRuntimeException e) {
            Throwable cause = ExceptionUtils.getCauseOfTypes(e, MalformedPathException.class);
            if (cause != null) {
                return false;
            } else {
                throw e;
            }
        }
        return exists;
    }

    /**
     * "Shallow" folder import in a new tx
     *
     * @param jcrFolder
     * @param settings
     * @param status
     */
    public RepoPath importFolder(LocalRepo repo, RepoPath jcrFolder, ImportSettings settings, StatusHolder status) {
        JcrFolder folder = repo.getLockedJcrFolder(jcrFolder, true);
        folder.importFrom(settings, status);
        return folder.getRepoPath();
    }

    public JcrFile importFileViaWorkingCopy(
            JcrFolder parentFolder, File file, ImportSettings settings, StatusHolder status) {
        String name = file.getName();
        RepoPath repoPath = new RepoPath(parentFolder.getRepoPath(), name);
        if (log.isDebugEnabled()) {
            log.debug("Importing '" + repoPath + "'.");
        }
        assertValidDeployment(parentFolder, name);
        JcrFile jcrFile = null;
        try {
            jcrFile = parentFolder.getLocalRepo().getLockedJcrFile(repoPath, true);
            jcrFile.importFrom(file, settings, status);
            repoPath = jcrFile.getRepoPath();
            if (log.isDebugEnabled()) {
                log.debug("Imported '" + repoPath + "'.");
            }
            AccessLogger.deployed(repoPath);
        } catch (Exception e) {
            status.setError("Failed to import file '" + file.getAbsolutePath() + "' into " + jcrFile + ".", e, log);
        }
        return jcrFile;
    }

    public boolean delete(JcrFsItem fsItem) {
        JcrSession session = getManagedSession();
        String absPath = fsItem.getAbsolutePath();
        if (!session.itemExists(absPath)) {
            return false;
        }

        //TODO: [by yl] This event should really be emmitted from the fsitem itself, but is done here for tx propagation
        fsItem.getLocalRepo().onDelete(fsItem);
        return true;
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
                if (!MetadataAware.NODE_ARTIFACTORY_METADATA.equals(name)) {
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
            String tempFolderName = UUID.randomUUID().toString();
            String trashFolderPath = JcrPath.get().getTrashJcrRootPath() + "/" + tempFolderName;
            getOrCreateUnstructuredNode(trashFolderPath);
            JcrSession session = getManagedSession();
            //Move items to the trash folder
            for (JcrFsItem item : items) {
                String path = item.getPath();
                session.move(path, trashFolderPath + "/" + item.getName());
            }
            Trashman trashman = session.getOrCreateResource(Trashman.class);
            trashman.addTrashedFolder(tempFolderName);
        } catch (Exception e) {
            log.error("Could not move items to trash.", e);
        }
    }

    /**
     * Added @Transactional annotation because full system import fails (RTFACT-725)
     *
     * @param repoPath
     * @param repo
     * @return
     */
    public JcrFsItem getFsItem(RepoPath repoPath, LocalRepo repo) {
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
    public List<JcrFsItem> getChildren(JcrFolder folder, boolean withLock) {
        String absPath = folder.getAbsolutePath();
        JcrSession session = getManagedSession();
        List<JcrFsItem> items = new ArrayList<JcrFsItem>();
        if (!session.itemExists(absPath)) {
            return items;
        }
        Node parentNode = (Node) session.getItem(absPath);
        try {
            NodeIterator nodes = parentNode.getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                JcrFsItem item;
                if (withLock) {
                    item = folder.getLocalRepo().getLockedJcrFsItem(node);
                } else {
                    item = folder.getLocalRepo().getJcrFsItem(node);
                }
                if (item != null) {
                    items.add(item);
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to retrieve folder node items.", e);
        }
        return items;
    }

    public JcrFile importStream(
            JcrFolder parentFolder, String name, long lastModified, InputStream in) {
        RepoPath repoPath = new RepoPath(parentFolder.getRepoPath(), name);
        if (log.isDebugEnabled()) {
            log.debug("Importing '" + repoPath + "'.");
        }
        assertValidDeployment(parentFolder, name);
        JcrFile file = parentFolder.getLocalRepo().getLockedJcrFile(repoPath, true);
        file.fillData(lastModified, in);
        repoPath = file.getRepoPath();
        if (log.isDebugEnabled()) {
            log.debug("Imported '" + repoPath + "'.");
        }
        AccessLogger.deployed(repoPath);
        return file;
    }

    public Node getOrCreateUnstructuredNode(String absPath) {
        JcrSession session = getManagedSession();
        Node root = session.getRootNode();
        return getOrCreateUnstructuredNode(root, absPath);
    }

    public Node getOrCreateUnstructuredNode(Node parent, String name) {
        return getOrCreateNode(parent, name, "nt:unstructured");
    }

    public Node getOrCreateNode(Node parent, String name, String type) {
        String cleanPath = name.startsWith("/") ? name.substring(1) : name;
        try {
            if (!parent.hasNode(cleanPath)) {
                Node node = parent.addNode(cleanPath, type);
                //Make it lockable and referencable
                node.addMixin("mix:lockable");
                node.addMixin("mix:referenceable");
                if (log.isDebugEnabled()) {
                    log.debug("Created node: " + name + ".");
                }
                return node;
            } else {
                return parent.getNode(cleanPath);
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void exportFile(JcrFile jcrFile, ExportSettings settings, StatusHolder status) {
        jcrFile.exportTo(settings, status);
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
                session.getImportContentHandler(absPath, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        EntityResolvingContentHandler resolvingContentHandler = new EntityResolvingContentHandler(contentHandler);
        NonClosingInputStream ncis = null;
        try {
            SAXParserFactory factory = ArtifactoryXmlFactory.getFactory();
            SAXParser parser = factory.newSAXParser();
            ncis = new NonClosingInputStream(in);
            parser.parse(ncis, resolvingContentHandler);
        } catch (ParserConfigurationException e) {
            //Here ncis is always null
            throw new RepositoryException("SAX parser configuration error", e);
        } catch (Exception e) {
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
            log.warn("Failed to parse XML stream to import into '" + absPath + "'.", e);
        }
    }

    @SuppressWarnings({"unchecked"})
    public void commitWorkingCopy(long sleepBetweenFiles, TaskCallback callback) {
        File workingCopyDir = ArtifactoryHome.getWorkingCopyDir();
        try {
            taskService.pauseTasks(IndexerJob.class, false);
            Collection<File> files = FileUtils.listFiles(workingCopyDir,
                    FileFilterUtils.trueFileFilter(),
                    FileFilterUtils.trueFileFilter());
            if (files.size() == 0) {
                return;
            }
            log.info("Committing working copy ({} files)...", files.size());
            int count = 0;
            for (File file : files) {
                boolean stopped = taskService.blockIfPausedAndShouldBreak();
                if (stopped) {
                    return;
                }
                //if (!task.isCanceled()) { ...
                String workingCopyAbsPath = file.getAbsolutePath().substring(workingCopyDir.getAbsolutePath().length());
                JcrService service = InternalContextHelper.get().getJcrService();
                boolean committed = service.commitSingleFile(workingCopyAbsPath);
                if (committed) {
                    count++;
                }
                //Delete the file (import didn't necessarily find an unmaterialized stream)
                if (file.exists() && !file.canWrite()) {
                    log.error("Cannot delete imported file: " + file.getAbsolutePath() +
                            ". The user under which Artifactory is running does not have " +
                            "sufficient privileges or file does not exist anymore. Aborting " +
                            "import.");
                    return;
                }
                try {
                    Thread.sleep(sleepBetweenFiles);
                } catch (InterruptedException e) {
                    log.warn("Error in working copy commit: " + e.getMessage());
                    break;
                }
            }
            log.info("Working copy commit done (" + count + " files).");
        } finally {
            org.artifactory.util.FileUtils.cleanupEmptyDirectories(workingCopyDir);
            //Resume
            taskService.resumeTasks(IndexerJob.class);
        }
    }

    public boolean commitSingleFile(String workingCopyAbsPath) {
        RepoPath repoPath = JcrPath.get().getRepoPath(workingCopyAbsPath);
        LocalRepo localRepo = repositoryService.localOrCachedRepositoryByKey(repoPath.getRepoKey());
        if (localRepo == null) {
            log.error("Working copy path does not point to existing local repository " +
                    repoPath.getRepoKey());
            return false;
        }
        JcrFile jcrFile = localRepo.getLockedJcrFile(repoPath, false);
        if (jcrFile != null) {
            try {
                boolean deleted = jcrFile.extractWorkingCopyFile();
                if (deleted) {
                    log.debug("Committed wc file: " + workingCopyAbsPath + " was deleted.");
                } else {
                    log.debug("Committed wc file: " + workingCopyAbsPath + ".");
                }
                return true;
            } catch (Exception e) {
                log.error("Error during commit of single file " + workingCopyAbsPath, e);
            }
        }
        return false;
    }

    /**
     * Dumps the contents of the given node with its children to standard output. INTERNAL for debugging only.
     *
     * @param node the node to be dumped
     * @throws RepositoryException on repository errors
     */
    public static void dump(Node node) throws RepositoryException {
        System.out.println(node.getPath());
        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            System.out.print(property.getPath() + "=");
            if (property.getDefinition().isMultiple()) {
                Value[] values = property.getValues();
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) {
                        System.out.println(",");
                    }
                    System.out.println(values[i].getString());
                }
            } else {
                System.out.print(property.getString());
            }
            System.out.println();
        }
        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node child = nodes.nextNode();
            dump(child);
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

    @PostConstruct
    private void basicInit() throws Exception {
        // Don't call the parent the repository is not created yet
        cleanupAfterLastShutdown();
        System.setProperty("derby.stream.error.file",
                new File(ArtifactoryHome.getLogDir(), "derby.log").getAbsolutePath());

        //Copy the indexing config
        try {
            File indexDir = new File(ArtifactoryHome.getJcrRootDir(), "index");
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

        //Get the initializer strategy
        strategy = initStrategyFactory.getInitJcrRepoStrategy(this);

        //Create the repository
        final JackrabbitRepository repository = strategy.createJcrRepository(repoXml);
        sessionFactory.setRepository(repository);
        // Now that the repo is half done we can call super
        sessionFactory.afterPropertiesSet();

        InternalArtifactoryContext context = InternalContextHelper.get();
        JcrTransactionManager txMgr = context.beanForType(JcrTransactionManager.class);
        txMgr.setSessionFactory(sessionFactory);

        context.addReloadableBean(JcrService.class);
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
                log.debug("Cleanning up temp files in '" + tmpDirPath + "'.");
            }
            File tmpDir = new File(tmpDirPath);
            Collection<File> tmpfiles =
                    FileUtils.listFiles(tmpDir, new WildcardFileFilter("bin*.tmp"), null);
            for (File tmpfile : tmpfiles) {
                tmpfile.delete();
            }
        } else {
            log.warn("Not cleanning up any temp files: failed to determine temp dir.");
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
            throw new RepositoryRuntimeException(
                    "The repository '" + repoKey + "' is not configured.");
        }
        StatusHolder statusHolder = repositoryService.assertValidDeployPath(repo, relPath);
        if (statusHolder.isError()) {
            if (statusHolder.getException() != null) {
                throw new RepositoryRuntimeException(statusHolder.getStatusMsg(),
                        statusHolder.getException());
            }
            throw new RepositoryRuntimeException(statusHolder.getStatusMsg());
        }
    }

    public JcrFsItem getFsItem(Node node, LocalRepo repo) {
        String typeName;
        String nodePath = null;
        try {
            nodePath = node.getPath();
            typeName = node.getPrimaryNodeType().getName();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Failed to get the primary type for the node at '" + nodePath + "'.", e);
        }
        if (typeName.equals(JcrFolder.NT_ARTIFACTORY_FOLDER)) {
            return new JcrFolder(node, repo);
        } else if (typeName.equals(JcrFile.NT_ARTIFACTORY_FILE)) {
            return new JcrFile(node, repo);
        } else {
            return null;
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

    private QueryResult executeJcrQuery(String query, String queryType) throws RepositoryException {
        long start = System.currentTimeMillis();
        JcrSession session = getManagedSession();
        Workspace workspace = session.getWorkspace();
        QueryManager queryManager = workspace.getQueryManager();
        Query queryJar = queryManager.createQuery(query, queryType);
        QueryResult queyResult = queryJar.execute();
        log.debug("{} query execution time: {} ms",
                queryType, System.currentTimeMillis() - start);
        return queyResult;
    }

    /**
     * Returns the number of artifacts currently being served
     */
    public ArtifactCount getArtifactCount() throws RepositoryException {
        String count_query = "SELECT * FROM nt:file WHERE artifactory:name LIKE ";
        QueryResult resultJar = executeSqlQuery(count_query + "'%.jar'");
        QueryResult resultPom = executeSqlQuery(count_query + "'%.pom'");
        int jarCount = 0;
        int pomCount = 0;
        if (resultJar.getNodes().hasNext()) {
            jarCount += resultJar.getNodes().getSize();
        }
        if (resultPom.getNodes().hasNext()) {
            pomCount += resultPom.getNodes().getSize();
        }
        return new ArtifactCount(jarCount, pomCount);
    }

    public List<DeployableUnit> getDeployableUnitsUnder(RepoPath repoPath) throws RepositoryException {
        StringBuilder qb = new StringBuilder();
        qb.append("/jcr:root/repositories/");
        qb.append(repoPath.getRepoKey());
        if (PathUtils.hasText(repoPath.getPath())) {
            // the path might contain node names starting with numbers so we must escape
            // the path for the xpath query
            String relativePath = ISO9075.encodePath(repoPath.getPath());
            qb.append('/').append(relativePath);
        }
        qb.append("//element(*, artifactory:file) [jcr:like(@artifactory:name,'%.pom')]");

        QueryResult result = executeXpathQuery(qb.toString());
        ArrayList<DeployableUnit> deployableUnits = new ArrayList<DeployableUnit>();
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

    public void garbageCollect() {
        JcrSession session = getManagedSession();
        GarbageCollector gc = null;
        try {
            SessionImpl internalSession = (SessionImpl) session.getSession();
            gc = internalSession.createDataStoreGarbageCollector();
            if (gc.getDataStore() == null) {
                log.info("Datastore not yet initialize. Not running garbage collector...");
            }
            log.debug("Runnning Jackrabbit's datastore garbage collector...");
            gc.setSleepBetweenNodes(1000);
            gc.scan();
            gc.stopScan();
            int count = gc.deleteUnused();
            log.info("Jackrabbit's datastore garbage collector deleted " + count + " unreferenced item(s).");
        } catch (Exception e) {
            if (gc != null) {
                try {
                    gc.stopScan();
                } catch (RepositoryException re) {
                    log.debug("GC scanning could not be stopped.", re);
                }
            }
            throw new RuntimeException(
                    "Jackrabbit's datastore garbage collector execution failed.", e);
        }
    }
}