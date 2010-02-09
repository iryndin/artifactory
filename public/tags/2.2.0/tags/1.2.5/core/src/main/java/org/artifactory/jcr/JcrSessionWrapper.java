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

import org.apache.log4j.Logger;
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

    public Session impersonate(Credentials credentials) throws RepositoryException {
        return session.impersonate(credentials);
    }

    public Node getRootNode() throws RepositoryException {
        return session.getRootNode();
    }

    public Node getNodeByUUID(String uuid) throws RepositoryException {
        return session.getNodeByUUID(uuid);
    }

    public Item getItem(String absPath) throws RepositoryException {
        return session.getItem(absPath);
    }

    public boolean itemExists(String absPath) throws RepositoryException {
        return session.itemExists(absPath);
    }

    public void move(String srcAbsPath, String destAbsPath) throws RepositoryException {
        session.move(srcAbsPath, destAbsPath);
    }

    public void save() throws RepositoryException {
        if (!rollbackOnly && session.hasPendingChanges()) {
            session.save();
        }
    }

    public void refresh(boolean keepChanges) throws RepositoryException {
        session.refresh(keepChanges);
    }

    public boolean hasPendingChanges() throws RepositoryException {
        return session.hasPendingChanges();
    }

    public ValueFactory getValueFactory() throws RepositoryException {
        return session.getValueFactory();
    }

    public void checkPermission(String absPath, String actions)
            throws AccessControlException, RepositoryException {
        session.checkPermission(absPath, actions);
    }

    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior)
            throws RepositoryException {
        return session.getImportContentHandler(parentAbsPath, uuidBehavior);
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
            boolean skipBinary, boolean noRecurse) throws RepositoryException, SAXException {
        session.exportDocumentView(absPath, contentHandler, skipBinary, noRecurse);
    }

    public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary,
            boolean noRecurse) throws IOException, RepositoryException {
        session.exportDocumentView(absPath, out, skipBinary, noRecurse);
    }

    public void setNamespacePrefix(String prefix, String uri) throws RepositoryException {
        session.setNamespacePrefix(prefix, uri);
    }

    public String[] getNamespacePrefixes() throws RepositoryException {
        return session.getNamespacePrefixes();
    }

    public String getNamespaceURI(String prefix) throws RepositoryException {
        return session.getNamespaceURI(prefix);
    }

    public String getNamespacePrefix(String uri) throws RepositoryException {
        return session.getNamespacePrefix(uri);
    }

    public void logout() {
        session.logout();
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
}
