package org.artifactory.aql.model;

/**
 * @author Gidi Shabat
 */
public enum AqlDomainValueTypeEnum {
    fields("$fields"), name("$names"), values("$values");
    public String signature;

    AqlDomainValueTypeEnum(String signature) {
        this.signature = signature;
    }

    public static AqlDomainValueTypeEnum value(String domainValueType) {
        domainValueType = domainValueType.toLowerCase();
        for (AqlDomainValueTypeEnum valueType : values()) {
            if (valueType.signature.equals(domainValueType)) {
                return valueType;
            }
        }
        return null;
    }
}
