/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.XASessionImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.io.File;
import java.io.InputStream;

public abstract class RepositoryTestBase {

    JackrabbitRepository repository;

    @BeforeMethod
    protected void initRepository() throws Exception {
        InputStream is = getClass().getResourceAsStream("test_repo.xml");
        assert is != null;
        // Load the configuration and create the repository
        String home = "./tmp";
        File homeDir = new File(home);
        if (homeDir.exists()) {
            FileUtils.forceDelete(homeDir);
        }
        FileUtils.forceMkdir(homeDir);
        RepositoryConfig rc = RepositoryConfig.create(is, home);
        repository = new TransientRepository(rc);
    }

    @AfterMethod
    protected void shutdownRepository() throws Exception {
        repository.shutdown();
    }

    public static void beginTx(Xid xid1, XASessionImpl session) throws XAException {
        session.start(xid1, XAResource.TMNOFLAGS);
    }

    public static void commitTx(Xid xid1, XASessionImpl session) throws XAException {
        session.end(xid1, XAResource.TMSUCCESS);
        session.prepare(xid1);
        session.commit(xid1, false);
    }

    protected XASessionImpl login() throws RepositoryException {
        //Transient repo grants everyone
        XASessionImpl session =
                (XASessionImpl) repository.login(new SimpleCredentials("user", "password".toCharArray()));
        return session;
    }
}