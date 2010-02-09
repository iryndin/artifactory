package org.artifactory.common.property;

/**
 * @author Yoav Landman
 */
class SamePropertyMapper extends PropertyMapperBase {
    protected SamePropertyMapper(String origPropertyName) {
        super(origPropertyName);
    }

    public String map(String origValue) {
        return origValue;
    }

}
