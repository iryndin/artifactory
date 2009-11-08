package org.artifactory.common.property;

/**
 * @author Yoav Landman
 */
class NullPropertyMapper extends PropertyMapperBase {

    protected NullPropertyMapper() {
        super(null);
    }

    public String map(String origValue) {
        return null;
    }
}