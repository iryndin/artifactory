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
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.log4j.Logger;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.config.CentralConfigServiceImpl;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.md.MetadataAware;
import org.artifactory.jcr.md.MetadataService;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.index.IndexerManagerImpl;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.schedule.ArtifactoryTimerTask;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.PostInitializingBean;
import org.artifactory.utils.ExceptionUtils;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springmodules.jcr.SessionFactoryUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Spring based session factory for tx jcr sessions
 *
 * @author yoavl
 */
@Repository
public class JcrServiceImpl implements JcrService {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrServiceImpl.class);

    public final String[] OCM_CLASSES = {
            "org.artifactory.jcr.ocm.OcmStorable",
            "org.artifactory.keyval.KeyVals",
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
    private IndexerManagerImpl indexerManager;

    private Mapper ocmMapper;
    private InitJcrRepoStrategy strategy;

    //Don't inject or CGLib will crash the key for Tx resorce
    private JcrSessionFactory sessionFactory = new JcrSessionFactory();

    @Transactional
    public javax.jcr.Repository getRepository() {
        return getManagedSession().getRepository();
    }

    public JcrSession getManagedSession() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new RepositoryRuntimeException("Trying to get a session outside a Tx scope");
        }
        JcrSession session;
        try {
            session = (JcrSession) SessionFactoryUtils.doGetSession(sessionFactory, true);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        LOGGER.debug("Returning session " + session);
        return session;
    }

    void setOcmMapper(Mapper ocmMapper) {
        this.ocmMapper = ocmMapper;
    }

    @Transactional
    public void init() {
        //Initialize the created jcr repository
        strategy.init();
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends PostInitializingBean>[] initAfter() {
        return new Class[]{CentralConfigServiceImpl.class};
    }

    @PreDestroy
    public void destroy() {
        try {
            sessionFactory.destroy();
        } catch (Exception e) {
            LOGGER.warn("Failed to destroy the jcr session factory.", e);
        }
    }

    public ObjectContentManager getOcm() {
        if (ocmMapper == null) {
            return null;
        }
        JcrSession session = getManagedSession();
        return new ObjectContentManagerImpl(session, ocmMapper);
    }

    @Transactional
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
            Throwable cause =
                    ExceptionUtils.getCauseOfTypes(e, MalformedPathException.class);
            if (cause != null) {
                return false;
            } else {
                throw e;
            }
        }
        return exists;
    }

    /**
     * Breadth-first folder import walking
     *
     * @param foldersToScan
     * @param settings
     * @param status
     */
    public void importFolders(LinkedList<JcrFolder> foldersToScan, ImportSettings settings,
            StatusHolder status) {
        JcrFolder jcrFolder;
        while ((jcrFolder = foldersToScan.poll()) != null) {
            JcrService transactionalMe = InternalContextHelper.get().getJcrService();
            //Import the folder (shallow) in a new tx
            transactionalMe.importFolder(jcrFolder, settings, status);
            jcrFolder.importChildren(settings, status, foldersToScan);
        }
        //No more folders to walk
    }

    /**
     * "Shallow" folder import in a new tx
     *
     * @param jcrFolder
     * @param settings
     * @param status
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void importFolder(JcrFolder jcrFolder, ImportSettings settings, StatusHolder status) {
        jcrFolder.importFrom(settings, status);
    }

    @Transactional
    public void importFile(
            JcrFolder parentFolder, File file, ImportSettings settings, StatusHolder status) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RepositoryRuntimeException(
                    "Imported file could not be found: " + file.getAbsolutePath(), e);
        }
        BufferedInputStream bis = new BufferedInputStream(fis);
        JcrFile jcrFile;
        try {
            jcrFile = importStream(parentFolder, file.getName(), file.lastModified(), bis);
        } finally {
            IOUtils.closeQuietly(bis);
        }
        //Update the metadata
        jcrFile.importFrom(settings, status);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void importFileViaWorkingCopy(
            JcrFolder parentFolder, File file, ImportSettings settings, StatusHolder status) {
        String name = file.getName();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Importing '" + parentFolder.getPath() + "/" + name + "'.");
        }
        assertValidDeployment(parentFolder, name);
        JcrFile jcrFile;
        try {
            jcrFile = new JcrFile(parentFolder, file, settings, status);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Imported '" + parentFolder.getPath() + "/" + name + "'.");
            }
            AccessLogger.deployed(jcrFile.getRepoPath());
        } catch (Exception e) {
            throw new RepositoryRuntimeException(
                    "Failed to import file '" + file.getAbsolutePath() + "'.", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean delete(String absPath) {
        JcrSession session = getManagedSession();
        if (!session.itemExists(absPath)) {
            return false;
        }
        Node node = (Node) session.getItem(absPath);
        try {
            node.remove();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to remove node.", e);
        }
        return true;
    }

    @Transactional
    public List<String> getChildrenNames(String absPath) {
        JcrSession session = getManagedSession();
        if (!session.itemExists(absPath)) {
            throw new RepositoryRuntimeException(
                    "Tried to get children of a non exiting node '" + absPath + "'.");
        }
        Node node = (Node) session.getItem(absPath);
        List<String> names = new ArrayList<String>();
        try {
            node.getNodes();
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

    // TODO: Should not be transactional. You cannot do anything with JcrFile without a Tx!
    @Transactional
    public JcrFile getJcrFile(LocalRepo repo, String relPath) throws FileExpectedException {
        String absPath = repo.getRepoRootPath() + "/" + relPath;
        mdService.getInfoFromCache(absPath, FileInfo.class);
        if (itemNodeExists(absPath)) {
            JcrFsItem item = getFsItem(repo.getRepoRootPath(), relPath);
            if (item.isFile()) {
                return (JcrFile) item;
            } else {
                RepoPath repoPath = new RepoPath(repo.getKey(), relPath);
                throw new FileExpectedException(repoPath);
            }
        }
        return null;
    }

    @Transactional
    public FileInfo getFileInfo(LocalRepo repo, String path) {
        String absPath = repo.getRepoRootPath() + "/" + path;
        FileInfo info = mdService.getInfoFromCache(absPath, FileInfo.class);
        if (info != null) {
            return info;
        }
        if (itemNodeExists(absPath)) {
            JcrFsItem item = getFsItem(repo.getRepoRootPath(), path);
            if (item.isFile()) {
                FileInfo fileInfo = ((JcrFile) item).getInfo();
                //Sanity check - should never hapen
                if (fileInfo.getRelPath() == null) {
                    LOGGER.warn("FileInfo assertion violated.");
                    return null;
                }
                return fileInfo;
            } else {
                RepoPath repoPath = new RepoPath(repo.getKey(), path);
                throw new FileExpectedException(repoPath);
            }
        }
        return null;
    }

    @Transactional
    public FileInfo getLockedFileInfo(LocalRepo repo, String path) {
        JcrFile jcrFile = getJcrFile(repo, path);
        return jcrFile != null ? jcrFile.getLockedInfo() : null;
    }

    @Transactional
    public JcrFsItem getFsItem(String repoRootPath, String relPath) {
        JcrSession session = getManagedSession();
        Node repoNode = (Node) session.getItem(repoRootPath);
        Node node;
        if (relPath.length() > 0) {
            try {
                node = repoNode.getNode(relPath);
            } catch (RepositoryException e) {
                throw new RepositoryRuntimeException("Failed to get node '" + relPath +
                        "' in repo '" + repoRootPath + "'.", e);
            }
        } else {
            node = repoNode;
        }
        String typeName;
        try {
            typeName = node.getPrimaryNodeType().getName();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Failed to get the primary type for node '" + relPath +
                            "' in repo '" + repoRootPath + "'.", e);
        }
        if (typeName.equals(JcrFolder.NT_ARTIFACTORY_FOLDER)) {
            return new JcrFolder(node);
        } else if (typeName.equals(JcrFile.NT_ARTIFACTORY_FILE)) {
            return new JcrFile(node);
        } else {
            throw new RuntimeException("Did not find a file system item for node '" + relPath +
                    "' in repo '" + repoRootPath + "'.");
        }
    }

    @Transactional
    public List<JcrFsItem> getChildren(JcrFolder folder) {
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
                String typeName = node.getPrimaryNodeType().getName();
                if (typeName.equals(JcrFolder.NT_ARTIFACTORY_FOLDER)) {
                    items.add(new JcrFolder(node));
                } else if (typeName.equals(JcrFile.NT_ARTIFACTORY_FILE)) {
                    items.add(new JcrFile(node));
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to retrieve folder node items.", e);
        }
        return items;
    }

    @Transactional
    public JcrFile importStream(
            JcrFolder parentFolder, String name, long lastModified, InputStream in) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Importing '" + parentFolder.getPath() + "/" + name + "'.");
        }
        assertValidDeployment(parentFolder, name);
        JcrFile file = new JcrFile(parentFolder, name, lastModified, in);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Imported '" + parentFolder.getPath() + "/" + name + "'.");
        }
        AccessLogger.deployed(file.getRepoPath());
        return file;
    }

    public Node getOrCreateUnstructuredNode(String name) {
        JcrSession session = getManagedSession();
        Node root = session.getRootNode();
        return getOrCreateUnstructuredNode(root, name);
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
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Created node: " + name + ".");
                }
                return node;
            } else {
                return parent.getNode(cleanPath);
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @SuppressWarnings({"unchecked"})
    public void commitWorkingCopy(long sleepBetweenFiles, ArtifactoryTimerTask task) {
        LOGGER.info("Commiting working copy...");
        //Pause the scheduler
        indexerManager.unschedule();
        try {
            File workingCopyDir = ArtifactoryHome.getWorkingCopyDir();
            Collection<File> files = FileUtils.listFiles(workingCopyDir,
                    FileFilterUtils.trueFileFilter(),
                    FileFilterUtils.trueFileFilter());
            if (files.size() == 0) {
                //Remove any left over folders and return
                try {
                    FileUtils.cleanDirectory(workingCopyDir);
                } catch (IOException e) {
                    LOGGER.warn("Could not clean up the working copy directory: " + e.getMessage());
                }
            }
            int count = 0;
            for (File file : files) {
                //if (!task.isCanceled()) { ...
                String workingCopyAbsPath =
                        file.getAbsolutePath().substring(workingCopyDir.getAbsolutePath().length());
                JcrService service = InternalContextHelper.get().getJcrService();
                boolean committed = service.commitSingleFile(workingCopyAbsPath);
                if (committed) {
                    count++;
                }
                //Delete the file (import didn't necessarily find an unmaterialized stream)
                if (file.exists() && !file.canWrite()) {
                    LOGGER.error("Cannot delete imported file: " + file.getAbsolutePath() +
                            ". The user under which Artifactory is running does not have " +
                            "sufficient privileges or file does not exist anymore. Aborting " +
                            "import.");
                    return;
                }
                try {
                    Thread.sleep(sleepBetweenFiles);
                } catch (InterruptedException e) {
                    LOGGER.warn("Error in working copy commit: " + e.getMessage());
                }
            }
            LOGGER.info("Working copy commit done (" + count + " files).");
        } finally {
            //Reschedule
            indexerManager.init();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean commitSingleFile(String workingCopyAbsPath) {
        JcrFile jcrFile = new JcrFile(workingCopyAbsPath);
        if (jcrFile.exists()) {
            //Trying to get the stream will initiate materialization from wc (commit)
            InputStream is = null;
            try {
                is = jcrFile.getStream();
                LOGGER.info("Committed wc file: " + workingCopyAbsPath + ".");
                return true;
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
        return false;
    }

    /**
     * Dumps the contents of the given node with its children to standard output. INTERNAL for
     * debugging only.
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

        context.addPostInit(JcrService.class);
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
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Cleanning up temp files in '" + tmpDirPath + "'.");
            }
            File tmpDir = new File(tmpDirPath);
            Collection<File> tmpfiles =
                    FileUtils.listFiles(tmpDir, new WildcardFileFilter("bin*.tmp"), null);
            for (File tmpfile : tmpfiles) {
                tmpfile.delete();
            }
        } else {
            LOGGER.warn("Not cleanning up any temp files: failed to determine temp dir.");
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
            throw new RepositoryRuntimeException(statusHolder.getStatusMsg());
        }
    }
}