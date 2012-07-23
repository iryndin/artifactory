package org.artifactory.jcr.search;

import com.google.common.collect.Lists;
import org.artifactory.sapi.search.InvalidQueryRuntimeException;
import org.artifactory.sapi.search.VfsBoolType;
import org.artifactory.sapi.search.VfsQueryCriterion;

import java.util.List;

/**
 * Date: 8/6/11
 * Time: 12:56 PM
 *
 * @author Fred Simon
 */
class BaseGroupCriterion extends BaseVfsQueryCriterion {
    private final boolean isDefault;
    private final VfsQueryJcrImpl parent;
    private final List<BaseVfsQueryCriterion> criteria = Lists.newArrayList();

    BaseGroupCriterion(VfsQueryJcrImpl parent) {
        this.parent = parent;
        this.isDefault = true;
    }

    private BaseGroupCriterion(BaseGroupCriterion owner) {
        this.parent = owner.parent;
        this.isDefault = false;
    }

    BaseVfsQueryCriterion addCriterion(BaseVfsQueryCriterion criterion) {
        if (criterion != null && criterion.isValid()) {
            criteria.add(criterion);
            return criterion;
        }
        return null;
    }

    @Override
    public VfsQueryCriterion addPropertySubPath(String... pathElements) {
        throw new InvalidQueryRuntimeException("Cannot have properties sub path on group");
    }

    @SuppressWarnings({"SuspiciousMethodCalls"})
    public VfsQueryCriterion group(VfsQueryCriterion... vfsQueryCriteria) {
        if (vfsQueryCriteria.length < 2) {
            throw new InvalidQueryRuntimeException("Cannot create and empty or only one group");
        }
        BaseGroupCriterion groupCriterion = new BaseGroupCriterion(this);
        for (VfsQueryCriterion queryCriterion : vfsQueryCriteria) {
            if (!(queryCriterion instanceof BaseVfsQueryCriterion)
                    || !criteria.remove(queryCriterion)) {
                throw new InvalidQueryRuntimeException("Cannot create a group of criterion not created in this query");
            }
            BaseVfsQueryCriterion criterionJcr = (BaseVfsQueryCriterion) queryCriterion;
            groupCriterion.addCriterion(criterionJcr);
        }
        criteria.add(groupCriterion);
        return groupCriterion;
    }

    @Override
    public boolean isValid() {
        // Always valid
        return true;
    }

    @Override
    protected VfsBoolType fill(StringBuilder query) {
        if (!criteria.isEmpty()) {
            if (isDefault) {
                query.append(" [");
            } else {
                query.append(" (");
            }
            VfsBoolType nextBool = null;
            for (BaseVfsQueryCriterion criterion : this.criteria) {
                if (nextBool != null) {
                    query.append(" ").append(nextBool.str).append(" ");
                }
                nextBool = criterion.fill(query);
            }
            if (isDefault) {
                query.append("] ");
            } else {
                query.append(") ");
            }
        }
        return nextBool;
    }
}
