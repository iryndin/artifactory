package org.artifactory.test.ldap;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.server.protocol.shared.SocketAcceptor;
import org.apache.directory.shared.ldap.exception.LdapConfigurationException;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.mina.util.AvailablePortFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper around an embedded Apache Directory Server.
 *
 * @author Yossi Shaul
 */
public class LdapServer {
    private final static Logger log = LoggerFactory.getLogger(LdapServer.class);

    private boolean doDelete = true;

    protected DirectoryService ds;
    protected int port;
    protected LdapService ldapService;

    public static void main(String[] args) throws Exception {
        LdapServer server = new LdapServer(389);
        server.start();
        server.importLdif("/ldap/users.ldif");
    }

    public LdapServer() {
        this(AvailablePortFinder.getNextAvailable(1024));
    }

    public LdapServer(int port) {
        this.port = port;
    }

    /**
     * Empty test just to avoid a warning to be thrown when launching the test
     */
    public void start() throws Exception {
        log.debug("Starting embedded ldap server on port {}", port);
        ds = new DefaultDirectoryService();
        ds.setWorkingDirectory(new File("target/ldap-wd"));
        ds.setShutdownHookEnabled(false);

        JdbmPartition partition = new JdbmPartition();
        partition.setId("jfrog");
        partition.setSuffix("dc=jfrog,dc=org");
        partition.setCacheSize(100);
        ds.addPartition(partition);

        SocketAcceptor socketAcceptor = new SocketAcceptor(null);
        ldapService = new LdapService();
        ldapService.setSocketAcceptor(socketAcceptor);
        ldapService.setDirectoryService(ds);
        ldapService.setIpPort(port);

        // delete the working copy
        doDelete(ds.getWorkingDirectory());

        ds.startup();

        ldapService.start();
        log.info("Started embedded ldap server on port {}", port);
    }

    public void shutdown() {
        ldapService.stop();
        try {
            ds.shutdown();
        } catch (Exception e) {
            log.error("Error during shutdown", e);
        }
    }

    public void importLdif(String resourcePath) throws NamingException {
        log.info("Importing LDIF file from {}", resourcePath);
        importLdif(LdapServer.class.getResourceAsStream(resourcePath));
    }

    protected void importLdif(InputStream in) throws NamingException {
        if (in == null) {
            throw new IllegalArgumentException("Ldif input stream is null");
        }
        try {
            for (LdifEntry ldifEntry : new LdifReader(in)) {
                CoreSession rootDSE = ds.getAdminSession();
                rootDSE.add(new DefaultServerEntry(ds.getRegistries(), ldifEntry.getEntry()));
            }
        }
        catch (Exception e) {
            String msg = "failed while trying to parse system ldif file";
            NamingException ne = new LdapConfigurationException(msg);
            ne.setRootCause(e);
            throw ne;
        }
    }

    protected void doDelete(File wkdir) throws IOException {
        if (doDelete) {
            FileUtils.deleteDirectory(wkdir);
        }
    }

}