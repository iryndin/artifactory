package org.artifactory.jcr.search;

import org.artifactory.sapi.search.VfsBoolType;
import org.artifactory.sapi.search.VfsQueryCriterion;

/**
 * Date: 8/6/11
 * Time: 12:51 PM
 *
 * @author Fred Simon
 */
public abstract class BaseVfsQueryCriterion implements VfsQueryCriterion {
    VfsBoolType nextBool;

    public BaseVfsQueryCriterion() {
        this.nextBool = VfsBoolType.AND;
    }

    @Override
    public VfsQueryCriterion nextBool(VfsBoolType boolType) {
        nextBool = boolType;
        return this;
    }

    protected abstract VfsBoolType fill(StringBuilder query);

    public abstract boolean isValid();
}
