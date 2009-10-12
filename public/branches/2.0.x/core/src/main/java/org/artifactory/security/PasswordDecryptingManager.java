package org.artifactory.security;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.security.PasswordSettings;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PrivateKey;

/**
 * This authentication manager will decrypted any encrypted passwords according to the password and encryption policy
 * and delegate the authentication to a standard authentication provider.
 *
 * @author Yossi Shaul
 */
public class PasswordDecryptingManager implements AuthenticationManager {
    private final static Logger log = LoggerFactory.getLogger(PasswordDecryptingManager.class);

    private AuthenticationManager delegate;

    @Autowired
    private CentralConfigService centralConfigService;

    @Autowired
    private UserGroupService userGroupService;

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        log.trace("Received authentication request for {}", authentication);
        String password = authentication.getCredentials().toString();
        if (needsDecryption(password)) {
            String username = authentication.getPrincipal().toString();
            log.trace("Decrypting user password for user '{}'", username);
            password = decryptPassword(password, username);
            authentication = new UsernamePasswordAuthenticationToken(username, password);
        }

        return delegate.authenticate(authentication);
    }

    private boolean needsDecryption(String password) {
        CentralConfigDescriptor centralConfigDescriptor = centralConfigService.getDescriptor();
        SecurityDescriptor securityDescriptor = centralConfigDescriptor.getSecurity();
        PasswordSettings passwordSettings = securityDescriptor.getPasswordSettings();
        boolean encryptionEnabled = passwordSettings.isEncryptionEnabled();
        if (!encryptionEnabled) {
            return false;
        }
        boolean isEncrypted = CryptoHelper.isEncrypted(password);
        log.trace("Detected {} password", isEncrypted ? "encrypted" : "cleartext");
        if (!isEncrypted) {
            if (passwordSettings.isEncryptionRequired()) {
                log.debug("Cleartext passwords not allowed. Sendind unauthorized response");
                throw new PasswordEncryptionException("Artifactory configured to accept only " +
                        "encrypted passwords but received a clear text password");
            } else {
                return false;
            }
        }
        log.trace("Password needs decryption");
        return true;
    }

    private String decryptPassword(String encryptedPassword, String username) {
        if (!CryptoHelper.isEncrypted(encryptedPassword)) {
            throw new IllegalArgumentException("Password not encrypted");
        }

        PrivateKey privateKey = getPrivateKey(username);
        SecretKey secretKey = CryptoHelper.generatePbeKey(CryptoHelper.toBase64(privateKey));
        try {
            return CryptoHelper.decryptSymmetric(encryptedPassword, secretKey);
        } catch (Exception e) {
            log.debug("Failed to decrypt user password: " + e.getMessage());
            throw new PasswordEncryptionException("Failed to decrypt password", e);
        }
    }

    private PrivateKey getPrivateKey(String username) {
        UserInfo userInfo = userGroupService.findUser(username);
        String privateKey = userInfo.getPrivateKey();
        String publicKey = userInfo.getPublicKey();
        if (privateKey == null || publicKey == null) {
            String message = "User with no key pair tries to authenticate with encrypted password";
            log.trace(message);
            throw new PasswordEncryptionException(message);
        }

        KeyPair keyPair = CryptoHelper.createKeyPair(privateKey, publicKey);
        return keyPair.getPrivate();
    }

    public void setDelegate(AuthenticationManager delegate) {
        this.delegate = delegate;
    }
}
