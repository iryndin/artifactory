/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.JackrabbitSecurityManager;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.authentication.AuthContext;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Null jackrabbit security manager thar allows access to all resources to any user. Artifactory is doing its own
 * security management so a "real" jackrabbit security manager is not needed.
 *
 * @author Yossi Shaul
 */
public class NullJackrabbitSecurityManager implements JackrabbitSecurityManager {
    private static final Logger log = LoggerFactory.getLogger(NullJackrabbitSecurityManager.class);
    private Object unsupportedOperationProxy;
    private AccessManager nullAccessManager;

    @Override
    public void init(Repository repository, Session systemSession) {
        log.debug("Using null security manager for jackrabbit");

        unsupportedOperationProxy = Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[]{UserManager.class, PrincipalManager.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        throw new UnsupportedOperationException("Method not supported: " + method.getName());
                    }
                });

        nullAccessManager = new AccessManager() {
            @Override
            public void init(AMContext context) {
            }

            @Override
            public void init(AMContext context, AccessControlProvider acProvider, WorkspaceAccessManager wspAccessMgr) {
            }

            @Override
            public void close() {
            }

            @Override
            public void checkPermission(ItemId id, int permissions) {
            }

            @Override
            public void checkPermission(Path absPath, int permissions) throws RepositoryException {
            }

            @Override
            public boolean isGranted(ItemId id, int permissions) {
                return true;
            }

            @Override
            public boolean isGranted(Path absPath, int permissions) throws RepositoryException {
                return true;
            }

            @Override
            public boolean isGranted(Path parentPath, Name childName, int permissions) throws RepositoryException {
                return true;
            }

            public boolean canRead(Path itemPath) throws RepositoryException {
                return true;
            }

            @Override
            public boolean canRead(Path itemPath, ItemId itemId) throws RepositoryException {
                return true;
            }

            @Override
            public boolean canAccess(String workspaceName) throws RepositoryException {
                return true;
            }
        };
    }

    @Override
    public void dispose(String workspaceName) {
    }

    @Override
    public void close() {
    }

    @Override
    public AuthContext getAuthContext(Credentials creds, final Subject subject, String workspaceName) {
        return new AuthContext() {
            @Override
            public void login() throws LoginException {
            }

            @Override
            public Subject getSubject() {
                return subject;
            }

            @Override
            public void logout() throws LoginException {
            }
        };
    }

    @Override
    public AccessManager getAccessManager(Session session, AMContext amContext) {
        return nullAccessManager;
    }

    @Override
    public PrincipalManager getPrincipalManager(Session session) {
        return (PrincipalManager) unsupportedOperationProxy;
    }

    @Override
    public UserManager getUserManager(Session session) {
        return (UserManager) unsupportedOperationProxy;
    }

    @Override
    public String getUserID(Subject subject, String workspaceName) throws RepositoryException {
        return SecurityConstants.ADMIN_ID;
    }
}
