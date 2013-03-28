package org.artifactory.sapi.search;

/**
 * Date: 8/6/11
 * Time: 12:00 PM
 *
 * @author Fred Simon
 */
public interface VfsQueryCriterion {
    VfsQueryCriterion addPropertySubPath(String... pathElements);

    VfsQueryCriterion nextBool(VfsBoolType boolType);
}
