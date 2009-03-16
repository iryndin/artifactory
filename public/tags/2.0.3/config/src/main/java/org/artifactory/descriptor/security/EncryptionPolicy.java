package org.artifactory.descriptor.security;

import javax.xml.bind.annotation.XmlEnumValue;

/**
 * Password encryption policy types.
 *
 * @author Yossi Shaul
 */
public enum EncryptionPolicy {
    @XmlEnumValue("supported")SUPPORTED,
    @XmlEnumValue("required")REQUIRED,
    @XmlEnumValue("unsupported")UNSUPPORTED
}
