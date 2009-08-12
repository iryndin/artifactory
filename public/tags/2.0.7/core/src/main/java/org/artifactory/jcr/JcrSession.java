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

import org.apache.commons.pool.impl.StackObjectPool;
import org.apache.jackrabbit.api.XASession;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.jcr.lock.SessionLockManager;
import org.artifactory.tx.SessionResource;
import org.artifactory.tx.SessionResourceManager;
import org.artifactory.tx.SessionResourceManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.jcr.Credentials;
import javax.jcr.Item;
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
    private SessionResourceManager sessionResourceManager = null;
    private StackObjectPool pool;

    public JcrSession(XASession session, StackObjectPool pool) {
        this.session = session;
        this.pool = pool;
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

    public void move(String srcAbsPath, String destAbsPath) {
        try {
            session.move(srcAbsPath, destAbsPath);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void save() {
        try {
            getSessionResourceManager().onSessionSave();
            session.save();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void refresh(boolean keepChanges) {
        try {
            session.refresh(keepChanges);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public boolean hasPendingChanges() {
        try {
            return getSessionResourceManager().hasPendingChanges() || session.hasPendingChanges();
        } catch (RepositoryException e) {
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

    public void checkPermission(String absPath, String actions)
            throws AccessControlException, RepositoryException {
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
        //Return ourseleves to the pool
        try {
            pool.returnObject(this);
        } catch (Exception e) {
            log.warn("Failed to return jcr session to pool.", e);
        }
    }

    public boolean isLive() {
        return session != null && session.isLive();
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

    public XAResource getXAResource() {
        return session.getXAResource();
    }

    @Override
    protected void finalize() throws Throwable {
        if (sessionResourceManager != null) {
            sessionResourceManager.afterCompletion(false);
        }
        session.logout();
        super.finalize();
    }

    public SessionResourceManager getSessionResourceManager() {
        if (sessionResourceManager == null) {
            sessionResourceManager = new SessionResourceManagerImpl();
            sessionResourceManager.getOrCreateResource(SessionLockManager.class);
        }
        return sessionResourceManager;
    }

    public <T extends SessionResource> T getOrCreateResource(Class<T> resourceClass) {
        return getSessionResourceManager().getOrCreateResource(resourceClass);
    }

}
