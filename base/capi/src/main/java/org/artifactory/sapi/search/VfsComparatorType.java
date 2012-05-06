package org.artifactory.sapi.search;

/**
 * Date: 8/5/11
 * Time: 6:38 PM
 *
 * @author Fred Simon
 */
public enum VfsComparatorType {
    ANY("in"), NONE("is null"), EQUAL("="),
    GREATER_THAN(">"), LOWER_THAN("<"),
    GREATER_THAN_EQUAL(">="), LOWER_THAN_EQUAL("<="),
    CONTAINS("like");

    public final String str;

    VfsComparatorType(String str) {
        this.str = str;
    }
}
