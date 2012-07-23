package org.artifactory.jcr.search;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.util.Text;
import org.artifactory.sapi.data.VfsNodeType;
import org.artifactory.sapi.search.InvalidQueryRuntimeException;
import org.artifactory.sapi.search.VfsBoolType;
import org.artifactory.sapi.search.VfsComparatorType;
import org.artifactory.sapi.search.VfsQueryPathCriterion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.artifactory.storage.StorageConstants.PROP_ARTIFACTORY_METADATA_NAME;
import static org.artifactory.storage.StorageConstants.PROP_ARTIFACTORY_NAME;

/**
 * Date: 8/6/11
 * Time: 12:01 PM
 *
 * @author Fred Simon
 */
class VfsQueryPathCriterionJcrImpl extends VfsQueryCriterionJcrImpl implements VfsQueryPathCriterion {

    private VfsNodeType matchNodeType;

    VfsQueryPathCriterionJcrImpl(@Nonnull VfsComparatorType comparator, @Nullable String value,
            @Nullable VfsNodeType nodeType) {
        super(nodeType == VfsNodeType.METADATA ? PROP_ARTIFACTORY_METADATA_NAME : PROP_ARTIFACTORY_NAME, comparator);
        switch (comparator) {
            case ANY:
                this.value = ALL_PATH_VALUE;
                break;
            case EQUAL:
                this.value = ISO9075.encodePath(value);
                break;
            case CONTAINS:
                this.value = "'" + value + "'";
                break;
            default:
                throw new InvalidQueryRuntimeException("Path filter comparator type " + comparator + " not supported");
        }
    }

    @Override
    public void setNodeTypeFilter(VfsNodeType nodeType) {
        if (nodeType == VfsNodeType.METADATA && !PROP_ARTIFACTORY_METADATA_NAME.equals(propertyName)) {
            throw new InvalidQueryRuntimeException("Cannot change a path to metadata after creation!");
        }
        this.matchNodeType = nodeType;
    }

    @Override
    public boolean isContains() {
        return comparator == VfsComparatorType.CONTAINS;
    }

    @Override
    protected VfsBoolType fill(StringBuilder query) {
        // A path criteria always starts with a slash
        JcrQueryHelper.addSlashIfNeeded(query);
        if (comparator == VfsComparatorType.ANY) {
            // We need the double slash //
            JcrQueryHelper.addDoubleSlashesIfNeeded(query);
            if (matchNodeType != null) {
                JcrQueryHelper.fillWithNodeTypeFilter(query, matchNodeType);
            }
        } else if (comparator == VfsComparatorType.EQUAL) {
            if (matchNodeType != null) {
                JcrQueryHelper.fillWithNodeTypeFilter(query, matchNodeType);
                this.value = "'" + Text.escapeIllegalXpathSearchChars(this.value) + "'";
                fillStandard(query);
            } else {
                // In GAVC search was doing a property equals instead of simple path writing
                // TODO: Check if ok like that
                query.append(value);
            }
        } else {
            if (matchNodeType != null) {
                JcrQueryHelper.fillWithNodeTypeFilter(query, matchNodeType);
            } else {
                query.append(".");
            }
            fillStandard(query);
        }

        return nextBool;
    }

    private void fillStandard(StringBuilder query) {
        query.append(" [");
        super.fill(query);
        query.append("]");
    }
}
