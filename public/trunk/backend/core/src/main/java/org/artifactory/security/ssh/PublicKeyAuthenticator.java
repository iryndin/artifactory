package org.artifactory.security.ssh;

import org.apache.commons.codec.binary.Base64;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.artifactory.security.UserInfo;
import org.artifactory.storage.security.service.UserGroupStoreService;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

/**
 * @author Noam Y. Tenne
 */
public class PublicKeyAuthenticator implements PublickeyAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(PublicKeyAuthenticator.class);

    private UserGroupStoreService userGroupStoreService;

    public PublicKeyAuthenticator(UserGroupStoreService userGroupStoreService) {
        this.userGroupStoreService = userGroupStoreService;
    }

    @Override
    public boolean authenticate(String username, PublicKey key, ServerSession session) {
        String sshPublicKey;
        try {
            sshPublicKey = decodedPublicKey(key);
        } catch (IOException e) {
            log.error("Failed to read public key as blob", e);
            return false;
        }
        UserInfo userInfo = userGroupStoreService.findUserByProperty("sshPublicKey", sshPublicKey);
        if (userInfo != null) {
            session.setAttribute(new UsernameAttributeKey(), userInfo.getUsername());
            return true;
        }
        return false;
    }

    /**
     * decode public key and return it as string
     * @param key - public key
     * @return
     * @throws IOException
     */
    private String decodedPublicKey(PublicKey key) throws IOException {
        String algorithm = key.getAlgorithm();
        if ("RSA".equals(algorithm)) {
            byte[] rawKey = rsaKeyToBytes((RSAPublicKey) key);
            return "ssh-rsa " + Base64.encodeBase64String(rawKey);
        }
        return "";
    }

    /**
     * convert rsa key to bytes
     * @param publicKey - rsa public key
     * @return rsa key as byte array
     * @throws IOException
     */
    private static byte[] rsaKeyToBytes(RSAPublicKey publicKey) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeLengthFirst("ssh-rsa".getBytes(), out);
        writeLengthFirst(publicKey.getPublicExponent().toByteArray(), out);
        writeLengthFirst(publicKey.getModulus().toByteArray(), out);
        return out.toByteArray();
    }

    // http://www.ietf.org/rfc/rfc4253.txt

    /**
     * write public key data to byte array output stream
     * @param array - byte array
     * @param out - byte array output stream
     * @throws IOException
     */
    private static void writeLengthFirst(byte[] array, ByteArrayOutputStream out) throws IOException {
        out.write((array.length >>> 24) & 0xFF);
        out.write((array.length >>> 16) & 0xFF);
        out.write((array.length >>> 8) & 0xFF);
        out.write((array.length) & 0xFF);
        if (array.length == 1 && array[0] == (byte) 0x00) {
            out.write(new byte[0]);
        } else {
            out.write(array);
        }
    }
}
