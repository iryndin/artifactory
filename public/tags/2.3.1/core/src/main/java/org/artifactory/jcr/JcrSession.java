/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

import org.apache.jackrabbit.api.XASession;
import org.apache.lucene.util.CloseableThreadLocal;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.jcr.lock.InternalLockManager;
import org.artifactory.jcr.lock.SessionLockManager;
import org.artifactory.jcr.lock.aop.LockingAdvice;
import org.artifactory.log.LoggerFactory;
import org.artifactory.tx.SessionResource;
import org.artifactory.tx.SessionResourceManager;
import org.artifactory.tx.SessionResourceManagerImpl;
import org.slf4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;
import javax.transaction.xa.XAResource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;

/**
 * @author Yoav Landman
 */
public class JcrSession implements XASession {
    private static final Logger log = LoggerFactory.getLogger(JcrSession.class);

    private final XASession session;
    private final SessionResourceManager sessionResourceManager;

    public JcrSession(XASession session) {
        this.session = session;
        sessionResourceManager = new SessionResourceManagerImpl();
        sessionResourceManager.getOrCreateResource(SessionLockManager.class);
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

    public Session impersonate(Credentials credentials) {
        try {
            return session.impersonate(credentials);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public Node getRootNode() {
        try {
            return session.getRootNode();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Deprecated
    public Node getNodeByUUID(String uuid) {
        try {
            return session.getNodeByIdentifier(uuid);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public boolean nodeExists(String absPath) {
        try {
            return session.nodeExists(absPath);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public Item getItem(String absPath) {
        try {
            return session.getItem(absPath);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public boolean itemExists(String absPath) {
        try {
            return session.itemExists(absPath);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void move(String srcAbsPath, String tgtAbsPath) {
        log.trace("Moving {} to {}", srcAbsPath, tgtAbsPath);
        try {
            session.move(srcAbsPath, tgtAbsPath);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Could not move '" + srcAbsPath + "' to '" + tgtAbsPath + "'.", e);
        }
    }

    public void copy(String srcAbsPath, String tgtAbsPath) {
        log.trace("Copying {} to {}", srcAbsPath, tgtAbsPath);
        Workspace workspace = session.getWorkspace();
        try {
            workspace.copy(srcAbsPath, tgtAbsPath);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Could not copy '" + srcAbsPath + "' to '" + tgtAbsPath + "': " + e.getMessage(), e);
        }
    }

    public void save() {
        try {
            getSessionResourceManager().onSessionSave();
            log.debug("saving session");
            session.save();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void refresh(boolean keepChanges) {
        try {
            //Don't release any locks - they will be released at the end of lock advice
            session.refresh(keepChanges);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public boolean hasPendingChanges() {
        try {
            InternalLockManager lockManager = LockingAdvice.getLockManager();
            return (lockManager != null && lockManager.hasPendingResources()) || session.hasPendingChanges();
        } catch (Exception e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public ValueFactory getValueFactory() {
        try {
            return session.getValueFactory();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void checkPermission(String absPath, String actions) throws AccessControlException {
        try {
            session.checkPermission(absPath, actions);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) {
        try {
            return session.getImportContentHandler(parentAbsPath, uuidBehavior);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException {
        try {
            session.importXML(parentAbsPath, in, uuidBehavior);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void exportSystemView(String absPath, ContentHandler contentHandler,
            boolean skipBinary, boolean noRecurse) throws SAXException {
        try {
            session.exportSystemView(absPath, contentHandler, skipBinary, noRecurse);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse)
            throws IOException {
        try {
            session.exportSystemView(absPath, out, skipBinary, noRecurse);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void exportDocumentView(String absPath, ContentHandler contentHandler,
            boolean skipBinary, boolean noRecurse) throws SAXException {
        try {
            session.exportDocumentView(absPath, contentHandler, skipBinary, noRecurse);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse)
            throws IOException {
        try {
            session.exportDocumentView(absPath, out, skipBinary, noRecurse);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void setNamespacePrefix(String prefix, String uri) {
        try {
            session.setNamespacePrefix(prefix, uri);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public String[] getNamespacePrefixes() {
        try {
            return session.getNamespacePrefixes();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public String getNamespaceURI(String prefix) {
        try {
            return session.getNamespaceURI(prefix);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public String getNamespacePrefix(String uri) {
        try {
            return session.getNamespacePrefix(uri);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void logout() {
        try {
            // Verify no pending changes
            validateSessionCleaness();
            session.logout();
        } finally {
            // If no lock manager clean Lucene resources
            if (LockingAdvice.getLockManager() == null) {
                CloseableThreadLocal.closeAllThreadLocal();
            }
        }
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private void validateSessionCleaness() {
        SessionResourceManager resourceManager = getSessionResourceManager();
        if (resourceManager != null && resourceManager.hasPendingResources()) {
            IllegalStateException e = new IllegalStateException("Tried to return a session with unprocessed pending " +
                    "resources: " + resourceManager.pendingResources());
            log.error("Session is not clean: ", e);
        }
    }

    public boolean isLive() {
        return session != null && session.isLive();
    }

    @SuppressWarnings({"deprecation"})
    @Deprecated
    public void addLockToken(String lt) {
        session.addLockToken(lt);
    }

    @SuppressWarnings({"deprecation"})
    @Deprecated
    public String[] getLockTokens() {
        return session.getLockTokens();
    }

    @SuppressWarnings({"deprecation"})
    @Deprecated
    public void removeLockToken(String lt) {
        session.removeLockToken(lt);
    }

    public XAResource getXAResource() {
        return session.getXAResource();
    }

    public Node getNodeByIdentifier(String id) {
        try {
            return session.getNodeByIdentifier(id);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public Node getNode(String absPath) {
        try {
            return session.getNode(absPath);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public Property getProperty(String absPath) {
        try {
            return session.getProperty(absPath);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public boolean propertyExists(String absPath) {
        try {
            return session.propertyExists(absPath);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void removeItem(String absPath) {
        try {
            session.removeItem(absPath);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public boolean hasPermission(String absPath, String actions) {
        try {
            return session.hasPermission(absPath, actions);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public boolean hasCapability(String methodName, Object target, Object[] arguments) {
        try {
            return session.hasCapability(methodName, target, arguments);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public AccessControlManager getAccessControlManager() {
        try {
            return session.getAccessControlManager();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public RetentionManager getRetentionManager() {
        try {
            return session.getRetentionManager();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /*@Override
    protected void finalize() throws Throwable {
        if (sessionResourceManager != null) {
            sessionResourceManager.afterCompletion(false);
        }
        session.logout();
        super.finalize();
    }*/

    public SessionResourceManager getSessionResourceManager() {
        return sessionResourceManager;
    }

    public <T extends SessionResource> T getOrCreateResource(Class<T> resourceClass) {
        return getSessionResourceManager().getOrCreateResource(resourceClass);
    }
}
