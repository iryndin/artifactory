package org.artifactory.security.ssh;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.descriptor.security.sshserver.SshServerSettings;
import org.artifactory.security.crypto.CryptoHelper;
import org.artifactory.security.props.auth.SshTokenManager;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.security.service.UserGroupStoreService;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Noam Y. Tenne
 * @author Chen Keinan
 */
@Service
@Reloadable(beanClass = InternalSshAuthService.class)
public class SshAuthServiceImpl implements InternalSshAuthService {

    private static final Logger log = LoggerFactory.getLogger(SshAuthServiceImpl.class);

    public final static String PUBLIC_KEY_FILE_NAME = "artifactory.ssh.public";
    public final static String PRIVATE_KEY_FILE_NAME = "artifactory.ssh.private";

    @Autowired
    CentralConfigService centralConfigService;

    @Autowired
    UserGroupStoreService userGroupStoreService;

    @Autowired
    SshTokenManager tokenManager;

    private SshServer server;

    @Override
    public void init() {
        configureAndStartServer();
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
        stopServer();
        configureAndStartServer();
    }

    @Override
    public void destroy() {
        stopServer();
    }

    @Override
    public boolean hasPublicKey() {
        return Files.exists(getPublicKeyFile());
    }

    @Override
    public boolean hasPrivateKey() {
        return Files.exists(getPrivateKeyFile());
    }

    @Override
    public Path getPublicKeyFile() {
        return sshFolder().resolve(PUBLIC_KEY_FILE_NAME);
    }

    @Override
    public Path getPrivateKeyFile() {
        return sshFolder().resolve(PRIVATE_KEY_FILE_NAME);
    }

    @Override
    public void savePublicKey(String publicKey) throws Exception {
        createSshFolder();
        Path path = getPublicKeyFile();
        if (Files.notExists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                throw new Exception("Failed creating public key file: " + path.toAbsolutePath());
            }
        }
        Files.write(path, publicKey.getBytes());
        log.info("Successfully updated SSH public key");
    }

    @Override
    public void savePrivateKey(String privateKey) throws Exception {
        createSshFolder();
        Path path = getPrivateKeyFile();
        if (Files.notExists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                throw new Exception("Failed creating private key file: " + path.toAbsolutePath());
            }
        }
        Files.write(path, privateKey.getBytes());
        log.info("Successfully updated SSH private key");
    }

    @Override
    public void removePublicKey() throws Exception {
        Path publicKeyFile = getPublicKeyFile();
        try {
            Files.deleteIfExists(publicKeyFile);
        } catch (IOException e) {
            throw new Exception("Failed to delete SSH public key file");
        }
        log.info("SSH public key was deleted");
    }

    @Override
    public void removePrivateKey() throws Exception {
        Path privateKeyFile = getPrivateKeyFile();
        try {
            Files.deleteIfExists(privateKeyFile);
        } catch (IOException e) {
            throw new Exception("Failed to delete SSH private key file");
        }
        log.info("SSH private key was deleted");
    }

    /**
     * create ssh folder and check folder permission
     * @throws IOException
     */
    private void createSshFolder() throws IOException {
        Path securityFolder = sshFolder();
        if (Files.notExists(securityFolder)) {
            Files.createDirectory(securityFolder);
            CryptoHelper.setPermissionsOnSecurityFolder(securityFolder);
        }
        CryptoHelper.checkPermissionsOnSecurityFolder(securityFolder);
    }

    /**
     * configure and start ssh server
     */
    private void configureAndStartServer() {
        if (centralConfigService != null && centralConfigService.getDescriptor() != null) {
            SecurityDescriptor securityDescriptor = centralConfigService.getDescriptor().getSecurity();
            if (securityDescriptor != null) {
                SshServerSettings sshServerSettings = securityDescriptor.getSshServerSettings();
                if (sshServerSettings != null) {
                    if (!sshServerSettings.isEnableSshServer()) {
                        return;
                    }
                    configServer();
                    try {
                        server.start();
                    } catch (IOException e) {
                        log.error("Failed to start SSH server", e);
                    }
                }
            }
        }
    }

    /**
     * stop ssh server
     */
    private void stopServer() {
        if (server == null) {
            return;
        }
        try {
            server.stop(true);
            while(!server.isClosed() && server.isClosing()) {
                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            log.error("Failed to stop SSH server", e);
        }
    }

    /**
     * configure ssh server
     */
    private void configServer() {
        server = SshServer.setUpDefaultServer();
        SshServerSettings sshServerSettings = centralConfigService.getDescriptor().getSecurity().getSshServerSettings();
        server.setPort(sshServerSettings.getSshServerPort());

        String[] keys = new String[]{getPrivateKeyFile().toString(), getPublicKeyFile().toString()};
        server.setKeyPairProvider(new FileKeyPairProvider(keys));
        server.setPublickeyAuthenticator(new PublicKeyAuthenticator(userGroupStoreService));
        ArtifactoryCommandFactory commandFactory = new ArtifactoryCommandFactory(centralConfigService,
                userGroupStoreService, tokenManager);
        server.setCommandFactory(commandFactory);
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    private Path sshFolder() {
        return ArtifactoryHome.get().getHaAwareEtcDir().toPath().resolve("ssh");
    }
}
