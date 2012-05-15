package org.artifactory.sapi.search;

import org.artifactory.sapi.data.VfsNodeType;

/**
 * Date: 8/6/11
 * Time: 12:02 PM
 *
 * @author Fred Simon
 */
public interface VfsQueryPathCriterion extends VfsQueryCriterion {
    String ALL_PATH_VALUE = "**";

    void setNodeTypeFilter(VfsNodeType nodeType);

    boolean isContains();
}
