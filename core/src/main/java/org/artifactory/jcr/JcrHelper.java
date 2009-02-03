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
import org.apache.commons.io.filefilter.WildcardFilter;
import static org.apache.jackrabbit.JcrConstants.*;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.log4j.Logger;
import org.artifactory.ArtifactoryHome;
import org.artifactory.config.CentralConfig;
import org.artifactory.io.NonClosingInputStream;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.ChecksumInputStream;
import org.artifactory.io.checksum.ChecksumType;
import static org.artifactory.jcr.ArtifactoryJcrConstants.*;
import org.artifactory.jcr.xml.EntityResolvingContentHandler;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.security.AccessLogger;
import org.artifactory.security.SecurityHelper;
import org.artifactory.spring.ContextHelper;
import org.artifactory.utils.ExceptionUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import sun.net.www.MimeEntry;
import sun.net.www.MimeTable;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrHelper implements DisposableBean {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrHelper.class);

    private static final String ROOT_NODE_TYPE_NAME = "rep:root";

    private JackrabbitRepository repository;

    private static MimeTable mimeTable;
    private static SAXParserFactory factory;

    static {
        //Create the sax parser factory
        factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        try {
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", true);
            factory.setFeature("http://xml.org/sax/features/resolve-dtd-uris", false);
            factory.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
        } catch (Exception e) {
            throw new RuntimeException("SAX parser factory initialization error.", e);
        }
        //Update the system mime table
        mimeTable = MimeTable.getDefaultTable();
        MimeEntry mimeEntry = new MimeEntry("application/xml");
        mimeEntry.setExtensions(".xml,.pom");
        mimeTable.add(mimeEntry);
    }

    public JcrHelper() {
        cleanupAfterLastShutdown();
        System.setProperty("derby.stream.error.file", ArtifactoryHome.path() + "/logs/derby.log");
        String repoHome = CentralConfig.DATA_DIR + "/jcr";
        try {
            InputStream is = getClass().getResourceAsStream("/jcr/repo.xml");
            RepositoryConfig config = RepositoryConfig.create(is, repoHome);
            repository = RepositoryImpl.create(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to config jcr repo.", e);
        }
        //Register new node types
        doInSession(new JcrCallback<Node>() {
            @SuppressWarnings({"unchecked"})
            public Node doInJcr(JcrSessionWrapper session) throws RepositoryException {
                try {
                    Workspace workspace = session.getWorkspace();
                    //Register the artifactory ns if it is not registered already
                    NamespaceRegistry nsReg = workspace.getNamespaceRegistry();
                    List<String> nsPrefixes = Arrays.asList(nsReg.getPrefixes());
                    if (!nsPrefixes.contains("artifactory")) {
                        nsReg.registerNamespace("artifactory", "http://artifactory.jfrog.org/1.0");
                    }
                    InputStream is =
                            getClass().getResourceAsStream("/jcr/artifactory_nodetypes.xml");
                    NodeTypeDef[] types = NodeTypeReader.read(is);
                    //Get the NodeTypeManager from the Workspace.
                    //Note that it must be cast from the generic JCR NodeTypeManager to the
                    //Jackrabbit-specific implementation.
                    NodeTypeManagerImpl ntmgr =
                            (NodeTypeManagerImpl) workspace.getNodeTypeManager();
                    //Acquire the NodeTypeRegistry
                    NodeTypeRegistry ntReg = ntmgr.getNodeTypeRegistry();
                    //Create or update (reregister) all NodeTypeDefs
                    for (NodeTypeDef ntd : types) {
                        Name name = ntd.getName();
                        if (!ntReg.isRegistered(name)) {
                            ntReg.registerNodeType(ntd);
                        } else {
                            try {
                                ntReg.reregisterNodeType(ntd);
                            } catch (RepositoryException e) {
                                throw new RuntimeException("The underlying schema has changed. " +
                                        "Please start with a clean repository (delete the " +
                                        "$ARTIFACTORY_HOME/data/jcr folder) and import any of " +
                                        "your previously exported repositories to it.");
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RepositoryException("Failed to register custome node types.", e);
                }
                //Create the repositories root
                Node root = session.getRootNode();
                Node repositoriesNode = root.addNode(LocalRepo.REPO_ROOT, "nt:folder");
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Created the repositories root node: " +
                            repositoriesNode.getPath() + ".");
                }
                return repositoriesNode;
            }
        });
    }

    public void destroy() {
        repository.shutdown();
    }

    public JackrabbitRepository getRepository() {
        return repository;
    }

    public JcrSessionWrapper bindSession() {
        //If there is already an existing thread-bound session, use it
        JcrSessionWrapper existingSession = JcrSessionThreadBinder.getSession();
        if (existingSession != null) {
            return existingSession;
        } else {
            JcrSessionWrapper wrapper = newSession();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Created session: " + wrapper.getSession() + ".");
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Binding session: " + wrapper.getSession() + ".");
            }
            JcrSessionThreadBinder.bind(wrapper);
            return wrapper;
        }
    }

    public JcrSessionWrapper unbindSession(boolean save) {
        JcrSessionWrapper wrapper = JcrSessionThreadBinder.getSession();
        try {
            closeSession(wrapper, save);
        } catch (RepositoryException e) {
            LOGGER.warn("Failed to close jcr session.", e);
        } finally {
            JcrSessionThreadBinder.unbind();
        }
        return wrapper;
    }

    public <T> T doInSession(JcrCallback<T> callback) {
        T result;
        JcrSessionWrapper wrapper;
        wrapper = bindSession();
        try {
            result = callback.doInJcr(wrapper);
            return result;
        } catch (Exception e) {
            wrapper.setRollbackOnly();
            throw new RuntimeException("Failed to execute JcrCallback.", e);
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public Node createPath(Node parentNode, final String relPath, final String repoKey)
            throws RepositoryException {
        StringTokenizer tokenizer = new StringTokenizer(relPath, "/");
        while (tokenizer.hasMoreElements()) {
            String dirName = tokenizer.nextToken();
            parentNode = getOrCreateFolderNode(parentNode, dirName, repoKey);
        }
        return parentNode;
    }

    @SuppressWarnings({"unchecked"})
    public boolean itemNodeExists(final String absPath) {
        return doInSession(new JcrCallback<Boolean>() {
            public Boolean doInJcr(JcrSessionWrapper session) throws RepositoryException {
                //Sanity check to avoid exception in "session.itemExists()"
                if (StringUtils.isEmpty(absPath) || !absPath.startsWith("/")) {
                    return false;
                }
                boolean exists;
                try {
                    exists = session.itemExists(absPath);
                } catch (RepositoryException e) {
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
        });
    }

    public static boolean isFileNode(Node node) throws RepositoryException {
        NodeType primaryNodeType = node.getPrimaryNodeType();
        return NT_ARTIFACTORY_FILE.equals(primaryNodeType.getName());
    }

    @SuppressWarnings({"OverlyComplexMethod"})
    public static Node importStream(
            Node parentNode, String name, String repoKey, long lastModifiedTime, InputStream in,
            boolean createChecksum)
            throws RepositoryException, IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Importing '" + parentNode.getPath() + "/" + name + "'.");
        }
        //First check that the repository approves the deployment
        String targetPath = parentNode.getPath() + "/" + name;
        int relPathStart = targetPath.indexOf(repoKey + "/");
        String relPath = targetPath.substring(relPathStart + repoKey.length() + 1);
        CentralConfig cc = ContextHelper.get().getCentralConfig();
        VirtualRepo virtualRepo = cc.getGlobalVirtualRepo();
        LocalRepo repo = virtualRepo.localOrCachedRepositoryByKey(repoKey);
        if (repo == null) {
            throw new RepositoryException(
                    "The repository '" + repoKey + "' is not configured." +
                            "Some artifacts might have been incorrectly imported - please remove " +
                            "them manually.");
        }
        if (!repo.handles(relPath)) {
            throw new RepositoryException(
                    "The repository '" + repoKey + "' rejected the artifact '" + relPath +
                            "' due to its snapshot/release handling policy. Some artifacts might " +
                            "have been incorrectly imported - please remove them manually.");
        }
        if (!repo.accepts(relPath)) {
            throw new RepositoryException(
                    "The repository '" + repoKey + "' rejected the artifact '" + relPath +
                            "' due to its include/exclude patterns settings. Some artifacts " +
                            "might have been incorrectly imported - please remove them manually.");
        }
        //Create a temp folder node and add the file there
        Session session = parentNode.getSession();
        Node rootNode = session.getRootNode();
        Node tempParentNode =
                rootNode.addNode(name + "_" + System.currentTimeMillis(), "nt:folder");
        Node fileNode = tempParentNode.addNode(name, NT_ARTIFACTORY_FILE);
        fileNode.setProperty(PROP_ARTIFACTORY_NAME, name);
        fileNode.setProperty(PROP_ARTIFACTORY_REPO_KEY, repoKey);
        Calendar lastUpdated = Calendar.getInstance();
        fileNode.setProperty(PROP_ARTIFACTORY_LAST_UPDATED, lastUpdated);
        String userId = SecurityHelper.getUsername();
        fileNode.setProperty(PROP_ARTIFACTORY_MODIFIED_BY, userId);
        //Check whether to create the file as xml (w. xml-aware imported data) or regular file
        boolean xml = MavenUtils.isXml(name);
        if (xml) {
            fileNode.addMixin(MIX_ARTIFACTORY_XML_AWARE);
        }
        Node resNode = fileNode.addNode(JCR_CONTENT, NT_RESOURCE);
        String mimeType = mimeTable.getContentTypeFor(name);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        resNode.setProperty(JCR_MIMETYPE, mimeType);
        //Default encoding
        resNode.setProperty(JCR_ENCODING, "utf-8");
        try {
            //If it is an xml document import its native xml content into the repo
            if (xml) {
                in.mark(Integer.MAX_VALUE);
                //If it is a pom, verify that its groupId/artifactId/version match the dest path
                if (MavenUtils.isPom(name)) {
                    MavenUtils.validatePomTargetPath(in, relPath);
                    in.reset();
                }
                //Reset the stream
                //Temporarily disable xml import, since in JR 1.4 the stream is closed after import
                Node xmlNode = fileNode.addNode(ARTIFACTORY_XML, NT_ARTIFACTORY_XML_CONTENT);
                importXml(xmlNode, in);
                in.reset();
                //session.importXML(absPath, in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            }
            //Check if needs to create checksum and not checksum file
            boolean calcChecksum = createChecksum && !MavenUtils.isChecksum(name);
            InputStream resourceInputStream;
            if (calcChecksum) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Calculating checksum for '" + name + "' (createChecksum=" +
                            createChecksum + ").");
                }
                resourceInputStream = new ChecksumInputStream(in,
                        new Checksum(name, ChecksumType.md5),
                        new Checksum(name, ChecksumType.sha1));
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Not calculating checksum for '" + name + "' (createChecksum=" +
                            createChecksum + ").");
                }
                resourceInputStream = in;
            }
            //Do this after xml import since Jackrabbit 1.4
            //org.apache.jackrabbit.core.value.BLOBInTempFile.BLOBInTempFile will close the stream
            resNode.setProperty(JCR_DATA, resourceInputStream);
            Calendar lastModified = Calendar.getInstance();
            lastModified.setTimeInMillis(lastModifiedTime);
            resNode.setProperty(JCR_LASTMODIFIED, lastModified);
            if (calcChecksum) {
                Checksum[] checksums = ((ChecksumInputStream) resourceInputStream).getChecksums();
                for (Checksum checksum : checksums) {
                    String checksumFileName = checksum.getFileName();
                    InputStream checksumInputStream = checksum.asInputStream();
                    //Save the checksum
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Saving checksum for '" + name + "' (checksum=" +
                                checksum.getChecksum() + ").");
                    }
                    importStream(parentNode, checksumFileName, repoKey, lastModifiedTime,
                            checksumInputStream, false);
                }
            }
            //Lock both the parent and the grandparent nodes in order to avoid a situation of
            //pending changes on the parent node (we locked a child folder first in the same
            //transaction). For example, the version metadata.xml is deployed (by the deployer)
            //after the artifact metadata and the after the artifacts. We cannot unlock the children
            //(required for locking the parent - no pending changes on children) without saving them
            //first which will compromise the 'atomicity' of the transaction.
            //Do not go up with locking more than 2 levels in order not to fail on pending changes
            //(grandparent node was already locked so you cannot lock the grand-grandparent).
            //We assume we are not importing files under the root node (or getParent() will fail).
            //-
            //For an artifact deploy this locks the artifactId and last group suffix folders.
            //We need to be watching http://issues.apache.org/jira/browse/JCR-314, since this is a
            //pretty coarse locking scope.
            //See also the discussion at: http://www.nabble.com/Jackrabbit-Performance-Tuning---Large-%22Transaction%22---Concurrent-Access-to-Repository-t3095196.html)
            Node grandParentNode = parentNode.getParent();
            if (!NodeLock.isLockedByCurrentSession(parentNode)) {
                NodeLock.lock(grandParentNode);
                NodeLock.lock(parentNode);
            }
            //Remove any existing node with the same file name
            if (parentNode.hasNode(name)) {
                Node oldFileNode = parentNode.getNode(name);
                oldFileNode.remove();
            }
            //Move the imported file from the temp parent to the real parent node
            session.move(fileNode.getPath(), targetPath);
            //Delete the temp parent node
            tempParentNode.remove();
        } catch (RepositoryException re) {
            //Rollback on excption
            JcrSessionThreadBinder.getSession().setRollbackOnly();
            throw re;
        } catch (IOException ioe) {
            //Rollback on excption
            JcrSessionThreadBinder.getSession().setRollbackOnly();
            throw ioe;
        } finally {
            IOUtil.close(in);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Imported '" + fileNode.getPath() + "'.");
        }
        JcrFile jcrFile = new JcrFile(fileNode);
        AccessLogger.deployed(jcrFile.getRepoPath());
        return fileNode;
    }

    /**
     * Creates a folder with no locking
     *
     * @param parentNode
     * @param folderName
     * @param repoKey
     * @return
     * @throws RepositoryException
     */
    public static Node createFolder(Node parentNode, String folderName, String repoKey)
            throws RepositoryException {
        Node folderNode;
        folderNode = parentNode.addNode(folderName, NT_ARTIFACTORY_FOLDER);
        folderNode.setProperty(PROP_ARTIFACTORY_REPO_KEY, repoKey);
        String userId = SecurityHelper.getUsername();
        folderNode.setProperty(PROP_ARTIFACTORY_MODIFIED_BY, userId);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Created folder node: " + folderNode.getPath() + ".");
        }
        return folderNode;
    }

    public static void rename(Node node, String newName) throws RepositoryException {
        node.getSession().move(node.getPath(), node.getParent().getPath() + "/" + newName);
    }

    public static Node getOrCreateFolderNode(Node parentNode, String folderName, String repoKey)
            throws RepositoryException {
        Node childNode;
        if (parentNode.hasNode(folderName)) {
            //Do not lock to check if the node exists. Eventhough checking existense with no locking
            //compromises atomicity, it results in a too-wide lock scope. Since JCR serializes
            //readers (JCR-314) and since multi-module projects upload artifacts concurrently onto
            //adjacent paths, this would cause a lock failure. Here we risk getting an item does not
            //exist exception from time to time.
            //http://www.nabble.com/Jackrabbit-Performance-Tuning---Large-%22Transaction%22---Concurrent-Access-to-Repository-tf3095196.html#a8647811
            childNode = parentNode.getNode(folderName);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Folder node exists (no parent locking): " + childNode.getPath() + ".");
            }
        } else {
            //We check node existence again, this time with locking, just before creation, since
            //it is much more probable that the node (path) has already been created by another
            //deployer request
            boolean rootNode = parentNode.getPrimaryNodeType().isNodeType(ROOT_NODE_TYPE_NAME);
            //Do not lock the root node - it is not lockable
            if (!rootNode) {
                NodeLock.lock(parentNode);
            }
            if (parentNode.hasNode(folderName)) {
                childNode = parentNode.getNode(folderName);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Folder node exists (with parent locking): " +
                            childNode.getPath() + ".");
                }
            } else {
                try {
                    childNode = createFolder(parentNode, folderName, repoKey);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Folder node created: " + childNode.getPath() + ".");
                    }
                } catch (ItemExistsException e) {
                    //Sanity check
                    throw new RepositoryException("Folder node exists eventough it was " +
                            "determined as non-exitent with parent locking!", e);
                }
            }
            JcrFolder jcrFolder = new JcrFolder(childNode);
            AccessLogger.deployed(jcrFolder.getRepoPath());
        }
        return childNode;
    }

    /**
     * Dumps the contents of the given node with its children to standard output. INTERNAL for
     * debugging only.
     *
     * @param node the node to be dumped
     * @throws RepositoryException on repository errors
     */
    public static void dump(Node node) throws RepositoryException {
        NodeLock.lock(node);
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

    //TODO: [by yl] Use a session pool (commons-pool)
    @SuppressWarnings({"UnnecessaryLocalVariable"})
    protected JcrSessionWrapper newSession() {
        try {
            Session session = repository.login();
            JcrSessionWrapper sessionWrapper = new JcrSessionWrapper(session);
            return sessionWrapper;
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to create jcr session.", e);
        }
    }

    protected void closeSession(JcrSessionWrapper session, boolean save)
            throws RepositoryException {
        if (session == null || !session.isLive()) {
            return;
        }
        try {
            if (save) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Saving session: " + session.getSession() + ".");
                }
                session.save();
            }
        } finally {
            //Extremely important to call this so that all sesion-scoped node locks are cleaned!
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unbinding session: " + session.getSession() + ".");
            }
            session.logout();
        }
    }

    /**
     * Import xml with characters entity resolving
     *
     * @param xmlNode
     * @param in
     * @throws RepositoryException
     * @throws IOException
     */
    private static void importXml(Node xmlNode, InputStream in)
            throws RepositoryException, IOException {
        JcrSessionWrapper session = JcrSessionThreadBinder.getSession();
        String absPath = xmlNode.getPath();
        ContentHandler contentHandler =
                session.getImportContentHandler(absPath, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        EntityResolvingContentHandler resolvingContentHandler =
                new EntityResolvingContentHandler(contentHandler);
        NonClosingInputStream ncis = null;
        try {
            SAXParser parser = factory.newSAXParser();
            ncis = new NonClosingInputStream(in);
            parser.parse(ncis, resolvingContentHandler);
        } catch (SAXException se) {
            //Check for wrapped repository exception
            Exception e = se.getException();
            if (e != null && e instanceof RepositoryException) {
                if (ncis != null) {
                    ncis.forceClose();
                }
                throw (RepositoryException) e;
            } else {
                LOGGER.warn("Failed to parse XML stream to import into '" + absPath + "'.", e);
            }
        } catch (ParserConfigurationException e) {
            if (ncis != null) {
                ncis.forceClose();
            }
            throw new RepositoryException("SAX parser configuration error", e);
        }
    }

    @SuppressWarnings({"unchecked"})
    private static void cleanupAfterLastShutdown() {
        //Cleanup any left over bin*.tmp files from the temp directory, due to unclean shutdown
        String tmpDirPath;
        try {
            File tmp = File.createTempFile("bin", ".tmp");
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
                    FileUtils.listFiles(tmpDir, new WildcardFilter("bin*.tmp"), null);
            for (File tmpfile : tmpfiles) {
                tmpfile.delete();
            }
        } else {
            LOGGER.warn("Not cleanning up any temp files: failed to determine temp dir.");
        }
    }
}
