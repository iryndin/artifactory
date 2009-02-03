package org.artifactory.jcr;

import static org.apache.jackrabbit.JcrConstants.*;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.name.QName;
import org.apache.log4j.Logger;
import org.artifactory.ArtifactoryHome;
import static org.artifactory.jcr.ArtifactoryJcrConstants.*;
import org.artifactory.jcr.xml.EntityResolvingContentHandler;
import org.artifactory.repo.CentralConfig;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import sun.net.www.MimeTable;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrHelper implements DisposableBean {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrHelper.class);

    private JackrabbitRepository repository;

    public JcrHelper() {
        System.setProperty("derby.stream.error.file",
                ArtifactoryHome.path() + "/logs/derby.log");
        String repoHome = CentralConfig.LOCAL_REPOS_DIR + "/jcr";
        try {
            InputStream is = getClass().getResourceAsStream("/jcr/repo.xml");
            RepositoryConfig config = RepositoryConfig.create(is, repoHome);
            repository = RepositoryImpl.create(config);
        } catch (Exception e) {
            throw new RuntimeException("Faild to config jcr repo.", e);
        }
        //Register new node types
        doInSession(new JcrCallback() {
            @SuppressWarnings({"unchecked"})
            public Object doInJcr(JcrSessionWrapper session) throws RepositoryException {
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
                    //Register the NodeTypeDefs if they are not registered already
                    for (NodeTypeDef ntd : types) {
                        QName qname = ntd.getName();
                        if (!ntReg.isRegistered(qname)) {
                            ntReg.registerNodeType(ntd);
                        }
                    }
                } catch (Exception e) {
                    throw new RepositoryException("Failed to register custome node types.", e);
                }
                return null;
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

    public Object doInSession(JcrCallback callback) {
        Object result;
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
            parentNode = getOrCreateChildNode(parentNode, dirName, repoKey);
        }
        return parentNode;
    }

    public boolean fileNodeExists(final String absPath) {
        return (Boolean) doInSession(new JcrCallback<Boolean>() {
            public Boolean doInJcr(JcrSessionWrapper session) throws RepositoryException {
                //Sanity check to avoid exception in "session.itemExists()"
                if (StringUtils.isEmpty(absPath) || !absPath.startsWith("/")) {
                    return false;
                }
                boolean exists = session.itemExists(absPath);
                if (exists) {
                    Node node = (Node) session.getItem(absPath);
                    if (isFileNode(node)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public static boolean isFileNode(Node node) throws RepositoryException {
        NodeType primaryNodeType = node.getPrimaryNodeType();
        return NT_ARTIFACTORY_FILE.equals(primaryNodeType.getName());
    }

    /**
     * Imports a File.
     *
     * @param parentNode Parent Repository Node
     * @param file       File to be imported
     * @param repoKey
     * @throws RepositoryException on repository errors
     * @throws java.io.IOException on io errors
     */
    public static void importFile(String repoKey, Node parentNode, File file)
            throws RepositoryException, IOException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        importStream(parentNode, file.getName(), repoKey, file.lastModified(), bis);
    }

    public static void importStream(
            Node parentNode, String name, String repoKey, long lastModifiedTime,
            BufferedInputStream in)
            throws RepositoryException, IOException {
        MimeTable mt = MimeTable.getDefaultTable();
        String mimeType = mt.getContentTypeFor(name);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        //Remove any existing node with the same file name
        Node fileNode;
        if (parentNode.hasNode(name)) {
            fileNode = parentNode.getNode(name);
            fileNode.remove();
        }
        boolean xml = isXml(name);
        fileNode = parentNode.addNode(name, NT_ARTIFACTORY_FILE);
        fileNode.setProperty(PROP_ARTIFACTORY_NAME, name);
        fileNode.setProperty(PROP_ARTIFACTORY_REPO_KEY, repoKey);
        Calendar lastUpdated = Calendar.getInstance();
        fileNode.setProperty(PROP_ARTIFACTORY_LAST_UPDATED, lastUpdated);
        String userId = parentNode.getSession().getUserID();
        fileNode.setProperty(PROP_ARTIFACTORY_MODIFIED_BY, userId);
        //Check whether to create the file as xml (w. xml-aware imported data) or regular file
        if (xml) {
            fileNode.addMixin(MIX_ARTIFACTORY_XML_AWARE);
        }
        Node resNode = fileNode.addNode(JCR_CONTENT, NT_RESOURCE);
        resNode.setProperty(JCR_MIMETYPE, mimeType);
        resNode.setProperty(JCR_ENCODING, "");
        try {
            if (xml) {
                in.mark(Integer.MAX_VALUE);
            }
            resNode.setProperty(JCR_DATA, in);
            Calendar lastModified = Calendar.getInstance();
            lastModified.setTimeInMillis(lastModifiedTime);
            resNode.setProperty(JCR_LASTMODIFIED, lastModified);
            //If it is an xml document import its native xml content into the repo
            if (xml) {
                Node xmlNode = fileNode.addNode(ARTIFACTORY_XML, NT_ARTIFACTORY_XML_CONTENT);
                Session session = xmlNode.getSession();
                //Reset the stream
                in.reset();
                importXml(xmlNode, in, session);
                //session.importXML(absPath, in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            }
        } finally {
            IOUtil.close(in);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Imported " + fileNode.getPath());
        }
    }

    /**
     * Import a Folder.
     *
     * @param parentNode Parent Repository Node
     * @param directory  Directory to be traversed
     * @param repoKey
     * @throws RepositoryException on repository errors
     * @throws java.io.IOException on io errors
     */
    public static void importFolder(
            String repoKey, Node parentNode, File directory) throws RepositoryException,
            IOException {
        File[] dirEntries = directory.listFiles();
        for (File dirEntry : dirEntries) {
            String fileName = dirEntry.getName();
            if (dirEntry.isDirectory()) {
                Node childNode = getOrCreateChildNode(parentNode, fileName, repoKey);
                LOGGER.info("Importing folder '" + fileName + "' into '" + repoKey + "'...");
                importFolder(repoKey, childNode, dirEntry);
            } else {
                LOGGER.info("Importing file '" + fileName + "' into '" + repoKey + "'...");
                importFile(repoKey, parentNode, dirEntry);
            }
        }
    }

    public static Node createFolder(Node parentNode, String folderName, String repoKey)
            throws RepositoryException {
        Node folderNode = parentNode.addNode(folderName, NT_ARTIFACTORY_FOLDER);
        folderNode.setProperty(PROP_ARTIFACTORY_REPO_KEY, repoKey);
        String userId = parentNode.getSession().getUserID();
        folderNode.setProperty(PROP_ARTIFACTORY_MODIFIED_BY, userId);
        return folderNode;
    }

    public static void rename(Node node, String newName) throws RepositoryException {
        node.getSession().move(node.getPath(), node.getParent().getPath() + "/" + newName);
    }

    public static Node getOrCreateChildNode(Node parentNode, String folderName, String repoKey)
            throws RepositoryException {
        Node childNode;
        if (parentNode.hasNode(folderName)) {
            childNode = parentNode.getNode(folderName);
        } else {
            childNode = createFolder(parentNode, folderName, repoKey);
        }
        return childNode;
    }
    
    /**
     * Dumps the contents of the given node to standard output.
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

    protected void closeSession(JcrSessionWrapper wrapper, boolean save)
            throws RepositoryException {
        Session session = wrapper.getSession();
        if (session == null || !session.isLive()) {
            return;
        }
        try {
            if (save && !wrapper.isRollbackOnly() && session.hasPendingChanges()) {
                session.save();
            }
        } finally {
            session.logout();
        }
    }

    /**
     * Import xml with characters entity resolving
     *
     * @param xmlNode
     * @param in
     * @param session
     * @throws RepositoryException
     * @throws IOException
     */
    private static void importXml(Node xmlNode, BufferedInputStream in, Session session)
            throws RepositoryException, IOException {
        String absPath = xmlNode.getPath();
        ContentHandler contentHandler = session.getImportContentHandler(absPath,
                ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        EntityResolvingContentHandler resolvingContentHandler =
                new EntityResolvingContentHandler(contentHandler);
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
            factory.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
            SAXParser parser = factory.newSAXParser();
            parser.parse(in, resolvingContentHandler);
        } catch (SAXException se) {
            // check for wrapped repository exception
            Exception e = se.getException();
            if (e != null && e instanceof RepositoryException) {
                throw (RepositoryException) e;
            } else {
                String msg = "failed to parse XML stream";
                throw new InvalidSerializedDataException(msg, se);
            }
        } catch (ParserConfigurationException e) {
            throw new RepositoryException("SAX parser configuration error", e);
        }
    }

    private static boolean isXml(String name) {
        return name.endsWith(".xml") || name.endsWith(".XML") ||
                name.endsWith(".pom") || name.endsWith(".POM");
    }
}
