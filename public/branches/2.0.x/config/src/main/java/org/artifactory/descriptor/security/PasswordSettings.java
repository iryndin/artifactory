package org.artifactory.descriptor.security;

import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * The password policy related settings.
 *
 * @author Yossi Shaul
 */
@XmlType(name = "PasswordSettingsType")
public class PasswordSettings implements Descriptor {

    @XmlElement(defaultValue = "supported", required = false)
    private EncryptionPolicy encryptionPolicy = EncryptionPolicy.SUPPORTED;

    public EncryptionPolicy getEncryptionPolicy() {
        return encryptionPolicy;
    }

    public void setEncryptionPolicy(EncryptionPolicy encryptionPolicy) {
        this.encryptionPolicy = encryptionPolicy;
    }

    /**
     * @return True if encryption is required.
     */
    public boolean isEncryptionRequired() {
        return EncryptionPolicy.REQUIRED.equals(encryptionPolicy);
    }

    /**
     * @return True if encryption is supported\required. False if not.
     */
    public boolean isEncryptionEnabled() {
        return (EncryptionPolicy.SUPPORTED.equals(encryptionPolicy) || EncryptionPolicy.REQUIRED.equals(encryptionPolicy));
    }
}