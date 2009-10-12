package org.artifactory.descriptor.repo;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

/**
 * Enum of the checksum policies for repositories.
 *
 * @author Yossi Shaul
 */
@XmlEnum(String.class)
public enum ChecksumPolicyType {
    @XmlEnumValue("fail")FAIL,
    @XmlEnumValue("generate-if-absent")GEN_IF_ABSENT,
    @XmlEnumValue("ignore-and-generate")IGNORE_AND_GEN,
    @XmlEnumValue("pass-thru")PASS_THRU
}