package org.artifactory.descriptor.security;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the PasswordSettings.
 *
 * @author Yossi Shaul
 */
@Test
public class PasswordSettingsTest {

    public void defaultConstructor() {
        PasswordSettings passwordSettings = new PasswordSettings();
        EncryptionPolicy policy = passwordSettings.getEncryptionPolicy();
        Assert.assertEquals(policy, EncryptionPolicy.SUPPORTED);
        Assert.assertTrue(passwordSettings.isEncryptionEnabled());
    }

    public void encryptionEnabled() {
        PasswordSettings passwordSettings = new PasswordSettings();
        Assert.assertTrue(passwordSettings.isEncryptionEnabled());
        passwordSettings.setEncryptionPolicy(EncryptionPolicy.REQUIRED);
        Assert.assertTrue(passwordSettings.isEncryptionEnabled());
        passwordSettings.setEncryptionPolicy(EncryptionPolicy.UNSUPPORTED);
        Assert.assertFalse(passwordSettings.isEncryptionEnabled());
    }
}
