package org.artifactory.descriptor.config;

import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.utils.AlreadyExistsException;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests the MutableCentralConfigDescriptorImpl class.
 *
 * @author Yossi Shaul
 */
@Test
public class CentralConfigDescriptorImplTest {
    private CentralConfigDescriptorImpl cc;

    @BeforeMethod
    public void initCentralConfig() {
        cc = new CentralConfigDescriptorImpl();

        LocalRepoDescriptor local1 = new LocalRepoDescriptor();
        local1.setKey("local1");
        cc.addLocalRepository(local1);
        LocalRepoDescriptor local2 = new LocalRepoDescriptor();
        local2.setKey("local2");
        cc.addLocalRepository(local2);

        RemoteRepoDescriptor remote1 = new HttpRepoDescriptor();
        remote1.setKey("remote1");
        cc.addRemoteRepository(remote1);

        VirtualRepoDescriptor virtual1 = new VirtualRepoDescriptor();
        virtual1.setKey("virtual1");
        cc.addVirtualRepository(virtual1);

        ProxyDescriptor proxy1 = new ProxyDescriptor();
        proxy1.setKey("proxy1");
        cc.addProxy(proxy1);

        ProxyDescriptor proxy2 = new ProxyDescriptor();
        proxy2.setKey("proxy2");
        cc.addProxy(proxy2);

        BackupDescriptor backup1 = new BackupDescriptor();
        backup1.setKey("backup1");
        cc.addBackup(backup1);

        BackupDescriptor backup2 = new BackupDescriptor();
        backup2.setKey("backup2");
        cc.addBackup(backup2);
    }

    public void defaultsTest() {
        CentralConfigDescriptorImpl cc = new CentralConfigDescriptorImpl();
        assertNotNull(cc.getLocalRepositoriesMap(), "Local repos map should not be null");
        assertNotNull(cc.getRemoteRepositoriesMap(), "Remote repos map should not be null");
        assertNotNull(cc.getVirtualRepositoriesMap(), "Virtual repos map should not be null");
        assertNotNull(cc.getBackups(), "Backups list should not be null");
        assertNotNull(cc.getProxies(), "Proxies list should not be null");
        assertNull(cc.getIndexer(), "Indexer should not be null");
        assertNotNull(cc.getSecurity(), "Security should not be null");
        assertNull(cc.getServerName(), "Server name should not be null");
        assertNotNull(cc.getDateFormat(), "Date format should not be null");
        assertTrue(cc.getFileUploadMaxSizeMb() > 50,
                "Default max file upload size should be bigger than 50mb");
        assertFalse(cc.isOfflineMode(), "Offline mode should be false by default");
    }

    public void uniqueKeyExistence() {
        assertFalse(cc.isKeyAvailable("local1"));
        assertFalse(cc.isRepositoryExists("backup2"));
        assertFalse(cc.isRepositoryExists("proxy2"));

        assertTrue(cc.isKeyAvailable("proxy22"));
        cc.setSecurity(new SecurityDescriptor());
        assertTrue(cc.isKeyAvailable("ldap1"));
    }

    public void repositoriesExistence() {
        assertEquals(cc.getLocalRepositoriesMap().size(), 2, "Local repos count mismatch");
        assertEquals(cc.getRemoteRepositoriesMap().size(), 1, "Remote repos count mismatch");
        assertEquals(cc.getVirtualRepositoriesMap().size(), 1,
                "Virtual repos count mismatch");
        assertTrue(cc.isRepositoryExists("local1"));
        assertTrue(cc.isRepositoryExists("local2"));
        assertTrue(cc.isRepositoryExists("remote1"));
        assertTrue(cc.isRepositoryExists("virtual1"));
    }

    @Test(expectedExceptions = AlreadyExistsException.class)
    public void duplicateLocalRepo() {
        assertTrue(cc.isRepositoryExists("local2"));
        LocalRepoDescriptor repo = new LocalRepoDescriptor();
        repo.setKey("local2");
        cc.addLocalRepository(repo);// should throw an exception
    }

    @Test(expectedExceptions = AlreadyExistsException.class)
    public void duplicateRemoteRepo() {
        assertTrue(cc.isRepositoryExists("remote1"));
        RemoteRepoDescriptor repo = new HttpRepoDescriptor();
        repo.setKey("remote1");
        cc.addRemoteRepository(repo);// should throw an exception
    }

    @Test(expectedExceptions = AlreadyExistsException.class)
    public void duplicateVirtualRepo() {
        assertTrue(cc.isRepositoryExists("virtual1"));
        VirtualRepoDescriptor repo = new VirtualRepoDescriptor();
        repo.setKey("virtual1");
        cc.addVirtualRepository(repo);// should throw an exception
    }

    public void proxyExistence() {
        assertEquals(cc.getProxies().size(), 2, "Proxies count mismatch");
        assertTrue(cc.isProxyExists("proxy1"));
        assertTrue(cc.isProxyExists("proxy2"));
    }

    public void removeProxy() {
        assertTrue(cc.isProxyExists("proxy2"));
        ProxyDescriptor removedProxy = cc.removeProxy("proxy2");
        assertEquals(removedProxy.getKey(), "proxy2");
        assertEquals(cc.getProxies().size(), 1, "Only one proxy expected");
        assertFalse(cc.isProxyExists("proxy2"));
    }

    public void removeReferencedProxy() {
        ProxyDescriptor proxy = new ProxyDescriptor();
        proxy.setKey("referencedProxy");
        cc.addProxy(proxy);

        HttpRepoDescriptor remoteRepo =
                (HttpRepoDescriptor) cc.getRemoteRepositoriesMap().get("remote1");
        remoteRepo.setProxy(proxy);

        assertNotNull(remoteRepo.getProxy(), "Just checking ...");

        cc.removeProxy("referencedProxy");

        assertNull(remoteRepo.getProxy(), "Proxy should have been removed from the remote repo");
    }

    public void backupExistence() {
        assertEquals(cc.getBackups().size(), 2, "Backups count mismatch");
        assertTrue(cc.isBackupExists("backup1"));
        assertTrue(cc.isBackupExists("backup2"));
    }

    public void removeBackup() {
        assertTrue(cc.isBackupExists("backup2"));
        BackupDescriptor removedBackup = cc.removeBackup("backup2");
        assertEquals(removedBackup.getKey(), "backup2");
        assertEquals(cc.getBackups().size(), 1, "Only one backup expected");
        assertFalse(cc.isBackupExists("backup2"));
    }
}
