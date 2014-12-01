package org.artifactory.aql.model;


/**
 * @author Gidi Shabat
 */
public enum AqlOperatorEnum {
    and("$and"), or("$or"), freezeJoin("$join");
    public String signature;

    AqlOperatorEnum(String signature) {
        this.signature = signature;
    }

    public static AqlOperatorEnum value(String operator) {
        operator = operator.toLowerCase();
        for (AqlOperatorEnum operatorEnum : values()) {
            if (operatorEnum.signature.equals(operator)) {
                return operatorEnum;
            }
        }
        return null;
    }

}
