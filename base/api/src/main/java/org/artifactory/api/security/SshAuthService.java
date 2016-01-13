package org.artifactory.api.security;

import java.nio.file.Path;

/**
 * @author Noam Y. Tenne
 */
public interface SshAuthService {
    boolean hasPublicKey();

    boolean hasPrivateKey();

    Path getPublicKeyFile();

    Path getPrivateKeyFile();

    void savePublicKey(String publicKey) throws Exception;

    void savePrivateKey(String privateKey) throws Exception;

    void removePublicKey() throws Exception;

    void removePrivateKey() throws Exception;
}
