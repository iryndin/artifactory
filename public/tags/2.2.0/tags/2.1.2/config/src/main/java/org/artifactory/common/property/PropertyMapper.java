package org.artifactory.common.property;

/**
 * @author Yoav Landman
 */
public interface PropertyMapper {
    /**
     * Map a property value of an olde property to a new one
     *
     * @param origValue
     * @return
     */
    String map(String origValue);

    String getNewPropertyName();
}
