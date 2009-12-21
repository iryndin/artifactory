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

import org.apache.commons.pool.ObjectPool;
import org.apache.jackrabbit.api.XASession;
import org.apache.jackrabbit.core.SessionListener;
import org.apache.jackrabbit.core.XASessionImpl;
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
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.transaction.xa.XAResource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrSession implements XASession {
    private static final Logger log = LoggerFactory.getLogger(JcrSession.class);

    private final XASession session;
    private final SessionResourceManager sessionResourceManager;
    private ObjectPool pool;

    public JcrSession(XASession session, ObjectPool pool) {
        this.session = session;
        this.pool = pool;
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

    public Node getNodeByUUID(String uuid) {
        try {
            return session.getNodeByUUID(uuid);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public boolean nodeUUIDExists(String uuid) {
        try {
            session.getNodeByUUID(uuid);
            return true;
        } catch (ItemNotFoundException e) {
            return false;
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
            throw new RepositoryRuntimeException("Could not copy '" + srcAbsPath + "' to '" + tgtAbsPath + "'.", e);
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

    public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException {
        session.checkPermission(absPath, actions);
    }

    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) {
        try {
            return session.getImportContentHandler(parentAbsPath, uuidBehavior);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior)
            throws IOException, RepositoryException {
        session.importXML(parentAbsPath, in, uuidBehavior);
    }

    public void exportSystemView(String absPath, ContentHandler contentHandler,
            boolean skipBinary, boolean noRecurse) throws SAXException, RepositoryException {
        session.exportSystemView(absPath, contentHandler, skipBinary, noRecurse);
    }

    public void exportSystemView(String absPath, OutputStream out, boolean skipBinary,
            boolean noRecurse) throws IOException, RepositoryException {
        session.exportSystemView(absPath, out, skipBinary, noRecurse);
    }

    public void exportDocumentView(String absPath, ContentHandler contentHandler,
            boolean skipBinary, boolean noRecurse) throws SAXException {
        try {
            session.exportDocumentView(absPath, contentHandler, skipBinary, noRecurse);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary,
            boolean noRecurse) throws IOException, RepositoryException {
        session.exportDocumentView(absPath, out, skipBinary, noRecurse);
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
        //Return ourselves to the pool
        try {
            refresh(false);
            pool.invalidateObject(this);
            pool.returnObject(this);
        } catch (Exception e) {
            log.warn("Failed to return jcr session to pool.", e);
        }
    }

    public boolean isLive() {
        return session != null && session.isLive();
    }

    public void addLockToken(String lt) {
        try {
            session.addLockToken(lt);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public String[] getLockTokens() {
        return session.getLockTokens();
    }

    public void removeLockToken(String lt) {
        session.removeLockToken(lt);
    }

    public XAResource getXAResource() {
        return session.getXAResource();
    }

    public void addListener(SessionListener listener) {
        ((XASessionImpl) session).addListener(listener);
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
