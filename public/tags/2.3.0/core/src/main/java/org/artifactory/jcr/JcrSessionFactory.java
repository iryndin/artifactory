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
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.springframework.extensions.jcr.jackrabbit.JackrabbitSessionFactory;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

/**
 * User: freds Date: Jul 21, 2008 Time: 7:10:02 PM
 */
public class JcrSessionFactory extends JackrabbitSessionFactory {

    @Override
    public void setRepository(final Repository repository) {
        super.setRepository(repository);
    }

    @Override
    public void destroy() throws Exception {
        super.destroy();
        RepositoryImpl repository = (RepositoryImpl) getRepository();
        repository.shutdown();
        DataStore store = repository.getDataStore();
        if (store != null) {
            store.close();
        }
    }

    /**
     * For internal framework use only!
     *
     * @return
     * @throws javax.jcr.RepositoryException
     */
    @Override
    public Session getSession() throws RepositoryException {
        JcrSession session = newSession();
        return addListeners(session);
    }

    /*
    * (non-Javadoc)
    * @see org.springmodules.jcr.JcrSessionFactory#registerNodeTypes()
    */

    @Override
    protected void registerNodeTypes() throws Exception {
        //Do nothing will do it later in init
    }

    @Override
    protected void registerNamespaces() throws Exception {
        //Do nothing will do it later in init
    }

    @Override
    protected void unregisterNodeTypes() throws Exception {
        //Do nothing
    }

    @Override
    protected void unregisterNamespaces() throws Exception {
        //Do nothing
    }

    private JcrSession newSession() {
        try {
            Session session = getRepository().login(new SimpleCredentials(SecurityConstants.ADMIN_ID, new char[]{}));
            JcrSession jcrSession = new JcrSession((XASession) session);
            return jcrSession;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create jcr session.", e);
        }
    }
}
