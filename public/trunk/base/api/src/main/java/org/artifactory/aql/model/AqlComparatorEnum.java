package org.artifactory.aql.model;

/**
 * @author Gidi Shabat
 */
public enum AqlComparatorEnum {
    notEquals("$not_equals"), equals("$equals"), greaterEquals("$greater_equals"), greater("$greater"),
    matches("$matches"),notMatches("$not_matches"), lessEquals("$less_equals"), less("$less");
    public String signature;

    AqlComparatorEnum(String signature) {
        this.signature = signature;
    }

    public static AqlComparatorEnum value(String comparator) {
        comparator = comparator.toLowerCase();
        for (AqlComparatorEnum comparatorEnum : values()) {
            if (comparatorEnum.signature.equals(comparator)) {
                return comparatorEnum;
            }
        }
        return null;
    }


}
