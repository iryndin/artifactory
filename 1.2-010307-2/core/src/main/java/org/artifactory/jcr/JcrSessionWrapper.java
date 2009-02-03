package org.artifactory.jcr;

import org.apache.log4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrSessionWrapper implements Session {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrSessionWrapper.class);

    private boolean rollbackOnly;
    private final Session session;


    public JcrSessionWrapper(Session session) {
        this.session = session;
        rollbackOnly = false;
    }

    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    public boolean isRollbackOnly() {
        return rollbackOnly;
    }

    public Session getSession() {
        return session;
    }

    public Repository getRepository() {
        return session.getRepository();
    }

    public String getUserID() {
        return session.getUserID();
    }

    public Object getAttribute(String name) {
        return session.getAttribute(name);
    }

    public String[] getAttributeNames() {
        return session.getAttributeNames();
    }

    public Workspace getWorkspace() {
        return session.getWorkspace();
    }

    public Session impersonate(Credentials credentials)
            throws LoginException, RepositoryException {
        return session.impersonate(credentials);
    }

    public Node getRootNode() throws RepositoryException {
        return session.getRootNode();
    }

    public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException {
        return session.getNodeByUUID(uuid);
    }

    public Item getItem(String absPath) throws PathNotFoundException, RepositoryException {
        return session.getItem(absPath);
    }

    public boolean itemExists(String absPath) throws RepositoryException {
        return session.itemExists(absPath);
    }

    public void move(String srcAbsPath, String destAbsPath) throws ItemExistsException,
            PathNotFoundException, VersionException, ConstraintViolationException, LockException,
            RepositoryException {
        session.move(srcAbsPath, destAbsPath);
    }

    public void save() throws AccessDeniedException, ItemExistsException,
            ConstraintViolationException, InvalidItemStateException, VersionException,
            LockException, NoSuchNodeTypeException, RepositoryException {
        session.save();
    }

    public void refresh(boolean keepChanges) throws RepositoryException {
        session.refresh(keepChanges);
    }

    public boolean hasPendingChanges() throws RepositoryException {
        return session.hasPendingChanges();
    }

    public ValueFactory getValueFactory()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return session.getValueFactory();
    }

    public void checkPermission(String absPath, String actions)
            throws AccessControlException, RepositoryException {
        session.checkPermission(absPath, actions);
    }

    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws
            PathNotFoundException, ConstraintViolationException, VersionException, LockException,
            RepositoryException {
        return session.getImportContentHandler(parentAbsPath, uuidBehavior);
    }

    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws
            IOException, PathNotFoundException, ItemExistsException,
            ConstraintViolationException,
            VersionException, InvalidSerializedDataException, LockException, RepositoryException {
        session.importXML(parentAbsPath, in, uuidBehavior);
    }

    public void exportSystemView(String absPath, ContentHandler contentHandler,
            boolean skipBinary, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        session.exportSystemView(absPath, contentHandler, skipBinary, noRecurse);
    }

    public void exportSystemView(String absPath, OutputStream out, boolean skipBinary,
            boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException {
        session.exportSystemView(absPath, out, skipBinary, noRecurse);
    }

    public void exportDocumentView(String absPath, ContentHandler contentHandler,
            boolean skipBinary, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        JcrSessionWrapper.this
                .exportDocumentView(absPath, contentHandler, skipBinary, noRecurse);
    }

    public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary,
            boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException {
        session.exportDocumentView(absPath, out, skipBinary, noRecurse);
    }

    public void setNamespacePrefix(String prefix, String uri)
            throws NamespaceException, RepositoryException {
        session.setNamespacePrefix(prefix, uri);
    }

    public String[] getNamespacePrefixes() throws RepositoryException {
        return session.getNamespacePrefixes();
    }

    public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException {
        return session.getNamespaceURI(prefix);
    }

    public String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException {
        return session.getNamespacePrefix(uri);
    }

    public void logout() {
        session.logout();
    }

    public boolean isLive() {
        return session.isLive();
    }

    public void addLockToken(String lt) {
        session.addLockToken(lt);
    }

    public String[] getLockTokens() {
        return session.getLockTokens();
    }

    public void removeLockToken(String lt) {
        session.removeLockToken(lt);
    }
}
