package org.artifactory.security.ssh;

import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.common.KeyExchange;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.forward.DefaultTcpipForwarderFactory;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.random.BouncyCastleRandom;
import org.apache.sshd.common.random.SingletonRandomFactory;
import org.apache.sshd.common.util.Buffer;
import org.artifactory.api.security.UserInfoBuilder;
import org.artifactory.config.InternalCentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptorImpl;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.descriptor.security.sshserver.SshServerSettings;
import org.artifactory.md.Properties;
import org.artifactory.model.xstream.fs.PropertiesImpl;
import org.artifactory.security.MutableUserInfo;
import org.artifactory.security.UserInfo;
import org.artifactory.storage.db.security.service.UserGroupServiceImpl;
import org.artifactory.storage.security.service.UserGroupStoreService;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.artifactory.util.Files;
import org.artifactory.util.ResourceUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.testng.Assert.*;

/**
 * @author Noam Y. Tenne
 */
public class SshAuthServiceImplTest extends ArtifactoryHomeBoundTest {

    SshAuthServiceImpl sshAuthService;

    @BeforeClass
    public void init() {
        sshAuthService = new SshAuthServiceImpl();
    }

    @BeforeMethod
    public void clearKeys() throws Exception {
        sshAuthService.removePrivateKey();
        sshAuthService.removePublicKey();
    }

    @AfterMethod
    public void shutdown() throws Exception {
        sshAuthService.destroy();
    }

    @Test(enabled = false)
    public void installPublicKey() throws Exception {
        assertFalse(sshAuthService.hasPublicKey());

        sshAuthService.savePublicKey("key");
        assertTrue(sshAuthService.hasPublicKey());

        Path path = sshAuthService.getPublicKeyFile();
        String keyContent = Files.readFileToString(path.toFile());
        assertEquals("key", keyContent);
        sshAuthService.removePublicKey();
        assertFalse(sshAuthService.hasPublicKey());
    }

    @Test(enabled = false)
    public void installPrivateKey() throws Exception {
        assertFalse(sshAuthService.hasPrivateKey());

        sshAuthService.savePrivateKey("privateKey");
        assertTrue(sshAuthService.hasPrivateKey());

        Path path = sshAuthService.getPrivateKeyFile();
        String keyContent = Files.readFileToString(path.toFile());
        assertEquals("privateKey", keyContent);
        sshAuthService.removePrivateKey();
        assertFalse(sshAuthService.hasPrivateKey());
    }

    @Test(enabled = false)
    public void ssh() throws Exception {
        prepareServiceForSsh();

        SshClient sshClient = prepareClient();
        ClientSession session = createSession(sshClient);

        String string = "git-lfs-authenticate" +
                " artifactory/jim upload 46eb912dc29d5000f4412f329c1a1a22400bead03a07491e77e33cdfe923fa76";

        ClientChannel channel = session.createExecChannel(string);
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        PipedOutputStream pipedIn = new TeePipedOutputStream(sent);
        channel.setIn(new PipedInputStream(pipedIn));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        channel.setOut(out);
        channel.setErr(err);
        channel.open();
        pipedIn.write(string.getBytes());
        pipedIn.flush();
        channel.waitFor(ClientChannel.CLOSED, 0);
        channel.close(false);
        sshClient.stop();

        assertEquals("{\"header\":{\"Authorization\":\"Bearer meintoken\"},\"href\":\"http://127.0.0.1:8080/artifactory/api/lfs/jim\"}", out.toString());
    }

    private SshClient prepareClient() {
        SshClient sshClient = SshClient.setUpDefaultClient();

        String[] keys = new String[]{sshAuthService.getPrivateKeyFile().toAbsolutePath().toString(),
                sshAuthService.getPublicKeyFile().toAbsolutePath().toString()};
        sshClient.setKeyPairProvider(new FileKeyPairProvider(keys));

        sshClient.start();
        return sshClient;
    }

    private ClientSession createSession(SshClient sshClient) throws IOException, InterruptedException {
        ConnectFuture connectFuture = sshClient.connect("git", "localhost", 1337);
        assertTrue(connectFuture.await(5, TimeUnit.SECONDS), "Couldn't connect to the server");
        ClientSession session = connectFuture.getSession();
        AuthFuture authFuture = session.auth();
        assertTrue(authFuture.await(500, TimeUnit.SECONDS), "Couldn't authenticate with the server");
        return session;
    }

    private void prepareServiceForSsh() throws Exception {
        installUserGroupStore();
        installCentralConfig();
        installKeys();
        sshAuthService.init();
    }

    private void installUserGroupStore() {
        MutableUserInfo jim = new UserInfoBuilder("jim").build();
        UserGroupServiceImpl userGroupServiceImpl = createMock(UserGroupServiceImpl.class);
        expect(userGroupServiceImpl.findUserByProperty("sshPublicKey", Files.readFileToString(
                ResourceUtils.getResourceAsFile("/org/artifactory/security/ssh/id_rsa.pub")))).andReturn(
                jim).anyTimes();
        PropertiesImpl properties = new PropertiesImpl();
        properties.put("basictoken", "meintoken");
        expect(userGroupServiceImpl.findPropertiesForUser("jim")).andReturn(properties).anyTimes();
        replay(userGroupServiceImpl);

        sshAuthService.userGroupStoreService = userGroupServiceImpl;
    }

    private void installCentralConfig() {
        SshServerSettings sshServerSettings = new SshServerSettings();
        sshServerSettings.setEnableSshServer(true);
        sshServerSettings.setSshServerPort(1337);

        SecurityDescriptor securityDescriptor = new SecurityDescriptor();
        securityDescriptor.setSshServerSettings(sshServerSettings);

        CentralConfigDescriptorImpl configDescriptor = new CentralConfigDescriptorImpl();
        configDescriptor.setSecurity(securityDescriptor);

        InternalCentralConfigService centralConfigServiceMock = createMock(InternalCentralConfigService.class);
        expect(centralConfigServiceMock.getDescriptor()).andReturn(configDescriptor).anyTimes();
        replay(centralConfigServiceMock);

        sshAuthService.centralConfigService = centralConfigServiceMock;
    }

    private void installKeys() throws Exception {
        sshAuthService.savePrivateKey(
                Files.readFileToString(ResourceUtils.getResourceAsFile("/org/artifactory/security/ssh/id_rsa")));
        sshAuthService.savePublicKey(
                Files.readFileToString(ResourceUtils.getResourceAsFile("/org/artifactory/security/ssh/id_rsa.pub")));
    }

    public class TeePipedOutputStream extends PipedOutputStream {

        private OutputStream tee;

        public TeePipedOutputStream(OutputStream tee) {
            this.tee = tee;
        }

        @Override
        public void write(int b) throws IOException {
            super.write(b);
            tee.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            super.write(b, off, len);
            tee.write(b, off, len);
        }
    }
}