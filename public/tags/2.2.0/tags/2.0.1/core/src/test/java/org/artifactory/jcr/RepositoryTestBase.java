package org.artifactory.jcr;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.XASessionImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.io.File;
import java.io.InputStream;

public abstract class RepositoryTestBase {

    private JackrabbitRepository repository;

    @BeforeClass
    protected void setUp() throws Exception {
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

    @AfterClass
    protected void tearDown() throws Exception {
        repository.shutdown();
    }

    public JackrabbitRepository getRepository() {
        return repository;
    }

    public static void beginTx(Xid xid1, XASessionImpl session) throws XAException {
        session.start(xid1, XAResource.TMNOFLAGS);
    }

    public static void commitTx(Xid xid1, XASessionImpl session) throws XAException {
        session.end(xid1, XAResource.TMSUCCESS);
        session.prepare(xid1);
        session.commit(xid1, false);
    }
}